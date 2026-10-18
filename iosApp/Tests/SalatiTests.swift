import XCTest
import AVFoundation
import UIKit
import SalatiCore
@testable import Salati
final class SalatiTests:XCTestCase {
    func testFrameworkRamadanAndNextDayFajr() throws {
        let engine=SalatiEngine()
        func day(_ date:String,_ now:Double=0) throws -> PrayerDay {
            try engine.day(dateISO:date,latitude:24.4672,longitude:39.6111,zone:"Asia/Riyadh",method:"UMM_AL_QURA",hanafi:false,highLatitude:"ANGLE_BASED",fajr:0,sunrise:0,dhuhr:0,asr:0,maghrib:0,isha:0,hijriOffset:0,nowEpoch:now)
        }
        let ramadan=try day("2027-02-20")
        XCTAssertEqual(ramadan.events[5].epochSeconds-ramadan.events[4].epochSeconds,7200)
        let after=try day("2027-02-20",ramadan.events[5].epochSeconds+60)
        XCTAssertEqual(after.next.epochSeconds,try day("2027-02-21").events[0].epochSeconds)
    }
    func testBoundedPlanRemindersAndCancellationInputs() {
        var settings=Settings()
        settings.policies["FAJR"]=PrayerPolicy(delivery:.alarm,sound:"default",reminder:10)
        let now=Date(timeIntervalSince1970:1000)
        let events=(0..<100).map {SnapshotEvent(id:"\($0).FAJR",title:"Fajr",date:now.addingTimeInterval(Double(($0+1)*3600)))}
        let plan=ScheduledAlert.plan(events:events,settings:settings,now:now)
        XCTAssertEqual(plan.count,60)
        XCTAssertTrue(plan.first!.reminder)
        XCTAssertTrue(plan.allSatisfy {$0.date>now})
        settings.policies["FAJR"]?.delivery = .off
        XCTAssertTrue(ScheduledAlert.plan(events:events,settings:settings,now:now).isEmpty)
    }
    func testOfflineArabicAndEnglishSearch() async throws {
        let places=OfflinePlaces()
        let english=try await places.search("Madinah")
        let arabic=try await places.search("المدينة")
        XCTAssertFalse(english.isEmpty)
        XCTAssertFalse(arabic.isEmpty)
        let punctuation=try await places.search("* ()")
        XCTAssertTrue(punctuation.isEmpty)
        let nearest=try await places.nearest(latitude:24.4672,longitude:39.6111)
        XCTAssertNotNil(nearest)
        XCTAssertLessThan(nearest!.1,20)
    }
    @MainActor func testSelectedZoneAndWesternDigits() {
        let model=AppModel()
        var settings=Settings()
        settings.location=SavedLocation(name:"Madinah",latitude:24.4672,longitude:39.6111,zone:"Asia/Riyadh")
        settings.use24Hour=true
        settings.language="ar"
        model.settings=settings
        let instant=ISO8601DateFormatter().date(from:"2026-01-15T05:00:00Z")!
        XCTAssertEqual(model.time(instant.timeIntervalSince1970),"08:00")
        XCTAssertEqual(model.iso(ISO8601DateFormatter().date(from:"2026-01-15T22:00:00Z")!),"2026-01-16")
    }
    func testBundledAudioResources() throws {
        XCTAssertEqual(Recording.all.count,11)
        for recording in Recording.all {
            let full=try XCTUnwrap(Bundle.main.url(forResource:recording.id,withExtension:"mp3"))
            let excerpt=try XCTUnwrap(Bundle.main.url(forResource:recording.id+"_short",withExtension:"caf"))
            XCTAssertGreaterThan(try AVAudioPlayer(contentsOf:full).duration,0)
            let duration=try AVAudioPlayer(contentsOf:excerpt).duration
            XCTAssertGreaterThan(duration,0)
            XCTAssertLessThanOrEqual(duration,25.1)
        }
    }
    func testWidgetAppGroupRoundTrip() throws {
        let event=SnapshotEvent(id:"2026-10-18.FAJR",title:"Fajr",date:Date(timeIntervalSince1970:1792288800),timeText:"05:00")
        let snapshot=ScheduleSnapshot(city:"Madinah",events:[event],validUntil:event.date,language:"ar",use24Hour:true)
        try snapshot.save()
        let loaded=try XCTUnwrap(ScheduleSnapshot.load())
        XCTAssertEqual(loaded.city,"Madinah")
        XCTAssertEqual(loaded.events,[event])
        XCTAssertEqual(loaded.language,"ar")
    }
    @MainActor func testAccentIsOpaqueAndContrastsInBothAppearances() throws {
        let color=try XCTUnwrap(UIColor(named:"AccentColor"))
        for style in [UIUserInterfaceStyle.light,.dark] {
            let resolved=color.resolvedColor(with:UITraitCollection(userInterfaceStyle:style))
            var r:CGFloat=0,g:CGFloat=0,b:CGFloat=0,a:CGFloat=0
            XCTAssertTrue(resolved.getRed(&r,green:&g,blue:&b,alpha:&a))
            XCTAssertGreaterThanOrEqual(a,0.99,"Accent controls must not disappear into their background")
            func linear(_ value:CGFloat) -> Double {let v=Double(value);return v<=0.04045 ? v/12.92 : pow((v+0.055)/1.055,2.4)}
            let luminance=0.2126*linear(r)+0.7152*linear(g)+0.0722*linear(b)
            let contrast=style == .light ? 1.05/(luminance+0.05) : (luminance+0.05)/0.05
            XCTAssertGreaterThanOrEqual(contrast,4.5)
        }
    }
    @MainActor func testAlertHorizonUsesSelectedZoneAndWesternDigits() {
        var settings=Settings()
        settings.location=SavedLocation(name:"Madinah",latitude:24.4672,longitude:39.6111,zone:"Asia/Riyadh")
        settings.language="ar";settings.use24Hour=true
        let date=ISO8601DateFormatter().date(from:"2026-01-15T05:00:00Z")!
        let text=PrayerScheduler.formattedHorizon(date,settings:settings)
        XCTAssertTrue(text.contains("08:00"))
        XCTAssertNil(text.range(of:"[٠-٩۰-۹]",options:.regularExpression))
    }
    func testResignedWidgetReadsAppSnapshotFromAuthorizedGroup() throws {
        let appID="com.salati.prayertimes.ios.TESTTEAM01"
        let group="group."+appID
        XCTAssertEqual(SharedScheduleStorage.groups(bundleIdentifier:appID),SharedScheduleStorage.groups(bundleIdentifier:appID+".widgets"))
        XCTAssertEqual(SharedScheduleStorage.groups(bundleIdentifier:SharedScheduleStorage.originalAppIdentifier),[SharedScheduleStorage.declaredGroup])
        // Use the actual signed simulator App Group. Model iLoader's profile by denying
        // the original group, so falling back to the old hardcoded ID cannot pass.
        let authorized=try XCTUnwrap(FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:group))
        let container: (String)->URL? = {$0 == group ? authorized : nil}
        let appStorage=SharedScheduleStorage(bundleIdentifier:appID,containerURL:container)
        let widgetStorage=SharedScheduleStorage(bundleIdentifier:appID+".widgets",containerURL:container)
        XCTAssertEqual(appStorage.files,widgetStorage.files)
        let now=Date()
        let event=SnapshotEvent(id:"today.FAJR",title:"Fajr",date:now.addingTimeInterval(600),timeText:"05:00")
        let snapshot=ScheduleSnapshot(city:"Madinah",events:[event],validUntil:now.addingTimeInterval(3600),language:"ar",use24Hour:true)
        try snapshot.save(storage:appStorage)
        let loaded=try XCTUnwrap(ScheduleSnapshot.load(storage:widgetStorage))
        XCTAssertEqual(loaded.nextEvent(after:now),event)
        XCTAssertEqual(loaded.city,"Madinah")
        XCTAssertEqual(loaded.language,"ar")
        XCTAssertNil(loaded.nextEvent(after:loaded.validUntil))
    }
    func testSharedSnapshotFallsBackFromUnusableContainer() throws {
        let root=FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at:root,withIntermediateDirectories:true)
        defer {try? FileManager.default.removeItem(at:root)}
        let blocked=root.appendingPathComponent("not-a-directory")
        try Data("blocked".utf8).write(to:blocked)
        let storage=SharedScheduleStorage(bundleIdentifier:"com.salati.prayertimes.ios.TESTTEAM01") {
            $0 == SharedScheduleStorage.declaredGroup ? root : blocked
        }
        let snapshot=ScheduleSnapshot(city:"Damascus",events:[],validUntil:Date(),language:"en",use24Hour:false)
        try snapshot.save(storage:storage)
        XCTAssertEqual(ScheduleSnapshot.load(storage:storage)?.city,"Damascus")
        let denied=SharedScheduleStorage(bundleIdentifier:nil,containerURL:{_ in nil})
        XCTAssertThrowsError(try snapshot.save(storage:denied))
        XCTAssertNil(ScheduleSnapshot.load(storage:denied))
    }

}
