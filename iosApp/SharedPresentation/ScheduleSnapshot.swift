import Foundation
import ActivityKit

struct SnapshotEvent: Codable, Identifiable, Equatable {
    let id: String
    let title: String
    let date: Date
    var timeText: String = ""
}
struct SnapshotDay: Codable {
    let dateISO: String
    let start: Date
    let end: Date
    let hijriEnglish: String
    let hijriArabic: String
    let events: [SnapshotEvent]
}
struct SnapshotMoment: Codable {
    var date: Date
    let civilDateISO: String
    let religiousDateISO: String
    let hijriEnglish: String
    let hijriArabic: String
    let currentEventID: String?
    let nextEventID: String?
    var displayEventIDs: [String]? = nil
    var tomorrowEventID: String? = nil
    var previousEventID: String? = nil
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
    var days: [SnapshotDay] = []
    var moments: [SnapshotMoment] = []
    var zone: String = "UTC"
    init(city:String,events:[SnapshotEvent],validUntil:Date,language:String,use24Hour:Bool,
         days:[SnapshotDay]=[],moments:[SnapshotMoment]=[],zone:String="UTC") {
        self.city=city;self.events=events;self.validUntil=validUntil;self.language=language
        self.use24Hour=use24Hour;self.days=days;self.moments=moments;self.zone=zone
    }
    enum CodingKeys:String,CodingKey {case city,events,validUntil,language,use24Hour,days,moments,zone}
    init(from decoder:Decoder) throws {
        let c=try decoder.container(keyedBy:CodingKeys.self)
        city=try c.decode(String.self,forKey:.city);events=try c.decode([SnapshotEvent].self,forKey:.events)
        validUntil=try c.decode(Date.self,forKey:.validUntil);language=try c.decode(String.self,forKey:.language)
        use24Hour=try c.decode(Bool.self,forKey:.use24Hour)
        days=try c.decodeIfPresent([SnapshotDay].self,forKey:.days) ?? []
        moments=try c.decodeIfPresent([SnapshotMoment].self,forKey:.moments) ?? []
        zone=try c.decodeIfPresent(String.self,forKey:.zone) ?? "UTC"
    }
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
        // Version-1 files still decode, but need one app refresh to obtain shared boundary decisions.
        guard let id=moment(at:date)?.nextEventID else {return nil}
        return events.first {$0.id == id && $0.date>date}
    }
    func moment(at date:Date) -> SnapshotMoment? {
        guard date<validUntil else {return nil}
        return moments.last {$0.date<=date}
    }
    func day(at date:Date) -> SnapshotDay? {
        if let civil=moment(at:date)?.civilDateISO {return days.first {$0.dateISO == civil}}
        return days.first {$0.start<=date && date<$0.end}
    }
    func displayEvents(at date:Date) -> [SnapshotEvent] {
        guard let ids=moment(at:date)?.displayEventIDs else {return day(at:date)?.events ?? []}
        let byID=Dictionary(events.map {($0.id,$0)},uniquingKeysWith:{first,_ in first})
        return ids.compactMap {byID[$0]}
    }
}
struct PrayerActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var title: String
        var date: Date
        var eventID: String = ""
        var language: String = "en"
        var start: Date? = nil
        var timeText: String = ""
        var endMessage: String = ""
        var finished:Bool = false
        init(title:String,date:Date,eventID:String="",language:String="en",start:Date?=nil,timeText:String="",endMessage:String="") {
            self.title=title;self.date=date;self.eventID=eventID;self.language=language
            self.start=start;self.timeText=timeText;self.endMessage=endMessage
        }
        enum CodingKeys:String,CodingKey {case title,date,eventID,language,start,timeText,endMessage,finished}
        init(from decoder:Decoder) throws {
            let c=try decoder.container(keyedBy:CodingKeys.self)
            title=try c.decode(String.self,forKey:.title);date=try c.decode(Date.self,forKey:.date)
            eventID=try c.decodeIfPresent(String.self,forKey:.eventID) ?? ""
            language=try c.decodeIfPresent(String.self,forKey:.language) ?? "en"
            start=try c.decodeIfPresent(Date.self,forKey:.start)
            timeText=try c.decodeIfPresent(String.self,forKey:.timeText) ?? ""
            endMessage=try c.decodeIfPresent(String.self,forKey:.endMessage) ?? ""
            finished=try c.decodeIfPresent(Bool.self,forKey:.finished) ?? false
        }
    }
    var city: String
}
