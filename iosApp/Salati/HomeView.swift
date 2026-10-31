import SwiftUI
import SalatiCore

// Suhoor ends at Fajr and the fast ends at Maghrib; the nearer of the two gets a live countdown.
private struct RamadanSection: View {
    @EnvironmentObject var model: AppModel
    var body: some View {
        let events=model.displayEvents
        if let fajr=events.first(where:{$0.id.hasSuffix(".FAJR")}),let maghrib=events.first(where:{$0.id.hasSuffix(".MAGHRIB")}) {
            let now=Date()
            let upcoming=[fajr,maghrib].filter {$0.date>now}.min {$0.date<$1.date}
            Section {
                LabeledContent {
                    Text(fajr.timeText).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                } label: {Label("Suhoor ends",systemImage:"moon.stars")}
                LabeledContent {
                    Text(maghrib.timeText).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                } label: {Label("Iftar",systemImage:"sunset")}
                if let upcoming {
                    LabeledContent {
                        Text(timerInterval:now...upcoming.date,countsDown:true).monospacedDigit().foregroundStyle(Color.accentColor)
                            .environment(\.locale,Locale(identifier:"en_US_POSIX")).environment(\.layoutDirection,.leftToRight)
                    } label: {Text(LocalizedStringKey(upcoming.id.hasSuffix(".FAJR") ? "Until suhoor ends" : "Until iftar"))}
                }
            } header: {Text("Ramadan")}
        }
    }
}

struct HomeView: View {
    @EnvironmentObject var model: AppModel
    @State private var showPlaces=false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ObservedObject private var audio=AdhanPlayer.shared
    var body: some View {
        List {
            if let day=model.day {
                Section {
                    VStack(alignment:.leading,spacing:16) {
                        VStack(alignment:.leading,spacing:8) {
                            Text(model.locationDisplayName).font(.subheadline).foregroundStyle(.secondary)
                                .fixedSize(horizontal:false,vertical:true).accessibilityIdentifier("homeSelectedLocation")
                            Label("Up next",systemImage:"clock").font(.subheadline.weight(.medium)).foregroundStyle(Color.accentColor)
                        }
                        if let next=model.presentationSnapshot?.events.first(where:{$0.id == model.nextEventID}) {
                        ViewThatFits(in:.horizontal) {
                            HStack(alignment:.firstTextBaseline) { Text(next.title).font(.largeTitle.weight(.semibold)); Spacer(); Text(next.timeText).font(.title.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                            VStack(alignment:.leading) { Text(next.title).font(.largeTitle.weight(.semibold)); Text(next.timeText).font(.title).monospacedDigit().environment(\.layoutDirection,.leftToRight) }
                        }
                        let countdownStart=Date()
                        if next.date>countdownStart {
                            Text(timerInterval:countdownStart...next.date,countsDown:true)
                                .font(.system(.largeTitle,design:.rounded,weight:.light)).monospacedDigit().foregroundStyle(Color.accentColor).environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                .environment(\.layoutDirection,.leftToRight).accessibilityLabel(Text("Time until next prayer"))
                        }
                        // How far the day has moved from the current prayer towards the next one.
                        if let current=model.presentationSnapshot?.events.first(where:{$0.id == model.currentEventID}),
                           current.date<=countdownStart,current.date<next.date,next.date>countdownStart {
                            ProgressView(timerInterval:current.date...next.date,countsDown:false)
                                .tint(Color.accentColor).labelsHidden().frame(height:4).clipShape(Capsule())
                                .accessibilityHidden(true)
                        }
                        }
                        Text(model.arabic ? day.hijriArabic : day.hijriEnglish).font(.subheadline).foregroundStyle(.secondary)
                    }.padding(.vertical,12)
                }.listRowBackground(Color.accentColor.opacity(0.09))
                Section("Prayer times") {
                    ForEach(model.displayEvents) { event in
                        let next=event.id == model.nextEventID
                        let current=event.id == model.currentEventID
                        let rowTint:Color?=next ? Color.accentColor.opacity(0.10) : nil
                        Group {
                            if dynamicTypeSize.isAccessibilitySize {
                                VStack(alignment:.leading,spacing:8) {
                                    Text(event.title).fontWeight(next || current ? .semibold:.regular)
                                    Text(event.timeText).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                                    if current {Text("Current prayer").font(.caption).foregroundStyle(.primary)}
                                    else if next {Text("Up next").font(.caption).foregroundStyle(Color.accentColor)}
                                }.frame(maxWidth:.infinity,alignment:.leading)
                            } else {
                                // Same language as the widget: an arrow and accent colour for the next prayer,
                                // a dot and bold text for the current one, secondary for the rest.
                                HStack(spacing:10) {
                                    Group {
                                        if next {Image(systemName:"arrowtriangle.forward.fill").font(.system(size:8)).foregroundStyle(Color.accentColor)}
                                        else {Circle().fill(current ? Color.primary : Color.clear).frame(width:6,height:6)}
                                    }.frame(width:10).accessibilityHidden(true)
                                    Image(systemName:symbol(event.id)).foregroundStyle(next ? Color.accentColor : (current ? Color.primary : Color.secondary)).frame(width:28).accessibilityHidden(true)
                                    Text(event.title).fontWeight(next || current ? .semibold:.regular)
                                    Spacer(minLength:8)
                                    Text(event.timeText).monospacedDigit().environment(\.layoutDirection,.leftToRight).fontWeight(next || current ? .semibold:.regular)
                                }.accessibilityValue(Text(LocalizedStringKey(current ? "Current prayer" : (next ? "Up next" : ""))))
                            }
                        }.foregroundStyle(next ? Color.accentColor : (current ? Color.primary : Color.secondary))
                            .padding(.vertical,7).accessibilityElement(children:.combine).accessibilityIdentifier("homePrayer."+event.id)
                            .listRowBackground(rowTint)
                    }
                }
                if model.isRamadan {RamadanSection()}
                Section {
                    LabeledContent("Islamic midnight",value:model.time(day.midnightEpoch))
                    LabeledContent("Last third",value:model.time(day.lastThirdEpoch))
                } header: {Text("The night")}
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
