import SwiftUI
import BackgroundTasks
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,didFinishLaunchingWithOptions launchOptions:[UIApplication.LaunchOptionsKey:Any]?=nil) -> Bool {
        let center=UNUserNotificationCenter.current()
        center.delegate=self
        let play=UNNotificationAction(identifier:"PLAY_ADHAN",title:Settings.load().language == "ar" ? "تشغيل الأذان كاملًا الآن" : "Play full adhan now",options:.foreground)
        center.setNotificationCategories([UNNotificationCategory(identifier:"PRAYER",actions:[play],intentIdentifiers:[])])
        BGTaskScheduler.shared.register(forTaskWithIdentifier:"com.salati.prayertimes.ios.refresh",using:nil) { task in
            let work=Task { @MainActor in
                let model=AppModel(); await model.refresh(); task.setTaskCompleted(success:!Task.isCancelled && model.error == nil)
            }
            task.expirationHandler={ work.cancel() }
        }
        return true
    }
    func userNotificationCenter(_ center:UNUserNotificationCenter,willPresent notification:UNNotification,withCompletionHandler completion:@escaping (UNNotificationPresentationOptions)->Void) {completion([.banner,.list,.sound])}
    func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse,withCompletionHandler completion:@escaping ()->Void) {
        if response.actionIdentifier == "PLAY_ADHAN",let recording=response.notification.request.content.userInfo["recording"] as? String {
            Task { @MainActor in AdhanPlayer.shared.play(recording) }
        }
        completion()
    }
    static func requestRefresh() {
        let request=BGAppRefreshTaskRequest(identifier:"com.salati.prayertimes.ios.refresh")
        request.earliestBeginDate=Date(timeIntervalSinceNow:12*3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}
@main struct SalatiApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var model=AppModel()
    @Environment(\.scenePhase) private var phase
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(model)
                .environment(\.locale,model.locale)
                .environment(\.layoutDirection,model.arabic ? .rightToLeft : .leftToRight)
                .preferredColorScheme(model.settings.appearance == "dark" ? .dark : model.settings.appearance == "light" ? .light : nil)
                .tint(Color("AccentColor"))
                .task { await model.refresh() }
                .onChange(of:phase) { _,phase in if phase == .active {model.queueRefresh();if model.settings.location?.isGPS == true {model.locationService.request()}} }
                .onReceive(NotificationCenter.default.publisher(for:UIApplication.significantTimeChangeNotification)) { _ in model.queueRefresh() }
        }
    }
}
struct RootView: View {
    @EnvironmentObject var model: AppModel
    var body: some View {
        Group {
            if !model.settings.onboardingComplete { OnboardingView() }
            else {
                TabView {
                    Tab("Prayer",systemImage:"sun.horizon") { NavigationStack { HomeView() } }
                    Tab("Calendar",systemImage:"calendar") { NavigationStack { HijriView() } }
                    Tab("Qibla",systemImage:"location.north.circle") { NavigationStack { QiblaView() } }
                    Tab("Settings",systemImage:"slider.horizontal.3") { NavigationStack { SettingsView() } }
                }
            }
        }
        .alert("Unable to update",isPresented:Binding(get:{model.error != nil},set:{if !$0 {model.error=nil}})) { Button("OK") {model.error=nil} } message: {Text(model.error ?? "")}
    }
}
