import Foundation
import ActivityKit

struct SnapshotEvent: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let date: Date
}
struct ScheduleSnapshot: Codable {
    static let group = "group.com.salati.prayertimes"
    let city: String
    let events: [SnapshotEvent]
    let validUntil: Date
    let language: String
    let use24Hour: Bool
    static var file: URL? { FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group)?.appendingPathComponent("schedule-v1.json") }
    func save() throws {
        guard let file = Self.file else { throw CocoaError(.fileNoSuchFile) }
        try JSONEncoder().encode(self).write(to:file,options:.atomic)
    }
    static func load() -> ScheduleSnapshot? {
        guard let file, let data = try? Data(contentsOf:file) else { return nil }
        return try? JSONDecoder().decode(Self.self,from:data)
    }
}
struct PrayerActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable { var title: String; var date: Date }
    var city: String
}
