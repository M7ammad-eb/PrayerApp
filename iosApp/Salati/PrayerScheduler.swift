import Foundation
import UserNotifications
import AlarmKit
import ActivityKit
import SwiftUI
import Combine
import CryptoKit
import UIKit
import AppIntents

struct PrayerAlarmMetadata: AlarmMetadata { var prayer: String }

struct PrayerAlarmStopIntent:LiveActivityIntent {
    static var title:LocalizedStringResource="Stop prayer alarm"
    static var openAppWhenRun=false
    static var isDiscoverable=false
    @Parameter(title:"Prayer event") var scheduleID:String
    init() {scheduleID=""}
    init(scheduleID:String) {self.scheduleID=scheduleID}
    func perform() async throws -> some IntentResult {
        // The system handles Stop. This callback is an actual supported
        // interaction execution path, not a promise to wake on every alarm.
        await Self.replenish(scheduleID)
        return .result()
    }
    @MainActor private static func replenish(_ id:String) async {
        PrayerScheduler.shared.recordAlarmInteraction(id)
        let model=AppModel();await model.refresh(context:.liveActivityIntent)
    }
}

enum SchedulingContext {
    case application,liveActivityIntent
    func canPrepareLiveActivities(applicationIsActive:Bool)->Bool {
        applicationIsActive || self == .liveActivityIntent
    }
}

// Domain decisions and stable identities come from the shared Kotlin plan.
struct ScheduledAlert: Equatable {
    let id: String
    let prayer: String
    let title: String
    let date: Date
    let prayerDate: Date
    let policy: PrayerPolicy
    let reminder: Bool
}
struct ScheduledLivePrayer: Equatable {
    let id: String
    let title: String
    let date: Date
    let start: Date
}
@MainActor struct LiveActivitySubmissionBatch {
    struct Result {var accepted:[ScheduledLivePrayer]=[];var failures:[String]=[];var blocked=false}
    func run(_ desired:[ScheduledLivePrayer],skip:(ScheduledLivePrayer)->Bool = {_ in false},retain:(ScheduledLivePrayer) async -> Bool,
             submit:(ScheduledLivePrayer) async throws -> Void) async -> Result {
        var result=Result()
        for item in desired {
            if skip(item) {continue}
            // Matching scheduled activities still count after the first capacity
            // error; only new API submissions are blocked for this batch.
            if await retain(item) {result.accepted.append(item);continue}
            guard !result.blocked,!skip(item) else {continue}
            do {try await submit(item);result.accepted.append(item)}
            catch {
                result.failures.append(error.localizedDescription)
                if let authorization=error as? ActivityAuthorizationError {
                    switch authorization {
                    case .globalMaximumExceeded,.targetMaximumExceeded,.denied,.unentitled,.unsupported,.unsupportedTarget,.visibility:
                        result.blocked=true
                    default:break
                    }
                }
            }
        }
        return result
    }
}
enum NativeAlertRoute: String, Codable { case notification, alarm }
struct NativePendingAlert: Equatable {
    var id: String
    var route: NativeAlertRoute
    var date: Date?
    var authorized = true
    var isAlerting = false
}
struct NativeAlertRequest: Codable, Equatable {
    var id: String
    var route: NativeAlertRoute
    var date: Date
    var prayerDate: Date
    var prayer: String
    var title: String
    var body: String
    var sound: String
    var recording: String?
    var language: String
    var reminder: Bool
    var isTest = false
    var nativeID: String {
        // UN add replaces the same identifier atomically. AlarmKit generations
        // use a stable content hash so replacement can precede cancellation.
        if route == .notification {return id}
        let encoder=JSONEncoder();encoder.outputFormatting=[.sortedKeys]
        let bytes=Array(SHA256.hash(data:(try? encoder.encode(self)) ?? Data()).prefix(16))
        return UUID(uuid:(bytes[0],bytes[1],bytes[2],bytes[3],bytes[4],bytes[5],bytes[6],bytes[7],bytes[8],bytes[9],bytes[10],bytes[11],bytes[12],bytes[13],bytes[14],bytes[15])).uuidString
    }
    static func make(_ item: ScheduledAlert,settings: Settings) -> Self {
        let recording=Recording.all.contains {$0.id == item.policy.sound} ? item.policy.sound : nil
        let alarm=item.policy.delivery == .alarm && !item.reminder
        let sound=alarm ? (recording.map {$0+".mp3"} ?? item.policy.sound) : (item.reminder ? (settings.reminderSound ? "default" : "silent") : (item.policy.sound == "silent" ? "silent" : "default"))
        return Self(id:item.id,route:alarm ? .alarm : .notification,date:item.date,prayerDate:item.prayerDate,prayer:item.prayer,title:item.title,
                    body:settings.language == "ar" ? (item.reminder ? "اقترب موعد الصلاة" : "حان وقت الصلاة") : (item.reminder ? "Prayer time is approaching" : "It is time for prayer"),
                    sound:sound,recording:item.reminder ? nil : recording,language:settings.language,reminder:item.reminder)
    }
}

@MainActor protocol AlertDeliveryBackend {
    func pending() async throws -> [NativePendingAlert]
    func submit(_ request: NativeAlertRequest) async throws
    func cancel(_ pending: NativePendingAlert) async throws
}
private enum SchedulingFailure: LocalizedError {
    case alarmPermission, notificationPermission, silentAlarm, foregroundActivity
    var errorDescription: String? {
        switch self {
        case .alarmPermission:return "Allow system alarms to use the selected alarm policy."
        case .notificationPermission:return "Allow notifications for the selected alerts and reminders."
        case .silentAlarm:return "System alarms require an audible sound. Choose Notification for silent delivery."
        case .foregroundActivity:return "Open Salati to prepare automatic Live Activities."
        }
    }
}
@MainActor final class SystemAlertDelivery: AlertDeliveryBackend {
    private let center=UNUserNotificationCenter.current()
    func pending() async throws -> [NativePendingAlert] {
        let permission=await center.notificationSettings()
        let allowed=permission.authorizationStatus == .authorized || permission.authorizationStatus == .provisional
        var result=await center.pendingNotificationRequests().map {
            NativePendingAlert(id:$0.identifier,route:.notification,date:($0.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate() ?? ($0.trigger as? UNTimeIntervalNotificationTrigger)?.nextTriggerDate(),authorized:allowed)
        }
        if AlarmManager.shared.authorizationState == .authorized {
            result += try AlarmManager.shared.alarms.map { alarm in
                let date:Date?
                if case let .fixed(value)? = alarm.schedule {date=value} else {date=nil}
                return NativePendingAlert(id:alarm.id.uuidString,route:.alarm,date:date,isAlerting:alarm.state == .alerting)
            }
        }
        return result
    }
    func submit(_ request: NativeAlertRequest) async throws {
        if request.route == .alarm {
            guard AlarmManager.shared.authorizationState == .authorized else {throw SchedulingFailure.alarmPermission}
            guard request.sound != "silent" else {throw SchedulingFailure.silentAlarm}
            let stop=AlarmButton(text:request.language == "ar" ? "إيقاف" : "Stop",textColor:.white,systemImageName:"stop.fill")
            let alert=AlarmPresentation.Alert(title:LocalizedStringResource(stringLiteral:request.title),stopButton:stop)
            let attributes=AlarmAttributes(presentation:AlarmPresentation(alert:alert),metadata:PrayerAlarmMetadata(prayer:request.prayer),tintColor:Color("AccentColor"))
            let sound:AlertConfiguration.AlertSound=request.sound == "default" ? .default : .named(request.sound)
            // Absolute fixed instant: no countdown, post-alert interval or snooze.
            _=try await AlarmManager.shared.schedule(id:UUID(uuidString:request.nativeID)!,configuration:AlarmManager.AlarmConfiguration.alarm(schedule:.fixed(request.date),attributes:attributes,stopIntent:PrayerAlarmStopIntent(scheduleID:request.id),sound:sound))
        } else {
            let permission=await center.notificationSettings()
            guard permission.authorizationStatus == .authorized || permission.authorizationStatus == .provisional else {throw SchedulingFailure.notificationPermission}
            let content=UNMutableNotificationContent()
            content.title=request.title;content.body=request.body
            content.sound=request.sound == "silent" ? nil : .default
            content.userInfo=["scheduleID":request.id,"plannedEpoch":request.date.timeIntervalSince1970,"prayerEpoch":request.prayerDate.timeIntervalSince1970,"role":request.reminder ? "reminder" : "prayer"]
            if let recording=request.recording {content.categoryIdentifier="PRAYER";content.userInfo["recording"]=recording}
            var calendar=Calendar(identifier:.gregorian);calendar.timeZone = .gmt
            var components=calendar.dateComponents([.year,.month,.day,.hour,.minute,.second],from:request.date);components.timeZone = .gmt
            try await center.add(UNNotificationRequest(identifier:request.nativeID,content:content,trigger:UNCalendarNotificationTrigger(dateMatching:components,repeats:false)))
        }
    }
    func cancel(_ pending: NativePendingAlert) async throws {
        if pending.route == .alarm,let id=UUID(uuidString:pending.id) {try AlarmManager.shared.cancel(id:id)}
        else if pending.route == .notification {center.removePendingNotificationRequests(withIdentifiers:[pending.id])}
    }
}

struct ScheduleDiagnostic: Codable {
    let at: Date
    let operation: String
    let id: String
    let requested: Date?
    let prayer: Date?
    let result: String
}

@MainActor final class SerialSchedulingQueue {
    private var tail:Task<String,Never>?
    private var revision=0
    func latest(_ work:@escaping @MainActor () async -> String) async -> String {
        revision+=1;let current=revision
        return await enqueue { [weak self] in
            guard self?.revision == current else {return ""}
            return await work()
        }
    }
    func enqueue(_ work:@escaping @MainActor () async -> String) async -> String {
        let previous=tail
        let task=Task { @MainActor in _=await previous?.value;return await work() }
        tail=task
        return await task.value
    }
}

// A stateful backend boundary permits failure/migration tests without device alerts.
@MainActor final class AlertReconciler {
    private let backend: any AlertDeliveryBackend
    private let defaults: UserDefaults
    private(set) var records:[String:NativeAlertRequest]
    var diagnostic:((String,NativeAlertRequest?,String)->Void)?
    init(backend:any AlertDeliveryBackend,defaults:UserDefaults) {
        self.backend=backend;self.defaults=defaults
        records=(defaults.data(forKey:"schedule.ledger.v2").flatMap {try? JSONDecoder().decode([String:NativeAlertRequest].self,from:$0)}) ?? [:]
    }
    private func persist() {if let data=try? JSONEncoder().encode(records) {defaults.set(data,forKey:"schedule.ledger.v2")}}
    struct Result {var prepared=0;var horizon:Date?;var failures:[String]=[];var retainedOnFailure=0;var accepted=Set<String>()}
    func reconcile(_ desired:[NativeAlertRequest],now:Date,production:Bool=true) async -> Result {
        var result=Result()
        let pending:[NativePendingAlert]
        do {pending=try await backend.pending()} catch {result.failures=[error.localizedDescription];return result}
        let pendingIDs=Set(pending.map(\.id))
        let authorizedIDs=Set(pending.filter(\.authorized).map(\.id))
        let alertingIDs=Set(pending.filter(\.isAlerting).map(\.id))
        let requested=Dictionary(desired.map {($0.id,$0)},uniquingKeysWith:{first,_ in first})
        var horizons:[NativeAlertRoute:Date]=[:]
        var firstFailure:Date?
        var failedRoutes=Set<NativeAlertRoute>()
        var successful=Set<String>()
        for request in desired where request.date>now {
            let previous=records[request.id]
            if previous == request && authorizedIDs.contains(request.nativeID) {
                successful.insert(request.id);result.prepared+=1;horizons[request.route]=max(horizons[request.route] ?? request.date,request.date);continue
            }
            do {
                try await backend.submit(request)
                records[request.id]=request;persist()
                successful.insert(request.id);result.prepared+=1;horizons[request.route]=max(horizons[request.route] ?? request.date,request.date)
                diagnostic?("submitted",request,"accepted by scheduling API; delivery not observed")
                if let previous,previous.nativeID != request.nativeID {
                    try await backend.cancel(.init(id:previous.nativeID,route:previous.route,date:previous.date))
                    diagnostic?("replaced",previous,"old generation cancelled after replacement accepted")
                }
            } catch {
                result.failures.append(error.localizedDescription)
                failedRoutes.insert(request.route)
                firstFailure=min(firstFailure ?? request.date,request.date)
                if let previous,authorizedIDs.contains(previous.nativeID),previous.date>now {result.retainedOnFailure+=1}
                diagnostic?("failed",request,error.localizedDescription)
            }
        }
        for (key,record) in records where record.isTest == !production && requested[key] == nil {
            // Replenishing at the prayer boundary must not cut off the alarm
            // that has just begun. The person's Stop action owns this lifecycle.
            guard !alertingIDs.contains(record.nativeID) else {continue}
            guard record.date<=now || !failedRoutes.contains(record.route) else {continue}
            do {
                if pendingIDs.contains(record.nativeID) {try await backend.cancel(.init(id:record.nativeID,route:record.route,date:record.date))}
                records.removeValue(forKey:key);persist()
                diagnostic?("removed",record,"no longer in desired plan")
            } catch {result.failures.append(error.localizedDescription)}
        }
        if production {
            let knownIDs=Set(records.values.map(\.nativeID))
            let successfulRequests=desired.filter {successful.contains($0.id)}
            for old in pending where !knownIDs.contains(old.id) && !old.isAlerting {
                var safe=false
                if old.route == .notification,let separator=old.id.lastIndex(of:"."),UUID(uuidString:String(old.id[old.id.index(after:separator)...])) != nil {
                    let legacyID=String(old.id[..<separator])
                    safe=successful.contains(legacyID) || (!failedRoutes.contains(.notification) && legacyID.hasPrefix("salati."))
                } else if old.route == .alarm,let date=old.date {
                    // Only THIS app's alarms are returned. Phase1 used fixed
                    // dates/random UUIDs and had no persisted scheduling ledger.
                    safe = !failedRoutes.contains(.alarm) || successfulRequests.contains {$0.route == .alarm && abs($0.date.timeIntervalSince(date))<1}
                }
                if safe {do {try await backend.cancel(old);diagnostic?("legacy cleanup",nil,old.route.rawValue+" previous Salati generation removed")} catch {result.failures.append(error.localizedDescription)}}
            }
        }
        result.horizon=horizons.values.min()
        if let firstFailure {result.horizon=min(result.horizon ?? firstFailure,firstFailure)}
        result.accepted=successful
        return result
    }
    func cancelTests() async throws {
        for (key,record) in records where record.isTest {
            try await backend.cancel(.init(id:record.nativeID,route:record.route,date:record.date))
            records.removeValue(forKey:key);persist()
            diagnostic?("test cancelled",record,"test-only identity")
        }
    }
}

@MainActor final class PrayerScheduler: ObservableObject {
    static let shared=PrayerScheduler()
    @Published private(set) var testStatus:String?
    @Published private(set) var diagnosticsText=""
    private let notifications=UNUserNotificationCenter.current()
    private let defaults=UserDefaults.standard
    private let reconciler:AlertReconciler
    private let operations=SerialSchedulingQueue()
    private var expiryTask:Task<Void,Never>?
    private let liveLifecycle=PrayerLiveActivityLifecycle(backend:SystemPrayerActivityBackend(),defaults:.standard)
    private var liveStateObservers:[String:Task<Void,Never>]=[:]
    private var activityObserver:Task<Void,Never>?
    private var alarmObserver:Task<Void,Never>?
    private var alarmAuthorizationObserver:Task<Void,Never>?
    private var journal:[ScheduleDiagnostic]=[]
    private init() {
        reconciler=AlertReconciler(backend:SystemAlertDelivery(),defaults:.standard)
        journal=(defaults.data(forKey:"schedule.diagnostics.v2").flatMap {try? JSONDecoder().decode([ScheduleDiagnostic].self,from:$0)}) ?? []
        reconciler.diagnostic={ [weak self] operation,request,result in self?.record(operation,request:request,result:result) }
        updateDiagnostics()
        activityObserver=Task { @MainActor [weak self] in
            for await enabled in ActivityAuthorizationInfo().activityEnablementUpdates {
                guard !Task.isCancelled else {return}
                self?.record("Live Activity authorization",result:enabled ? "enabled" : "disabled")
                if !enabled {await self?.cleanupLiveActivities(enabled:false)}
                NotificationCenter.default.post(name:.salatiScheduleNeedsRefresh,object:nil)
            }
        }
        alarmAuthorizationObserver=Task { @MainActor [weak self] in
            for await state in AlarmManager.shared.authorizationUpdates {
                guard !Task.isCancelled else {return}
                self?.record("AlarmKit authorization",result:String(describing:state))
                NotificationCenter.default.post(name:.salatiScheduleNeedsRefresh,object:nil)
            }
        }
        alarmObserver=Task { @MainActor [weak self] in
            var previous:[UUID:String]=[:]
            for await alarms in AlarmManager.shared.alarmUpdates {
                guard !Task.isCancelled else {return}
                var current:[UUID:String]=[:]
                for alarm in alarms {
                    let state=String(describing:alarm.state);current[alarm.id]=state
                    if state != previous[alarm.id] && alarm.state == .alerting {
                        self?.record("AlarmKit alerting observed",result:alarm.id.uuidString+"; OS state observed while executing; not proof of audible start")
                        NotificationCenter.default.post(name:.salatiScheduleNeedsRefresh,object:nil)
                    }
                }
                previous=current
            }
        }
    }
    func authorizeNotifications() async -> String {
        do {return try await notifications.requestAuthorization(options:[.alert,.sound,.badge]) ? "Notifications allowed" : "Notifications were not allowed. You can change this in iOS Settings."}
        catch {return error.localizedDescription}
    }
    func authorizeAlarms() async -> String {
        do {return try await AlarmManager.shared.requestAuthorization() == .authorized ? "System alarms allowed" : "System alarms were not allowed. You can change this in iOS Settings."}
        catch {return error.localizedDescription}
    }
    func reconcile(alerts:[ScheduledAlert],live:[ScheduledLivePrayer],city:String,settings:Settings,context:SchedulingContext = .application) async -> String {
        await operations.latest { [weak self] in
            guard let self else {return ""}
            return await self.perform(alerts:alerts,live:live,city:city,settings:settings,context:context)
        }
    }
    private func perform(alerts:[ScheduledAlert],live:[ScheduledLivePrayer],city:String,settings:Settings,context:SchedulingContext) async -> String {
        let now=Date()
        let liveResult=await prepareLiveActivities(live,city:city,settings:settings,now:now,context:context)
        let filtered=alerts.filter {item in
            !item.reminder || !liveResult.accepted.contains {$0.id == item.id.replacingOccurrences(of:".reminder",with:".live") && abs($0.start.timeIntervalSince(item.date))<1}
        }
        var requests=filtered.filter {$0.date>now}.map {NativeAlertRequest.make($0,settings:settings)}
        for failed in live where !liveResult.accepted.contains(where:{$0.id == failed.id}) && failed.start>now && !liveLifecycle.isDismissed(failed.id,now:now) {
            if !requests.contains(where:{$0.reminder && abs($0.date.timeIntervalSince(failed.start))<1 && $0.prayerDate == failed.date}) {
                requests.append(.init(id:failed.id+".fallback",route:.notification,date:failed.start,prayerDate:failed.date,prayer:"",title:failed.title,body:settings.language == "ar" ? "اقترب موعد الصلاة" : "Prayer time is approaching",sound:settings.reminderSound ? "default" : "silent",recording:nil,language:settings.language,reminder:true))
            }
        }
        requests.sort {$0.date<$1.date}
        let pending=await notifications.pendingNotificationRequests()
        let externalCount=pending.filter {!$0.identifier.hasPrefix("salati.")}.count
        let testCount=pending.filter {$0.identifier.hasPrefix("salati.test.")}.count
        // Notification capacity is independent of AlarmKit and ActivityKit. Keep
        // room for test, expiry warning and other requests; handle actual errors.
        let notificationBudget=max(0,60-externalCount-testCount)
        var used=0
        requests=requests.filter {request in if request.route == .alarm {return true};used+=1;return used<=notificationBudget}
        let result=await reconciler.reconcile(requests,now:now)
        await prepareExpiryWarning(horizon:result.horizon,settings:settings)
        var failures=result.failures+liveResult.failures
        for item in liveResult.outdated {
            let fallbackAccepted=requests.contains {$0.reminder && $0.prayerDate == item.date && abs($0.date.timeIntervalSince(item.start))<1 && result.accepted.contains($0.id)}
            let previous=Activity<PrayerActivityAttributes>.activities.filter {$0.content.state.eventID == item.id}
            if fallbackAccepted {
                for activity in previous {await liveLifecycle.finish(id:activity.id)}
                record("outdated Live Activity removed",result:item.id+"; corrected reminder accepted first")
            } else if !previous.isEmpty {
                failures.append(settings.language == "ar" ? "بقي نشاط مباشر سابق قد يعرض موعدًا أو موقعًا قديمًا؛ تعذّر استبداله وتجهيز تذكيره البديل. افتح التشخيص المحلي." : "A previous Live Activity may show an outdated time or location: replacement and its fallback were unavailable. Review local diagnostics.")
                record("outdated Live Activity retained",result:item.id+"; replacement/fallback unavailable; previous presentation may be outdated")
            }
        }
        if used>notificationBudget {failures.append(settings.language == "ar" ? "حد التنبيهات: افتح صلاتي لتجديد الجدول." : "Notification capacity reached; open Salati to replenish the rolling schedule.")}
        if result.retainedOnFailure>0 {failures.append(settings.language == "ar" ? "تم الاحتفاظ ببعض المواعيد السابقة بعد تعذّر استبدالها؛ راجع التشخيص المحلي." : "Some previous times were retained after replacement failed; review local diagnostics.")}
        if requests.isEmpty && live.isEmpty && failures.isEmpty {return ""}
        let horizon=result.horizon.map {Self.formattedHorizon($0,settings:settings)} ?? "—"
        let error=Array(Set(failures)).sorted().joined(separator:" ")
        record("reconcile",result:"\(result.prepared) alerts; \(liveResult.accepted.count) Live Activities; horizon \(result.horizon?.ISO8601Format() ?? "none"); \(error)")
        return settings.language == "ar" ? "تم تجهيز \(result.prepared) تنبيهًا حتى \(horizon)، و\(liveResult.accepted.count) أنشطة مباشرة. \(error)" : "\(result.prepared) alerts prepared through \(horizon); \(liveResult.accepted.count) Live Activities. \(error)"
    }
    private struct LiveResult {var accepted:[ScheduledLivePrayer]=[];var failures:[String]=[];var outdated:[ScheduledLivePrayer]=[]}
    func dismissLiveActivity(eventID:String) async {
        _=await operations.enqueue { [self] in
            await liveLifecycle.dismiss(eventID:eventID)
            record("Live Activity closed",result:eventID)
            armLiveActivityExpiry()
            return ""
        }
    }
    func cleanupLiveActivities(enabled:Bool=true) async {
        await liveLifecycle.cleanup(enabled:enabled)
        observeLiveActivityStates()
        armLiveActivityExpiry()
    }
    private func observeLiveActivityStates() {
        let activities=Activity<PrayerActivityAttributes>.activities
        let ids=Set(activities.map(\.id))
        for id in liveStateObservers.keys where !ids.contains(id) {liveStateObservers.removeValue(forKey:id)?.cancel()}
        for activity in activities where liveStateObservers[activity.id] == nil {
            liveStateObservers[activity.id]=Task { @MainActor [weak self] in
                for await state in activity.activityStateUpdates {
                    guard !Task.isCancelled else {return}
                    if state == .dismissed {
                        self?.liveLifecycle.recordSystemDismissal(id:activity.id,eventID:activity.content.state.eventID,date:activity.content.state.date)
                    }
                    if state == .stale || state == .ended || state == .dismissed {
                        await self?.liveLifecycle.cleanup()
                        self?.armLiveActivityExpiry()
                    }
                }
            }
        }
    }
    private func armLiveActivityExpiry() {
        expiryTask?.cancel()
        guard let next=liveLifecycle.nextExpiry else {expiryTask=nil;return}
        // No suspended-process execution promise: system stale UI and Close remain
        // usable offline, and launch/foreground cleanup does not depend on a refresh.
        expiryTask=Task { @MainActor [weak self] in
            do {try await Task.sleep(for:.seconds(max(0.05,next.timeIntervalSinceNow)));try Task.checkCancellation()} catch {return}
            await self?.liveLifecycle.cleanup()
            self?.armLiveActivityExpiry()
        }
    }
    private func prepareLiveActivities(_ desired:[ScheduledLivePrayer],city:String,settings:Settings,now:Date,context:SchedulingContext) async -> LiveResult {
        var result=LiveResult()
        let enabled=settings.liveActivitiesEnabled && ActivityAuthorizationInfo().areActivitiesEnabled
        await liveLifecycle.cleanup(now:now,enabled:enabled)
        guard enabled else {return result}
        let future=desired.filter {$0.date>now && !liveLifecycle.isDismissed($0.id,now:now)}
        let batch=await LiveActivitySubmissionBatch().run(future,skip:{self.liveLifecycle.isDismissed($0.id,now:Date())},retain:{item in
            let old=Activity<PrayerActivityAttributes>.activities.filter {$0.content.state.eventID == item.id}
            if let existing=old.first(where:{$0.content.state.date == item.date && $0.content.state.start == item.start && $0.content.state.title == item.title && $0.content.state.timeText == Self.formattedTime(item.date,settings:settings) && $0.content.state.language == settings.language && $0.attributes.city == city && $0.activityState != .dismissed && $0.activityState != .ended}) {
                for duplicate in old where duplicate.id != existing.id {await self.liveLifecycle.finish(id:duplicate.id)}
                self.record("Live Activity retained",result:existing.id+"; event "+item.id);return true
            }
            return false
        },submit:{item in
            guard context.canPrepareLiveActivities(applicationIsActive:UIApplication.shared.applicationState == .active) else {throw ActivityAuthorizationError.visibility}
            let old=Activity<PrayerActivityAttributes>.activities.filter {$0.content.state.eventID == item.id}
            let state=PrayerActivityAttributes.ContentState(title:item.title,date:item.date,eventID:item.id,language:settings.language,start:item.start,timeText:Self.formattedTime(item.date,settings:settings),endMessage:settings.language == "ar" ? "حان وقت الصلاة" : "It is time for prayer")
            let content=ActivityContent(state:state,staleDate:item.date)
            do {
                if item.start>now {
                    _=try Activity<PrayerActivityAttributes>.request(attributes:PrayerActivityAttributes(city:city),content:content,pushType:nil,style:.standard,alertConfiguration:AlertConfiguration(title:LocalizedStringResource(stringLiteral:item.title),body:LocalizedStringResource(stringLiteral:settings.language == "ar" ? "اقترب موعد الصلاة" : "Prayer time is approaching"),sound:settings.reminderSound ? .default : .named("salati_silent.caf")),start:item.start)
                } else {_=try Activity<PrayerActivityAttributes>.request(attributes:PrayerActivityAttributes(city:city),content:content,pushType:nil)}
                for previous in old {await self.liveLifecycle.finish(id:previous.id)}
                self.record("Live Activity submitted",result:item.id+"; start "+item.start.ISO8601Format()+"; prayer "+item.date.ISO8601Format()+"; staleDate is not dismissal")
            } catch {self.record("Live Activity failed",result:item.id+"; "+error.localizedDescription);throw error}
        })
        result.accepted=batch.accepted;result.failures=batch.failures
        result.outdated=future.filter {item in !result.accepted.contains(where:{$0.id == item.id}) && Activity<PrayerActivityAttributes>.activities.contains(where:{$0.content.state.eventID == item.id})}
        if batch.blocked {record("Live Activity batch stopped",result:"capacity/authorization/visibility prevents new requests; existing matching activities retained; remaining events use notification fallback")}
        if result.accepted.count == future.count {
            for old in Activity<PrayerActivityAttributes>.activities where !desired.contains(where:{$0.id == old.content.state.eventID}) && !old.content.state.eventID.hasPrefix("salati.showcase.") {await liveLifecycle.finish(id:old.id)}
        }
        await liveLifecycle.cleanup()
        observeLiveActivityStates();armLiveActivityExpiry()
        return result
    }
    private func prepareExpiryWarning(horizon:Date?,settings:Settings) async {
        let id="salati.status.expiry"
        guard let horizon,horizon.timeIntervalSinceNow>24*3600 else {notifications.removePendingNotificationRequests(withIdentifiers:[id]);return}
        let content=UNMutableNotificationContent();content.title="Salati · صلاتي"
        content.body=settings.language == "ar" ? "افتح صلاتي لتجديد مواعيد الصلاة؛ الجدول المحضّر يقترب من نهايته." : "Open Salati to replenish prayer alerts; the prepared schedule is nearing its end."
        content.sound=nil
        let trigger=UNTimeIntervalNotificationTrigger(timeInterval:max(1,horizon.timeIntervalSinceNow-24*3600),repeats:false)
        do {try await notifications.add(UNNotificationRequest(identifier:id,content:content,trigger:trigger))} catch {record("expiry warning failed",result:error.localizedDescription)}
    }
    func scheduleTest(delivery:Delivery,sound:String,settings:Settings) async {
        _=await operations.enqueue { [weak self] in
            guard let self,delivery != .off else {return ""}
            let date=Date().addingTimeInterval(60)
            let item=ScheduledAlert(id:"salati.test."+delivery.rawValue,prayer:"TEST",title:settings.language == "ar" ? "اختبار تنبيه صلاتي" : "Salati test alert",date:date,prayerDate:date,policy:PrayerPolicy(delivery:delivery,sound:sound,reminder:0),reminder:false)
            var request=NativeAlertRequest.make(item,settings:settings);request.isTest=true
            let result=await self.reconciler.reconcile([request],now:Date(),production:false)
            self.testStatus=result.failures.isEmpty ? (settings.language == "ar" ? "تمت جدولة اختبار بعد نحو دقيقة. اقفل الهاتف الآن." : "Test scheduled about one minute ahead. Lock the phone now.") : result.failures.joined(separator:" ")
            return self.testStatus ?? ""
        }
    }
    func cancelTests() async {
        _=await operations.enqueue { [weak self] in
            guard let self else {return ""}
            do {try await self.reconciler.cancelTests();self.testStatus="Test alerts cancelled"} catch {self.testStatus=error.localizedDescription}
            return self.testStatus ?? ""
        }
    }
    func observeNotification(_ notification:UNNotification,action:String) {
        let info=notification.request.content.userInfo
        let actual=action == "foreground presentation" ? "notification delegate invoked in foreground" : "user response observed; not a delivery timestamp"
        record(action,result:"\(info["scheduleID"] ?? notification.request.identifier); submitted UTC \(info["plannedEpoch"] ?? "unknown"); prayer UTC \(info["prayerEpoch"] ?? "unknown"); \(actual)")
    }
    func recordAudio(_ message:String) {record("audio",result:message)}
    func recordAlarmInteraction(_ id:String) {record("AlarmKit Stop interaction",result:id+"; actual intent execution; not an alarm delivery timestamp")}
    private func record(_ operation:String,request:NativeAlertRequest?=nil,result:String) {
        journal.append(.init(at:Date(),operation:operation,id:request?.id ?? "",requested:request?.date,prayer:request?.prayerDate,result:String(result.prefix(500))))
        journal=Array(journal.suffix(160))
        if let data=try? JSONEncoder().encode(journal) {defaults.set(data,forKey:"schedule.diagnostics.v2")}
        updateDiagnostics()
    }
    private func updateDiagnostics() {
        diagnosticsText=journal.reversed().map {entry in
            "\(entry.at.ISO8601Format()) · \(entry.operation) · \(entry.id)\nrequested UTC: \(entry.requested?.ISO8601Format() ?? "—"); prayer UTC: \(entry.prayer?.ISO8601Format() ?? "—")\n\(entry.result)"
        }.joined(separator:"\n\n")
    }
    func clearDiagnostics() {journal=[];defaults.removeObject(forKey:"schedule.diagnostics.v2");updateDiagnostics()}
    static func formattedHorizon(_ date:Date,settings:Settings) -> String {
        let formatter=formatter(settings:settings);formatter.dateFormat=settings.use24Hour ? "d MMM yyyy, HH:mm" : "d MMM yyyy, h:mm a";return formatter.string(from:date)
    }
    private static func formattedTime(_ date:Date,settings:Settings) -> String {
        AppLanguage.time(date,language:settings.language,zone:TimeZone(identifier:settings.location?.zone ?? "UTC") ?? .gmt,use24Hour:settings.use24Hour)
    }
    private static func formatter(settings:Settings) -> DateFormatter {
        let formatter=DateFormatter();formatter.locale=AppLanguage.locale(settings.language);formatter.calendar=Calendar(identifier:.gregorian);formatter.timeZone=TimeZone(identifier:settings.location?.zone ?? "UTC") ?? .gmt;return formatter
    }
}
extension Notification.Name {static let salatiScheduleNeedsRefresh=Notification.Name("salati.schedule.needsRefresh")}
