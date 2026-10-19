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
            } footer: { Text("Choose a city offline, or use your precise location. Check the time zone if you are near a border.") }
            if searching {HStack {ProgressView();Text("Searching offline cities…")}}
            if let failure { Text(LocalizedStringKey(failure)).foregroundStyle(.secondary) }
            if let location=model.settings.location {
                Section("Selected location") {
                    Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").foregroundStyle(Color.accentColor)
                    Text(LocalizedStringKey(location.isGPS ? "Using exact GPS coordinates" : "Offline manual location")).font(.footnote).foregroundStyle(.secondary)
                }
            }
            if !searching && !query.isEmpty && results.isEmpty && failure == nil { ContentUnavailableView.search(text:query) }
            if query.isEmpty { ContentUnavailableView("Find your city",systemImage:"magnifyingglass",description:Text("Search in Arabic or English. No internet needed.")) }
            ForEach(results) { place in
                Button { model.select(place.location); dismiss() } label: {
                    VStack(alignment:.leading,spacing:4) { Text(model.arabic ? (place.arabicName ?? place.name) : place.name); Text("\(place.country) · \(place.zone)").font(.caption).foregroundStyle(.secondary) }
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
            Text("Choose the IANA time zone for these coordinates. The device time zone may differ.").font(.footnote).foregroundStyle(.secondary)
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
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var review=false
    @State private var showPlaces=false
    @State private var advanced=false
    @State private var authorization:String?
    @State private var requestingPermission=false
    private var step:Int {review ? model.setupReviewStep:model.onboardingStep}
    private let titles=["A moment for prayer","Prayer times for your location","Your prayer calculation","Alerts, your way","Ready for your day"]
    private let symbols=["moon.stars.fill","location.fill","sun.horizon.fill","bell.badge.fill","checkmark.circle.fill"]
    private var enabledCount:Int {model.settings.policies.values.filter {$0.delivery != .off}.count}
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(alignment:.leading,spacing:12) {
                        Image(systemName:symbols[step]).font(.system(size:32,weight:.medium)).foregroundStyle(Color.accentColor).accessibilityHidden(true)
                        Text(LocalizedStringKey(titles[step])).font(.title2.bold()).fixedSize(horizontal:false,vertical:true)
                        Text(LocalizedStringKey(subtitle)).foregroundStyle(.secondary).fixedSize(horizontal:false,vertical:true)
                    }.padding(.vertical,8)
                }.listRowBackground(Color.accentColor.opacity(0.08))
                page
            }
            .safeAreaInset(edge:.bottom) {
                VStack(spacing:10) {
                    ProgressView(value:Double(step+1),total:5).accessibilityLabel(Text("Setup progress"))
                    HStack {
                        Text("\(step+1) / 5").monospacedDigit().environment(\.layoutDirection,.leftToRight)
                            .accessibilityIdentifier("onboardingStep")
                        Spacer()
                        Text(LocalizedStringKey(["Language","Location","Calculation","Prayer alerts","Ready"][step]))
                    }.font(.caption).foregroundStyle(.secondary)
                    Button(action:advance) {Text(LocalizedStringKey(step == 4 ? (review ? "Done":"Start using Salati") : "Continue")).frame(maxWidth:.infinity,minHeight:44)}
                        .buttonStyle(.borderedProminent).disabled(step == 1 && model.settings.location == nil)
                        .accessibilityIdentifier("continueOnboarding")
                }.padding(.horizontal,20).padding(.vertical,12).background(.regularMaterial)
            }
            .navigationTitle(LocalizedStringKey(review ? "Review setup":"Welcome to Salati")).navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if step>0 {ToolbarItem(placement:.topBarLeading) {Button("Back",systemImage:"chevron.backward") {move(to:step-1)}.accessibilityIdentifier("onboardingBack")}}
                if review {ToolbarItem(placement:.cancellationAction) {Button("Close") {dismiss()}}}
            }
            .sheet(isPresented:$showPlaces) {NavigationStack {PlacesView()}}
            .onChange(of:step) {_,value in if value == 2 {model.applyOnboardingDefaults()}}
        }
    }
    private var subtitle:String {
        switch step {
        case 0:return "Precise times. A quiet space. Always offline."
        case 1:return "Choose a city without granting location access, or use GPS when you need it."
        case 2:return "A starting point for your location, with the freedom to adjust."
        case 3:return "Keep prayer times close. Choose only the alerts you want."
        default:return "Your choices are saved. You can change them anytime in Settings."
        }
    }
    @ViewBuilder private var page:some View {
        switch step {
        case 0:
            Section("Language") {
                Picker("Language",selection:$model.settings.language) {Text("English").tag("en");Text("العربية").tag("ar")}
                    .pickerStyle(.segmented).accessibilityIdentifier("onboardingLanguage")
                Label("Calculated on your device. No account or subscription.",systemImage:"iphone")
                    .font(.callout).foregroundStyle(.secondary)
            }
        case 1:
            Section {
                CurrentLocationButton(service:model.locationService).accessibilityIdentifier("onboardingGPS")
                LocationStatusView(service:model.locationService)
                Button {showPlaces=true} label: {Label("Choose location",systemImage:"magnifyingglass")}.accessibilityIdentifier("chooseLocation")
            } header:{Text("Location")}
            if let location=model.settings.location {
                Section("Selected location") {
                    Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").foregroundStyle(Color.accentColor).accessibilityIdentifier("onboardingSelectedLocation")
                    Text(LocalizedStringKey(location.isGPS ? "Using exact GPS coordinates":"Offline manual location")).font(.footnote).foregroundStyle(.secondary)
                    Text(location.zone).font(.caption).foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight)
                }
            }
        case 2:
            Section {
                LabeledContent("Method") {Text(LocalizedStringKey(model.settings.method))}
                LabeledContent("Asr") {Text(LocalizedStringKey(model.settings.hanafi ? "Hanafi":"Standard"))}
                Text(LocalizedStringKey(model.settings.onboardingComplete || model.settings.calculationMethodOverridden || model.settings.asrOverridden ? "Your chosen calculation settings are preserved.":model.onboardingDefaultsAreFallback ? "A regional method is not available for this time zone. Muslim World League and Standard Asr provide a reviewable starting point.":"Based on your time zone. Standard Asr is the default; you can change either choice.")).font(.footnote).foregroundStyle(.secondary)
            } header: {Text("Calculation")}
            Section {
                DisclosureGroup("Advanced settings",isExpanded:$advanced) {
                    Picker("Method",selection:Binding(get:{model.settings.method},set:{model.settings.calculationMethodOverridden=true;model.settings.method=$0})) {
                        ForEach(model.engine.calculationMethods,id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                    }
                    Toggle("Hanafi Asr",isOn:Binding(get:{model.settings.hanafi},set:{model.settings.asrOverridden=true;model.settings.hanafi=$0}))
                }.accessibilityIdentifier("onboardingAdvanced")
            } footer:{Text("Your changes are kept when you choose another location.")}
        case 3:
            Section {
                NavigationLink {NotificationSettingsView()} label: {Label("Choose prayer alerts",systemImage:"bell")}
                    .accessibilityIdentifier("onboardingAlertSettings")
                LabeledContent("Enabled prayers",value:String(enabledCount))
                Toggle("Automatic prayer countdown",isOn:$model.settings.liveActivitiesEnabled)
            } footer:{Text("Alerts stay off until you choose them. Continue without enabling alerts if you prefer.")}
            if model.settings.policies.values.contains(where:{$0.delivery == .notification || $0.reminder>0}) || model.settings.liveActivitiesEnabled {
                Section {
                    Button("Allow notifications") {requestNotifications()}.disabled(requestingPermission).accessibilityIdentifier("onboardingAllowNotifications")
                    if requestingPermission {ProgressView()}
                    if let authorization {Text(LocalizedStringKey(authorization)).font(.footnote)}
                } footer:{Text("Permission lets Salati deliver the reminders you selected. It does not turn on other prayer alerts.")}
            }
        default:
            Section {
                Label(model.locationDisplayName,systemImage:"location.fill")
                LabeledContent("Method") {Text(LocalizedStringKey(model.settings.method))}
                LabeledContent("Asr") {Text(LocalizedStringKey(model.settings.hanafi ? "Hanafi":"Standard"))}
                LabeledContent("Enabled prayers",value:String(enabledCount))
                LabeledContent("Automatic prayer countdown") {Text(LocalizedStringKey(model.settings.liveActivitiesEnabled ? "On":"Off"))}
            } header:{Text("Your setup")}
            Section {Label("Add a Salati widget to keep prayer times on your Home Screen.",systemImage:"rectangle.grid.2x2").font(.callout).foregroundStyle(.secondary)}
        }
    }
    private func move(to value:Int) {
        withAnimation(reduceMotion ? nil:.easeInOut(duration:0.2)) {if review {model.setupReviewStep=value}else {model.onboardingStep=value}}
    }
    private func advance() {
        if step<4 {move(to:step+1)}
        else if review {dismiss()}
        else {model.settings.onboardingComplete=true;model.onboardingStep=0}
    }
    private func requestNotifications() {
        requestingPermission=true
        Task {authorization=await model.scheduler.authorizeNotifications();requestingPermission=false;model.queueRefresh()}
    }
}

private struct CurrentLocationButton:View {
    @ObservedObject var service:LocationService
    var body:some View {
        Button {service.request()} label: {Label("Use current location",systemImage:"location.fill")}.disabled(service.isAcquiring)
    }
}
