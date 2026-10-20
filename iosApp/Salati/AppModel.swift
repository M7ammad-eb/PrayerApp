import SwiftUI
import Combine
import SalatiCore
import WidgetKit
import CoreLocation

@MainActor final class AppModel: ObservableObject {
    let engine = SalatiEngine()
    let places = OfflinePlaces()
    let locationService = LocationService()
    let scheduler = PrayerScheduler.shared
    @Published var settings = Settings.load() { didSet { if settings != oldValue {settings.save();queueRefresh()} } }
    @Published var day: PrayerDay?
    @Published var error: String?
    @Published var scheduleStatus: String?
    @Published var presentationSnapshot: ScheduleSnapshot?
    @Published var onboardingStep=0
    @Published var reviewingSetup=false
    @Published var setupReviewStep=0
    private var refreshTask: Task<Void,Never>?
    private var boundaryTask: Task<Void,Never>?
    private var generation=0
    private var locationGeneration=0
    private var lastLocationCheck=Date.distantPast
    var arabic: Bool { settings.language == "ar" }
    var locale: Locale { AppLanguage.locale(settings.language) }
    var languageDisplayName:String {AppLanguage.name(settings.language)}
    var displayEvents:[SnapshotEvent] {presentationSnapshot?.displayEvents(at:Date()) ?? []}
    var currentEventID:String? {presentationSnapshot?.moment(at:Date())?.currentEventID}
    var nextEventID:String? {presentationSnapshot?.moment(at:Date())?.nextEventID}
    func syncSystemLanguage() {if settings.language != AppLanguage.current {settings.language=AppLanguage.current}}
    var zone: TimeZone { TimeZone(identifier:settings.location?.zone ?? "UTC") ?? .gmt }
    var calendar: Calendar { var c=Calendar(identifier:.gregorian); c.timeZone=zone; return c }
    var todayISO: String { iso(Date()) }
    var religiousTodayISO:String {day?.religiousDateISO ?? todayISO}
    var locationDisplayName:String {
        guard let l=settings.location else {return "—"}
        return LocationLabels.shared.readable(english:l.name,ascii:l.asciiName ?? l.name,arabicName:l.arabicName,isArabic:arabic)
    }
    func calendarPage(monthDelta:Int32)->CalendarPage? {
        try? engine.calendarScoped(dateISO:religiousTodayISO,monthDelta:monthDelta,storedOffset:Int32(settings.hijriOffset),anchor:settings.hijriAnchor)
    }
    func calendarOffset(pageMonthKey:String)->Int {settings.hijriAnchor == pageMonthKey ? settings.hijriOffset:0}
    func setCalendarOffset(pageMonthKey:String,value:Int) {settings.hijriAnchor=pageMonthKey;settings.hijriOffset=max(-2,min(2,value))}
    func requestRoutineLocation() {
        guard settings.location?.isGPS == true,LocationAcceptance.shared.shouldAcquire(lastEpoch:max(lastLocationCheck,settings.location?.lastUpdated ?? .distantPast).timeIntervalSince1970,nowEpoch:Date().timeIntervalSince1970,isGps:true,explicit:false) else {return}
        lastLocationCheck=Date();locationService.request(explicit:false)
    }
    init() {
        if ProcessInfo.processInfo.arguments.contains("-uiTestingReset") {
            settings=Settings()
            if ProcessInfo.processInfo.arguments.contains("-uiTestingArabicDark") {settings.appearance="dark"}
            settings.save()
        }
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSSuccess") {
            var location=SavedLocation(name:"Madinah",arabicName:"المدينة المنورة",latitude:24.46861,longitude:39.61417,zone:"Asia/Riyadh",isGPS:true)
            location.lastUpdated=Date();location.accuracyMeters=8;settings.location=location
            locationService.status("Location updated from a simulator test fix.",date:Date())
        }
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSDenied") {
            locationService.status("Location access is unavailable. Choose a city or enter coordinates.")
        }
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSLoading") {
            locationService.isAcquiring=true;locationService.message="Finding your location…"
        }
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSPermission") {
            locationService.isAcquiring=true;locationService.message="Requesting location permission…"
        }
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSTimeout") {
            locationService.status("Location request timed out. Try again or choose a city.")
        }
        locationService.onFix = { [weak self] fix,explicit in
            guard let self else {return}
            let revision=self.locationGeneration
            Task {
                do {
                    guard let (place,distance)=try await self.places.nearest(latitude:fix.latitude,longitude:fix.longitude) else {throw CocoaError(.fileReadCorruptFile)}
                    guard revision == self.locationGeneration else {return}
                    if let current=self.settings.location,
                       !LocationAcceptance.shared.accept(currentLatitude:current.latitude,currentLongitude:current.longitude,currentIsGps:current.isGPS,
                        candidateLatitude:fix.latitude,candidateLongitude:fix.longitude,zoneChanged:current.zone != place.zone,explicit:explicit) {
                        self.locationService.status("Location is up to date.",date:fix.timestamp);return
                    }
                    var location=place.location;location.latitude=fix.latitude;location.longitude=fix.longitude
                    location.isGPS=true;location.nearestDistance=distance;location.lastUpdated=fix.timestamp;location.accuracyMeters=fix.accuracy
                    self.select(location);self.locationService.status("Location updated.",date:fix.timestamp)
                } catch {self.locationService.status("Couldn’t find your location. Try again or choose a city.");self.error=String(localized:"Prayer times could not be updated. Check your location and calculation settings.",locale:self.locale)}
            }
        }
    }
    func select(_ location:SavedLocation) {
        locationGeneration+=1;locationService.cancel()
        settings.location=location
        applyOnboardingDefaults()
    }
    var onboardingDefaultsAreFallback:Bool {
        OnboardingDefaults.shared.recommendation(zone:settings.location?.zone ?? "UTC").fallback
    }
    func applyOnboardingDefaults() {
        guard let location=settings.location else {return}
        let defaults=OnboardingDefaults.shared.resolve(zone:location.zone,currentMethod:settings.method,currentHanafi:settings.hanafi,
            methodOverridden:settings.calculationMethodOverridden,asrOverridden:settings.asrOverridden,completed:settings.onboardingComplete)
        var updated=settings;updated.method=defaults.method;updated.hanafi=defaults.hanafi;settings=updated
    }
    func iso(_ date: Date) -> String {
        let f=DateFormatter(); f.locale=Locale(identifier:"en_US_POSIX"); f.calendar=calendar; f.timeZone=zone; f.dateFormat="yyyy-MM-dd"; return f.string(from:date)
    }
    func time(_ epoch: Double) -> String {
        AppLanguage.time(Date(timeIntervalSince1970:epoch),language:settings.language,zone:zone,use24Hour:settings.use24Hour)
    }
    func displayDate(_ date: Date) -> String {
        let f=DateFormatter(); f.locale=locale; f.calendar=calendar; f.timeZone=zone; f.setLocalizedDateFormatFromTemplate("EEEE d MMMM"); return f.string(from:date)
    }
    func title(_ event: PrayerEvent) -> String {AppLanguage.text(event.id,language:settings.language)}
    func calculate(_ date: Date, now: Date = Date()) throws -> PrayerDay {
        guard let location=settings.location else { throw CocoaError(.validationMissingMandatoryProperty) }
        let a=settings.adjustments
        return try engine.dayScoped(dateISO:iso(date),latitude:location.latitude,longitude:location.longitude,zone:location.zone,
            method:settings.method,hanafi:settings.hanafi,highLatitude:settings.highLatitude,
            fajr:Int32(a[0]),sunrise:Int32(a[1]),dhuhr:Int32(a[2]),asr:Int32(a[3]),maghrib:Int32(a[4]),isha:Int32(a[5]),
            hijriOffset:Int32(settings.hijriOffset),hijriAnchor:settings.hijriAnchor,nowEpoch:now.timeIntervalSince1970)
    }
    private func makeSnapshot(schedules:[PrayerDay],now:Date,until:Date,configuration:Settings)->ScheduleSnapshot {
        func events(_ schedule:PrayerDay)->[SnapshotEvent] {
            schedule.events.map {SnapshotEvent(id:"\(schedule.dateISO).\($0.id)",title:title($0),date:Date(timeIntervalSince1970:$0.epochSeconds),timeText:time($0.epochSeconds))}
        }
        let all=schedules.flatMap(events)
        let moments=SharedPresentation.shared.timeline(days:schedules,nowEpoch:now.timeIntervalSince1970,validUntilEpoch:until.timeIntervalSince1970).map {
            SnapshotMoment(date:Date(timeIntervalSince1970:$0.epochSeconds),civilDateISO:$0.civilDateISO,religiousDateISO:$0.religiousDateISO,
                hijriEnglish:$0.hijriEnglish,hijriArabic:$0.hijriArabic,currentEventID:$0.currentEventID,nextEventID:$0.nextEventID,
                displayEventIDs:$0.displayEventIDs,tomorrowEventID:$0.tomorrowEventID,previousEventID:$0.previousEventID)
        }
        let days=schedules.map {SnapshotDay(dateISO:$0.dateISO,start:Date(timeIntervalSince1970:$0.civilStartEpoch),end:Date(timeIntervalSince1970:$0.civilEndEpoch),
            hijriEnglish:$0.civilHijriEnglish,hijriArabic:$0.civilHijriArabic,events:events($0))}
        return ScheduleSnapshot(city:locationDisplayName,events:all,validUntil:until,language:configuration.language,use24Hour:configuration.use24Hour,days:days,moments:moments,zone:zone.identifier)
    }
    // Render-test inputs go through the production Kotlin calculator and presentation policy.
    // These fixtures never replace the persisted widget schedule.
    func showcaseSnapshot(phase:String)->(snapshot:ScheduleSnapshot,date:Date)? {
        guard ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase"),
              let baseDate=calendar.date(from:DateComponents(year:2026,month:10,day:19)),
              let base=try? calculate(baseDate,now:baseDate) else {return nil}
        let instant=Date(timeIntervalSince1970:phase == "afterIsha" ? base.events.last!.epochSeconds+60:base.civilStartEpoch+300)
        let schedules=(-1...2).compactMap {delta -> PrayerDay? in
            guard let date=calendar.date(byAdding:.day,value:delta,to:instant) else {return nil}
            return try? calculate(date,now:instant)
        }
        let fixture=makeSnapshot(schedules:schedules,now:instant,until:instant.addingTimeInterval(86400),configuration:settings)
        // Translate a shared-calculated test scenario onto the current clock so
        // previews exercise WidgetKit's real flexible timer, not static text.
        let reference=Date(),shift=reference.timeIntervalSince(instant)
        func event(_ value:SnapshotEvent)->SnapshotEvent {
            .init(id:value.id,title:value.title,date:value.date.addingTimeInterval(shift),timeText:value.timeText)
        }
        let days=fixture.days.map {SnapshotDay(dateISO:$0.dateISO,start:$0.start.addingTimeInterval(shift),end:$0.end.addingTimeInterval(shift),hijriEnglish:$0.hijriEnglish,hijriArabic:$0.hijriArabic,events:$0.events.map(event))}
        let moments=fixture.moments.map {value -> SnapshotMoment in var copy=value;copy.date=value.date.addingTimeInterval(shift);return copy}
        return (ScheduleSnapshot(city:fixture.city,events:fixture.events.map(event),validUntil:fixture.validUntil.addingTimeInterval(shift),language:fixture.language,use24Hour:fixture.use24Hour,days:days,moments:moments,zone:fixture.zone),reference)
    }
    func queueRefresh() {
        refreshTask?.cancel()
        refreshTask=Task {
            do { try await Task.sleep(for:.milliseconds(200)); try Task.checkCancellation(); await refresh() }
            catch { }
        }
    }
    func refresh(context:SchedulingContext = .application) async {
        guard settings.location != nil else {return}
        generation+=1;let revision=generation;let configuration=settings;let now=Date()
        do {
            let current=try calculate(now,now:now);day=current
            var schedules:[PrayerDay]=[]
            for delta in -1...14 {
                guard let date=calendar.date(byAdding:.day,value:delta,to:now) else {continue}
                schedules.append(try calculate(date,now:now))
            }
            let until=calendar.date(byAdding:.day,value:14,to:calendar.startOfDay(for:now))!
            let snapshot=makeSnapshot(schedules:schedules,now:now,until:until,configuration:configuration)
            let all=snapshot.events
            presentationSnapshot=snapshot
            var storageStatus:String?
            do {try snapshot.save();WidgetCenter.shared.reloadAllTimelines()}
            catch {storageStatus=String(localized:"Widget storage is unavailable. The app and widget must share the same signed App Group.",locale:locale)}
            boundaryTask?.cancel()
            if let next=snapshot.moments.first(where:{$0.date>now}) {
                boundaryTask=Task {[weak self] in
                    do {try await Task.sleep(for:.seconds(max(1,next.date.timeIntervalSinceNow)));try Task.checkCancellation();self?.queueRefresh()}
                    catch {}
                }
            }
            let inputs=all.map {PlanEvent(id:$0.id,prayer:String($0.id.split(separator:".").last ?? ""),title:$0.title,epochSeconds:$0.date.timeIntervalSince1970)}
            let policies=configuration.policies.map {PlanPolicy(prayer:$0.key,delivery:$0.value.delivery.rawValue,reminderMinutes:Int32($0.value.reminder))}
            let plan=SharedScheduling.shared.plan(events:inputs,policies:policies,nowEpoch:now.timeIntervalSince1970,liveActivitiesEnabled:configuration.liveActivitiesEnabled)
            let alerts=plan.filter {$0.kind != "live"}.map {item in
                ScheduledAlert(id:item.id,prayer:item.prayer,title:item.title,date:Date(timeIntervalSince1970:item.triggerEpoch),prayerDate:Date(timeIntervalSince1970:item.eventEpoch),
                    policy:configuration.policies[item.prayer] ?? PrayerPolicy(),reminder:item.kind == "reminder")
            }
            let live=plan.filter {$0.kind == "live"}.map {ScheduledLivePrayer(id:$0.id,title:$0.title,date:Date(timeIntervalSince1970:$0.eventEpoch),start:Date(timeIntervalSince1970:$0.triggerEpoch))}
            let result=await scheduler.reconcile(alerts:alerts,live:live,city:snapshot.city,settings:configuration,context:context)
            guard generation == revision else {return}
            let status=[result,storageStatus].compactMap {$0}.filter {!$0.isEmpty}.joined(separator:" ")
            scheduleStatus=status.isEmpty ? nil:status;error=nil;AppDelegate.requestRefresh()
        } catch is CancellationError {} catch {if generation == revision {self.error=String(localized:"Prayer times could not be updated. Check your location and calculation settings.",locale:locale)}}
    }
}
