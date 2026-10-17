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
    @Published var settings = Settings.load() { didSet { settings.save(); queueRefresh() } }
    @Published var day: PrayerDay?
    @Published var error: String?
    @Published var scheduleStatus: String?
    private var refreshTask: Task<Void,Never>?
    var arabic: Bool { settings.language == "ar" }
    var locale: Locale { Locale(identifier: arabic ? "ar_SA@numbers=latn" : "en_US@numbers=latn") }
    var zone: TimeZone { TimeZone(identifier:settings.location?.zone ?? "UTC") ?? .gmt }
    var calendar: Calendar { var c=Calendar(identifier:.gregorian); c.timeZone=zone; return c }
    var todayISO: String { iso(Date()) }
    init() {
        if ProcessInfo.processInfo.arguments.contains("-uiTestingReset") { settings=Settings(); settings.save() }
        locationService.onFix = { [weak self] fix in
            guard let self else { return }
            Task {
                do {
                    guard let (place,distance)=try await self.places.nearest(latitude:fix.coordinate.latitude,longitude:fix.coordinate.longitude) else { throw CocoaError(.fileReadCorruptFile) }
                    var location=place.location
                    location.latitude=fix.coordinate.latitude; location.longitude=fix.coordinate.longitude
                    location.isGPS=true; location.nearestDistance=distance
                    self.select(location)
                } catch { self.error=error.localizedDescription }
            }
        }
    }
    func select(_ location: SavedLocation) {
        if settings.location == nil { settings.method=engine.suggestedMethod(zone:location.zone) }
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
        let offset=settings.hijriAnchor == engine.monthKey(dateISO:iso(date)) ? settings.hijriOffset : 0
        return try engine.day(dateISO:iso(date),latitude:location.latitude,longitude:location.longitude,zone:location.zone,
            method:settings.method,hanafi:settings.hanafi,highLatitude:settings.highLatitude,
            fajr:Int32(a[0]),sunrise:Int32(a[1]),dhuhr:Int32(a[2]),asr:Int32(a[3]),maghrib:Int32(a[4]),isha:Int32(a[5]),hijriOffset:Int32(offset),nowEpoch:now.timeIntervalSince1970)
    }
    func queueRefresh() {
        refreshTask?.cancel()
        refreshTask=Task {
            do { try await Task.sleep(for:.milliseconds(200)); try Task.checkCancellation(); await refresh() }
            catch { }
        }
    }
    func refresh() async {
        guard settings.location != nil else { return }
        do {
            // Determine the unadjusted current month before applying the month-scoped offset.
            let current=try calculate(Date()); day=current
            var all: [SnapshotEvent]=[]
            for delta in 0..<6 {
                guard let date=calendar.date(byAdding:.day,value:delta,to:Date()) else { continue }
                let schedule=try calculate(date)
                all += schedule.events.map { SnapshotEvent(id:"\(schedule.dateISO).\($0.id)",title:title($0),date:Date(timeIntervalSince1970:$0.epochSeconds)) }
            }
            let snapshot=ScheduleSnapshot(city:settings.location!.name,events:all,validUntil:calendar.date(byAdding:.day,value:6,to:calendar.startOfDay(for:Date()))!,language:settings.language,use24Hour:settings.use24Hour)
            do { try snapshot.save(); WidgetCenter.shared.reloadAllTimelines() }
            catch { scheduleStatus="App Group storage is unavailable. Widgets need the signed App Group entitlement." }
            try Task.checkCancellation()
            let result=await scheduler.reconcile(events:all,settings:settings)
            if !Task.isCancelled { scheduleStatus=result; error=nil }
            AppDelegate.requestRefresh()
        } catch is CancellationError { }
        catch { self.error=error.localizedDescription }
    }
}
