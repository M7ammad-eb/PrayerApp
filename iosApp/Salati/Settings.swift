import Foundation

struct SavedLocation: Codable, Equatable {
    var name: String
    var arabicName: String?
    var latitude: Double
    var longitude: Double
    var zone: String
    var isGPS = false
    var nearestDistance: Double?
}
enum Delivery: String, Codable, CaseIterable { case off, notification, alarm }
struct PrayerPolicy: Codable, Equatable {
    var delivery: Delivery = .off
    var sound = "default"
    var reminder = 0
}
struct Settings: Codable {
    var location: SavedLocation?
    var method = "UMM_AL_QURA"
    var hanafi = false
    var highLatitude = "ANGLE_BASED"
    var adjustments = [Int](repeating: 0, count: 6)
    var hijriOffset = 0
    var hijriAnchor: String?
    var language = "en"
    var appearance = "system"
    var use24Hour = false
    var onboardingComplete = false
    var policies = Dictionary(uniqueKeysWithValues: ["FAJR","DHUHR","ASR","MAGHRIB","ISHA"].map { ($0, PrayerPolicy()) })
    static let defaults = UserDefaults(suiteName: ScheduleSnapshot.group) ?? .standard
    static func load() -> Settings {
        guard let data = defaults.data(forKey: "settings.v1"), let value = try? JSONDecoder().decode(Settings.self,from:data), value.adjustments.count == 6 else { return Settings() }
        return value
    }
    func save() { if let data = try? JSONEncoder().encode(self) { Self.defaults.set(data,forKey:"settings.v1") } }
}
struct Recording: Identifiable, Decodable {
    let id: String
    let english: String
    let arabic: String
    static let all: [Recording] = {
        guard let url=Bundle.main.url(forResource:"Recordings",withExtension:"json"),let data=try? Data(contentsOf:url),let values=try? JSONDecoder().decode([Recording].self,from:data) else {return []}
        return values
    }()
}
