import AppIntents
import ActivityKit

// Siri, Spotlight, the Shortcuts app and the Action button can ask for the next prayer or start
// the countdown without opening Salati first (the countdown does need the app for a moment).
struct NextPrayerIntent: AppIntent {
    static var title: LocalizedStringResource = "Next prayer"
    static var description = IntentDescription("Tells you the next prayer and how long until it.")

    func perform() async throws -> some IntentResult & ProvidesDialog {
        guard let snapshot=ScheduleSnapshot.load(),let next=snapshot.nextEvent(after:Date()) else {
            return .result(dialog:IntentDialog(stringLiteral:AppLanguage.text("Open Salati to load your prayer times.",language:AppLanguage.current)))
        }
        let language=snapshot.language
        var calendar=Calendar(identifier:.gregorian);calendar.locale=AppLanguage.locale(language)
        let formatter=DateComponentsFormatter()
        formatter.calendar=calendar;formatter.unitsStyle = .full;formatter.allowedUnits=[.hour,.minute];formatter.maximumUnitCount=2
        let remaining=formatter.string(from:max(60,next.date.timeIntervalSinceNow)) ?? ""
        let text=String(format:AppLanguage.text("%@ is at %@, in %@.",language:language),next.title,next.timeText,remaining)
        return .result(dialog:IntentDialog(stringLiteral:text))
    }
}

struct StartPrayerCountdownIntent: AppIntent {
    static var title: LocalizedStringResource = "Start prayer countdown"
    static var description = IntentDescription("Shows the countdown to the next prayer in the Dynamic Island and on the Lock Screen.")
    static var openAppWhenRun = true

    @MainActor func perform() async throws -> some IntentResult & ProvidesDialog {
        let key:String
        switch await PrayerScheduler.shared.startNextPrayerCountdown() {
        case .started:key="Countdown started."
        case .activitiesOff:key="Live Activities are turned off for Salati."
        case .noSchedule:key="Open Salati to load your prayer times."
        case .failed:key="The countdown couldn't start. Open Salati and try again."
        }
        return .result(dialog:IntentDialog(stringLiteral:AppLanguage.text(key,language:AppLanguage.current)))
    }
}

struct SalatiShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(intent:NextPrayerIntent(),
                    phrases:["When is the next prayer in \(.applicationName)","Next prayer in \(.applicationName)"],
                    shortTitle:"Next prayer",systemImageName:"clock")
        AppShortcut(intent:StartPrayerCountdownIntent(),
                    phrases:["Start the prayer countdown in \(.applicationName)"],
                    shortTitle:"Start prayer countdown",systemImageName:"timer")
    }
}
