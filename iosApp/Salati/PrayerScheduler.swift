import Foundation
import UserNotifications
import AlarmKit
import ActivityKit
import SwiftUI
import SalatiCore

struct PrayerAlarmMetadata: AlarmMetadata { var prayer: String }
struct ScheduledAlert: Equatable {
    let id: String
    let prayer: String
    let title: String
    let date: Date
    let policy: PrayerPolicy
    let reminder: Bool
    static func plan(events:[SnapshotEvent],settings:Settings,now:Date) -> [ScheduledAlert] {
        var items:[ScheduledAlert]=[]
        for event in events {
            let prayer=String(event.id.split(separator:".").last ?? "")
            guard prayer != "SUNRISE",let policy=settings.policies[prayer],policy.delivery != .off else {continue}
            if event.date>now {items.append(.init(id:"salati.\(event.id)",prayer:prayer,title:event.title,date:event.date,policy:policy,reminder:false))}
            let reminder=event.date.addingTimeInterval(Double(-policy.reminder*60))
            if policy.reminder>0 && reminder>now {items.append(.init(id:"salati.\(event.id).reminder",prayer:prayer,title:event.title,date:reminder,policy:policy,reminder:true))}
        }
        return Array(items.sorted {$0.date<$1.date}.prefix(60))
    }
}
@MainActor final class PrayerScheduler {
    static let shared=PrayerScheduler()
    private let notifications=UNUserNotificationCenter.current()
    private var revision=0
    func authorizeNotifications() async -> String {
        do {return try await notifications.requestAuthorization(options:[.alert,.sound,.badge]) ? "Notifications allowed" : "Notifications were not allowed. You can change this in iOS Settings."}
        catch {return error.localizedDescription}
    }
    func authorizeAlarms() async -> String {
        do {return try await AlarmManager.shared.requestAuthorization() == .authorized ? "System alarms allowed" : "System alarms were not allowed. You can change this in iOS Settings."}
        catch {return error.localizedDescription}
    }
    func reconcile(events:[SnapshotEvent],settings:Settings) async -> String {
        revision+=1; let current=revision
        let batch=UUID().uuidString
        var failures:[String]=[]
        let pending=await notifications.pendingNotificationRequests()
        guard current==revision && !Task.isCancelled else {return ""}
        notifications.removePendingNotificationRequests(withIdentifiers:pending.filter {$0.identifier.hasPrefix("salati.")}.map(\.identifier))
        // Query the system so interrupted prior scheduling cannot orphan a persisted alarm.
        if AlarmManager.shared.authorizationState == .authorized {
            do {for alarm in try AlarmManager.shared.alarms {try AlarmManager.shared.cancel(id:alarm.id)}}
            catch {failures.append("Could not clear previous alarms: \(error.localizedDescription)")}
        }
        let permission=await notifications.notificationSettings()
        guard current==revision && !Task.isCancelled else {return ""}
        let plan=ScheduledAlert.plan(events:events,settings:settings,now:Date())
        var scheduled=0
        var lastScheduled:Date?
        for item in plan {
            guard current==revision && !Task.isCancelled else {return ""}
            do {
                if item.policy.delivery == .alarm && !item.reminder {
                    guard AlarmManager.shared.authorizationState == .authorized else {failures.append("Allow system alarms to use the selected alarm policy.");continue}
                    guard item.policy.sound != "silent" else {failures.append("System alarms require an audible sound. Choose Notification for silent delivery.");continue}
                    let stop=AlarmButton(text:settings.language == "ar" ? "إيقاف" : "Stop",textColor:.white,systemImageName:"stop.fill")
                    let alert=AlarmPresentation.Alert(title:LocalizedStringResource(stringLiteral:item.title),stopButton:stop)
                    let attributes=AlarmAttributes(presentation:AlarmPresentation(alert:alert),metadata:PrayerAlarmMetadata(prayer:item.prayer),tintColor:Color.green)
                    let sound: AlertConfiguration.AlertSound = item.policy.sound == "default" || item.policy.sound == "silent" ? .default : .named("\(item.policy.sound)_short.caf")
                    // AlarmKit is intentionally a prominent audible policy. A silent system alarm
                    // is not represented as an Android audio-stream mapping.
                    let alarmID=UUID()
                    _=try await AlarmManager.shared.schedule(id:alarmID,configuration:AlarmManager.AlarmConfiguration.alarm(schedule:.fixed(item.date),attributes:attributes,sound:sound))
                    guard current==revision && !Task.isCancelled else {try AlarmManager.shared.cancel(id:alarmID);return ""}
                } else {
                    guard permission.authorizationStatus == .authorized || permission.authorizationStatus == .provisional else {failures.append("Allow notifications for the selected alerts and reminders.");continue}
                    let content=UNMutableNotificationContent()
                    content.title=item.title
                    if Recording.all.contains(where:{$0.id == item.policy.sound}) {
                        content.categoryIdentifier="PRAYER"
                        content.userInfo=["recording":item.policy.sound]
                    }
                    content.body=item.reminder ? (settings.language == "ar" ? "اقترب موعد الصلاة" : "Prayer time is approaching") : (settings.language == "ar" ? "حان وقت الصلاة" : "It is time for prayer")
                    content.sound=item.policy.sound == "silent" ? nil : item.policy.sound == "default" ? .default : UNNotificationSound(named:UNNotificationSoundName("\(item.policy.sound)_short.caf"))
                    var calendar=Calendar(identifier:.gregorian);calendar.timeZone = .gmt
                    var components=calendar.dateComponents([.year,.month,.day,.hour,.minute,.second],from:item.date);components.timeZone = .gmt
                    let identifier=item.id+"."+batch
                    try await notifications.add(UNNotificationRequest(identifier:identifier,content:content,trigger:UNCalendarNotificationTrigger(dateMatching:components,repeats:false)))
                    guard current==revision && !Task.isCancelled else {notifications.removePendingNotificationRequests(withIdentifiers:[identifier]);return ""}
                }
                scheduled+=1
                lastScheduled=item.date
            } catch {failures.append(error.localizedDescription)}
        }
        if plan.isEmpty && failures.isEmpty {return ""}
        let unique=Array(Set(failures)).sorted().joined(separator:" ")
        let horizon=lastScheduled.map {Self.formattedHorizon($0,settings:settings)} ?? "—"
        return settings.language == "ar" ? "تم تجهيز \(scheduled) تنبيهًا حتى \(horizon). \(unique)" : "\(scheduled) alerts prepared through \(horizon). \(unique)"
    }
    static func formattedHorizon(_ date:Date,settings:Settings) -> String {
        let formatter=DateFormatter()
        formatter.locale=Locale(identifier:settings.language == "ar" ? "ar_SA@numbers=latn" : "en_US@numbers=latn")
        formatter.calendar=Calendar(identifier:.gregorian)
        formatter.timeZone=TimeZone(identifier:settings.location?.zone ?? "UTC") ?? .gmt
        formatter.dateFormat=settings.use24Hour ? "d MMM yyyy, HH:mm" : "d MMM yyyy, h:mm a"
        return formatter.string(from:date)
    }
    func reconcileLiveActivities(next:PrayerEvent,city:String) async {
        let date=Date(timeIntervalSince1970:next.epochSeconds)
        for activity in Activity<PrayerActivityAttributes>.activities where activity.content.state.date != date || activity.attributes.city != city {
            await activity.end(nil,dismissalPolicy:.immediate)
        }
    }
    func startLiveActivity(day:PrayerDay,city:String,title:String) async throws {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {throw CocoaError(.featureUnsupported)}
        for existing in Activity<PrayerActivityAttributes>.activities {await existing.end(nil,dismissalPolicy:.immediate)}
        let date=Date(timeIntervalSince1970:day.next.epochSeconds)
        guard date>Date() else {return}
        let content=ActivityContent(state:PrayerActivityAttributes.ContentState(title:title,date:date),staleDate:date)
        _=try Activity.request(attributes:PrayerActivityAttributes(city:city),content:content,pushType:nil)
    }
    // One scheduled countdown, started shortly before the next prayer. Requesting it is an
    // explicit foreground action; there is no server or assumption of indefinite execution.
    func scheduleLiveActivity(day:PrayerDay,city:String,title:String) throws {
        let date=Date(timeIntervalSince1970:day.next.epochSeconds)
        let start=date.addingTimeInterval(-300)
        guard start>Date(),ActivityAuthorizationInfo().areActivitiesEnabled else {throw CocoaError(.featureUnsupported)}
        guard Activity<PrayerActivityAttributes>.activities.isEmpty else {throw CocoaError(.validationMultipleErrors)}
        let content=ActivityContent(state:PrayerActivityAttributes.ContentState(title:title,date:date),staleDate:date)
        _=try Activity<PrayerActivityAttributes>.request(attributes:PrayerActivityAttributes(city:city),content:content,pushType:nil,style:.standard,alertConfiguration:AlertConfiguration(title:LocalizedStringResource(stringLiteral:title),body:"Prayer countdown",sound:.default),start:start)
    }
}
