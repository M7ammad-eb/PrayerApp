import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var model: AppModel
    @State private var places=false
    var body: some View {
        Form {
            Section("Location") {
                Button {places=true} label: {LabeledContent("City",value:model.settings.location?.name ?? "—")}
                if let location=model.settings.location {
                    Text(String(format:"%.5f, %.5f",locale:Locale(identifier:"en_US_POSIX"),location.latitude,location.longitude)).monospacedDigit().font(.footnote)
                    if location.isGPS,let distance=location.nearestDistance {
                        Text("Nearest offline city: \(location.name) · \(String(format:"%.1f",distance)) km").font(.footnote)
                    }
                    Text(location.zone).font(.footnote).foregroundStyle(.secondary)
                }
            }
            Section("Calculation") {
                Picker("Method",selection:$model.settings.method) { ForEach(model.engine.calculationMethods,id:\.self) { Text(LocalizedStringKey($0)).tag($0) } }
                Toggle("Hanafi Asr",isOn:$model.settings.hanafi)
                Picker("High latitudes",selection:$model.settings.highLatitude) { ForEach(model.engine.highLatitudeRules,id:\.self) {Text(LocalizedStringKey($0)).tag($0)} }
                NavigationLink("Manual adjustments") { Form { ForEach(0..<6) {i in Stepper(value:$model.settings.adjustments[i],in:-60...60) {LabeledContent(LocalizedStringKey(["Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"][i]),value:model.settings.adjustments[i].description+" min")} } }.navigationTitle("Manual adjustments") }
                Stepper(value:Binding(get:{model.settings.hijriOffset},set:{model.settings.hijriAnchor=model.day?.hijriMonthKey;model.settings.hijriOffset=$0}),in:-2...2) {LabeledContent("Hijri adjustment",value:model.settings.hijriOffset.description)}
            }
            Section("Display") {
                Picker("Language",selection:$model.settings.language) { Text("English").tag("en");Text("العربية").tag("ar") }
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
                Text("Offline city data: GeoNames, CC BY 4.0. Calendar month boundaries: OpenJDK Hijrah-umalqura.").font(.footnote)
                Link("GeoNames attribution",destination:URL(string:"https://www.geonames.org/about.html")!)
            }
        }.navigationTitle("Settings").sheet(isPresented:$places) {NavigationStack {PlacesView()}}
    }
}
struct NotificationSettingsView: View {
    @EnvironmentObject var model: AppModel
    @StateObject private var player=AdhanPlayer()
    @State private var authorization:String?
    var body: some View {
        Form {
            Section {
                Button("Allow notifications") {Task {authorization=await model.scheduler.authorizeNotifications();model.queueRefresh()}}
                Button("Allow system alarms") {Task {authorization=await model.scheduler.authorizeAlarms();model.queueRefresh()}}
                if let authorization {Text(authorization).font(.footnote)}
            } footer: {Text("Notifications follow your system settings. System alarms require separate permission. No alerts are enabled by granting permission alone.")}
            ForEach(["FAJR","DHUHR","ASR","MAGHRIB","ISHA"],id:\.self) {id in
                Section(LocalizedStringKey(id)) {
                    Picker("Delivery",selection:policy(id,\.delivery)) {
                        Text("Off").tag(Delivery.off);Text("Notification").tag(Delivery.notification);Text("System alarm").tag(Delivery.alarm)
                    }
                    Picker("Sound",selection:policy(id,\.sound)) {
                        Text("System sound").tag("default");if model.settings.policies[id]?.delivery != .alarm {Text("Silent").tag("silent")}
                        ForEach(Recording.all) {Text(model.arabic ? $0.arabic : $0.english).tag($0.id)}
                    }
                    Stepper(value:policy(id,\.reminder),in:0...60,step:5) {LabeledContent("Reminder before prayer",value:(model.settings.policies[id]?.reminder ?? 0).description+" min")}
                    if let sound=model.settings.policies[id]?.sound,Recording.all.contains(where:{$0.id == sound}) {
                        Button("Play full adhan now") {player.play(sound)}
                        Button("Stop playback") {player.stop()}
                    }
                }
            }
            Section {
                Text("Local notifications use a short excerpt under 30 seconds. Full recordings can be played here while the app is open. Full-length automatic AlarmKit playback requires physical-device validation and is not guaranteed.")
                Text("Up to six days are prepared. Open Salati regularly to replenish the schedule; background refresh is best effort.")
                if let error=player.error {Text(error).foregroundStyle(.red)}
            }.font(.footnote).foregroundStyle(.secondary)
        }.navigationTitle("Notifications and adhan").onDisappear {player.stop()}
    }
    func policy<T>(_ id:String,_ keyPath:WritableKeyPath<PrayerPolicy,T>) -> Binding<T> {
        Binding(get:{(model.settings.policies[id] ?? PrayerPolicy())[keyPath:keyPath]},set:{value in var p=model.settings.policies[id] ?? PrayerPolicy();p[keyPath:keyPath]=value;model.settings.policies[id]=p})
    }
}
