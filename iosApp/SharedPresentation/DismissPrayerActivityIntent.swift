import AppIntents
import ActivityKit

struct DismissPrayerActivityIntent:LiveActivityIntent {
    static var title:LocalizedStringResource="Close countdown"
    static var openAppWhenRun=false
    static var isDiscoverable=false
    @Parameter(title:"Prayer event") var eventID:String
    init() {eventID=""}
    init(eventID:String) {self.eventID=eventID}
    func perform() async throws -> some IntentResult {
        #if SALATI_APP
        await PrayerScheduler.shared.dismissLiveActivity(eventID:eventID)
        #endif
        return .result()
    }
}

extension PrayerActivityAttributes.ContentState {
    var finalContent:ActivityContent<Self> {
        var final=self
        final.finished=true
        if date>Date() {final.endMessage=AppLanguage.text("Countdown closed",language:language)}
        else if final.endMessage.isEmpty {final.endMessage=AppLanguage.text("It is time for prayer",language:language)}
        return ActivityContent(state:final,staleDate:nil,relevanceScore:0)
    }
}
