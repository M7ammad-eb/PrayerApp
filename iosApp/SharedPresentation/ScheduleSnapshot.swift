import Foundation
import ActivityKit

struct SnapshotEvent: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let date: Date
    var timeText: String = ""
}
// AltStore records signed groups in ALTAppGroups. iLoader instead derives the group
// from the rewritten parent app ID. Both routes still require signed entitlement access.
// Both executables must derive the parent app ID, rather than use the widget ID.
// containerURL still enforces the groups actually authorized by the signature.
struct SharedScheduleStorage {
    static let declaredGroup = "group.com.salati.prayertimes"
    static let originalAppIdentifier = "com.salati.prayertimes.ios"
    let files: [URL]

    static func groups(bundleIdentifier: String?, signedGroups: [String] = []) -> [String] {
        // Ignore other groups AltStore may include for its own backup services.
        var candidates=signedGroups.filter {$0 == declaredGroup || $0.hasPrefix(declaredGroup+".")}
        if var appID=bundleIdentifier, !appID.isEmpty {
            if appID.hasSuffix(".widgets") {appID.removeLast(".widgets".count)}
            if appID != originalAppIdentifier {candidates.append("group."+appID)}
        }
        candidates.append(declaredGroup)
        return candidates.reduce(into:[]) {if !$0.contains($1) {$0.append($1)}}
    }
    init(bundleIdentifier: String?, signedGroups: [String] = [], containerURL: (String)->URL?) {
        files=Self.groups(bundleIdentifier:bundleIdentifier,signedGroups:signedGroups).compactMap {
            containerURL($0)?.appendingPathComponent("schedule-v1.json")
        }
    }
    static var current: Self {
        Self(bundleIdentifier:Bundle.main.bundleIdentifier,signedGroups:Bundle.main.object(forInfoDictionaryKey:"ALTAppGroups") as? [String] ?? []) {
            FileManager.default.containerURL(forSecurityApplicationGroupIdentifier:$0)
        }
    }
}

struct ScheduleSnapshot: Codable {
    let city: String
    let events: [SnapshotEvent]
    let validUntil: Date
    let language: String
    let use24Hour: Bool
    func save(storage: SharedScheduleStorage = .current) throws {
        let data=try JSONEncoder().encode(self)
        var failure: Error = CocoaError(.fileNoSuchFile)
        for file in storage.files {
            do {try data.write(to:file,options:.atomic);return}
            catch {failure=error}
        }
        throw failure
    }
    static func load(storage: SharedScheduleStorage = .current) -> ScheduleSnapshot? {
        for file in storage.files {
            if let data=try? Data(contentsOf:file),let snapshot=try? JSONDecoder().decode(Self.self,from:data) {return snapshot}
        }
        return nil
    }
    func nextEvent(after date: Date) -> SnapshotEvent? {
        guard date<validUntil else {return nil}
        return events.filter {$0.date>date}.min {$0.date<$1.date}
    }
}
struct PrayerActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable { var title: String; var date: Date }
    var city: String
}
