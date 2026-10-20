import ActivityKit
import Foundation

struct PrayerActivityRecord {
    let id:String
    let eventID:String
    let date:Date
    let state:ActivityState
}
@MainActor protocol PrayerActivityBackend {
    func records()->[PrayerActivityRecord]
    func finish(id:String) async
}
@MainActor struct SystemPrayerActivityBackend:PrayerActivityBackend {
    func records()->[PrayerActivityRecord] {
        Activity<PrayerActivityAttributes>.activities.map {
            .init(id:$0.id,eventID:$0.content.state.eventID,date:$0.content.state.date,state:$0.activityState)
        }
    }
    func finish(id:String) async {
        guard let activity=Activity<PrayerActivityAttributes>.activities.first(where:{$0.id == id}) else {return}
        await activity.end(activity.content.state.finalContent,dismissalPolicy:.immediate)
    }
}

// Dismissal is a lifecycle operation, independent of calculations, widgets and
// alert replenishment. It can run immediately on launch or from a system button.
@MainActor final class PrayerLiveActivityLifecycle {
    private let backend:any PrayerActivityBackend
    private let defaults:UserDefaults
    private var dismissed:[String:Date]
    private var appEndedIDs=Set<String>()
    init(backend:any PrayerActivityBackend,defaults:UserDefaults) {
        self.backend=backend;self.defaults=defaults
        dismissed=defaults.data(forKey:"activities.dismissed.v1").flatMap {try? JSONDecoder().decode([String:Date].self,from:$0)} ?? [:]
    }
    private func persist() {
        if let data=try? JSONEncoder().encode(dismissed) {defaults.set(data,forKey:"activities.dismissed.v1")}
    }
    func isDismissed(_ eventID:String,now:Date)->Bool {(dismissed[eventID] ?? .distantPast)>now}
    func recordSystemDismissal(id:String,eventID:String,date:Date,now:Date=Date()) {
        guard !appEndedIDs.contains(id),!eventID.isEmpty,date>now else {return}
        dismissed[eventID]=date;persist()
    }
    func finish(id:String) async {
        appEndedIDs.insert(id)
        await backend.finish(id:id)
    }
    func dismiss(eventID:String,now:Date=Date()) async {
        guard !eventID.isEmpty else {return}
        let matches=backend.records().filter {$0.eventID == eventID}
        if let until=matches.map(\.date).max(),until>now {dismissed[eventID]=until;persist()}
        for record in matches {await finish(id:record.id)}
    }
    func cleanup(now:Date=Date(),enabled:Bool=true) async {
        dismissed=dismissed.filter {$0.value>now};persist()
        for record in backend.records() {
            if record.state == .dismissed && record.date>now {
                recordSystemDismissal(id:record.id,eventID:record.eventID,date:record.date,now:now)
            }
            if !enabled || record.date<=now || record.state == .ended || record.state == .dismissed || isDismissed(record.eventID,now:now) {
                await finish(id:record.id)
            }
        }
    }
    var nextExpiry:Date? {
        backend.records().filter {$0.state != .ended && $0.state != .dismissed && $0.date>Date()}.map(\.date).min()
    }
}
