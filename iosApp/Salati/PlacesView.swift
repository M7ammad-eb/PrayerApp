import SwiftUI
import CoreLocation
import UserNotifications

struct PlacesView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) var dismiss
    @State private var query=""
    @State private var results:[Place]=[]
    @State private var failure:String?
    @State private var searching=false
    @State private var manual=false
    var body: some View {
        List {
            Section {
                CurrentLocationButton(service:model.locationService)
                LocationStatusView(service:model.locationService)
                Button {manual=true} label: { Label("Enter coordinates",systemImage:"mappin.and.ellipse") }
            } footer: { Text("Search for a city, or use your current location.") }
            if searching {HStack {ProgressView();Text("Searching…")}}
            if let failure { Text(LocalizedStringKey(failure)).foregroundStyle(.secondary) }
            if let location=model.settings.location {
                Section("Selected location") {
                    Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").foregroundStyle(Color.accentColor)
                    Text(LocalizedStringKey(location.isGPS ? "Current location" : "Selected city")).font(.footnote).foregroundStyle(.secondary)
                }
            }
            if !searching && !query.isEmpty && results.isEmpty && failure == nil { ContentUnavailableView.search(text:query) }
            if query.isEmpty { ContentUnavailableView("Find your city",systemImage:"magnifyingglass",description:Text("Search by city name.")) }
            ForEach(results) { place in
                Button { model.select(place.location); dismiss() } label: {
                    VStack(alignment:.leading,spacing:4) { Text(model.arabic ? (place.arabicName ?? place.name) : place.name); Text(model.locale.localizedString(forRegionCode:place.country) ?? place.country).font(.caption).foregroundStyle(.secondary) }
                }.accessibilityIdentifier("city.\(place.id)")
            }
        }
        .searchable(text:$query,prompt:"City name")
        .navigationTitle("Location").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement:.confirmationAction) { Button("Done") {dismiss()} } }
        .sheet(isPresented:$manual) { NavigationStack { ManualLocationView() } }
        .task(id:query) {
            searching = !query.isEmpty;results=[];failure=nil
            do { try await Task.sleep(for:.milliseconds(200)); let found=try await model.places.search(query); try Task.checkCancellation(); results=found; searching=false }
            catch is CancellationError { }
            catch { failure="City search is unavailable. Try again or enter coordinates.";searching=false }
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
            Text("Choose the time zone for this location.").font(.footnote).foregroundStyle(.secondary)
        }
        .navigationTitle("Coordinates").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement:.confirmationAction) { Button("Save") {
            guard valid,let lat=coordinate(latitude),let lon=coordinate(longitude) else {return}
            model.select(.init(name:name.isEmpty ? "\(latitude), \(longitude)" : name,latitude:lat,longitude:lon,zone:zone)); dismiss()
        }.disabled(!valid) } }
    }
}
// First-run setup: welcome, location, alerts, ready. The system permission prompts appear on
// their own as the matching page opens (after a short beat so the page can be read first);
// nothing needs an extra "enable" tap, and declining either one still leaves a clear way forward.
struct OnboardingView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var review=false
    @State private var showPlaces=false
    @State private var showCalculation=false
    @State private var showAlerts=false
    @State private var locationAsked=false
    @State private var alertsAsked=false
    @State private var alertDefaultsApplied=false
    @State private var alertsStatus:UNAuthorizationStatus?
    private let stepCount=4
    private var step:Int {min(stepCount-1,max(0,review ? model.setupReviewStep:model.onboardingStep))}
    private let titles=["Welcome to Salati","Where do you pray?","Never miss a prayer","Make time for prayer"]
    private let subtitles=["Prayer times, wherever your day takes you.","Find prayer times for your city or current location.","Salati can notify you at each prayer time and count down to the next one.","Your prayer times are ready. Make Salati your own, or begin your day."]
    private let symbols=["sun.horizon.fill","location.fill","bell.badge.fill","checkmark"]
    private var buttonTitle:String {
        switch step {
        case 0:return "Get started"
        case stepCount-1:return review ? "Done":"Start using Salati"
        default:return "Continue"
        }
    }
    var body: some View {
        NavigationStack {
            VStack(spacing:0) {
                ScrollView {
                    VStack(spacing:28) {
                        VStack(spacing:14) {
                            Image(systemName:symbols[step]).font(.system(size:42,weight:.medium)).foregroundStyle(Color.accentColor)
                                .frame(width:96,height:96).background(Color.accentColor.opacity(0.12),in:Circle())
                                .accessibilityHidden(true)
                            Text(LocalizedStringKey(titles[step])).font(.largeTitle.bold()).multilineTextAlignment(.center).fixedSize(horizontal:false,vertical:true)
                            Text(LocalizedStringKey(subtitles[step])).font(.title3).foregroundStyle(.secondary).multilineTextAlignment(.center).fixedSize(horizontal:false,vertical:true)
                        }.padding(.top,16)
                        page
                    }.frame(maxWidth:560).padding(.horizontal,24).padding(.bottom,24).frame(maxWidth:.infinity)
                }.id(step)
                footer
            }
            .background(Color(uiColor:.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if step>0 {ToolbarItem(placement:.topBarLeading) {Button("Back",systemImage:"chevron.backward") {move(to:step-1)}.accessibilityIdentifier("onboardingBack")}}
                if review {ToolbarItem(placement:.cancellationAction) {Button("Close") {dismiss()}}}
            }
            .sheet(isPresented:$showPlaces) {NavigationStack {PlacesView()}}
            .sheet(isPresented:$showCalculation) {calculationSheet}
            .sheet(isPresented:$showAlerts) {
                NavigationStack {NotificationSettingsView().toolbar {ToolbarItem(placement:.confirmationAction) {Button("Done") {showAlerts=false}}}}
            }
            .task(id:step) {
                if step == 1 {await autoRequestLocation()}
                else if step == 2 {await autoRequestAlerts()}
            }
            .onChange(of:scenePhase) {_,phase in
                if phase == .active && step == 2 {Task {await refreshAlertsStatus()}}
            }
        }
    }
    private var footer:some View {
        VStack(spacing:14) {
            HStack(spacing:6) {
                ForEach(0..<stepCount,id:\.self) {index in
                    Capsule().fill(index == step ? Color.accentColor : Color.secondary.opacity(0.25))
                        .frame(width:index == step ? 22:8,height:8)
                }
            }.accessibilityElement(children:.ignore).accessibilityLabel(Text("Setup progress"))
                .accessibilityValue("\(step+1) / \(stepCount)").accessibilityIdentifier("onboardingStep")
            Button(action:advance) {
                Text(LocalizedStringKey(buttonTitle)).font(.headline).frame(maxWidth:.infinity,minHeight:50)
            }.buttonStyle(.borderedProminent).controlSize(.large).disabled(step == 1 && model.settings.location == nil)
                .accessibilityIdentifier("continueOnboarding")
        }.frame(maxWidth:560).padding(.horizontal,24).padding(.vertical,16).frame(maxWidth:.infinity).background(.bar)
    }
    @ViewBuilder private var page:some View {
        switch step {
        case 0:
            VStack(spacing:20) {
                VStack(alignment:.leading,spacing:18) {
                    featureRow("sun.horizon","Prayer times")
                    featureRow("location.north.circle","Qibla direction")
                    featureRow("calendar","Hijri calendar")
                }.setupCard()
                Button {
                    if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}
                } label: {
                    HStack {Label("Language",systemImage:"globe");Spacer();Text(model.languageDisplayName).foregroundStyle(.secondary);Image(systemName:"arrow.up.forward").font(.caption)}
                        .font(.subheadline).padding(.vertical,4)
                }.accessibilityIdentifier("onboardingLanguage")
            }
        case 1:
            SetupLocationStep(service:model.locationService,choose:{showPlaces=true})
        case 2:
            SetupAlertsStep(status:alertsStatus,language:model.settings.language,
                            customize:{showAlerts=true},
                            openSettings:{if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}})
        default:
            VStack(spacing:20) {
                VStack(alignment:.leading,spacing:16) {
                    if let next=model.presentationSnapshot?.events.first(where:{$0.id == model.nextEventID}) {
                        Text("Up next").font(.subheadline.weight(.medium)).foregroundStyle(Color.accentColor)
                        ViewThatFits(in:.horizontal) {
                            HStack(alignment:.firstTextBaseline) {
                                Text(next.title).font(.title.bold())
                                Spacer(minLength:16)
                                Text(next.timeText).font(.title2.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                            }
                            VStack(alignment:.leading,spacing:8) {
                                Text(next.title).font(.title.bold())
                                Text(next.timeText).font(.title2.weight(.medium)).monospacedDigit().environment(\.layoutDirection,.leftToRight)
                            }
                        }.accessibilityElement(children:.combine).accessibilityIdentifier("onboardingNextPrayer")
                        Divider()
                    }
                    Label(model.locationDisplayName,systemImage:"checkmark.circle.fill").font(.subheadline).foregroundStyle(.secondary)
                }.setupCard()
                VStack(spacing:0) {
                    Button {showCalculation=true} label:{option("Adjust prayer times",symbol:"slider.horizontal.3")}.accessibilityIdentifier("onboardingAdvanced")
                    Divider().padding(.horizontal,20)
                    Button {showAlerts=true} label:{option("Choose prayer alerts",symbol:"bell")}.accessibilityIdentifier("onboardingAlertSettings")
                }.background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24,style:.continuous))
                Text("You can change these choices in Settings.").font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
    private func featureRow(_ symbol:String,_ title:LocalizedStringKey)->some View {
        HStack(spacing:16) {
            Image(systemName:symbol).font(.title3).foregroundStyle(Color.accentColor).frame(width:32).accessibilityHidden(true)
            Text(title).font(.headline)
            Spacer(minLength:0)
        }
    }
    private func option(_ title:LocalizedStringKey,symbol:String)->some View {
        HStack {Label(title,systemImage:symbol);Spacer();Image(systemName:"chevron.forward").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)}
            .font(.body).padding(20).contentShape(Rectangle())
    }
    private var calculationSheet:some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Method",selection:Binding(get:{model.settings.method},set:{model.settings.calculationMethodOverridden=true;model.settings.method=$0})) {
                        ForEach(model.engine.calculationMethods,id:\.self) {Text(LocalizedStringKey($0)).tag($0)}
                    }
                    Toggle("Hanafi Asr",isOn:Binding(get:{model.settings.hanafi},set:{model.settings.asrOverridden=true;model.settings.hanafi=$0}))
                } footer:{Text("Choose the method used by your local mosque.")}
            }.navigationTitle("Prayer calculation").navigationBarTitleDisplayMode(.inline)
                .toolbar {ToolbarItem(placement:.confirmationAction) {Button("Done") {showCalculation=false}}}
        }
    }
    private func move(to value:Int) {
        withAnimation(reduceMotion ? nil:.easeInOut(duration:0.2)) {if review {model.setupReviewStep=value}else {model.onboardingStep=value}}
    }
    private func advance() {
        if step<stepCount-1 {if step == 1 {model.applyOnboardingDefaults()};move(to:step+1)}
        else if review {dismiss()}
        else {model.settings.onboardingComplete=true;model.onboardingStep=0}
    }
    // The location prompt opens by itself the first time this page appears. If location is turned
    // off for Salati, the page offers choosing a city instead.
    private func autoRequestLocation() async {
        guard !locationAsked,model.settings.location == nil else {return}
        locationAsked=true
        do {try await Task.sleep(for:.milliseconds(450))} catch {locationAsked=false;return}
        guard step == 1 else {locationAsked=false;return}
        switch CLLocationManager().authorizationStatus {
        case .denied,.restricted:break
        default:model.locationService.request()
        }
    }
    private func refreshAlertsStatus() async {
        alertsStatus=await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }
    // The notification prompt also opens by itself. Allowing it switches on prayer alerts and the
    // countdown with sensible defaults; they can be changed from the page or later in Settings.
    private func autoRequestAlerts() async {
        await refreshAlertsStatus()
        if alertsStatus == .notDetermined,!alertsAsked {
            alertsAsked=true
            do {try await Task.sleep(for:.milliseconds(450))} catch {alertsAsked=false;return}
            guard step == 2 else {alertsAsked=false;return}
            _=await model.scheduler.authorizeNotifications()
            await refreshAlertsStatus()
        }
        if !review,!alertDefaultsApplied,let status=alertsStatus,SetupAlertsStep.isAllowed(status) {
            alertDefaultsApplied=true
            var updated=model.settings
            for id in ["FAJR","DHUHR","ASR","MAGHRIB","ISHA"] {
                var policy=updated.policies[id] ?? PrayerPolicy()
                if policy.delivery == .off {policy.delivery = .notification}
                updated.policies[id]=policy
            }
            updated.liveActivitiesEnabled=true
            model.settings=updated
        }
    }
}

private extension View {
    func setupCard()->some View {
        padding(20).frame(maxWidth:.infinity,alignment:.leading)
            .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:24,style:.continuous))
    }
}

private struct SetupLocationStep:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject var service:LocationService
    let choose:()->Void
    var body:some View {
        VStack(spacing:16) {
            if let location=model.settings.location {
                HStack(spacing:14) {
                    Image(systemName:"checkmark.circle.fill").font(.system(size:34)).foregroundStyle(Color.accentColor).accessibilityHidden(true)
                    VStack(alignment:.leading,spacing:2) {
                        Text(model.locationDisplayName).font(.title3.weight(.semibold)).fixedSize(horizontal:false,vertical:true)
                            .accessibilityIdentifier("onboardingSelectedLocation")
                        Text(LocalizedStringKey(location.isGPS ? "Current location" : "Selected city")).font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer(minLength:0)
                }.setupCard()
                Button("Choose a different city",action:choose).buttonStyle(.bordered).accessibilityIdentifier("chooseLocation")
            } else if service.isAcquiring {
                HStack(spacing:14) {
                    ProgressView()
                    Text(LocalizedStringKey(service.message ?? "Finding your location…")).font(.headline)
                    Spacer(minLength:0)
                }.setupCard().accessibilityIdentifier("locationProgress")
            } else {
                VStack(alignment:.leading,spacing:10) {
                    Image(systemName:"mappin.slash").font(.title2).foregroundStyle(.secondary).accessibilityHidden(true)
                    Text("Choose your city to get prayer times. You can allow location later in iOS Settings.").font(.subheadline)
                    if let message=service.message {Text(LocalizedStringKey(message)).font(.footnote).foregroundStyle(.secondary)}
                }.setupCard()
                Button {choose()} label:{Text("Choose a city").font(.headline).frame(maxWidth:.infinity,minHeight:44)}
                    .buttonStyle(.borderedProminent).accessibilityIdentifier("chooseLocation")
                Button("Try again") {service.request()}.buttonStyle(.bordered)
            }
            Text("Prayer times are calculated entirely on your device.").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
    }
}

private struct SetupAlertsStep:View {
    let status:UNAuthorizationStatus?
    let language:String
    let customize:()->Void
    let openSettings:()->Void
    static func isAllowed(_ status:UNAuthorizationStatus)->Bool {
        status == .authorized || status == .provisional || status == .ephemeral
    }
    private var prayerNames:String {
        ["FAJR","DHUHR","ASR","MAGHRIB","ISHA"].map {AppLanguage.text($0,language:language)}.joined(separator:" · ")
    }
    var body:some View {
        VStack(spacing:16) {
            if let status,SetupAlertsStep.isAllowed(status) {
                VStack(alignment:.leading,spacing:12) {
                    Label("Alerts are on",systemImage:"checkmark.circle.fill").font(.headline).foregroundStyle(Color.accentColor)
                    Text(prayerNames).font(.subheadline)
                    Divider()
                    Label("Prayer countdown",systemImage:"timer").font(.subheadline.weight(.medium))
                    Text("Starts 15 minutes before each prayer.").font(.footnote).foregroundStyle(.secondary)
                }.setupCard()
                Button("Customize alerts",action:customize).buttonStyle(.bordered).accessibilityIdentifier("onboardingAlertSettings")
            } else if status == .denied {
                VStack(alignment:.leading,spacing:10) {
                    Label("Alerts are off",systemImage:"bell.slash").font(.headline)
                    Text("You can turn alerts on any time in iOS Settings.").font(.subheadline).foregroundStyle(.secondary)
                }.setupCard()
                Button("Open iOS Settings",action:openSettings).buttonStyle(.bordered)
            } else {
                VStack(alignment:.leading,spacing:10) {
                    Label("Allow notifications to be reminded at prayer time.",systemImage:"bell").font(.headline)
                }.setupCard()
            }
        }
    }
}

private struct CurrentLocationButton:View {
    @ObservedObject var service:LocationService
    var body:some View {
        Button {service.request()} label: {Label("Use current location",systemImage:"location.fill").frame(maxWidth:.infinity,minHeight:44,alignment:.leading).contentShape(Rectangle())}.disabled(service.isAcquiring)
    }
}
