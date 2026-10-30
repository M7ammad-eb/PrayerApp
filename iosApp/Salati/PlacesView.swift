import SwiftUI
import CoreLocation
import UserNotifications
import AlarmKit

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
// First-run setup. Each page is one decision, and nothing is requested until the person acts:
// choosing "Use my current location" opens the location prompt, and Continue on the alerts page
// (after choosing alarm or notification for all five prayers) opens the matching permission.
private enum SetupAlertChoice {case alarm,notification,off}
private enum SetupPage {case welcome,location,alerts,adhan,ready}

struct OnboardingView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ObservedObject private var player=AdhanPlayer.shared
    var review=false
    @State private var showPlaces=false
    @State private var showCalculation=false
    @State private var showAlerts=false
    @State private var alertChoice=SetupAlertChoice.alarm
    @State private var adhanChoice=Recording.all.first?.id ?? "default"
    @State private var countdown=true
    @State private var permissionNote:LocalizedStringKey?
    @State private var committing=false
    @State private var playingID:String?
    private let prayers=["FAJR","DHUHR","ASR","MAGHRIB","ISHA"]
    private var pages:[SetupPage] {
        alertChoice == .alarm ? [.welcome,.location,.alerts,.adhan,.ready] : [.welcome,.location,.alerts,.ready]
    }
    private var step:Int {min(pages.count-1,max(0,review ? model.setupReviewStep:model.onboardingStep))}
    private var current:SetupPage {pages[step]}
    private var title:String {
        switch current {
        case .welcome:"Welcome to Salati"
        case .location:"Where do you pray?"
        case .alerts:"How should Salati alert you?"
        case .adhan:"Choose the adhan"
        case .ready:"Make time for prayer"
        }
    }
    private var subtitle:String {
        switch current {
        case .welcome:"Prayer times, wherever your day takes you."
        case .location:"Find prayer times for your city or current location."
        case .alerts:"Choose once for all five prayers. You can fine-tune each prayer later."
        case .adhan:"Pick the recitation that plays at prayer time."
        case .ready:"Your prayer times are ready. Make Salati your own, or begin your day."
        }
    }
    private var symbol:String {
        switch current {
        case .welcome:"moon.stars.fill"
        case .location:"location.fill"
        case .alerts:"bell.badge.fill"
        case .adhan:"speaker.wave.2.fill"
        case .ready:"checkmark"
        }
    }
    private var buttonTitle:String {
        switch current {
        case .welcome:"Let's begin"
        case .ready:review ? "Done":"Start using Salati"
        default:"Continue"
        }
    }
    private var canContinue:Bool {!(current == .location && model.settings.location == nil) && !committing}
    var body: some View {
        NavigationStack {
            VStack(spacing:0) {
                ScrollView {
                    VStack(spacing:26) {
                        header
                        content
                    }.frame(maxWidth:560).padding(.horizontal,20).padding(.top,12).padding(.bottom,20).frame(maxWidth:.infinity)
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
            .onAppear {if review {loadCurrentChoices()}}
            .onChange(of:alertChoice) {permissionNote=nil}
            .onDisappear {stopPreview()}
        }
    }
    private var header:some View {
        VStack(spacing:12) {
            if current == .welcome {
                ZStack {
                    RoundedRectangle(cornerRadius:28,style:.continuous)
                        .fill(LinearGradient(colors:[Color.accentColor,Color.accentColor.opacity(0.7)],startPoint:.topLeading,endPoint:.bottomTrailing))
                    Image(systemName:symbol).font(.system(size:50,weight:.medium)).foregroundStyle(.white)
                }.frame(width:104,height:104).shadow(color:Color.accentColor.opacity(0.25),radius:14,y:6).accessibilityHidden(true)
            } else {
                Image(systemName:symbol).font(.system(size:28,weight:.semibold)).foregroundStyle(Color.accentColor)
                    .frame(width:64,height:64).background(Color.accentColor.opacity(0.12),in:Circle()).accessibilityHidden(true)
            }
            Text(LocalizedStringKey(title)).font(.title.bold()).fixedSize(horizontal:false,vertical:true)
            Text(LocalizedStringKey(subtitle)).font(.body).foregroundStyle(.secondary).fixedSize(horizontal:false,vertical:true)
        }.multilineTextAlignment(.center).padding(.top,current == .welcome ? 28:8)
    }
    private var footer:some View {
        VStack(spacing:14) {
            HStack(spacing:6) {
                ForEach(0..<pages.count,id:\.self) {index in
                    Capsule().fill(index == step ? Color.accentColor : Color.secondary.opacity(0.25))
                        .frame(width:index == step ? 20:7,height:7)
                }
            }.accessibilityElement(children:.ignore).accessibilityLabel(Text("Setup progress"))
                .accessibilityValue("\(step+1) / \(pages.count)").accessibilityIdentifier("onboardingStep")
            Button(action:advance) {
                Text(LocalizedStringKey(buttonTitle)).font(.headline).frame(maxWidth:260,minHeight:44)
            }.buttonStyle(.borderedProminent).buttonBorderShape(.capsule).disabled(!canContinue)
                .accessibilityIdentifier("continueOnboarding")
        }.frame(maxWidth:.infinity).padding(.top,10).padding(.bottom,16)
    }
    @ViewBuilder private var content:some View {
        switch current {
        case .welcome:
            VStack(spacing:18) {
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
        case .location:
            SetupLocationStep(service:model.locationService,choose:{showPlaces=true})
        case .alerts:
            VStack(spacing:12) {
                alertOption(.alarm,symbol:"alarm.fill",title:"System alarm",subtitle:"Rings like an alarm, even when your iPhone is on silent.",badge:"Recommended")
                alertOption(.notification,symbol:"bell.fill",title:"Notification",subtitle:"A banner and sound at prayer time.")
                alertOption(.off,symbol:"bell.slash.fill",title:"Off",subtitle:"No prayer alerts. You can set them up later.")
                Toggle(isOn:$countdown) {
                    VStack(alignment:.leading,spacing:3) {
                        Text("Prayer countdown").font(.headline)
                        Text("Starts 15 minutes before each prayer.").font(.subheadline).foregroundStyle(.secondary)
                    }
                }.padding(14).background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:20,style:.continuous))
                if alertChoice == .alarm && permissionNote == nil {
                    Text("You'll be asked to allow alarms and notifications.").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                if let permissionNote {
                    VStack(spacing:8) {
                        Label {Text(permissionNote)} icon:{Image(systemName:"exclamationmark.triangle.fill")}.font(.subheadline.weight(.medium))
                        Button("Open iOS Settings") {if let url=URL(string:UIApplication.openSettingsURLString) {openURL(url)}}.buttonStyle(.bordered).controlSize(.small)
                        Text("You can still continue and allow this later.").font(.footnote).foregroundStyle(.secondary)
                    }.multilineTextAlignment(.center).padding(.top,4)
                }
            }
        case .adhan:
            VStack(spacing:10) {
                ForEach(Recording.all) {recording in
                    let selected=adhanChoice == recording.id
                    let playing=player.isPlaying && playingID == recording.id
                    HStack(spacing:8) {
                        Button {adhanChoice=recording.id} label: {
                            HStack(spacing:10) {
                                Image(systemName:selected ? "checkmark.circle.fill":"circle").font(.title3)
                                    .foregroundStyle(selected ? Color.accentColor : Color.secondary.opacity(0.5))
                                Text(model.arabic ? recording.arabic : recording.english).font(.body).multilineTextAlignment(.leading)
                                Spacer(minLength:0)
                            }.padding(.vertical,14).padding(.leading,14).contentShape(Rectangle())
                        }.buttonStyle(.plain)
                        Button {
                            if playing {stopPreview()} else {adhanChoice=recording.id;playingID=recording.id;player.play(recording.id)}
                        } label: {
                            Image(systemName:playing ? "stop.circle.fill":"play.circle.fill").font(.title).foregroundStyle(Color.accentColor)
                                .frame(width:48,height:48).contentShape(Rectangle())
                        }.buttonStyle(.plain).accessibilityLabel(Text(LocalizedStringKey(playing ? "Stop playback":"Play full adhan now")))
                    }.background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:18,style:.continuous))
                        .overlay(RoundedRectangle(cornerRadius:18,style:.continuous).strokeBorder(selected ? Color.accentColor : Color.clear,lineWidth:2))
                }
            }
        case .ready:
            VStack(spacing:18) {
                VStack(alignment:.leading,spacing:14) {
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
                    Label(model.locationDisplayName,systemImage:"mappin.and.ellipse").font(.subheadline).foregroundStyle(.secondary)
                    Label(LocalizedStringKey(alertSummary),systemImage:alertChoice == .off ? "bell.slash":"bell").font(.subheadline).foregroundStyle(.secondary)
                }.setupCard()
                VStack(spacing:0) {
                    Button {showCalculation=true} label:{option("Adjust prayer times",symbol:"slider.horizontal.3")}.accessibilityIdentifier("onboardingAdvanced")
                    Divider().padding(.horizontal,20)
                    Button {showAlerts=true} label:{option("Choose prayer alerts",symbol:"bell")}.accessibilityIdentifier("onboardingAlertSettings")
                }.background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:20,style:.continuous))
                Text("You can change these choices in Settings.").font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
    private var alertSummary:String {
        switch alertChoice {
        case .alarm:"System alarm"
        case .notification:"Notification"
        case .off:"Alerts are off"
        }
    }
    private func alertOption(_ choice:SetupAlertChoice,symbol:String,title:LocalizedStringKey,subtitle:LocalizedStringKey,badge:LocalizedStringKey?=nil)->some View {
        Button {alertChoice=choice} label: {
            SetupOptionCard(symbol:symbol,title:title,subtitle:subtitle,badge:badge,selected:alertChoice == choice)
        }.buttonStyle(.plain).accessibilityAddTraits(alertChoice == choice ? .isSelected : [])
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
            .font(.body).padding(18).contentShape(Rectangle())
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
    private func stopPreview() {
        if player.isPlaying {player.stop(reason:"setup preview ended")}
        playingID=nil
    }
    private func move(to value:Int) {
        stopPreview()
        withAnimation(reduceMotion ? nil:.easeInOut(duration:0.2)) {if review {model.setupReviewStep=value}else {model.onboardingStep=value}}
    }
    private func advance() {
        switch current {
        case .alerts:Task {await commitAlerts()}
        case .adhan:applyAdhan();move(to:step+1)
        case .location:model.applyOnboardingDefaults();move(to:step+1)
        case .ready:
            if review {dismiss()} else {model.settings.onboardingComplete=true;model.onboardingStep=0}
        case .welcome:move(to:step+1)
        }
    }
    private func loadCurrentChoices() {
        let policy=model.settings.policies["FAJR"] ?? PrayerPolicy()
        switch policy.delivery {
        case .alarm:alertChoice = .alarm
        case .notification:alertChoice = .notification
        case .off:alertChoice = .off
        }
        if Recording.all.contains(where:{$0.id == policy.sound}) {adhanChoice=policy.sound}
        countdown=model.settings.liveActivitiesEnabled
    }
    // One choice for all five prayers; fine-tuning per prayer stays in Settings.
    private func applyAlertChoice() {
        var updated=model.settings
        for id in prayers {
            var policy=updated.policies[id] ?? PrayerPolicy()
            switch alertChoice {
            case .alarm:
                policy.delivery = .alarm
                if !Recording.all.contains(where:{$0.id == policy.sound}) {policy.sound=adhanChoice}
            case .notification:policy.delivery = .notification
            case .off:policy.delivery = .off
            }
            updated.policies[id]=policy
        }
        updated.liveActivitiesEnabled=countdown
        model.settings=updated
    }
    private func applyAdhan() {
        guard alertChoice == .alarm else {return}
        var updated=model.settings
        for id in prayers {
            var policy=updated.policies[id] ?? PrayerPolicy()
            policy.sound=adhanChoice
            updated.policies[id]=policy
        }
        model.settings=updated
    }
    // Continue on the alerts page opens the matching system permission first and saves the choice
    // afterwards, so the scheduler is not rebuilding alarms underneath the prompt. If a permission
    // is declined the page says so once, and a second Continue moves on.
    private func commitAlerts() async {
        guard !committing else {return}
        committing=true
        defer {committing=false}
        if permissionNote != nil {applyAlertChoice();move(to:step+1);return}
        switch alertChoice {
        case .off:
            applyAlertChoice();move(to:step+1)
        case .notification:
            let granted=await requestNotifications()
            applyAlertChoice()
            if granted {move(to:step+1)} else {permissionNote="Notifications are turned off in iOS Settings."}
        case .alarm:
            let granted=await requestAlarms()
            // The countdown fallback and the "open Salati to refresh" reminder are notifications,
            // so alarm users are asked for that permission as well.
            _=await requestNotifications()
            applyAlertChoice()
            if granted {move(to:step+1)} else {permissionNote="Alarms are turned off in iOS Settings."}
        }
    }
    private func requestNotifications() async -> Bool {
        let center=UNUserNotificationCenter.current()
        var status=await center.notificationSettings().authorizationStatus
        if status == .notDetermined {
            _=await model.scheduler.authorizeNotifications()
            status=await center.notificationSettings().authorizationStatus
        }
        return status == .authorized || status == .provisional || status == .ephemeral
    }
    // Waits for the person's decision itself rather than for the request call to return, so the
    // page can never stay stuck behind a request that does not come back.
    private func requestAlarms() async -> Bool {
        if AlarmManager.shared.authorizationState == .notDetermined {
            Task {_=await model.scheduler.authorizeAlarms()}
            for _ in 0..<480 {
                if AlarmManager.shared.authorizationState != .notDetermined {break}
                try? await Task.sleep(for:.milliseconds(250))
            }
        }
        return AlarmManager.shared.authorizationState == .authorized
    }
}

private extension View {
    func setupCard()->some View {
        padding(20).frame(maxWidth:.infinity,alignment:.leading)
            .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:22,style:.continuous))
    }
}

private struct SetupOptionCard:View {
    let symbol:String
    let title:LocalizedStringKey
    var subtitle:LocalizedStringKey?=nil
    var badge:LocalizedStringKey?=nil
    var selected:Bool?=nil
    var body:some View {
        HStack(spacing:14) {
            Image(systemName:symbol).font(.title3).foregroundStyle(Color.accentColor).frame(width:42,height:42)
                .background(Color.accentColor.opacity(0.12),in:RoundedRectangle(cornerRadius:12,style:.continuous)).accessibilityHidden(true)
            VStack(alignment:.leading,spacing:3) {
                HStack(spacing:6) {
                    Text(title).font(.headline)
                    if let badge {
                        Text(badge).font(.caption2.weight(.semibold)).foregroundStyle(Color.accentColor)
                            .padding(.horizontal,7).padding(.vertical,2).background(Color.accentColor.opacity(0.15),in:Capsule())
                    }
                }
                if let subtitle {Text(subtitle).font(.subheadline).foregroundStyle(.secondary).fixedSize(horizontal:false,vertical:true)}
            }.multilineTextAlignment(.leading)
            Spacer(minLength:0)
            if let selected {
                Image(systemName:selected ? "checkmark.circle.fill":"circle").font(.title3)
                    .foregroundStyle(selected ? Color.accentColor : Color.secondary.opacity(0.5))
            } else {
                Image(systemName:"chevron.forward").font(.footnote.weight(.semibold)).foregroundStyle(.tertiary)
            }
        }
        .padding(14).frame(maxWidth:.infinity,alignment:.leading)
        .background(Color(uiColor:.secondarySystemGroupedBackground),in:RoundedRectangle(cornerRadius:20,style:.continuous))
        .overlay(RoundedRectangle(cornerRadius:20,style:.continuous).strokeBorder(selected == true ? Color.accentColor : Color.clear,lineWidth:2))
        .contentShape(RoundedRectangle(cornerRadius:20,style:.continuous))
    }
}

// Nothing is requested here until the person taps "Use my current location".
private struct SetupLocationStep:View {
    @EnvironmentObject var model:AppModel
    @ObservedObject var service:LocationService
    let choose:()->Void
    var body:some View {
        VStack(spacing:12) {
            if let location=model.settings.location {
                HStack(spacing:14) {
                    Image(systemName:"checkmark.circle.fill").font(.system(size:32)).foregroundStyle(Color.accentColor).accessibilityHidden(true)
                    VStack(alignment:.leading,spacing:2) {
                        Text(model.locationDisplayName).font(.title3.weight(.semibold)).fixedSize(horizontal:false,vertical:true)
                            .accessibilityIdentifier("onboardingSelectedLocation")
                        Text(LocalizedStringKey(location.isGPS ? "Current location" : "Selected city")).font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer(minLength:0)
                }.setupCard()
                Button("Choose a different city",action:choose).buttonStyle(.bordered).controlSize(.small).accessibilityIdentifier("chooseLocation")
            } else if service.isAcquiring {
                HStack(spacing:14) {
                    ProgressView()
                    Text(LocalizedStringKey(service.message ?? "Finding your location…")).font(.headline)
                    Spacer(minLength:0)
                }.setupCard().accessibilityIdentifier("locationProgress")
            } else {
                Button {service.request()} label: {
                    SetupOptionCard(symbol:"location.fill",title:"Use my current location",subtitle:"Finds the nearest city. Your location never leaves your iPhone.")
                }.buttonStyle(.plain).accessibilityIdentifier("onboardingGPS")
                Button {choose()} label: {
                    SetupOptionCard(symbol:"magnifyingglass",title:"Choose a city",subtitle:"Search by name, without sharing your location.")
                }.buttonStyle(.plain).accessibilityIdentifier("chooseLocation")
                if let message=service.message {
                    Text(LocalizedStringKey(message)).font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center).padding(.top,4)
                }
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
