import SwiftUI

// Row label in the style of the system Settings app: a small rounded icon tile and the title.
struct SettingsLabel: View {
    let title: LocalizedStringKey
    let symbol: String
    init(_ title: LocalizedStringKey, symbol: String) {self.title=title;self.symbol=symbol}
    var body: some View {
        Label {
            Text(title)
        } icon: {
            Image(systemName:symbol).font(.system(size:14,weight:.semibold)).foregroundStyle(.white)
                .frame(width:28,height:28).background(Color.accentColor,in:RoundedRectangle(cornerRadius:7,style:.continuous))
        }
    }
}

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @State private var places=false
    @Environment(\.openURL) private var openURL
    var body: some View {
        Form {
            // Alerts come first: it is the part of Settings people return to most.
            Section {
                NavigationLink {NotificationSettingsView()} label: {SettingsLabel("Notifications and adhan",symbol:"bell.badge.fill")}
            } header: {Text("Prayer alerts")}
            Section("Location") {
                Button {places=true} label: {
                    LabeledContent {Text(model.locationDisplayName)} label: {SettingsLabel("City",symbol:"mappin.and.ellipse")}
                }.foregroundStyle(.primary)
                if let location=model.settings.location {
                    Label(LocalizedStringKey(location.isGPS ? "Current location" : "Selected city"),systemImage:location.isGPS ? "location.fill":"mappin")
                        .font(.footnote).foregroundStyle(.secondary)
                    DisclosureGroup("Location details") {
                        Text(String(format:"%.5f, %.5f",locale:Locale(identifier:"en_US_POSIX"),location.latitude,location.longitude))
                            .monospacedDigit().font(.footnote).environment(\.layoutDirection,.leftToRight)
                        Text(location.zone).font(.footnote).foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight)
                        if let date=location.lastUpdated {HStack {Text("Last location update");Text(date,style:.relative)}.font(.caption).foregroundStyle(.secondary)}
                    }
                }
            }
            Section("Calculation") {
                Picker("Method",selection:Binding(get:{model.settings.method},set:{model.settings.calculationMethodOverridden=true;model.settings.method=$0})) { ForEach(model.engine.calculationMethods,id:\.self) { Text(LocalizedStringKey($0)).tag($0) } }
                Toggle("Hanafi Asr",isOn:Binding(get:{model.settings.hanafi},set:{model.settings.asrOverridden=true;model.settings.hanafi=$0}))
                Picker("High latitudes",selection:$model.settings.highLatitude) { ForEach(model.engine.highLatitudeRules,id:\.self) {Text(LocalizedStringKey($0)).tag($0)} }
                NavigationLink("Manual adjustments") { Form { ForEach(0..<6) {i in Stepper(value:$model.settings.adjustments[i],in:-60...60) {LabeledContent(LocalizedStringKey(["Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"][i]),value:model.settings.adjustments[i].description+" min")} } }.navigationTitle("Manual adjustments").navigationBarTitleDisplayMode(.inline) }
            }
            // Time format follows the iPhone's own 24-hour setting, so there is no second switch here.
            Section("Display") {
                Button {
                    if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}
                } label: {
                    LabeledContent {
                        HStack {Text(model.languageDisplayName);Image(systemName:"arrow.up.forward").font(.caption)}
                    } label: {SettingsLabel("Language",symbol:"globe")}
                }.foregroundStyle(.primary).accessibilityIdentifier("appLanguage")
                Picker(selection:$model.settings.appearance) {
                    Text("System").tag("system");Text("Light").tag("light");Text("Dark").tag("dark")
                } label: {SettingsLabel("Appearance",symbol:"circle.lefthalf.filled")}
            }
            Section("Help") {
                NavigationLink {WidgetHelpView()} label: {SettingsLabel("Add a widget",symbol:"rectangle.grid.2x2.fill")}
                Button {model.setupReviewStep=0;model.reviewingSetup=true} label: {SettingsLabel("Review setup",symbol:"checklist")}.foregroundStyle(.primary)
                NavigationLink {SchedulingDiagnosticsView()} label: {SettingsLabel("Troubleshooting",symbol:"wrench.and.screwdriver.fill")}
            }
            Section("About") {
                Text("Salati · صلاتي").font(.headline)
                Text("Version \(Bundle.main.object(forInfoDictionaryKey:"CFBundleShortVersionString") as? String ?? "—") (\(Bundle.main.object(forInfoDictionaryKey:"CFBundleVersion") as? String ?? "—"))")
                    .font(.footnote).monospacedDigit().accessibilityIdentifier("appBuildIdentity")
                NavigationLink("Acknowledgements") {
                    List {
                        Section("City data") {Text("GeoNames / CC BY 4.0");Link("GeoNames attribution",destination:URL(string:"https://www.geonames.org/about.html")!)}
                        Section("Calendar") {Text("OpenJDK Hijrah-umalqura")}
                    }.navigationTitle("Acknowledgements").navigationBarTitleDisplayMode(.inline)
                }
            }
        }.navigationTitle("Settings").navigationBarTitleDisplayMode(.inline).sheet(isPresented:$places) {NavigationStack {PlacesView()}}
            .sheet(isPresented:$model.reviewingSetup) {OnboardingView(review:true)}
    }
}

private struct WidgetHelpView:View {
    var body: some View {
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
