import SwiftUI
import SalatiCore

struct HijriView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo:.body) private var rowHeight:CGFloat=58
    @ScaledMetric(relativeTo:.body) private var dayFont:CGFloat=18
    @ScaledMetric(relativeTo:.caption2) private var dateFont:CGFloat=10
    @State private var visibleMonth:HijriYearMonth?
    @State private var selected:CalendarCell?
    private var page: CalendarPage? {
        if let visibleMonth {return try? model.engine.calendarMonth(year:visibleMonth.year,month:visibleMonth.month,storedOffset:Int32(model.settings.hijriOffset),anchor:model.settings.hijriAnchor)}
        return model.calendarPage(monthDelta:0)
    }
    var body: some View {
        List {
            if let page {
                Section {
                    VStack(spacing:12) {
                        HStack(spacing:8) {
                            Button {navigate(-1,page:page)} label: {Image(systemName:"chevron.backward").frame(width:44,height:44)}.accessibilityLabel("Previous month")
                            Spacer(minLength:0)
                            Text("\(model.arabic ? page.titleArabic : page.titleEnglish) \(page.year.description)")
                                .font(.headline).multilineTextAlignment(.center).accessibilityIdentifier("calendarMonth")
                            Spacer(minLength:0)
                            Button {navigate(1,page:page)} label: {Image(systemName:"chevron.forward").frame(width:44,height:44)}.accessibilityLabel("Next month")
                        }.buttonStyle(.borderless)
                        HStack(alignment:.center,spacing:8) {
                            Stepper(value:Binding(get:{model.calendarOffset(pageMonthKey:page.monthKey)},set:{setOffset($0,page:page)}),in:-2...2) {
                                VStack(alignment:.leading,spacing:3) {
                                    Text("Hijri adjustment").font(.caption)
                                    Text(model.calendarOffset(pageMonthKey:page.monthKey).formatted(.number.sign(strategy:.always()).locale(Locale(identifier:"en_US_POSIX"))))
                                        .font(.subheadline.monospacedDigit()).environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                        .accessibilityIdentifier("calendarAdjustmentValue")
                                }
                            }.accessibilityIdentifier("calendarAdjustment")
                            Button("Reset") {setOffset(0,page:page)}.font(.caption)
                                .disabled(model.calendarOffset(pageMonthKey:page.monthKey) == 0)
                                .accessibilityIdentifier("calendarReset")
                        }
                        HStack {
                            Text("For this month").font(.caption).foregroundStyle(.secondary)
                            Spacer()
                            Button("Today") {withAnimation(reduceMotion ? nil : .easeOut(duration:0.18)) {visibleMonth=nil;selected=nil}}
                                .buttonStyle(.bordered).controlSize(.small).accessibilityIdentifier("calendarToday")
                        }
                    }
                    calendarGrid(page)
                        .contentShape(Rectangle())
                        .simultaneousGesture(DragGesture(minimumDistance:30).onEnded {value in
                            guard abs(value.translation.width)>50,abs(value.translation.width)>abs(value.translation.height)*1.6 else {return}
                            let forward=model.arabic ? value.translation.width>0 : value.translation.width<0
                            navigate(forward ? 1 : -1,page:page)
                        })
                }
                if let selected {
                    Section(gregorian(selected.dateISO,full:true)) {
                        Text("Hijri day: \(selected.day.description)").font(.headline)
                        if selected.dateISO == model.religiousTodayISO {Label("Today",systemImage:"sun.horizon")}
                        if selected.observances.isEmpty {Text("No special observance")}
                        ForEach(selected.observances,id:\.self) {Text(LocalizedStringKey($0))}
                    }
                }
                if let anchor=model.settings.hijriAnchor,model.settings.hijriOffset != 0 {
                    Section {
                        Text("Active adjustment: \(model.settings.hijriOffset) for AH \(anchor)").font(.footnote)
                        Text("Neighboring month boundaries reconcile the correction while keeping every month 29 or 30 days.").font(.footnote).foregroundStyle(.secondary)
                        if anchor != page.monthKey {
                            Button("Reset active adjustment") {model.setCalendarOffset(pageMonthKey:anchor,value:0);selected=nil}
                                .accessibilityIdentifier("resetActiveHijriAdjustment")
                        }
                    }
                }
                Section {
                    Text("Umm al-Qura calendar").font(.footnote).foregroundStyle(.secondary)
                    Text("The current Hijri date advances at Maghrib. Gregorian cell dates stay civil dates; prayer times stay on their civil day.").font(.footnote).foregroundStyle(.secondary)
                }
            } else {ContentUnavailableView("Calendar unavailable",systemImage:"calendar.badge.exclamationmark")}
        }.navigationTitle("Hijri calendar").navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if visibleMonth == nil,ProcessInfo.processInfo.arguments.contains("-uiTestingCalendarGeometry") {
                    visibleMonth=HijriYearMonth(year:1448,month:11)
                }
            }
    }
    private func calendarGrid(_ page:CalendarPage)->some View {
        LazyVGrid(columns:Array(repeating:GridItem(.flexible(minimum:0),spacing:2),count:7),spacing:5) {
            ForEach(0..<7) {index in Text(weekday(index)).font(.caption).foregroundStyle(.secondary).frame(maxWidth:.infinity)}
            ForEach(page.cells,id:\.dateISO) {cell in
                Button {selected=cell} label: {
                    VStack(spacing:3) {
                        Text(cell.day.description).font(.system(size:min(dayFont,28),weight:cell.dateISO == model.religiousTodayISO ? .bold : .regular)).monospacedDigit()
                        Text(gregorian(cell.dateISO)).font(.system(size:min(dateFont,14),weight:.medium)).monospacedDigit()
                            .foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight).lineLimit(1).minimumScaleFactor(0.85)
                        Circle().fill(cell.observances.isEmpty ? .clear : Color.accentColor).frame(width:4,height:4)
                    }.frame(maxWidth:.infinity).frame(height:rowHeight)
                        .foregroundStyle(cell.inMonth ? Color.primary : Color.secondary)
                        .background(cell.dateISO == model.religiousTodayISO ? Color.accentColor.opacity(0.16) : .clear,in:RoundedRectangle(cornerRadius:10))
                        .overlay {RoundedRectangle(cornerRadius:10).stroke(selected?.dateISO == cell.dateISO ? Color.accentColor : .clear,lineWidth:1.5)}
                }.buttonStyle(.plain).accessibilityLabel("\(cell.day), \(gregorian(cell.dateISO,full:true))")
                    .accessibilityIdentifier("calendarCell."+cell.dateISO)
                    .accessibilityValue(gregorian(cell.dateISO))
                    .accessibilityAddTraits(selected?.dateISO == cell.dateISO ? .isSelected : [])
            }
            // Reserve geometry only. Empty slots have no buttons, dates or accessibility nodes.
            ForEach(page.cells.count..<42,id:\.self) {_ in
                Color.clear.frame(height:rowHeight).allowsHitTesting(false).accessibilityHidden(true)
            }
        }.accessibilityElement(children:.contain).accessibilityIdentifier("calendarGrid").accessibilityValue(String(page.cells.count/7))
    }
    private func navigate(_ amount:Int32,page:CalendarPage) {
        withAnimation(reduceMotion ? nil : .easeOut(duration:0.18)) {
            visibleMonth=HijriYearMonth(year:page.year,month:page.month).plusMonths(amount:amount);selected=nil
        }
    }
    private func setOffset(_ value:Int,page:CalendarPage) {
        visibleMonth=HijriYearMonth(year:page.year,month:page.month)
        model.setCalendarOffset(pageMonthKey:page.monthKey,value:value);selected=nil
    }
    private func gregorian(_ iso:String,full:Bool=false)->String {
        let pieces=iso.split(separator:"-")
        if !full,pieces.count == 3,let day=Int(pieces[2]),let month=Int(pieces[1]) {return "\(day)/\(month)"}
        let input=DateFormatter();input.locale=Locale(identifier:"en_US_POSIX");input.calendar=model.calendar;input.timeZone=model.zone;input.dateFormat="yyyy-MM-dd"
        guard let date=input.date(from:iso) else {return iso}
        let output=DateFormatter();output.locale=model.locale;output.calendar=model.calendar;output.timeZone=model.zone
        output.setLocalizedDateFormatFromTemplate("EEEE d MMMM yyyy")
        return output.string(from:date)
    }
    private func weekday(_ index:Int)->String {
        let f=DateFormatter();f.locale=model.locale
        return f.veryShortStandaloneWeekdaySymbols[(index+6)%7]
    }
}
struct QiblaView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    var body: some View {QiblaContent(model:model,service:model.locationService,reduceMotion:reduceMotion)}
}
struct QiblaContent: View {
    @ObservedObject var model:AppModel
    @ObservedObject var service:LocationService
    @Environment(\.scenePhase) private var scenePhase
    @State private var tracker=CompassAlignmentTracker()
    @State private var compass:CompassState?
    @State private var alignmentFeedback=0
    let reduceMotion:Bool
    var body:some View {
        ScrollView {
            VStack(spacing:20) {
                if let location=model.settings.location {
                    let bearing=model.engine.qiblaBearing(latitude:location.latitude,longitude:location.longitude)
                    let distance=model.engine.qiblaDistance(latitude:location.latitude,longitude:location.longitude)
                    compassFace(bearing:bearing)
                    if compass?.aligned == true {
                        Label("Aligned with Qibla",systemImage:"checkmark.circle.fill").font(.headline).foregroundStyle(Color.accentColor)
                            .accessibilityIdentifier("qiblaAligned")
                    }
                    Text(String(format:"%.0f°",locale:Locale(identifier:"en_US_POSIX"),bearing)).font(.system(.largeTitle,design:.rounded)).monospacedDigit()
                        .environment(\.layoutDirection,.leftToRight)
                    Text("Qibla from true north").foregroundStyle(.secondary)
                    Text(String(format:"%.0f km",locale:Locale(identifier:"en_US_POSIX"),distance)).font(.title3).monospacedDigit()
                        .environment(\.layoutDirection,.leftToRight)
                    if service.heading == nil {
                        Label {Text(LocalizedStringKey(service.message ?? "Compass unavailable. Use the true-north bearing above."))} icon:{Image(systemName:"info.circle")}
                    } else if compass?.reliable != true {
                        Label("Compass accuracy is low. Move away from metal and calibrate your device.",systemImage:"exclamationmark.triangle")
                    }
                    Text("Keep your phone flat and away from magnets. Confirm direction with a reliable local reference.").font(.footnote).foregroundStyle(.secondary)
                } else {ContentUnavailableView("Choose your location",systemImage:"location")}
            }.multilineTextAlignment(.center).padding(20).frame(maxWidth:560).frame(maxWidth:.infinity)
        }
        .navigationTitle("Qibla").navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.alignment,trigger:alignmentFeedback)
        .onAppear {service.startHeading();updateCompass()}
        .onDisappear {service.stopHeading();compass=nil;tracker=CompassAlignmentTracker()}
        .task(id:"\(service.heading ?? -1)/\(service.accuracy ?? -1)/\(scenePhase)") {
            guard scenePhase == .active else {return}
            updateCompass()
            // A stationary sensor need not emit a second sample. Feed the shared
            // debounce policy one later observation; Swift contains no tolerance rules.
            do {try await Task.sleep(for:.milliseconds(650));try Task.checkCancellation();updateCompass()} catch {}
        }
        .onChange(of:scenePhase) {_,phase in
            if phase == .active {service.startHeading();updateCompass()}
            else {service.stopHeading();compass=nil;tracker=CompassAlignmentTracker()}
        }
    }
    private func updateCompass() {
        guard let location=model.settings.location else {compass=nil;return}
        let bearing=model.engine.qiblaBearing(latitude:location.latitude,longitude:location.longitude)
        guard let heading=service.heading else {
            _=tracker.update(bearing:bearing,heading:0,accuracy:-1,nowEpoch:Date().timeIntervalSince1970)
            compass=nil;return
        }
        let state=tracker.update(bearing:bearing,heading:heading,accuracy:service.accuracy ?? -1,nowEpoch:Date().timeIntervalSince1970)
        compass=state
        if state.enteredAlignment {alignmentFeedback+=1}
    }
    private func compassFace(bearing:Double)->some View {
        let rotation=compass?.rotation ?? bearing
        let aligned=compass?.aligned == true
        return ZStack {
            Circle().fill(aligned ? Color.accentColor.opacity(0.1) : Color.secondary.opacity(0.035))
            Circle().stroke(aligned ? Color.accentColor : Color.secondary.opacity(0.35),lineWidth:aligned ? 4 : 1.5)
                .shadow(color:aligned ? Color.accentColor.opacity(0.2) : .clear,radius:8)
            ZStack {
                ForEach(0..<36) {tick in
                    Capsule().fill(Color.secondary.opacity(tick%3 == 0 ? 0.6 : 0.25))
                        .frame(width:1.5,height:tick%3 == 0 ? 10 : 5).offset(y:-108).rotationEffect(.degrees(Double(tick)*10))
                }
                Text("N").font(.caption.weight(.semibold)).offset(y:-88)
                Text("E").font(.caption).offset(x:88)
                Text("S").font(.caption).offset(y:88)
                Text("W").font(.caption).offset(x:-88)
            }.rotationEffect(.degrees(rotation-bearing))
            Image(systemName:"location.north.fill").font(.system(size:64,weight:.light)).foregroundStyle(Color.accentColor)
                .rotationEffect(.degrees(rotation))
            Circle().fill(Color.accentColor).frame(width:9,height:9)
        }
        .frame(width:240,height:240).padding(.vertical,8)
        .environment(\.layoutDirection,.leftToRight)
        .animation(reduceMotion ? nil : .easeOut(duration:0.25),value:rotation)
        .accessibilityElement(children:.ignore)
        .accessibilityLabel("Qibla compass")
        .accessibilityValue(compass?.aligned == true ? "Aligned with Qibla" : (compass?.reliable == true ? "Following true heading" : "Heading unavailable or unreliable"))
        .accessibilityIdentifier("qiblaCompass")
    }
}
