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
                Button { model.locationService.request() } label: { Label("Use current location",systemImage:"location.fill") }
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
        .navigationTitle("Location")
        .toolbar { ToolbarItem(placement:.confirmationAction) { Button("Done") {dismiss()} } }
        .sheet(isPresented:$manual) { NavigationStack { ManualLocationView() } }
        .task(id:query) {
            do { try await Task.sleep(for:.milliseconds(200)); let found=try await model.places.search(query); try Task.checkCancellation(); results=found; failure=nil }
            catch is CancellationError { }
            catch { failure=error.localizedDescription }
        }
        .overlay(alignment:.bottom) { LocationMessage(service:model.locationService) }
    }
}
struct LocationMessage: View {
    @ObservedObject var service: LocationService
    var body: some View { if let message=service.message { Text(LocalizedStringKey(message)).font(.footnote).padding().background(.regularMaterial,in:RoundedRectangle(cornerRadius:16)).padding() } }
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
        .navigationTitle("Coordinates")
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
            ScrollView {
                VStack(alignment:.leading,spacing:28) {
                    Image(systemName:"moon.stars").font(.system(size:54,weight:.ultraLight)).foregroundStyle(Color.accentColor).accessibilityHidden(true)
                    Text("A moment for prayer").font(.largeTitle.weight(.semibold))
                    Text("Precise times. A quiet space. Always offline.").font(.title3).foregroundStyle(.secondary)
                    Label("Calculated on your device",systemImage:"iphone")
                    Label("Arabic and English",systemImage:"character.bubble")
                    Label("No account or subscription",systemImage:"hand.raised")
                    Picker("Language",selection:$model.settings.language) { Text("English").tag("en"); Text("العربية").tag("ar") }.pickerStyle(.segmented)
                    Text("Choose a city without granting location access, or use GPS when you need it. Notifications and alarms are optional and requested from Settings.").foregroundStyle(.secondary)
                    Button {showPlaces=true} label: { Text(model.settings.location?.name ?? (model.arabic ? "اختيار الموقع" : "Choose location")).frame(maxWidth:.infinity,minHeight:44) }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule).accessibilityIdentifier("chooseLocation")
                    Button("Continue") {model.settings.onboardingComplete=true}.buttonStyle(.bordered).buttonBorderShape(.capsule).disabled(model.settings.location == nil).accessibilityIdentifier("continueOnboarding")
                }.padding(28).frame(maxWidth:560)
            }.sheet(isPresented:$showPlaces) { NavigationStack { PlacesView() } }
        }
    }
}
