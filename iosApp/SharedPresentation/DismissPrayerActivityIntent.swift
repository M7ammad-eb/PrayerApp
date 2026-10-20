import AppIntents
import ActivityKit
import OSLog

struct DismissPrayerActivityIntent:LiveActivityIntent {
    static var title:LocalizedStringResource="Close countdown"
    static var openAppWhenRun=false
    static var supportedModes:IntentModes = .background
    static var isDiscoverable=false
    @Parameter(title:"Prayer event") var eventID:String
    init() {eventID=""}
    init(eventID:String) {self.eventID=eventID}
    func perform() async throws -> some IntentResult {
        let logger=Logger(subsystem:"com.salati.prayertimes.ios",category:"ActivityClose")
        logger.notice("Close entered in \(Bundle.main.bundleIdentifier ?? "unknown",privacy:.public), event \(eventID,privacy:.public)")
        #if SALATI_APP
        await PrayerScheduler.shared.dismissLiveActivity(eventID:eventID)
        logger.notice("Close completed")
        #else
        // LiveActivityIntent must execute in the application. Never report a
        // successful action if the system unexpectedly routes it to the extension.
        logger.error("Close was incorrectly routed to the widget extension")
        throw CloseRoutingError.applicationRequired
        #endif
        return .result()
    }
}
private enum CloseRoutingError:Error {case applicationRequired}

extension PrayerActivityAttributes.ContentState {
    var finalContent:ActivityContent<Self> {
        var final=self
        final.finished=true
        if date>Date() {final.endMessage=AppLanguage.text("Countdown closed",language:language)}
        else if final.endMessage.isEmpty {final.endMessage=AppLanguage.text("It is time for prayer",language:language)}
        return ActivityContent(state:final,staleDate:nil,relevanceScore:0)
    }
}
