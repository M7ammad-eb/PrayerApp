import SwiftUI

struct PlacesView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) var dismiss
    @State private var query=""
    @State private var results:[Place]=[]
    @State private var failure:String?
    @State private var searching=false
    @State private var manual=false
    var body: some View {
        List {
            Section {
                CurrentLocationButton(service:model.locationService)
                LocationStatusView(service:model.locationService)
                Button {manual=true} label: { Label("Enter coordinates",systemImage:"mappin.and.ellipse") }
            } footer: { Text("Search for a city, or use your current location.") }
            if searching {HStack {ProgressView();Text("Searching…")}}
            if let failure { Text(LocalizedStringKey(failure)).foregroundStyle(.secondary) }
            if let location=model.settings.location {
                Section("Selected location") {
                    Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").foregroundStyle(Color.accentColor)
                    Text(LocalizedStringKey(location.isGPS ? "Current location" : "Selected city")).font(.footnote).foregroundStyle(.secondary)
                }
            }
            if !searching && !query.isEmpty && results.isEmpty && failure == nil { ContentUnavailableView.search(text:query) }
            if query.isEmpty { ContentUnavailableView("Find your city",systemImage:"magnifyingglass",description:Text("Search by city name.")) }
            ForEach(results) { place in
                Button { model.select(place.location); dismiss() } label: {
                    VStack(alignment:.leading,spacing:4) { Text(model.arabic ? (place.arabicName ?? place.name) : place.name); Text(model.locale.localizedString(forRegionCode:place.country) ?? place.country).font(.caption).foregroundStyle(.secondary) }
                }.accessibilityIdentifier("city.\(place.id)")
            }
        }
        .searchable(text:$query,prompt:"City name")
        .navigationTitle("Location").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement:.confirmationAction) { Button("Done") {dismiss()} } }
        .sheet(isPresented:$manual) { NavigationStack { ManualLocationView() } }
        .task(id:query) {
            searching = !query.isEmpty;results=[];failure=nil
            do { try await Task.sleep(for:.milliseconds(200)); let found=try await model.places.search(query); try Task.checkCancellation(); results=found; searching=false }
            catch is CancellationError { }
            catch { failure="City search is unavailable. Try again or enter coordinates.";searching=false }
        }

    }
}
struct LocationStatusView: View {
    @ObservedObject var service: LocationService
    var showLastUpdate=true
    var body: some View {
        if service.isAcquiring {
            HStack(spacing:12) {ProgressView();Text(LocalizedStringKey(service.message ?? "Finding your location…"))}
                .accessibilityIdentifier("locationProgress")
        } else if let message=service.message {
            Text(LocalizedStringKey(message)).font(.footnote).foregroundStyle(.secondary)
                .accessibilityIdentifier("locationStatus")
        }
        if showLastUpdate,let date=service.lastUpdate {
            HStack {Text("Last location update");Text(date,style:.relative)}.font(.caption).foregroundStyle(.secondary)
        }
    }
}
struct ManualLocationView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) var dismiss
    @State private var name=""
    @State private var latitude=""
    @State private var longitude=""
    @State private var zone=TimeZone.current.identifier
    func coordinate(_ text:String) -> Double? {
        let latin=text.compactMap { character -> String? in
            if let number=character.wholeNumberValue {return String(number)}
            if character == "٫" {return "."}
            return String(character)
        }.joined()
        return Double(latin)
    }
    var valid: Bool { guard let lat=coordinate(latitude),let lon=coordinate(longitude) else {return false}; return (-90...90).contains(lat) && (-180...180).contains(lon) && TimeZone(identifier:zone) != nil }
    var body: some View {
        Form {
            TextField("Location name",text:$name)
            TextField("Latitude",text:$latitude).keyboardType(.numbersAndPunctuation).environment(\.layoutDirection,.leftToRight)
            TextField("Longitude",text:$longitude).keyboardType(.numbersAndPunctuation).environment(\.layoutDirection,.leftToRight)
            Picker("Time zone",selection:$zone) { ForEach(TimeZone.knownTimeZoneIdentifiers,id:\.self) { Text($0).tag($0) } }
            Text("Choose the time zone for this location.").font(.footnote).foregroundStyle(.secondary)
        }
        .navigationTitle("Coordinates").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement:.confirmationAction) { Button("Save") {
            guard valid,let lat=coordinate(latitude),let lon=coordinate(longitude) else {return}
            model.select(.init(name:name.isEmpty ? "\(latitude), \(longitude)" : name,latitude:lat,longitude:lon,zone:zone)); dismiss()
        }.disabled(!valid) } }
    }
}
struct OnboardingView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var review=false
    @State private var showPlaces=false
    @State private var showCalculation=false
    @State private var showAlerts=false
    private var step:Int {min(2,max(0,review ? model.setupReviewStep:model.onboardingStep))}
    private let titles=["Welcome to Salati","Where do you pray?","Make time for prayer"]
    private let subtitles=["Prayer times, wherever your day takes you.","Find prayer times for your city or current location.","Your prayer times are ready. Make Salati your own, or begin your day."]
    private let symbols=["sun.horizon.fill","location.fill","checkmark"]
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment:.leading,spacing:28) {
                    VStack(alignment:.leading,spacing:18) {
                        Image(systemName:symbols[step])
                            .font(.system(size:36,weight:.medium)).foregroundStyle(Color.accentColor)
                            .frame(width:80,height:80).background(Color.accentColor.opacity(0.10),in:RoundedRectangle(cornerRadius:24))
                            .accessibilityHidden(true)
                        Text(LocalizedStringKey(titles[step])).font(.largeTitle.bold()).fixedSize(horizontal:false,vertical:true)
                        Text(LocalizedStringKey(subtitles[step])).font(.title3).foregroundStyle(.secondary).fixedSize(horizontal:false,vertical:true)
                    }
                    page
                }.frame(maxWidth:560,alignment:.leading).padding(24).frame(maxWidth:.infinity)
            }
            .id(step)
            .background(Color(uiColor:.systemGroupedBackground))
            .safeAreaInset(edge:.bottom) {
                VStack(spacing:14) {
                    HStack {
                        ProgressView(value:Double(step+1),total:3).frame(maxWidth:90).accessibilityLabel(Text("Setup progress"))
                        Spacer()
                        Text("\(step+1) / 3").font(.caption).foregroundStyle(.secondary).monospacedDigit().environment(\.layoutDirection,.leftToRight).accessibilityIdentifier("onboardingStep")
                    }
                    Button(action:advance) {
                        Text(LocalizedStringKey(step == 2 ? (review ? "Done":"Start using Salati") : "Continue"))
                            .font(.headline).frame(maxWidth:.infinity,minHeight:46)
                    }.buttonStyle(.borderedProminent).disabled(step == 1 && model.settings.location == nil)
                        .accessibilityIdentifier("continueOnboarding")
                }.frame(maxWidth:560).padding(.horizontal,24).padding(.vertical,16).frame(maxWidth:.infinity).background(.regularMaterial)
            }
            .navigationTitle(LocalizedStringKey(review ? "Review setup":"Salati")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if step>0 {ToolbarItem(placement:.topBarLeading) {Button("Back",systemImage:"chevron.backward") {move(to:step-1)}.accessibilityIdentifier("onboardingBack")}}
                if review {ToolbarItem(placement:.cancellationAction) {Button("Close") {dismiss()}}}
            }
            .sheet(isPresented:$showPlaces) {NavigationStack {PlacesView()}}
            .sheet(isPresented:$showCalculation) {calculationSheet}
            .sheet(isPresented:$showAlerts) {
                NavigationStack {NotificationSettingsView().toolbar {ToolbarItem(placement:.confirmationAction) {Button("Done") {showAlerts=false}}}}
            }
        }
    }
    @ViewBuilder private var page:some View {
        switch step {
        case 0:
            VStack(alignment:.leading,spacing:20) {
                Label("Prayer times",systemImage:"sun.horizon")
                Label("Qibla direction",systemImage:"location.north.circle")
                Label("Hijri calendar",systemImage:"calendar")
            }.font(.headline).frame(maxWidth:.infinity,alignment:.leading).padding(22)
                .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24))
            Button {
                if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}
            } label: {
                HStack {Label("Language",systemImage:"globe");Spacer();Text(model.languageDisplayName).foregroundStyle(.secondary);Image(systemName:"arrow.up.forward").font(.caption)}
                    .font(.subheadline).padding(.vertical,12)
            }.accessibilityIdentifier("onboardingLanguage")
        case 1:
            VStack(alignment:.leading,spacing:18) {
                CurrentLocationButton(service:model.locationService).font(.headline).accessibilityIdentifier("onboardingGPS")
                Divider()
                Button {showPlaces=true} label: {Label("Choose a city",systemImage:"magnifyingglass").font(.headline).frame(maxWidth:.infinity,minHeight:44,alignment:.leading).contentShape(Rectangle())}.accessibilityIdentifier("chooseLocation")
                LocationStatusView(service:model.locationService,showLastUpdate:false)
            }.frame(maxWidth:.infinity,alignment:.leading).padding(22)
                .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24))
            if model.settings.location != nil {
                Label(model.locationDisplayName,systemImage:"checkmark.circle.fill")
                    .font(.headline).foregroundStyle(Color.accentColor).accessibilityIdentifier("onboardingSelectedLocation")
            }
        default:
            VStack(alignment:.leading,spacing:16) {
                if let next=model.presentationSnapshot?.events.first(where:{$0.id == model.nextEventID}) {
                    Text("Up next").font(.subheadline.weight(.medium)).foregroundStyle(Color.accentColor)
                    ViewThatFits(in:.horizontal) {
                        HStack(alignment:.firstTextBaseline) {
                            Text(next.title).font(.title.bold())
                            Spacer(minLength:16)
                            Text(next.timeText).font(.title2.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                        }
                        VStack(alignment:.leading,spacing:8) {
                            Text(next.title).font(.title.bold())
                            Text(next.timeText).font(.title2.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                        }
                    }.accessibilityElement(children:.combine).accessibilityIdentifier("onboardingNextPrayer")
                    Divider()
                }
                Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").font(.subheadline).foregroundStyle(.secondary)
            }.frame(maxWidth:.infinity,alignment:.leading).padding(22)
                .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24))
            VStack(spacing:0) {
                Button {showCalculation=true} label:{option("Adjust prayer times",symbol:"slider.horizontal.3")}.accessibilityIdentifier("onboardingAdvanced")
                Divider().padding(.horizontal,20)
                Button {showAlerts=true} label:{option("Choose prayer alerts",symbol:"bell")}.accessibilityIdentifier("onboardingAlertSettings")
            }.background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24))
            Text("You can change these choices in Settings.").font(.footnote).foregroundStyle(.secondary)
        }
    }
    private func option(_ title:LocalizedStringKey,symbol:String)->some View {
        HStack {Label(title,systemImage:symbol);Spacer();Image(systemName:"chevron.forward").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)}
            .font(.body).padding(20).contentShape(Rectangle())
    }
    private var calculationSheet:some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Method",selection:Binding(get:{model.settings.method},set:{model.settings.calculationMethodOverridden=true;model.settings.method=$0})) {
                        ForEach(model.engine.calculationMethods,id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                    }
                    Toggle("Hanafi Asr",isOn:Binding(get:{model.settings.hanafi},set:{model.settings.asrOverridden=true;model.settings.hanafi=$0}))
                } footer:{Text("Choose the method used by your local mosque.")}
            }.navigationTitle("Prayer calculation").navigationBarTitleDisplayMode(.inline)
                .toolbar {ToolbarItem(placement:.confirmationAction) {Button("Done") {showCalculation=false}}}
        }
    }
    private func move(to value:Int) {
        withAnimation(reduceMotion ? nil:.easeInOut(duration:0.2)) {if review {model.setupReviewStep=value}else {model.onboardingStep=value}}
    }
    private func advance() {
        if step<2 {if step == 1 {model.applyOnboardingDefaults()};move(to:step+1)}
        else if review {dismiss()}
        else {model.settings.onboardingComplete=true;model.onboardingStep=0}
    }
}

private struct CurrentLocationButton:View {
    @ObservedObject var service:LocationService
    var body:some View {
        Button {service.request()} label: {Label("Use current location",systemImage:"location.fill").frame(maxWidth:.infinity,minHeight:44,alignment:.leading).contentShape(Rectangle())}.disabled(service.isAcquiring)
    }
}
