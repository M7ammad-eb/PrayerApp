import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @State private var places=false
    var body: some View {
        Form {
            Section("Location") {
                Button {places=true} label: {LabeledContent("City",value:model.locationDisplayName)}
                if let location=model.settings.location {
                    Text(String(format:"%.5f, %.5f",locale:Locale(identifier:"en_US_POSIX"),location.latitude,location.longitude)).monospacedDigit().font(.footnote)
                    if location.isGPS,let distance=location.nearestDistance {
                        Text("Nearest offline city: \(model.locationDisplayName) · \(String(format:"%.1f",locale:Locale(identifier:"en_US_POSIX"),distance)) km").font(.footnote)
                    }
                    Text(location.zone).font(.footnote).foregroundStyle(.secondary)
                    Text(LocalizedStringKey(location.isGPS ? "Using exact GPS coordinates" : "Offline manual location")).font(.footnote).foregroundStyle(.secondary)
                    if let date=location.lastUpdated {HStack {Text("Last location update");Text(date,style:.relative)}.font(.caption).foregroundStyle(.secondary)}
                }
                LocationStatusView(service:model.locationService,showLastUpdate:false)
            }
            Section("Calculation") {
                Picker("Method",selection:$model.settings.method) { ForEach(model.engine.calculationMethods,id:\.self) { Text(LocalizedStringKey($0)).tag($0) } }
                Toggle("Hanafi Asr",isOn:$model.settings.hanafi)
                Picker("High latitudes",selection:$model.settings.highLatitude) { ForEach(model.engine.highLatitudeRules,id:\.self) {Text(LocalizedStringKey($0)).tag($0)} }
                NavigationLink("Manual adjustments") { Form { ForEach(0..<6) {i in Stepper(value:$model.settings.adjustments[i],in:-60...60) {LabeledContent(LocalizedStringKey(["Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"][i]),value:model.settings.adjustments[i].description+" min")} } }.navigationTitle("Manual adjustments").navigationBarTitleDisplayMode(.inline) }
                NavigationLink("Hijri adjustment") { HijriView() }
            }
            Section("Display") {
                Picker("Language",selection:$model.settings.language) {
                    Text("English").tag("en")
                    Text("العربية").tag("ar")
                }.pickerStyle(.segmented).accessibilityIdentifier("appLanguage")
                Picker("Appearance",selection:$model.settings.appearance) {Text("System").tag("system");Text("Light").tag("light");Text("Dark").tag("dark")}
                Toggle("24-hour time",isOn:$model.settings.use24Hour)
                Text("Western digits (0123456789) are used in both languages.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("Prayer alerts") {
                NavigationLink("Notifications and adhan") { NotificationSettingsView() }
                if let status=model.scheduleStatus {Text(status).font(.footnote).foregroundStyle(.secondary)}
            }
            Section("About") {
                Text("Salati · صلاتي").font(.headline)
                Text("Version \(Bundle.main.object(forInfoDictionaryKey:"CFBundleShortVersionString") as? String ?? "—") (\(Bundle.main.object(forInfoDictionaryKey:"CFBundleVersion") as? String ?? "—"))")
                    .font(.footnote).monospacedDigit().accessibilityIdentifier("appBuildIdentity")
                Text("Offline city data: GeoNames, CC BY 4.0. Calendar month boundaries: OpenJDK Hijrah-umalqura.").font(.footnote)
                Link("GeoNames attribution",destination:URL(string:"https://www.geonames.org/about.html")!)
            }
        }.navigationTitle("Settings").navigationBarTitleDisplayMode(.inline).sheet(isPresented:$places) {NavigationStack {PlacesView()}}
    }
}
