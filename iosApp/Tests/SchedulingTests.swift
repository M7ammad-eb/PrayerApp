import XCTest
import ActivityKit
@testable import Salati

@MainActor private final class FakeAlertDelivery:AlertDeliveryBackend {
    var items:[String:NativePendingAlert]=[:]
    var operations:[String]=[]
    var failingIDs=Set<String>()
    var failRead=false
    var failingCancelIDs=Set<String>()
    var notificationAuthorized=true
    func pending() async throws -> [NativePendingAlert] {
        if failRead {throw CocoaError(.fileReadUnknown)}
        return Array(items.values)
    }
    func submit(_ request:NativeAlertRequest) async throws {
        operations.append("submit:"+request.nativeID)
        if failingIDs.contains(request.id) || (request.route == .notification && !notificationAuthorized) {throw CocoaError(.fileWriteOutOfSpace)}
        items[request.nativeID] = .init(id:request.nativeID,route:request.route,date:request.date)
    }
    func cancel(_ pending:NativePendingAlert) async throws {
        operations.append("cancel:"+pending.id)
        if failingCancelIDs.contains(pending.id) {throw CocoaError(.fileWriteUnknown)}
        items.removeValue(forKey:pending.id)
    }
}
@MainActor private final class ScheduleTestGate {
    var continuation:CheckedContinuation<Void,Never>?
    func wait() async {await withCheckedContinuation {continuation=$0}}
    func release() {continuation?.resume();continuation=nil}
}

final class SchedulingTests:XCTestCase {
    func testBackgroundRefreshDoesNotInheritLiveActivityIntentExecution() {
        XCTAssertFalse(SchedulingContext.application.canPrepareLiveActivities(applicationIsActive:false))
        XCTAssertTrue(SchedulingContext.application.canPrepareLiveActivities(applicationIsActive:true))
        XCTAssertTrue(SchedulingContext.liveActivityIntent.canPrepareLiveActivities(applicationIsActive:false))
    }
    private let now=Date(timeIntervalSince1970:1_800_000_000)
    private func request(_ id:String="salati.2027-01-15.FAJR",route:NativeAlertRoute = .alarm,offset:Double=3600,test:Bool=false) -> NativeAlertRequest {
        NativeAlertRequest(id:id,route:route,date:now.addingTimeInterval(offset),prayerDate:now.addingTimeInterval(offset),prayer:"FAJR",title:"Fajr",body:"Prayer time",sound:"default",recording:nil,language:"en",reminder:false,isTest:test)
    }
    @MainActor private func isolatedStore(_ body:(UserDefaults) async throws -> Void) async throws {
        let name="SchedulingTests."+UUID().uuidString
        let defaults=try XCTUnwrap(UserDefaults(suiteName:name))
        defer {defaults.removePersistentDomain(forName:name)}
        try await body(defaults)
    }
    @MainActor func testIncrementalReconcileIsIdempotentAcrossRelaunch() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let item=request()
            let first=AlertReconciler(backend:backend,defaults:defaults)
            let added=await first.reconcile([item],now:now)
            XCTAssertEqual(added.prepared,1);XCTAssertEqual(backend.operations,["submit:"+item.nativeID])
            let restored=AlertReconciler(backend:backend,defaults:defaults)
            let retained=await restored.reconcile([item],now:now.addingTimeInterval(60))
            XCTAssertEqual(retained.prepared,1);XCTAssertEqual(backend.operations.count,1)
        }
    }
    @MainActor func testFailedReplacementRetainsPriorAlarmCoverage() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let old=request();_=await reconciler.reconcile([old],now:now)
            backend.operations=[];backend.failingIDs.insert(old.id)
            var replacement=old;replacement.date=old.date.addingTimeInterval(300);replacement.prayerDate=replacement.date
            let failed=await reconciler.reconcile([replacement],now:now)
            XCTAssertEqual(failed.retainedOnFailure,1);XCTAssertFalse(failed.failures.isEmpty)
            XCTAssertNotNil(backend.items[old.nativeID]);XCTAssertNil(backend.items[replacement.nativeID])
            XCTAssertEqual(reconciler.records[old.id],old)
            XCTAssertFalse(backend.operations.contains(where:{$0.hasPrefix("cancel:")}))
        }
    }
    @MainActor func testSuccessfulReplacementSubmitsBeforeCancellingOldGeneration() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let old=request();_=await reconciler.reconcile([old],now:now);backend.operations=[]
            var replacement=old;replacement.date=old.date.addingTimeInterval(-120);replacement.prayerDate=replacement.date
            let result=await reconciler.reconcile([replacement],now:now)
            XCTAssertTrue(result.failures.isEmpty)
            XCTAssertEqual(Array(backend.operations.prefix(2)),["submit:"+replacement.nativeID,"cancel:"+old.nativeID])
            XCTAssertNotNil(backend.items[replacement.nativeID]);XCTAssertNil(backend.items[old.nativeID])
        }
    }
    @MainActor func testDisablingProductionAndCancellingTestsHaveSeparateIdentities() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let production=request();let test=request("salati.test.alarm",offset:60,test:true)
            _=await reconciler.reconcile([production],now:now)
            _=await reconciler.reconcile([test],now:now,production:false)
            _=await reconciler.reconcile([],now:now)
            XCTAssertNil(backend.items[production.nativeID]);XCTAssertNotNil(backend.items[test.nativeID])
            _=await reconciler.reconcile([production],now:now)
            try await reconciler.cancelTests()
            XCTAssertNotNil(backend.items[production.nativeID]);XCTAssertNil(backend.items[test.nativeID])
        }
    }
    @MainActor func testLegacyMigrationRetainsUnreplacedAlarmWhenOtherSubmissionFails() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let first=request();let later=request("salati.2027-01-15.DHUHR",offset:7200)
            let legacySame=UUID().uuidString,legacyLater=UUID().uuidString
            backend.items[legacySame] = .init(id:legacySame,route:.alarm,date:first.date)
            backend.items[legacyLater] = .init(id:legacyLater,route:.alarm,date:later.date)
            backend.failingIDs.insert(later.id)
            let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let result=await reconciler.reconcile([first,later],now:now)
            XCTAssertFalse(result.failures.isEmpty)
            XCTAssertNil(backend.items[legacySame]);XCTAssertNotNil(backend.items[first.nativeID])
            XCTAssertNotNil(backend.items[legacyLater])
            backend.failingIDs=[]
            _=await reconciler.reconcile([first,later],now:now)
            XCTAssertNil(backend.items[legacyLater]);XCTAssertNotNil(backend.items[later.nativeID])
        }
    }
    @MainActor func testLegacyNotificationBatchRemovedOnlyAfterAcceptedReplacement() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let item=request(route:.notification)
            let oldID=item.id+"."+UUID().uuidString
            backend.items[oldID] = .init(id:oldID,route:.notification,date:item.date)
            backend.failingIDs.insert(item.id)
            let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            _=await reconciler.reconcile([item],now:now)
            XCTAssertNotNil(backend.items[oldID])
            backend.failingIDs=[]
            _=await reconciler.reconcile([item],now:now)
            XCTAssertNil(backend.items[oldID]);XCTAssertNotNil(backend.items[item.nativeID])
        }
    }
    @MainActor func testFailedPendingReadDoesNotCancelOrChangeLedger() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let item=request()
            let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            _=await reconciler.reconcile([item],now:now);backend.operations=[];backend.failRead=true
            let result=await reconciler.reconcile([],now:now)
            XCTAssertFalse(result.failures.isEmpty);XCTAssertTrue(backend.operations.isEmpty)
            XCTAssertEqual(reconciler.records[item.id],item)
        }
    }
    @MainActor func testNotificationUpdatesUseStableIdentifierWithoutCancelGap() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let item=request(route:.notification);_=await reconciler.reconcile([item],now:now);backend.operations=[]
            var changed=item;changed.title="الفجر";changed.language="ar";changed.date=changed.date.addingTimeInterval(60)
            _=await reconciler.reconcile([changed],now:now)
            XCTAssertEqual(changed.nativeID,item.nativeID)
            XCTAssertEqual(backend.operations,["submit:"+item.nativeID]);XCTAssertEqual(backend.items.count,1)
        }
    }
    func testSeparateSoundRolesPreserveOriginalRecordingAndReminderInstants() throws {
        let recording=try XCTUnwrap(Recording.all.first)
        var settings=Settings()
        let prayerDate=now.addingTimeInterval(3600)
        let policy=PrayerPolicy(delivery:.alarm,sound:recording.id,reminder:15)
        let alarm=ScheduledAlert(id:"salati.day.MAGHRIB",prayer:"MAGHRIB",title:"Maghrib",date:prayerDate,prayerDate:prayerDate,policy:policy,reminder:false)
        let request=NativeAlertRequest.make(alarm,settings:settings)
        XCTAssertEqual(request.sound,recording.id+".mp3");XCTAssertEqual(request.route,.alarm)
        let reminder=ScheduledAlert(id:alarm.id+".reminder",prayer:"MAGHRIB",title:"Maghrib",date:prayerDate.addingTimeInterval(-900),prayerDate:prayerDate,policy:policy,reminder:true)
        let reminderRequest=NativeAlertRequest.make(reminder,settings:settings)
        XCTAssertEqual(reminderRequest.sound,"default");XCTAssertNil(reminderRequest.recording)
        XCTAssertEqual(reminderRequest.prayerDate.timeIntervalSince(reminderRequest.date),900)
        settings.reminderSound=false
        XCTAssertEqual(NativeAlertRequest.make(reminder,settings:settings).sound,"silent")
        var notificationPolicy=policy;notificationPolicy.delivery = .notification
        let notification=ScheduledAlert(id:alarm.id,prayer:"MAGHRIB",title:"Maghrib",date:prayerDate,prayerDate:prayerDate,policy:notificationPolicy,reminder:false)
        XCTAssertEqual(NativeAlertRequest.make(notification,settings:settings).sound,"default")
        XCTAssertEqual(NativeAlertRequest.make(notification,settings:settings).recording,recording.id)
    }
    @MainActor func testHorizonUsesEarliestDeliveryApiCoverageNotLongestAlarmHorizon() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let notification=request("salati.day.FAJR",route:.notification,offset:3600)
            let alarm=request("salati.day.ISHA",route:.alarm,offset:14*86400)
            let result=await reconciler.reconcile([notification,alarm],now:now)
            XCTAssertEqual(result.horizon,notification.date)
        }
    }
    @MainActor func testRevokedNotificationPermissionIsNotReportedAsPrepared() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let item=request(route:.notification)
            _=await reconciler.reconcile([item],now:now)
            backend.notificationAuthorized=false;backend.items[item.nativeID]?.authorized=false
            let result=await reconciler.reconcile([item],now:now)
            XCTAssertEqual(result.prepared,0);XCTAssertFalse(result.failures.isEmpty)
            XCTAssertEqual(result.retainedOnFailure,0)
        }
    }
    @MainActor func testCancellationFailureIsVisibleAndRecoveredOnNextReconcile() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let old=request();_=await reconciler.reconcile([old],now:now)
            var updated=old;updated.date=old.date.addingTimeInterval(60)
            backend.failingCancelIDs.insert(old.nativeID)
            let failed=await reconciler.reconcile([updated],now:now)
            XCTAssertFalse(failed.failures.isEmpty)
            XCTAssertNotNil(backend.items[old.nativeID]);XCTAssertNotNil(backend.items[updated.nativeID])
            backend.failingCancelIDs=[]
            let recovered=await reconciler.reconcile([updated],now:now)
            XCTAssertTrue(recovered.failures.isEmpty)
            XCTAssertNil(backend.items[old.nativeID]);XCTAssertNotNil(backend.items[updated.nativeID])
        }
    }
    @MainActor func testSchedulingQueueSerializesAcrossSuspension() async {
        let queue=SerialSchedulingQueue(),gate=ScheduleTestGate()
        var events:[String]=[]
        let first=Task {await queue.enqueue {events.append("first started");await gate.wait();events.append("first finished");return "first"}}
        while gate.continuation == nil {await Task.yield()}
        let second=Task {await queue.enqueue {events.append("second started");return "second"}}
        await Task.yield()
        XCTAssertEqual(events,["first started"])
        gate.release()
        _=await first.value;_=await second.value
        XCTAssertEqual(events,["first started","first finished","second started"])
    }
    @MainActor func testSchedulingQueueSkipsSupersededWaitingRevision() async {
        let queue=SerialSchedulingQueue(),gate=ScheduleTestGate()
        var events:[String]=[]
        let blocker=Task {await queue.enqueue {await gate.wait();return ""}}
        while gate.continuation == nil {await Task.yield()}
        let old=Task {await queue.latest {events.append("obsolete");return "old"}}
        await Task.yield()
        let new=Task {await queue.latest {events.append("latest");return "new"}}
        await Task.yield()
        gate.release()
        _=await blocker.value;_=await old.value;_=await new.value
        XCTAssertEqual(events,["latest"])
    }
    @MainActor func testPrayerBoundaryReplenishmentDoesNotStopAlertingAudio() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let item=request(offset:60)
            _=await reconciler.reconcile([item],now:now)
            backend.items[item.nativeID]?.isAlerting=true;backend.operations=[]
            _=await reconciler.reconcile([],now:now.addingTimeInterval(65))
            XCTAssertTrue(backend.operations.isEmpty);XCTAssertNotNil(backend.items[item.nativeID])
            XCTAssertNotNil(reconciler.records[item.id])
            backend.items.removeValue(forKey:item.nativeID)
            _=await reconciler.reconcile([],now:now.addingTimeInterval(120))
            XCTAssertNil(reconciler.records[item.id])
        }
    }
    @MainActor func testNotificationFailureDoesNotLeaveDuplicateLegacyAlarmAfterSuccessfulAlarmMigration() async throws {
        try await isolatedStore {defaults in
            let backend=FakeAlertDelivery();let reconciler=AlertReconciler(backend:backend,defaults:defaults)
            let alarm=request();let notification=request("salati.day.ASR",route:.notification,offset:7200)
            let legacy=UUID().uuidString
            backend.items[legacy] = .init(id:legacy,route:.alarm,date:alarm.date.addingTimeInterval(120))
            backend.failingIDs.insert(notification.id)
            let result=await reconciler.reconcile([alarm,notification],now:now)
            XCTAssertFalse(result.failures.isEmpty)
            XCTAssertNotNil(backend.items[alarm.nativeID]);XCTAssertNil(backend.items[legacy])
        }
    }
    @MainActor func testLiveBatchStopsAtCapacityButRetainsLaterExistingActivity() async {
        let desired=(0..<75).map {index in ScheduledLivePrayer(id:"salati.\(index).FAJR.live",title:"Fajr",date:now.addingTimeInterval(Double(index+1)*3600),start:now.addingTimeInterval(Double(index+1)*3600-900))}
        var attempts:[String]=[]
        let result=await LiveActivitySubmissionBatch().run(desired,retain:{$0.id == desired[70].id},submit:{item in
            attempts.append(item.id)
            if attempts.count == 2 {throw ActivityAuthorizationError.targetMaximumExceeded}
        })
        XCTAssertEqual(attempts,[desired[0].id,desired[1].id])
        XCTAssertEqual(result.accepted.map(\.id),[desired[0].id,desired[70].id])
        XCTAssertTrue(result.blocked);XCTAssertEqual(result.failures.count,1)
        XCTAssertEqual(desired.filter {item in !result.accepted.contains(where:{$0.id == item.id})}.count,73)
    }
    @MainActor func testLiveBatchStopsForDenialButContinuesAfterOneBadPayload() async {
        let desired=(0..<3).map {index in ScheduledLivePrayer(id:"salati.\(index).live",title:"Fajr",date:now.addingTimeInterval(3600),start:now.addingTimeInterval(2700))}
        var attempts=0
        let denied=await LiveActivitySubmissionBatch().run(desired,retain:{_ in false},submit:{_ in attempts+=1;throw ActivityAuthorizationError.denied})
        XCTAssertEqual(attempts,1);XCTAssertTrue(denied.blocked);XCTAssertTrue(denied.accepted.isEmpty)
        attempts=0
        let payload=await LiveActivitySubmissionBatch().run(desired,retain:{_ in false},submit:{_ in attempts+=1;if attempts == 1 {throw ActivityAuthorizationError.attributesTooLarge}})
        XCTAssertEqual(attempts,3);XCTAssertFalse(payload.blocked);XCTAssertEqual(payload.accepted.count,2)
    }
}
