import XCTest
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
}
