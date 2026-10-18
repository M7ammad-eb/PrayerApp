import SwiftUI
import ActivityKit

// Keep the existing per-prayer page and its choices. Phase2 adds only delivery
// clarification, automatic countdown preference, real tests and local status.
struct NotificationSettingsView: View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var player=AdhanPlayer.shared
    @ObservedObject private var scheduler=PrayerScheduler.shared
    @State private var authorization:String?
    @State private var testPrayer="FAJR"
    @State private var testDelivery:Delivery = .notification
    @State private var testing=false
    private let prayers=["FAJR","DHUHR","ASR","MAGHRIB","ISHA"]
    var body:some View {
        Form {
            Section {
                Button("Allow notifications") {Task {authorization=await model.scheduler.authorizeNotifications();model.queueRefresh()}}
                Button("Allow system alarms") {Task {authorization=await model.scheduler.authorizeAlarms();model.queueRefresh()}}
                if let authorization {Text(LocalizedStringKey(authorization)).font(.footnote)}
            } footer: {Text("Notifications follow your system settings. System alarms require separate permission. No alerts are enabled by granting permission alone.")}
            ForEach(prayers,id:\.self) {id in
                Section(LocalizedStringKey(id)) {
                    Picker("Delivery",selection:policy(id,\.delivery)) {
                        Text("Off").tag(Delivery.off);Text("Notification").tag(Delivery.notification);Text("System alarm").tag(Delivery.alarm)
                    }
                    Picker("Sound",selection:policy(id,\.sound)) {
                        Text("System sound").tag("default")
                        if model.settings.policies[id]?.delivery != .alarm {Text("Silent").tag("silent")}
                        else if model.settings.policies[id]?.sound == "silent" {
                            Text(model.arabic ? "صامت — اختر صوتًا مسموعًا للمنبّه" : "Silent — choose an audible alarm sound").tag("silent")
                        }
                        if model.settings.policies[id]?.delivery == .alarm {
                            ForEach(Recording.all) {Text(model.arabic ? $0.arabic : $0.english).tag($0.id)}
                        } else if let selected=model.settings.policies[id]?.sound,Recording.all.contains(where:{$0.id == selected}) {
                            // Preserve the user's old recording choice through upgrade.
                            Text(model.arabic ? "الأذان المختار — صوت النظام للتنبيه مؤقتًا" : "Selected adhan — system notification sound for now").tag(selected)
                        }
                    }
                    Stepper(value:policy(id,\.reminder),in:0...60,step:5) {
                        LabeledContent("Reminder before prayer",value:(model.settings.policies[id]?.reminder ?? 0).description+" min")
                    }
                    if let sound=model.settings.policies[id]?.sound,Recording.all.contains(where:{$0.id == sound}) {
                        Button("Play full adhan now") {player.play(sound)}
                    }
                    if model.settings.policies[id]?.delivery == .alarm {
                        Text(model.arabic ? "يُرسل التسجيل الأصلي كاملًا إلى منبّه النظام. استمرار التشغيل الكامل أثناء القفل يحتاج إلى اختبار على هاتفك." : "The original full recording is requested from AlarmKit. Uninterrupted full playback while locked still needs testing on your phone.").font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
            Section(model.arabic ? "التذكير والأنشطة المباشرة" : "Reminders and Live Activities") {
                Toggle(model.arabic ? "عدّ تنازلي تلقائي قبل الصلاة بـ 15 دقيقة" : "Automatic countdown 15 minutes before prayer",isOn:$model.settings.liveActivitiesEnabled)
                Toggle(model.arabic ? "صوت النظام للتذكير المسبق" : "System sound for advance reminders",isOn:$model.settings.reminderSound)
                if model.settings.liveActivitiesEnabled && !ActivityAuthorizationInfo().areActivitiesEnabled {
                    Text(model.arabic ? "الأنشطة المباشرة غير مسموحة في إعدادات iOS. سيُستخدم تنبيه تذكير بديل." : "Live Activities are disabled in iOS Settings. A conventional reminder is used instead.").font(.footnote)
                }
                Text(model.arabic ? "يُجهّز العدّ التنازلي أثناء فتح التطبيق ويبدأ تلقائيًا عند الموعد. عند تعذّر تجهيزه يُستخدم تذكير عادي. قد يبقى النشاط منتهي الصلاحية حتى يتيح النظام للتطبيق تنفيذه مجددًا." : "Countdowns are prepared while Salati is open and start automatically at their scheduled time. A notification is used if preparation fails. An expired activity may remain until iOS lets the app execute again.").font(.footnote).foregroundStyle(.secondary)
            }
            Section(model.arabic ? "اختبار تنبيه مجدول" : "Scheduled test alert") {
                Picker(model.arabic ? "مسار الاختبار" : "Test delivery",selection:$testDelivery) {
                    Text("Notification").tag(Delivery.notification);Text("System alarm").tag(Delivery.alarm)
                }
                Picker(model.arabic ? "استخدام إعداد صوت الصلاة" : "Use prayer sound preference",selection:$testPrayer) {
                    ForEach(prayers,id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                }
                Button(model.arabic ? "اختبار بعد دقيقة — اقفل الهاتف" : "Test in one minute — lock the phone") {
                    testing=true
                    let sound=model.settings.policies[testPrayer]?.sound ?? "default"
                    Task {await scheduler.scheduleTest(delivery:testDelivery,sound:sound,settings:model.settings);testing=false}
                }.disabled(testing).accessibilityIdentifier("scheduleTestAlert")
                Button(model.arabic ? "إلغاء تنبيهات الاختبار" : "Cancel test alerts") {Task {await scheduler.cancelTests()}}
                if let status=scheduler.testStatus {Text(LocalizedStringKey(status)).font(.footnote)}
                Text(model.arabic ? "هذا تنبيه فعلي مجدول، وليس معاينة صوت فورية. له معرّفات منفصلة ولا يغيّر مواعيد الصلاة." : "This uses the real scheduled delivery path, not an immediate audio preview. Separate test identities preserve your prayer schedule.").font(.footnote).foregroundStyle(.secondary)
            }
            if player.isPlaying {
                Section {Button("Stop playback") {player.stop()}}
            }
            Section {
                Text(model.arabic ? "التنبيهات العادية تستخدم صوت النظام مؤقتًا أو الصمت حسب اختيارك. لم نعد نستخدم مقاطع الأذان المقتطعة. ننتظر تحديد مقطع قصير معتمد." : "Ordinary notifications temporarily use the system sound, or silence if selected. Arbitrary adhan excerpts are no longer used; an approved short recording is pending.")
                Text(model.arabic ? "تُجهَّز مواعيد متغيرة حسب سعة كل واجهة في iOS. افتح صلاتي بانتظام لتجديدها؛ تحديث الخلفية غير مضمون." : "Changing prayer times are prepared within each iOS API's capacity. Open Salati regularly to replenish them; background refresh is best effort.")
                if let status=model.scheduleStatus {Text(status)}
                if let error=player.error {Text(error).foregroundStyle(.red)}
                NavigationLink(model.arabic ? "تشخيص الجدولة المحلي" : "Local scheduling diagnostics") {SchedulingDiagnosticsView()}
            }.font(.footnote).foregroundStyle(.secondary)
        }.navigationTitle("Notifications and adhan").navigationBarTitleDisplayMode(.inline)
        // Deliberate user playback continues across navigation; only explicit
        // stop, completion or a real audio interruption ends it.
    }
    private func policy<T>(_ id:String,_ keyPath:WritableKeyPath<PrayerPolicy,T>) -> Binding<T> {
        Binding(get:{(model.settings.policies[id] ?? PrayerPolicy())[keyPath:keyPath]},set:{value in var p=model.settings.policies[id] ?? PrayerPolicy();p[keyPath:keyPath]=value;model.settings.policies[id]=p})
    }
}

private struct SchedulingDiagnosticsView:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject private var scheduler=PrayerScheduler.shared
    var body:some View {
        ScrollView {
            VStack(alignment:.leading,spacing:16) {
                Text(model.arabic ? "سجل محلي فقط. قبول الموعد من النظام لا يثبت وقت الصوت أو التسليم الفعلي. لا تعرض استجابة المستخدم على أنها وقت وصول التنبيه." : "Stored only on this phone. Scheduling acceptance does not prove audible delivery. A notification response records user interaction, not the actual delivery instant.").font(.callout)
                ShareLink(item:scheduler.diagnosticsText) {Label(model.arabic ? "مشاركة السجل" : "Share local log",systemImage:"square.and.arrow.up")}
                Text(scheduler.diagnosticsText.isEmpty ? "—" : scheduler.diagnosticsText).font(.caption.monospaced()).textSelection(.enabled).environment(\.layoutDirection,.leftToRight)
                Button(model.arabic ? "مسح السجل المحلي" : "Clear local log",role:.destructive) {scheduler.clearDiagnostics()}
            }.padding()
        }.navigationTitle(model.arabic ? "التشخيص المحلي" : "Local diagnostics").navigationBarTitleDisplayMode(.inline)
    }
}
