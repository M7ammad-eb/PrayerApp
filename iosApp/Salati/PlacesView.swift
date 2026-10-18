import SwiftUI

struct PlacesView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) var dismiss
    @State private var query=""
    @State private var results:[Place]=[]
    @State private var failure:String?
    @State private var manual=false
    var body: some View {
        List {
            Section {
                CurrentLocationButton(service:model.locationService)
                LocationStatusView(service:model.locationService)
                Button {manual=true} label: { Label("Enter coordinates",systemImage:"mappin.and.ellipse") }
            } footer: { Text("GPS coordinates stay exact. City names and time zones come from the nearest offline place; check the zone near borders.") }
            if let failure { Text(failure).foregroundStyle(.red) }
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
            do { try await Task.sleep(for:.milliseconds(200)); let found=try await model.places.search(query); try Task.checkCancellation(); results=found; failure=nil }
            catch is CancellationError { }
            catch { failure=error.localizedDescription }
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
    @State private var showPlaces=false
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Label("A moment for prayer",systemImage:"moon.stars").font(.title2.weight(.semibold))
                    Text("Precise times. A quiet space. Always offline.").foregroundStyle(.secondary)
                    Picker("Language",selection:$model.settings.language) {
                        Text("English").tag("en");Text("العربية").tag("ar")
                    }.pickerStyle(.segmented).accessibilityIdentifier("onboardingLanguage")
                }
                Section {
                    OnboardingLocationActions(service:model.locationService,showPlaces:$showPlaces)
                    if let location=model.settings.location {
                        Label(model.locationDisplayName,systemImage:"checkmark.circle.fill")
                            .foregroundStyle(Color.accentColor).accessibilityIdentifier("onboardingSelectedLocation")
                        Text(LocalizedStringKey(location.isGPS ? "Using exact GPS coordinates" : "Offline manual location")).font(.footnote).foregroundStyle(.secondary)
                    }
                } header: {Text("Location")} footer: {Text("Choose a city without granting location access, or use GPS when you need it.")}
                Section("Calculation") {
                    Picker("Method",selection:$model.settings.method) {
                        ForEach(model.engine.calculationMethods,id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                    }
                    Toggle("Hanafi Asr",isOn:$model.settings.hanafi)
                    Text("You can change calculation and display preferences in Settings.").font(.footnote).foregroundStyle(.secondary)
                }
                Section {
                    NavigationLink("Optional alerts and Live Activities") {NotificationSettingsView()}
                } footer: {Text("Notifications and alarms stay off until you choose them. Permission alone never enables audible alerts.")}
                Section("Ready") {
                    if model.settings.location != nil {LabeledContent("City",value:model.locationDisplayName)}
                    LabeledContent("Method") {Text(LocalizedStringKey(model.settings.method))}
                    Button("Continue") {model.settings.onboardingComplete=true}
                        .frame(maxWidth:.infinity,minHeight:44).buttonStyle(.borderedProminent)
                        .disabled(model.settings.location == nil).accessibilityIdentifier("continueOnboarding")
                } footer: {Text("Calculated on your device. No account or subscription.")}
            }
            .navigationTitle("Welcome to Salati").navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented:$showPlaces) {NavigationStack {PlacesView()}}
        }
    }
}
private struct OnboardingLocationActions: View {
    @EnvironmentObject var model: AppModel
    @ObservedObject var service:LocationService
    @Binding var showPlaces:Bool
    var body:some View {
        CurrentLocationButton(service:service).accessibilityIdentifier("onboardingGPS")
        LocationStatusView(service:model.locationService)
        Button {showPlaces=true} label: {Label("Choose location",systemImage:"magnifyingglass")}
            .accessibilityIdentifier("chooseLocation")
    }
}

private struct CurrentLocationButton:View {
    @ObservedObject var service:LocationService
    var body:some View {
        Button {service.request()} label: {Label("Use current location",systemImage:"location.fill")}.disabled(service.isAcquiring)
    }
}
