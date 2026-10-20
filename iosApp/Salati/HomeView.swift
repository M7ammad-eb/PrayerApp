import SwiftUI
import SalatiCore

struct HomeView: View {
    @EnvironmentObject var model: AppModel
    @State private var showPlaces=false
    @ObservedObject private var audio=AdhanPlayer.shared
    var body: some View {
        List {
            if let day=model.day {
                Section {
                    VStack(alignment:.leading,spacing:16) {
                        Label("Up next",systemImage:"clock").font(.subheadline.weight(.medium)).foregroundStyle(Color.accentColor)
                        if let next=model.presentationSnapshot?.events.first(where:{$0.id == model.nextEventID}) {
                        ViewThatFits(in:.horizontal) {
                            HStack(alignment:.firstTextBaseline) { Text(next.title).font(.largeTitle.weight(.semibold)); Spacer(); Text(next.timeText).font(.title.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                            VStack(alignment:.leading) { Text(next.title).font(.largeTitle.weight(.semibold)); Text(next.timeText).font(.title).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                        }
                        let countdownStart=Date()
                        if next.date>countdownStart {
                            Text(timerInterval:countdownStart...next.date,countsDown:true)
                                .font(.system(.largeTitle,design:.rounded,weight:.light)).monospacedDigit().environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                .environment(\.layoutDirection,.leftToRight).accessibilityLabel(Text("Time until next prayer"))
                        }
                        }
                        Text(model.arabic ? day.hijriArabic : day.hijriEnglish).font(.subheadline).foregroundStyle(.secondary)
                    }.padding(.vertical,12)
                }.listRowBackground(Color.accentColor.opacity(0.09))
                Section("Prayer times") {
                    ForEach(model.displayEvents) { event in
                        let next=event.id == model.nextEventID
                        let current=event.id == model.currentEventID
                        HStack(spacing:14) {
                            Image(systemName:symbol(event.id)).foregroundStyle(next ? Color.accentColor : (current ? Color.primary:Color.secondary)).frame(width:28).accessibilityHidden(true)
                            VStack(alignment:.leading,spacing:3) {
                                Text(event.title).fontWeight(next || current ? .semibold:.regular)
                                if current {Label("Current prayer",systemImage:"circle.fill").font(.caption).foregroundStyle(.primary)}
                                else if next {Label("Up next",systemImage:"circle.fill").font(.caption).foregroundStyle(Color.accentColor)}
                            }
                            Spacer(minLength:8)
                            Text(event.timeText).monospacedDigit().environment(\.layoutDirection,.leftToRight).fontWeight(next || current ? .semibold:.regular)
                        }.foregroundStyle(next ? Color.accentColor:(current ? Color.primary:Color.secondary))
                            .padding(.vertical,7).accessibilityElement(children:.combine).accessibilityIdentifier("homePrayer."+event.id)
                    }
                }
                Section {
                    LabeledContent("Islamic midnight",value:model.time(day.midnightEpoch))
                    LabeledContent("Last third",value:model.time(day.lastThirdEpoch))
                } header: {Text("The night")}
                Section {
                    NavigationLink("Notifications and adhan") { NotificationSettingsView() }
                }
                #if SALATI_APP
                if ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase") {
                    Section("Presentation previews") {
                        NavigationLink("Widget and activity previews") { Phase2ShowcaseView() }.accessibilityIdentifier("presentationShowcase")
                    }
                }
                #endif
            } else { ContentUnavailableView("Choose your location",systemImage:"location",description:Text("Prayer times are calculated entirely on your device.")) }
        }
        .navigationTitle("Salati").navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if audio.isPlaying {ToolbarItem(placement:.topBarLeading) {Button("Stop playback",systemImage:"stop.fill") {audio.stop()}}}
            ToolbarItem(placement:.topBarTrailing) { Button {showPlaces=true} label: { Label(model.settings.location == nil ? (model.arabic ? "الموقع" : "Location") : model.locationDisplayName,systemImage:"location").labelStyle(.titleAndIcon).lineLimit(1) } }
        }
        .sheet(isPresented:$showPlaces) { NavigationStack { PlacesView() } }
        .task(id:model.nextEventID) {
            guard let next=model.presentationSnapshot?.events.first(where:{$0.id == model.nextEventID}) else { return }
            let time=next.date.timeIntervalSince1970
            do { try await Task.sleep(for:.seconds(max(1,time-Date().timeIntervalSince1970+1))); model.queueRefresh() } catch { }
        }
    }
    func symbol(_ id: String) -> String {
        if id.contains("FAJR") {return "sun.horizon"}
        if id.contains("SUNRISE") {return "sunrise"}
        if id.contains("DHUHR") {return "sun.max"}
        if id.contains("ASR") {return "sun.haze"}
        if id.contains("MAGHRIB") {return "sunset"}
        return "moon.stars"
    }
}
