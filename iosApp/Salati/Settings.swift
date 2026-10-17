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
struct Recording: Identifiable {
    let id: String
    let name: String
    static let all: [Recording] = [
        .init(id:"athan_default",name:"Default"),
        .init(id:"athan_egypt_ahmad_dawod",name:"Egypt Ahmad Dawod"),
        .init(id:"athan_egypt_alhusain_sayed_abdulaati",name:"Egypt Alhusain Sayed Abdulaati"),
        .init(id:"athan_egypt_salah_alalfi",name:"Egypt Salah Alalfi"),
        .init(id:"athan_fajr1_kwait_mashary_alafasy",name:"Fajr1 Kwait Mashary Alafasy"),
        .init(id:"athan_fajr2_jordan_kamel_allala",name:"Fajr2 Jordan Kamel Allala"),
        .init(id:"athan_iraq_abu_omar_alamouri",name:"Iraq Abu Omar Alamouri"),
        .init(id:"athan_makkah_ali_bin_ahmad_mula",name:"Makkah Ali Bin Ahmad Mula"),
        .init(id:"athan_palastine_quds_nagi_qazaz_1",name:"Palastine Quds Nagi Qazaz 1"),
        .init(id:"athan_qatar_saleh_alnabet",name:"Qatar Saleh Alnabet"),
        .init(id:"athan_riyadh_naser_alqatami",name:"Riyadh Naser Alqatami")
    ]
}
