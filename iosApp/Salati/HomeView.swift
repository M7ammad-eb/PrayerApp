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
                        HStack {
                            Label("Up next",systemImage:"clock").font(.subheadline.weight(.medium)).foregroundStyle(Color.accentColor)
                            Spacer()
                            if !day.events.contains(where:{$0.epochSeconds == day.next.epochSeconds}) {Text("Tomorrow").font(.caption.weight(.semibold)).foregroundStyle(.secondary)}
                        }
                        ViewThatFits(in:.horizontal) {
                            HStack(alignment:.firstTextBaseline) { Text(model.title(day.next)).font(.largeTitle.weight(.semibold)); Spacer(); Text(model.time(day.next.epochSeconds)).font(.title.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                            VStack(alignment:.leading) { Text(model.title(day.next)).font(.largeTitle.weight(.semibold)); Text(model.time(day.next.epochSeconds)).font(.title).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                        }
                        if Date(timeIntervalSince1970:day.next.epochSeconds)>Date() {
                            Text(timerInterval:Date()...Date(timeIntervalSince1970:day.next.epochSeconds),countsDown:true)
                                .font(.system(.largeTitle,design:.rounded,weight:.light)).monospacedDigit().environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                .environment(\.layoutDirection,.leftToRight).accessibilityLabel(Text("Time until next prayer"))
                        }
                        Text(model.arabic ? day.hijriArabic : day.hijriEnglish).font(.subheadline).foregroundStyle(.secondary)
                        if !day.currentEnded && !day.events.contains(where:{$0.epochSeconds == day.current.epochSeconds}) {
                            HStack {
                                Text("Current prayer")
                                Text(model.title(day.current))
                                Text("Previous day")
                            }.font(.caption).foregroundStyle(.secondary).accessibilityElement(children:.combine)
                        }
                    }.padding(.vertical,12)
                }.listRowBackground(Color.accentColor.opacity(0.09))
                Section(model.displayDate(Date())) {
                    ForEach(day.events,id:\.id) { event in
                        HStack(spacing:14) {
                            Image(systemName:symbol(event.id)).foregroundStyle(event.epochSeconds == day.next.epochSeconds ? Color.accentColor : (event.epochSeconds == day.current.epochSeconds && !day.currentEnded ? Color.primary:Color.secondary)).frame(width:28).accessibilityHidden(true)
                            VStack(alignment:.leading,spacing:3) {
                                Text(model.title(event)).fontWeight(event.epochSeconds == day.next.epochSeconds ? .semibold : .regular)
                                if event.epochSeconds == day.current.epochSeconds && !day.currentEnded {Label("Current prayer",systemImage:"circle.fill").font(.caption).foregroundStyle(.primary)}
                                else if event.epochSeconds == day.next.epochSeconds {Label("Up next",systemImage:"circle.fill").font(.caption).foregroundStyle(Color.accentColor)}
                            }
                            Spacer(minLength:8)
                            Text(model.time(event.epochSeconds)).monospacedDigit().environment(\.layoutDirection,.leftToRight).fontWeight(event.epochSeconds == day.next.epochSeconds ? .semibold : .regular)
                        }.foregroundStyle(event.epochSeconds == day.next.epochSeconds ? Color.accentColor:(event.epochSeconds == day.current.epochSeconds && !day.currentEnded ? Color.primary:Color.secondary)).padding(.vertical,6).accessibilityElement(children:.combine)
                    }
                }
                if !day.events.contains(where:{$0.epochSeconds == day.next.epochSeconds}) {
                    Section("Tomorrow") {
                        HStack {
                            Label(model.title(day.next),systemImage:symbol(day.next.id))
                            Spacer()
                            Text(model.time(day.next.epochSeconds)).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                        }.fontWeight(.semibold).foregroundStyle(Color.accentColor)
                        Text("Up next").font(.caption).foregroundStyle(Color.accentColor)
                    }
                }
                Section {
                    LabeledContent("Islamic midnight",value:model.time(day.midnightEpoch))
                    LabeledContent("Last third",value:model.time(day.lastThirdEpoch))
                } header: {Text("The night")}
                Section {
                    NavigationLink("Notifications and adhan") { NotificationSettingsView() }
                } footer: { Text("Prayer countdowns start automatically when enabled. Open Salati regularly to replenish future alerts.") }
                #if SALATI_APP
                if ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase") {
                    Section("Presentation previews") {
                        NavigationLink("Widget and activity previews") { Phase2ShowcaseView() }.accessibilityIdentifier("presentationShowcase")
                    }
                }
                #endif
            } else { ContentUnavailableView("Choose your location",systemImage:"location",description:Text("Prayer times are calculated entirely on your device.")) }
            if model.scheduleStatus != nil {
                Section {NavigationLink {NotificationSettingsView()} label:{Label("Review your alert settings",systemImage:"bell.badge")}}
            }
        }
        .navigationTitle("Salati").navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if audio.isPlaying {ToolbarItem(placement:.topBarLeading) {Button("Stop playback",systemImage:"stop.fill") {audio.stop()}}}
            ToolbarItem(placement:.topBarTrailing) { Button {showPlaces=true} label: { Label(model.settings.location == nil ? (model.arabic ? "الموقع" : "Location") : model.locationDisplayName,systemImage:"location").labelStyle(.titleAndIcon).lineLimit(1) } }
        }
        .sheet(isPresented:$showPlaces) { NavigationStack { PlacesView() } }
        .task(id:model.day?.next.epochSeconds) {
            guard let time=model.day?.next.epochSeconds else { return }
            do { try await Task.sleep(for:.seconds(max(1,time-Date().timeIntervalSince1970+1))); model.queueRefresh() } catch { }
        }
    }
    func symbol(_ id: String) -> String {
        switch id { case "FAJR": "sun.horizon"; case "SUNRISE": "sunrise"; case "DHUHR": "sun.max"; case "ASR": "sun.haze"; case "MAGHRIB": "sunset"; default: "moon.stars" }
    }
}
