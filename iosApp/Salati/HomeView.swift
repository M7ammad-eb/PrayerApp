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
                        Label("Next prayer",systemImage:"sparkle").font(.subheadline).foregroundStyle(.secondary)
                        ViewThatFits(in:.horizontal) {
                            HStack(alignment:.firstTextBaseline) { Text(model.title(day.next)).font(.largeTitle.weight(.semibold)); Spacer(); Text(model.time(day.next.epochSeconds)).font(.title.weight(.medium)).monospacedDigit() }
                            VStack(alignment:.leading) { Text(model.title(day.next)).font(.largeTitle.weight(.semibold)); Text(model.time(day.next.epochSeconds)).font(.title).monospacedDigit() }
                        }
                        if Date(timeIntervalSince1970:day.next.epochSeconds)>Date() {
                            Text(timerInterval:Date()...Date(timeIntervalSince1970:day.next.epochSeconds),countsDown:true)
                                .font(.system(.largeTitle,design:.rounded,weight:.light)).monospacedDigit().environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                .accessibilityLabel(Text("Time until next prayer"))
                        }
                        Text(model.arabic ? day.hijriArabic : day.hijriEnglish).font(.subheadline).foregroundStyle(.secondary)
                    }.padding(.vertical,12)
                }.listRowBackground(Color.accentColor.opacity(0.09))
                Section(model.displayDate(Date())) {
                    ForEach(day.events,id:\.id) { event in
                        HStack(spacing:14) {
                            Image(systemName:symbol(event.id)).foregroundStyle(event.id == day.next.id ? Color.accentColor : .secondary).frame(width:28).accessibilityHidden(true)
                            VStack(alignment:.leading,spacing:3) {
                                Text(model.title(event)).fontWeight(event.id == day.next.id ? .semibold : .regular)
                                if event.id == day.currentId { Text(day.currentEnded ? "Prayer time ended" : "Current prayer").font(.caption).foregroundStyle(.secondary) }
                            }
                            Spacer(minLength:8)
                            Text(model.time(event.epochSeconds)).monospacedDigit().fontWeight(event.id == day.next.id ? .semibold : .regular)
                        }.padding(.vertical,6).accessibilityElement(children:.combine)
                    }
                }
                Section("The night") {
                    LabeledContent("Islamic midnight",value:model.time(day.midnightEpoch))
                    LabeledContent("Last third",value:model.time(day.lastThirdEpoch))
                }
                Section {
                    Button {startCountdown(day)} label: {Label("Show countdown on Lock Screen",systemImage:"lock.rectangle")}
                    Button("Schedule next prayer countdown") {
                        do {try model.scheduler.scheduleLiveActivity(day:day,city:model.settings.location?.name ?? "",title:model.title(day.next))}
                        catch {model.error=error.localizedDescription}
                    }
                } footer: { Text("The countdown uses system rendering. Open Salati regularly to refresh changing prayer schedules.") }
            } else { ContentUnavailableView("Choose your location",systemImage:"location",description:Text("Prayer times are calculated entirely on your device.")) }
            if let status=model.scheduleStatus { Section { Text(status).font(.footnote).foregroundStyle(.secondary) } }
        }
        .navigationTitle("Salati")
        .toolbar {
            if audio.isPlaying {ToolbarItem(placement:.topBarLeading) {Button("Stop playback",systemImage:"stop.fill") {audio.stop()}}}
            ToolbarItem(placement:.topBarTrailing) { Button {showPlaces=true} label: { Label(model.settings.location.map { model.arabic ? ($0.arabicName ?? $0.name) : $0.name } ?? (model.arabic ? "الموقع" : "Location"),systemImage:"location") } }
        }
        .sheet(isPresented:$showPlaces) { NavigationStack { PlacesView() } }
        .task(id:model.day?.next.epochSeconds) {
            guard let time=model.day?.next.epochSeconds else { return }
            do { try await Task.sleep(for:.seconds(max(1,time-Date().timeIntervalSince1970+1))); model.queueRefresh() } catch { }
        }
    }
    func startCountdown(_ day: PrayerDay) {
        Task {
            do {try await model.scheduler.startLiveActivity(day:day,city:model.settings.location?.name ?? "",title:model.title(day.next))}
            catch {model.error=error.localizedDescription}
        }
    }
    func symbol(_ id: String) -> String {
        switch id { case "FAJR": "sun.horizon"; case "SUNRISE": "sunrise"; case "DHUHR": "sun.max"; case "ASR": "sun.haze"; case "MAGHRIB": "sunset"; default: "moon.stars" }
    }
}
