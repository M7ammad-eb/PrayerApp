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
    func testSharedPlanHasStableIdentitiesAndSeparatePrayerReminderInstants() {
        let events=(0..<100).map {PlanEvent(id:"\($0).FAJR",prayer:"FAJR",title:"Fajr",epochSeconds:Double(1000+($0+1)*3600))}
        let policy=PlanPolicy(prayer:"FAJR",delivery:"alarm",reminderMinutes:10)
        let plan=SharedScheduling.shared.plan(events:events,policies:[policy],nowEpoch:1000,liveActivitiesEnabled:false)
        XCTAssertEqual(plan.count,200) // Capacity is API-specific, not a shared combined60 truncation.
        let reminder=plan.first!
        XCTAssertEqual(reminder.kind,"reminder");XCTAssertEqual(reminder.eventEpoch-reminder.triggerEpoch,600)
        XCTAssertTrue(plan.allSatisfy {$0.triggerEpoch>1000})
        XCTAssertEqual(plan.map(\.id),SharedScheduling.shared.plan(events:events,policies:[policy],nowEpoch:1000,liveActivitiesEnabled:false).map(\.id))
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
            XCTAssertNil(Bundle.main.url(forResource:recording.id+"_short",withExtension:"caf"),"No arbitrary adhan cut points are approved")
            XCTAssertGreaterThan(try AVAudioPlayer(contentsOf:full).duration,0)
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
        let moment=SnapshotMoment(date:now,civilDateISO:"today",religiousDateISO:"today",hijriEnglish:"",hijriArabic:"",currentEventID:nil,nextEventID:event.id)
        let snapshot=ScheduleSnapshot(city:"Madinah",events:[event],validUntil:now.addingTimeInterval(3600),language:"ar",use24Hour:true,moments:[moment])
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

    func testAltStoreMetadataSharesGroupAcrossDifferentBundleIDSuffixes() throws {
        let group="group.com.salati.prayertimes.TESTTEAM01"
        // AltStore can also include its own backup group. It is not our storage target.
        let metadata=["group.com.rileytestut.AltStore.TESTTEAM01",group]
        let appID="com.salati.prayertimes.ios.TESTTEAM01"
        let widgetID="com.salati.prayertimes.ios.widgets.TESTTEAM01"
        XCTAssertEqual(SharedScheduleStorage.groups(bundleIdentifier:appID,signedGroups:metadata).first,group)
        let authorized=try XCTUnwrap(FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:group))
        let container: (String)->URL? = {$0 == group ? authorized : nil}
        let appStorage=SharedScheduleStorage(bundleIdentifier:appID,signedGroups:metadata,containerURL:container)
        let widgetStorage=SharedScheduleStorage(bundleIdentifier:widgetID,signedGroups:metadata,containerURL:container)
        XCTAssertEqual(appStorage.files,widgetStorage.files)
        let snapshot=ScheduleSnapshot(city:"Makkah",events:[],validUntil:Date(),language:"ar",use24Hour:true)
        try snapshot.save(storage:appStorage)
        XCTAssertEqual(ScheduleSnapshot.load(storage:widgetStorage)?.city,"Makkah")
    }
    func testSettingsMigrateToPrivateStorageAndRetainLanguageChanges() throws {
        let prefix="SalatiTests."+UUID().uuidString
        let primary=try XCTUnwrap(UserDefaults(suiteName:prefix+".private"))
        let legacy=try XCTUnwrap(UserDefaults(suiteName:prefix+".legacy"))
        defer {
            primary.removePersistentDomain(forName:prefix+".private")
            legacy.removePersistentDomain(forName:prefix+".legacy")
        }
        var previous=Settings()
        previous.language="ar";previous.onboardingComplete=true
        previous.location=SavedLocation(name:"Madinah",latitude:24.4672,longitude:39.6111,zone:"Asia/Riyadh")
        legacy.set(try JSONEncoder().encode(previous),forKey:"settings.v1")
        var migrated=Settings.load(from:primary,legacy:legacy)
        XCTAssertEqual(migrated.language,"ar")
        XCTAssertTrue(migrated.onboardingComplete)
        XCTAssertEqual(migrated.location,previous.location)
        XCTAssertNotNil(primary.data(forKey:"settings.v1"))
        migrated.language="en";migrated.save(to:primary)
        XCTAssertEqual(Settings.load(from:primary,legacy:legacy).language,"en")
        // The old shared preference must never override a newer private preference.
        XCTAssertEqual(Settings.load(from:primary,legacy:legacy).location,previous.location)
    }

    func testBuild2SettingsDecodeWithoutResettingChoices() throws {
        var old=Settings();old.language="ar";old.onboardingComplete=true
        old.policies["MAGHRIB"]=PrayerPolicy(delivery:.alarm,sound:"athan_default",reminder:15)
        var object=try XCTUnwrap(JSONSerialization.jsonObject(with:JSONEncoder().encode(old)) as? [String:Any])
        object.removeValue(forKey:"liveActivitiesEnabled");object.removeValue(forKey:"reminderSound")
        let decoded=try JSONDecoder().decode(Settings.self,from:JSONSerialization.data(withJSONObject:object))
        XCTAssertEqual(decoded.language,"ar");XCTAssertTrue(decoded.onboardingComplete)
        XCTAssertEqual(decoded.policies,old.policies);XCTAssertFalse(decoded.liveActivitiesEnabled);XCTAssertTrue(decoded.reminderSound)
    }
    func testBuild2SnapshotDecodesButRequiresSharedBoundaryRefresh() throws {
        let snapshot=ScheduleSnapshot(city:"Madinah",events:[],validUntil:Date().addingTimeInterval(3600),language:"ar",use24Hour:true)
        var object=try XCTUnwrap(JSONSerialization.jsonObject(with:JSONEncoder().encode(snapshot)) as? [String:Any])
        for key in ["moments","days","zone"] {object.removeValue(forKey:key)}
        let decoded=try JSONDecoder().decode(ScheduleSnapshot.self,from:JSONSerialization.data(withJSONObject:object))
        XCTAssertEqual(decoded.city,"Madinah");XCTAssertEqual(decoded.language,"ar");XCTAssertTrue(decoded.moments.isEmpty)
        XCTAssertNil(decoded.nextEvent(after:Date()))
    }

}
