import Foundation

struct SavedLocation: Codable, Equatable {
    var name: String
    var arabicName: String?
    var latitude: Double
    var longitude: Double
    var zone: String
    var isGPS = false
    var nearestDistance: Double?
    var lastUpdated: Date?
    var accuracyMeters: Double?
    var asciiName: String?
}
enum Delivery: String, Codable, CaseIterable { case off, notification, alarm }
struct PrayerPolicy: Codable, Equatable {
    var delivery: Delivery = .off
    var sound = "default"
    var reminder = 0
}
struct Settings: Codable, Equatable {
    var location: SavedLocation?
    var method = "UMM_AL_QURA"
    var hanafi = false
    var calculationMethodOverridden = false
    var asrOverridden = false
    var highLatitude = "ANGLE_BASED"
    var adjustments = [Int](repeating: 0, count: 6)
    var hijriOffset = 0
    var hijriAnchor: String?
    var language = AppLanguage.current
    var appearance = "system"
    var use24Hour = false
    var onboardingComplete = false
    var liveActivitiesEnabled = false
    var reminderSound = true
    var policies = Dictionary(uniqueKeysWithValues: ["FAJR","DHUHR","ASR","MAGHRIB","ISHA"].map { ($0, PrayerPolicy()) })
    init() {}
    enum CodingKeys: String, CodingKey {
        case calculationMethodOverridden,asrOverridden,location,method,hanafi,highLatitude,adjustments,hijriOffset,hijriAnchor,language,appearance,use24Hour,onboardingComplete,policies,liveActivitiesEnabled,reminderSound
    }
    init(from decoder:Decoder) throws {
        self.init()
        let c=try decoder.container(keyedBy:CodingKeys.self)
        location=try c.decodeIfPresent(SavedLocation.self,forKey:.location)
        method=try c.decodeIfPresent(String.self,forKey:.method) ?? method
        hanafi=try c.decodeIfPresent(Bool.self,forKey:.hanafi) ?? hanafi
        highLatitude=try c.decodeIfPresent(String.self,forKey:.highLatitude) ?? highLatitude
        adjustments=try c.decodeIfPresent([Int].self,forKey:.adjustments) ?? adjustments
        hijriOffset=try c.decodeIfPresent(Int.self,forKey:.hijriOffset) ?? hijriOffset
        hijriAnchor=try c.decodeIfPresent(String.self,forKey:.hijriAnchor)
        language=try c.decodeIfPresent(String.self,forKey:.language) ?? language
        appearance=try c.decodeIfPresent(String.self,forKey:.appearance) ?? appearance
        use24Hour=try c.decodeIfPresent(Bool.self,forKey:.use24Hour) ?? use24Hour
        onboardingComplete=try c.decodeIfPresent(Bool.self,forKey:.onboardingComplete) ?? onboardingComplete
        // Legacy persisted choices have no override flags; conservatively preserve them,
        // even when an older setup was not completed. New saves carry explicit flags.
        calculationMethodOverridden=try c.decodeIfPresent(Bool.self,forKey:.calculationMethodOverridden) ?? (onboardingComplete || c.contains(.method))
        asrOverridden=try c.decodeIfPresent(Bool.self,forKey:.asrOverridden) ?? (onboardingComplete || c.contains(.hanafi))
        policies=try c.decodeIfPresent([String:PrayerPolicy].self,forKey:.policies) ?? policies
        liveActivitiesEnabled=try c.decodeIfPresent(Bool.self,forKey:.liveActivitiesEnabled) ?? false
        reminderSound=try c.decodeIfPresent(Bool.self,forKey:.reminderSound) ?? true
    }
    // Widgets consume only ScheduleSnapshot. App preferences must remain available even
    // when a signer changes or removes shared-group access.
    static func load(from defaults: UserDefaults = .standard, legacy: UserDefaults? = nil) -> Settings {
        func decode(_ data: Data?) -> Settings? {
            guard let data,let value=try? JSONDecoder().decode(Settings.self,from:data),value.adjustments.count == 6 else {return nil}
            return value
        }
        if var value=decode(defaults.data(forKey:"settings.v1")) {value.language=AppLanguage.current;return value}
        let previous=legacy ?? UserDefaults(suiteName:SharedScheduleStorage.declaredGroup)
        if var value=decode(previous?.data(forKey:"settings.v1")) {
            value.language=AppLanguage.current
            value.save(to:defaults)
            return value
        }
        return Settings()
    }
    func save(to defaults: UserDefaults = .standard) {
        if let data=try? JSONEncoder().encode(self) {defaults.set(data,forKey:"settings.v1")}
    }
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
