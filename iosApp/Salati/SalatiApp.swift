import SwiftUI
import BackgroundTasks
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    // Signers may rewrite the permitted ID along with the bundle identifier.
    static var refreshIdentifier: String {
        (Bundle.main.object(forInfoDictionaryKey:"BGTaskSchedulerPermittedIdentifiers") as? [String])?.first ?? "com.salati.prayertimes.ios.refresh"
    }
    func application(_ application: UIApplication,didFinishLaunchingWithOptions launchOptions:[UIApplication.LaunchOptionsKey:Any]?=nil) -> Bool {
        let center=UNUserNotificationCenter.current()
        center.delegate=self
        let play=UNNotificationAction(identifier:"PLAY_ADHAN",title:Settings.load().language == "ar" ? "تشغيل الأذان كاملًا الآن" : "Play full adhan now",options:.foreground)
        center.setNotificationCategories([UNNotificationCategory(identifier:"PRAYER",actions:[play],intentIdentifiers:[])])
        BGTaskScheduler.shared.register(forTaskWithIdentifier:Self.refreshIdentifier,using:nil) { task in
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
        let request=BGAppRefreshTaskRequest(identifier:refreshIdentifier)
        request.earliestBeginDate=Date(timeIntervalSinceNow:12*3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}
@main struct SalatiApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @StateObject private var model=AppModel()
    @State private var selectedTab=0
    @Environment(\.scenePhase) private var phase
    var body: some Scene {
        WindowGroup {
            RootView(selectedTab:$selectedTab)
                // Recreate UIKit-backed tab/navigation/form hosts when reading direction
                // changes. Keep selection and the app model outside this identity.
                .id(model.settings.language)
                .environmentObject(model)
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
    @Binding var selectedTab:Int
    var body: some View {
        Group {
            if !model.settings.onboardingComplete { OnboardingView() }
            else {
                TabView(selection:$selectedTab) {
                    Tab("Prayer",systemImage:"sun.horizon",value:0) { NavigationStack { HomeView() } }
                    Tab("Calendar",systemImage:"calendar",value:1) { NavigationStack { HijriView() } }
                    Tab("Qibla",systemImage:"location.north.circle",value:2) { NavigationStack { QiblaView() } }
                    Tab("Settings",systemImage:"slider.horizontal.3",value:3) { NavigationStack { SettingsView() } }
                }
            }
        }
        .alert("Unable to update",isPresented:Binding(get:{model.error != nil},set:{if !$0 {model.error=nil}})) { Button("OK") {model.error=nil} } message: {Text(model.error ?? "")}
    }
}
