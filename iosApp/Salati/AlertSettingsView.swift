import SwiftUI
import ActivityKit
import UserNotifications

struct NotificationSettingsView: View {
    @EnvironmentObject var model:AppModel
    @Environment(\.openURL) private var openURL
    @ObservedObject private var player=AdhanPlayer.shared
    @State private var authorization:String?
    @State private var requesting=false
    @State private var notificationDenied=false
    private let prayers=["FAJR","DHUHR","ASR","MAGHRIB","ISHA"]
    private var notificationsSelected:Bool {model.settings.liveActivitiesEnabled || model.settings.policies.values.contains {$0.delivery == .notification || $0.reminder>0}}
    private var alarmsSelected:Bool {model.settings.policies.values.contains {$0.delivery == .alarm}}
    var body:some View {
        Form {
            Section {
                ForEach(prayers,id:\.self) {id in
                    NavigationLink {PrayerAlertDetailView(prayer:id)} label: {
                        HStack {
                            Text(LocalizedStringKey(id)).fontWeight(.medium)
                            Spacer()
                            Text(LocalizedStringKey(deliveryTitle(model.settings.policies[id]?.delivery ?? .off))).foregroundStyle(.secondary).font(.subheadline)
                        }.padding(.vertical,4)
                    }.accessibilityIdentifier("prayerAlert."+id)
                }
            } header:{Text("Prayer alerts")} footer:{Text("Choose delivery and sound for each prayer. Off means no prayer alert; advance reminders are a separate choice.")}
            Section {
                Toggle("Automatic prayer countdown",isOn:$model.settings.liveActivitiesEnabled)
                Toggle("Sound for advance reminders",isOn:$model.settings.reminderSound)
                if model.settings.liveActivitiesEnabled && !ActivityAuthorizationInfo().areActivitiesEnabled {
                    Label("Allow Live Activities in iOS Settings to show the countdown.",systemImage:"info.circle").font(.footnote)
                    settingsButton
                }
            } header:{Text("Countdown and reminders")} footer:{Text("The countdown starts 15 minutes before prayer. If it cannot start, Salati uses a notification reminder when permitted.")}
            if notificationsSelected || alarmsSelected {
                Section {
                    if notificationsSelected {
                        if notificationDenied {
                            Label("Notifications are turned off in iOS Settings.",systemImage:"bell.slash").font(.callout)
                            settingsButton
                        } else {
                            Button("Allow notifications") {requestPermission(alarm:false)}.disabled(requesting)
                        }
                    }
                    if alarmsSelected {Button("Allow system alarms") {requestPermission(alarm:true)}.disabled(requesting)}
                    if requesting {ProgressView()}
                    if let authorization {Text(LocalizedStringKey(authorization)).font(.footnote)}
                } header:{Text("Permissions")} footer:{Text("Permission applies to the choices above. It does not enable other prayer alerts.")}
            }
            Section {
                NavigationLink {ScheduledAlertTestView()} label:{Label("Test an alert",systemImage:"bell.badge")}
                NavigationLink {AdhanPreviewView()} label:{Label("Listen to adhan",systemImage:"play.circle")}
            } header:{Text("Try your settings")} footer:{Text("Test an alert for delivery in one minute, or listen to a recording immediately.")}
            if player.isPlaying {Section {Button("Stop playback",systemImage:"stop.circle.fill") {player.stop()}}}
            if player.error != nil {Section {Label("The recording could not play. Try another recording.",systemImage:"exclamationmark.circle").font(.footnote)}}
            Section {
                Text("Open Salati regularly to keep future alerts ready.").font(.footnote).foregroundStyle(.secondary)
                NavigationLink("Alert status and diagnostics") {SchedulingDiagnosticsView()}
            }
        }.navigationTitle("Notifications and adhan").navigationBarTitleDisplayMode(.inline)
            .task {await readPermission()}
    }
    private var settingsButton:some View {Button("Open iOS Settings") {if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}}}
    private func deliveryTitle(_ delivery:Delivery)->String {switch delivery {case .off:"Off";case .notification:"Notification";case .alarm:"System alarm"}}
    private func readPermission() async {let settings=await UNUserNotificationCenter.current().notificationSettings();notificationDenied=settings.authorizationStatus == .denied}
    private func requestPermission(alarm:Bool) {
        requesting=true
        Task {if alarm {authorization=await model.scheduler.authorizeAlarms()}else {authorization=await model.scheduler.authorizeNotifications()};requesting=false;await readPermission();model.queueRefresh()}
    }
}

private struct PrayerAlertDetailView:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var player=AdhanPlayer.shared
    let prayer:String
    private var selection:PrayerPolicy {model.settings.policies[prayer] ?? PrayerPolicy()}
    var body:some View {
        Form {
            Section {
                Picker("Delivery",selection:policy(\.delivery)) {
                    Text("Off").tag(Delivery.off);Text("Notification").tag(Delivery.notification);Text("System alarm").tag(Delivery.alarm)
                }.accessibilityIdentifier("alertDelivery")
            } footer:{Text("Notifications follow your iPhone sound settings. System alarms use a separate permission.")}
            if selection.delivery != .off {
                Section {
                    Picker("Sound",selection:policy(\.sound)) {
                        Text("System sound").tag("default")
                        if selection.delivery != .alarm {Text("Silent").tag("silent")}
                        else if selection.sound == "silent" {Text("Choose an audible alarm sound").tag("silent")}
                        if selection.delivery == .alarm {
                            ForEach(Recording.all) {Text(model.arabic ? $0.arabic:$0.english).tag($0.id)}
                        } else if Recording.all.contains(where:{$0.id == selection.sound}) {
                            Text("Selected adhan — system notification sound for now").tag(selection.sound)
                        }
                    }
                    if Recording.all.contains(where:{$0.id == selection.sound}) {
                        Button("Play full adhan now",systemImage:"play.circle") {player.play(selection.sound)}
                    }
                } header:{Text("Sound")} footer:{
                    if selection.delivery == .alarm {
                        if Recording.all.contains(where:{$0.id == selection.sound}) {
                            Text("The selected full recording is prepared for the alarm. Use Test an alert to check playback on your iPhone.")
                        } else if selection.sound == "silent" {
                            Text("Choose an audible alarm sound")
                        } else {
                            Text("This alarm uses the system sound. Choose a recording above to hear the full adhan.")
                        }
                    } else {
                        Text("Notifications use the system sound or silence. Full adhan recordings are available with system alarms and in Listen to adhan.")
                    }
                }
            }
            Section {
                Stepper(value:policy(\.reminder),in:0...60,step:5) {
                    LabeledContent("Reminder before prayer") {
                        if selection.reminder == 0 {Text("Off")}else {Text("\(selection.reminder) min").monospacedDigit()}
                    }
                }
            } header:{Text("Advance reminder")} footer:{Text("Advance reminders use the system sound or silence, never adhan.")}
            if player.isPlaying {Section {Button("Stop playback",systemImage:"stop.circle.fill") {player.stop()}}}
        }.navigationTitle(LocalizedStringKey(prayer)).navigationBarTitleDisplayMode(.inline)
    }
    private func policy<T>(_ keyPath:WritableKeyPath<PrayerPolicy,T>)->Binding<T> {
        Binding(get:{selection[keyPath:keyPath]},set:{value in var p=selection;p[keyPath:keyPath]=value;model.settings.policies[prayer]=p})
    }
}

private struct AdhanPreviewView:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var player=AdhanPlayer.shared
    var body:some View {
        List {
            Section {ForEach(Recording.all) {recording in
                Button {player.play(recording.id)} label:{Label(model.arabic ? recording.arabic:recording.english,systemImage:"play.circle")}.padding(.vertical,4)
            }} footer:{Text("Plays immediately. Listening does not change your prayer alert settings.")}
            if player.isPlaying {Section {Button("Stop playback",systemImage:"stop.circle.fill") {player.stop()}}}
            if player.error != nil {Section {Text("The recording could not play. Try another recording.").foregroundStyle(.secondary)}}
        }.navigationTitle("Listen to adhan").navigationBarTitleDisplayMode(.inline)
    }
}

private struct ScheduledAlertTestView:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var scheduler=PrayerScheduler.shared
    @State private var testPrayer="FAJR"
    @State private var testDelivery:Delivery = .notification
    @State private var testing=false
    @State private var authorization:String?
    @State private var requesting=false
    var body:some View {
        Form {
            Section {
                Picker("Delivery",selection:$testDelivery) {Text("Notification").tag(Delivery.notification);Text("System alarm").tag(Delivery.alarm)}
                Picker("Use prayer sound preference",selection:$testPrayer) {
                    ForEach(["FAJR","DHUHR","ASR","MAGHRIB","ISHA"],id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                }
            } footer:{Text("This test uses the chosen prayer’s sound. Your prayer schedule stays unchanged.")}
            Section {
                Button(LocalizedStringKey(testDelivery == .alarm ? "Allow system alarms":"Allow notifications")) {
                    requesting=true
                    Task {
                        if testDelivery == .alarm {authorization=await scheduler.authorizeAlarms()}
                        else {authorization=await scheduler.authorizeNotifications()}
                        requesting=false;model.queueRefresh()
                    }
                }.disabled(requesting)
                if let authorization {Text(LocalizedStringKey(authorization)).font(.footnote)}
                Button("Test in one minute — lock the phone") {
                    testing=true
                    let sound=model.settings.policies[testPrayer]?.sound ?? "default"
                    Task {await scheduler.scheduleTest(delivery:testDelivery,sound:sound,settings:model.settings);testing=false}
                }.disabled(testing).accessibilityIdentifier("scheduleTestAlert")
                if testing {ProgressView()}
                Button("Cancel test alerts",role:.destructive) {Task {await scheduler.cancelTests()}}
                if let status=scheduler.testStatus {Text(LocalizedStringKey(status)).font(.footnote)}
            } footer:{Text("Allow the selected delivery type, then start the test and lock your iPhone.")}
        }.navigationTitle("Test an alert").navigationBarTitleDisplayMode(.inline)
    }
}

private struct SchedulingDiagnosticsView:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var scheduler=PrayerScheduler.shared
    var body:some View {
        ScrollView {
            VStack(alignment:.leading,spacing:16) {
                Text(model.arabic ? "سجل محلي فقط. قبول الموعد من النظام لا يثبت وقت الصوت أو التسليم الفعلي. لا تعرض استجابة المستخدم على أنها وقت وصول التنبيه." : "Stored only on this phone. Scheduling acceptance does not prove audible delivery. A notification response records user interaction, not the actual delivery instant.").font(.callout)
                if let status=model.scheduleStatus {Text(status).font(.callout)}
                ShareLink(item:scheduler.diagnosticsText) {Label(model.arabic ? "مشاركة السجل" : "Share local log",systemImage:"square.and.arrow.up")}
                Text(scheduler.diagnosticsText.isEmpty ? "—" : scheduler.diagnosticsText).font(.caption.monospaced()).textSelection(.enabled).environment(\.layoutDirection,.leftToRight)
                Button(model.arabic ? "مسح السجل المحلي" : "Clear local log",role:.destructive) {scheduler.clearDiagnostics()}
            }.padding()
        }.navigationTitle(model.arabic ? "التشخيص المحلي" : "Local diagnostics").navigationBarTitleDisplayMode(.inline)
    }
}
