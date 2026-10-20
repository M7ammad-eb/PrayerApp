import XCTest
import ActivityKit
@testable import Salati

@MainActor private final class FakePrayerActivities:PrayerActivityBackend {
    var items:[PrayerActivityRecord]=[]
    var ended:[String]=[]
    func records()->[PrayerActivityRecord] {items}
    func finish(id:String) async {ended.append(id);items.removeAll {$0.id == id}}
}
final class RecoveryTests:XCTestCase {
    func testLanguageResolutionSupportsCatalogExpansionAndRegionalFallback() {
        XCTAssertEqual(AppLanguage.resolved(supported:["en","ar","fr"],preferred:["fr-CA","en"]),"fr")
        XCTAssertEqual(AppLanguage.resolved(supported:["en","ar"],preferred:["ar-SA","en"]),"ar")
        XCTAssertTrue(AppLanguage.isRightToLeft("ar"))
        XCTAssertTrue(AppLanguage.isRightToLeft("he"))
        XCTAssertFalse(AppLanguage.isRightToLeft("fr"))
    }
    func testPrayerLabelsAndTimeRespectSelectedLanguageWithoutChangingDigits() {
        XCTAssertEqual(AppLanguage.text("FAJR",language:"ar"),"الفجر")
        XCTAssertEqual(AppLanguage.text("FAJR",language:"en"),"Fajr")
        XCTAssertEqual(AppLanguage.text("It is time for prayer",language:"ar"),"حان وقت الصلاة")
        let date=Date(timeIntervalSince1970:0)
        for language in ["en","ar","fr","he"] {
            let value=AppLanguage.time(date,language:language,zone:.gmt,use24Hour:true)
            XCTAssertEqual(value,"00:00")
        }
        XCTAssertTrue(AppLanguage.time(date,language:"ar",zone:.gmt,use24Hour:false).contains("ص"))
    }
    func testSystemLanguageWinsOverOldInAppPreferenceWithoutResettingSettings() throws {
        let name="RecoveryLanguage."+UUID().uuidString
        let store=try XCTUnwrap(UserDefaults(suiteName:name));defer {store.removePersistentDomain(forName:name)}
        var old=Settings();old.language=AppLanguage.current == "ar" ? "en":"ar";old.method="KARACHI";old.hanafi=true;old.onboardingComplete=true
        old.policies["FAJR"]=PrayerPolicy(delivery:.alarm,sound:"athan_default",reminder:10);old.save(to:store)
        let loaded=Settings.load(from:store,legacy:store)
        XCTAssertEqual(loaded.language,AppLanguage.current)
        XCTAssertEqual(loaded.method,old.method);XCTAssertEqual(loaded.hanafi,old.hanafi)
        XCTAssertEqual(loaded.policies,old.policies);XCTAssertTrue(loaded.onboardingComplete)
    }
    @MainActor func testExpiredAndEndedActivitiesCleanWithoutAPrayerRefresh() async throws {
        let name="RecoveryExpiry."+UUID().uuidString
        let store=try XCTUnwrap(UserDefaults(suiteName:name));defer {store.removePersistentDomain(forName:name)}
        let now=Date(),backend=FakePrayerActivities()
        backend.items=[.init(id:"expired",eventID:"fajr",date:now.addingTimeInterval(-1),state:.stale),
                       .init(id:"legacy",eventID:"",date:now.addingTimeInterval(600),state:.active),
                       .init(id:"empty-ended",eventID:"old",date:now.addingTimeInterval(60),state:.ended),
                       .init(id:"pending",eventID:"dhuhr",date:now.addingTimeInterval(3600),state:.pending)]
        let lifecycle=PrayerLiveActivityLifecycle(backend:backend,defaults:store)
        await lifecycle.cleanup(now:now);await lifecycle.cleanup(now:now)
        XCTAssertEqual(Set(backend.ended),Set(["expired","empty-ended","legacy"]));XCTAssertEqual(backend.ended.count,3)
        XCTAssertEqual(backend.items.map(\.id),["pending"])
    }
    @MainActor func testCloseEndsAllCopiesOfOneEventAndSurvivesRelaunch() async throws {
        let name="RecoveryClose."+UUID().uuidString
        let store=try XCTUnwrap(UserDefaults(suiteName:name));defer {store.removePersistentDomain(forName:name)}
        let now=Date(),end=now.addingTimeInterval(120),backend=FakePrayerActivities()
        backend.items=[.init(id:"one",eventID:"fajr",date:end,state:.active),.init(id:"duplicate",eventID:"fajr",date:end,state:.active),.init(id:"later",eventID:"dhuhr",date:end.addingTimeInterval(1000),state:.pending)]
        let lifecycle=PrayerLiveActivityLifecycle(backend:backend,defaults:store)
        await lifecycle.dismiss(eventID:"",now:now);XCTAssertTrue(backend.ended.isEmpty)
        await lifecycle.dismiss(eventID:"fajr",now:now)
        XCTAssertEqual(Set(backend.ended),Set(["one","duplicate"]));XCTAssertEqual(backend.items.map(\.id),["later"])
        let restored=PrayerLiveActivityLifecycle(backend:backend,defaults:store)
        XCTAssertTrue(restored.isDismissed("fajr",now:now));XCTAssertFalse(restored.isDismissed("dhuhr",now:now))
        await restored.cleanup(now:end.addingTimeInterval(1));XCTAssertFalse(restored.isDismissed("fajr",now:end.addingTimeInterval(1)))
    }
    func testFinalActivityContentIsCompleteAndExplicitlyFinished() {
        let state=PrayerActivityAttributes.ContentState(title:"Fajr",date:Date(),eventID:"fajr",language:"en",timeText:"05:03")
        let final=state.finalContent
        XCTAssertTrue(final.state.finished);XCTAssertEqual(final.state.title,"Fajr")
        XCTAssertEqual(final.state.timeText,"05:03");XCTAssertFalse(final.state.endMessage.isEmpty)
        XCTAssertNil(final.staleDate)
    }
    @MainActor func testDismissalObservationSurvivesRemovalButAppReplacementDoesNotSuppress() async throws {
        let name="RecoveryObservation."+UUID().uuidString
        let store=try XCTUnwrap(UserDefaults(suiteName:name));defer {store.removePersistentDomain(forName:name)}
        let backend=FakePrayerActivities(),now=Date(),end=Date().addingTimeInterval(120)
        let lifecycle=PrayerLiveActivityLifecycle(backend:backend,defaults:store)
        // ActivityKit may already have removed this item from its ongoing list.
        lifecycle.recordSystemDismissal(id:"removed",eventID:"fajr",date:end,now:now)
        XCTAssertTrue(lifecycle.isDismissed("fajr",now:now))
        await lifecycle.finish(id:"replacement-old")
        lifecycle.recordSystemDismissal(id:"replacement-old",eventID:"dhuhr",date:end,now:now)
        XCTAssertFalse(lifecycle.isDismissed("dhuhr",now:now))
    }
    @MainActor func testDismissalDuringRetainAwaitPreventsReplacementSubmission() async {
        let item=ScheduledLivePrayer(id:"fajr",title:"Fajr",date:Date().addingTimeInterval(600),start:Date())
        var dismissed=false,submitted=false
        let result=await LiveActivitySubmissionBatch().run([item],skip:{_ in dismissed},retain:{_ in dismissed=true;return false},submit:{_ in submitted=true})
        XCTAssertFalse(submitted);XCTAssertTrue(result.accepted.isEmpty);XCTAssertTrue(result.failures.isEmpty)
    }
}
