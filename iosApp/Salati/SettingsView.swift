import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @State private var places=false
    var body: some View {
        Form {
            Section("Location") {
                Button {places=true} label: {LabeledContent("City",value:model.locationDisplayName)}
                if let location=model.settings.location {
                    Label(LocalizedStringKey(location.isGPS ? "Using exact GPS coordinates" : "Offline manual location"),systemImage:location.isGPS ? "location.fill":"mappin")
                        .font(.footnote).foregroundStyle(.secondary)
                    DisclosureGroup("Location details") {
                        Text(String(format:"%.5f, %.5f",locale:Locale(identifier:"en_US_POSIX"),location.latitude,location.longitude))
                            .monospacedDigit().font(.footnote).environment(\.layoutDirection,.leftToRight)
                        Text(location.zone).font(.footnote).foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight)
                        if let date=location.lastUpdated {HStack {Text("Last location update");Text(date,style:.relative)}.font(.caption).foregroundStyle(.secondary)}
                    }
                }
                LocationStatusView(service:model.locationService,showLastUpdate:false)
            }
            Section("Calculation") {
                Picker("Method",selection:Binding(get:{model.settings.method},set:{model.settings.calculationMethodOverridden=true;model.settings.method=$0})) { ForEach(model.engine.calculationMethods,id:\.self) { Text(LocalizedStringKey($0)).tag($0) } }
                Toggle("Hanafi Asr",isOn:Binding(get:{model.settings.hanafi},set:{model.settings.asrOverridden=true;model.settings.hanafi=$0}))
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

            }
            Section("Help") {
                NavigationLink {WidgetHelpView()} label:{Label("Add a widget",systemImage:"rectangle.grid.2x2")}
                Button("Review setup") {model.setupReviewStep=0;model.reviewingSetup=true}
            }
            Section("About") {
                Text("Salati · صلاتي").font(.headline)
                Text("Version \(Bundle.main.object(forInfoDictionaryKey:"CFBundleShortVersionString") as? String ?? "—") (\(Bundle.main.object(forInfoDictionaryKey:"CFBundleVersion") as? String ?? "—"))")
                    .font(.footnote).monospacedDigit().accessibilityIdentifier("appBuildIdentity")
                Text("Offline city data: GeoNames, CC BY 4.0. Calendar month boundaries: OpenJDK Hijrah-umalqura.").font(.footnote)
                Link("GeoNames attribution",destination:URL(string:"https://www.geonames.org/about.html")!)
            }
        }.navigationTitle("Settings").navigationBarTitleDisplayMode(.inline).sheet(isPresented:$places) {NavigationStack {PlacesView()}}
            .sheet(isPresented:$model.reviewingSetup) {OnboardingView(review:true)}
    }
}

private struct WidgetHelpView:View {
    var body:some View {
        List {
            Section("Add a widget") {
                Label("Touch and hold your Home Screen, then choose Edit and Add Widget.",systemImage:"1.circle")
                Label("Search for Salati and choose a size.",systemImage:"2.circle")
                Label("Touch and hold the widget, then choose Edit Widget to choose location, date, and sunrise details.",systemImage:"3.circle")
            }
            Section {
                Text("Choose Prayer times for the original layout, or Prayer times · Horizontal for side-by-side prayer times in medium and large widgets.")
                Text("Open Salati after installing or changing location to update your widgets.")
            }.foregroundStyle(.secondary)
        }.navigationTitle("Widgets").navigationBarTitleDisplayMode(.inline)
    }
}
