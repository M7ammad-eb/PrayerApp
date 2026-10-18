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
    private var refreshTask: Task<Void,Never>?
    private var boundaryTask: Task<Void,Never>?
    private var generation=0
    private var locationGeneration=0
    private var lastLocationCheck=Date.distantPast
    var arabic: Bool { settings.language == "ar" }
    var locale: Locale { Locale(identifier: arabic ? "ar_SA@numbers=latn" : "en_US@numbers=latn") }
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
            if ProcessInfo.processInfo.arguments.contains("-uiTestingArabicDark") {settings.language="ar";settings.appearance="dark"}
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
                        self.locationService.status("Location unchanged; small GPS movements are ignored.",date:fix.timestamp);return
                    }
                    var location=place.location;location.latitude=fix.latitude;location.longitude=fix.longitude
                    location.isGPS=true;location.nearestDistance=distance;location.lastUpdated=fix.timestamp;location.accuracyMeters=fix.accuracy
                    self.select(location);self.locationService.status("Location updated from GPS.",date:fix.timestamp)
                } catch {self.locationService.status("Offline place lookup failed. Try again or select a location manually.");self.error=error.localizedDescription}
            }
        }
    }
    func select(_ location:SavedLocation) {
        locationGeneration+=1;locationService.cancel()
        if settings.location == nil {settings.method=engine.suggestedMethod(zone:location.zone)}
        settings.location=location
    }
    func iso(_ date: Date) -> String {
        let f=DateFormatter(); f.locale=Locale(identifier:"en_US_POSIX"); f.calendar=calendar; f.timeZone=zone; f.dateFormat="yyyy-MM-dd"; return f.string(from:date)
    }
    func time(_ epoch: Double) -> String {
        let parts=calendar.dateComponents([.hour,.minute],from:Date(timeIntervalSince1970:epoch))
        let h=parts.hour ?? 0, m=parts.minute ?? 0
        if settings.use24Hour { return String(format:"%02d:%02d",locale:Locale(identifier:"en_US_POSIX"),h,m) }
        let marker=arabic ? (h<12 ? "ص" : "م") : (h<12 ? "AM" : "PM")
        return String(format:"%d:%02d %@",locale:Locale(identifier:"en_US_POSIX"),h%12 == 0 ? 12 : h%12,m,marker)
    }
    func displayDate(_ date: Date) -> String {
        let f=DateFormatter(); f.locale=locale; f.calendar=calendar; f.timeZone=zone; f.setLocalizedDateFormatFromTemplate("EEEE d MMMM"); return f.string(from:date)
    }
    func title(_ event: PrayerEvent) -> String { arabic ? event.arabicTitle : event.title }
    func calculate(_ date: Date, now: Date = Date()) throws -> PrayerDay {
        guard let location=settings.location else { throw CocoaError(.validationMissingMandatoryProperty) }
        let a=settings.adjustments
        return try engine.dayScoped(dateISO:iso(date),latitude:location.latitude,longitude:location.longitude,zone:location.zone,
            method:settings.method,hanafi:settings.hanafi,highLatitude:settings.highLatitude,
            fajr:Int32(a[0]),sunrise:Int32(a[1]),dhuhr:Int32(a[2]),asr:Int32(a[3]),maghrib:Int32(a[4]),isha:Int32(a[5]),
            hijriOffset:Int32(settings.hijriOffset),hijriAnchor:settings.hijriAnchor,nowEpoch:now.timeIntervalSince1970)
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
            func events(_ schedule:PrayerDay)->[SnapshotEvent] {
                schedule.events.map {SnapshotEvent(id:"\(schedule.dateISO).\($0.id)",title:title($0),date:Date(timeIntervalSince1970:$0.epochSeconds),timeText:time($0.epochSeconds))}
            }
            let all=schedules.flatMap(events)
            let until=calendar.date(byAdding:.day,value:14,to:calendar.startOfDay(for:now))!
            let moments=SharedPresentation.shared.timeline(days:schedules,nowEpoch:now.timeIntervalSince1970,validUntilEpoch:until.timeIntervalSince1970).map {
                SnapshotMoment(date:Date(timeIntervalSince1970:$0.epochSeconds),civilDateISO:$0.civilDateISO,religiousDateISO:$0.religiousDateISO,
                    hijriEnglish:$0.hijriEnglish,hijriArabic:$0.hijriArabic,currentEventID:$0.currentEventID,nextEventID:$0.nextEventID)
            }
            let days=schedules.map {SnapshotDay(dateISO:$0.dateISO,start:Date(timeIntervalSince1970:$0.civilStartEpoch),end:Date(timeIntervalSince1970:$0.civilEndEpoch),
                hijriEnglish:$0.civilHijriEnglish,hijriArabic:$0.civilHijriArabic,events:events($0))}
            let snapshot=ScheduleSnapshot(city:locationDisplayName,events:all,validUntil:until,language:configuration.language,use24Hour:configuration.use24Hour,days:days,moments:moments,zone:zone.identifier)
            var storageStatus:String?
            do {try snapshot.save();WidgetCenter.shared.reloadAllTimelines()}
            catch {storageStatus=String(localized:"Widget storage is unavailable. The app and widget must share the same signed App Group.",locale:locale)}
            boundaryTask?.cancel()
            if let next=moments.first(where:{$0.date>now}) {
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
        } catch is CancellationError {} catch {if generation == revision {self.error=error.localizedDescription}}
    }
}
