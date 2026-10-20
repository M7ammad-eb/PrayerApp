import XCTest
import SalatiCore
@testable import Salati

final class RefinementTests:XCTestCase {
    func testLegacyCalculationChoicesAndOnboardingStateSurviveUpdate() throws {
        for completed in [false,true] {
            var old=Settings();old.method="KARACHI";old.hanafi=true;old.language="ar";old.onboardingComplete=completed
            old.hijriOffset = -2;old.hijriAnchor="1448-11"
            old.policies["FAJR"]=PrayerPolicy(delivery:.alarm,sound:"athan_default",reminder:15)
            var object=try XCTUnwrap(JSONSerialization.jsonObject(with:JSONEncoder().encode(old)) as? [String:Any])
            object.removeValue(forKey:"calculationMethodOverridden");object.removeValue(forKey:"asrOverridden")
            let migrated=try JSONDecoder().decode(Settings.self,from:JSONSerialization.data(withJSONObject:object))
            XCTAssertEqual(migrated.onboardingComplete,completed)
            XCTAssertEqual(migrated.method,old.method);XCTAssertEqual(migrated.hanafi,old.hanafi)
            XCTAssertEqual(migrated.language,old.language);XCTAssertEqual(migrated.policies,old.policies)
            XCTAssertEqual(migrated.hijriOffset,old.hijriOffset);XCTAssertEqual(migrated.hijriAnchor,old.hijriAnchor)
            XCTAssertTrue(migrated.calculationMethodOverridden);XCTAssertTrue(migrated.asrOverridden)
        }
    }
    func testNewSetupAutomaticFlagsSurviveSaveAndReload() throws {
        let fresh=try JSONDecoder().decode(Settings.self,from:JSONEncoder().encode(Settings()))
        XCTAssertFalse(fresh.calculationMethodOverridden);XCTAssertFalse(fresh.asrOverridden)
        let recommended=OnboardingDefaults.shared.resolve(zone:"Asia/Riyadh",currentMethod:fresh.method,currentHanafi:fresh.hanafi,methodOverridden:false,asrOverridden:false,completed:false)
        XCTAssertEqual(recommended.method,"UMM_AL_QURA");XCTAssertFalse(recommended.hanafi)
        let retained=OnboardingDefaults.shared.resolve(zone:"Asia/Riyadh",currentMethod:"ISNA",currentHanafi:true,methodOverridden:true,asrOverridden:true,completed:false)
        XCTAssertEqual(retained.method,"ISNA");XCTAssertTrue(retained.hanafi)
    }
    func testOvernightSnapshotRowsAndNextIdentityAgree() throws {
        let now=Date(timeIntervalSince1970:1000)
        let past=SnapshotEvent(id:"2026-10-19.FAJR",title:"Fajr",date:now.addingTimeInterval(-3600),timeText:"05:00")
        let next=SnapshotEvent(id:"2026-10-20.FAJR",title:"Fajr",date:now.addingTimeInterval(3600),timeText:"05:01")
        let moment=SnapshotMoment(date:now,civilDateISO:"2026-10-19",religiousDateISO:"2026-10-20",hijriEnglish:"",hijriArabic:"",currentEventID:nil,nextEventID:next.id,displayEventIDs:[next.id],tomorrowEventID:next.id)
        let source=ScheduleSnapshot(city:"Madinah",events:[past,next],validUntil:now.addingTimeInterval(7200),language:"ar",use24Hour:true,moments:[moment])
        let loaded=try JSONDecoder().decode(ScheduleSnapshot.self,from:JSONEncoder().encode(source))
        XCTAssertEqual(loaded.displayEvents(at:now),[next])
        XCTAssertEqual(loaded.nextEvent(after:now),next)
        XCTAssertEqual(loaded.moment(at:now)?.tomorrowEventID,next.id)
        let entry=PrayerEntry(date:now,snapshot:loaded)
        XCTAssertEqual(entry.schedule,[next])
        XCTAssertEqual(entry.next,next)
    }
    func testPreviousSnapshotStillDecodesAndUsesCivilRows() throws {
        let now=Date();let event=SnapshotEvent(id:"today.FAJR",title:"Fajr",date:now.addingTimeInterval(600))
        let moment=SnapshotMoment(date:now,civilDateISO:"today",religiousDateISO:"today",hijriEnglish:"",hijriArabic:"",currentEventID:nil,nextEventID:event.id)
        let day=SnapshotDay(dateISO:"today",start:now,end:now.addingTimeInterval(3600),hijriEnglish:"",hijriArabic:"",events:[event])
        let source=ScheduleSnapshot(city:"Madinah",events:[event],validUntil:day.end,language:"ar",use24Hour:true,days:[day],moments:[moment])
        let loaded=try JSONDecoder().decode(ScheduleSnapshot.self,from:JSONEncoder().encode(source))
        XCTAssertEqual(loaded.displayEvents(at:now),[event]);XCTAssertEqual(loaded.nextEvent(after:now),event)
    }
    func testNativeCalendarBridgeHasRequiredWeeksAndWesternISO() throws {
        let engine=SalatiEngine()
        for offset in -2...2 {
            let page=try engine.calendarMonth(year:1448,month:12,storedOffset:Int32(offset),anchor:"1448-12")
            XCTAssertEqual(page.reservedWeekCount,6);XCTAssertEqual(page.cells.count,Int(page.weekCount)*7)
            let days=page.cells.filter(\.inMonth)
            XCTAssertEqual(days.first?.day,1);XCTAssertEqual(days.count,30)
            XCTAssertEqual(Set(days.map(\.dateISO)).count,30)
            XCTAssertTrue(days.allSatisfy {$0.dateISO.range(of:"^[0-9]{4}-[0-9]{2}-[0-9]{2}$",options:.regularExpression) != nil})
        }
    }
}
