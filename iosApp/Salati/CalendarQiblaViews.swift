import SwiftUI
import SalatiCore

struct HijriView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo:.body) private var rowHeight:CGFloat=58
    @ScaledMetric(relativeTo:.body) private var dayFont:CGFloat=18
    @ScaledMetric(relativeTo:.caption2) private var dateFont:CGFloat=10
    // Months are real pages: the neighbours exist on either side of the visible one and slide in
    // under the finger. `anchorMonth` is the month at index 0 (nil means the current Hijri month).
    @State private var anchorMonth:HijriYearMonth?
    @State private var pageIndex=0
    @State private var selected:CalendarCell?
    private let pageWindow=60
    // Cell fonts are bounded for seven readable columns; keep their padding bounded too.
    private var cellHeight:CGFloat {min(rowHeight,80)}
    // Weekday header, six reserved week rows and the grid spacing between them.
    private var gridHeight:CGFloat {cellHeight*6+52}
    private var page: CalendarPage? {monthPage(pageIndex)}
    private func monthPage(_ index:Int)->CalendarPage? {
        if anchorMonth == nil,index == 0 {return model.calendarPage(monthDelta:0)}
        guard let base=anchorMonth ?? model.calendarPage(monthDelta:0).map({HijriYearMonth(year:$0.year,month:$0.month)}) else {return nil}
        let month=index == 0 ? base : base.plusMonths(amount:Int32(index))
        return try? model.engine.calendarMonth(year:month.year,month:month.month,storedOffset:Int32(model.settings.hijriOffset),anchor:model.settings.hijriAnchor)
    }
    var body: some View {
        List {
            if let page {
                Section {
                    VStack(spacing:12) {
                        HStack(spacing:8) {
                            Button {movePage(-1)} label: {Image(systemName:"chevron.backward").frame(width:44,height:44)}.accessibilityLabel("Previous month")
                            Spacer(minLength:0)
                            Text("\(model.arabic ? page.titleArabic : page.titleEnglish) \(page.year.description)")
                                .font(.headline).multilineTextAlignment(.center).accessibilityIdentifier("calendarMonth")
                            Spacer(minLength:0)
                            Button {movePage(1)} label: {Image(systemName:"chevron.forward").frame(width:44,height:44)}.accessibilityLabel("Next month")
                        }.buttonStyle(.borderless)
                        if dynamicTypeSize.isAccessibilitySize {
                            Text("Hijri adjustment").font(.caption).frame(maxWidth:.infinity,alignment:.leading)
                                .fixedSize(horizontal:false,vertical:true)
                        }
                        HStack(alignment:.center,spacing:8) {
                            Stepper(value:Binding(get:{model.calendarOffset(pageMonthKey:page.monthKey)},set:{setOffset($0,page:page)}),in:-2...2) {
                                VStack(alignment:.leading,spacing:3) {
                                    if !dynamicTypeSize.isAccessibilitySize {Text("Hijri adjustment").font(.caption)}
                                    Text(model.calendarOffset(pageMonthKey:page.monthKey).formatted(.number.sign(strategy:.always()).locale(Locale(identifier:"en_US_POSIX"))))
                                        .font(.subheadline.monospacedDigit()).environment(\.locale,Locale(identifier:"en_US_POSIX"))
                                        .accessibilityIdentifier("calendarAdjustmentValue")
                                }
                            }.accessibilityLabel(Text("Hijri adjustment")).accessibilityIdentifier("calendarAdjustment")
                            Button("Reset") {setOffset(0,page:page)}.font(.caption)
                                .disabled(model.calendarOffset(pageMonthKey:page.monthKey) == 0)
                                .accessibilityIdentifier("calendarReset")
                        }
                        HStack {
                            Text("For this month").font(.caption).foregroundStyle(.secondary)
                            Spacer()
                            Button("Today") {withAnimation(reduceMotion ? nil : .easeOut(duration:0.18)) {anchorMonth=nil;pageIndex=0;selected=nil}}
                                .buttonStyle(.bordered).controlSize(.small).accessibilityIdentifier("calendarToday")
                        }
                    }
                    // Page style follows the layout direction, so Arabic pages in the natural direction.
                    TabView(selection:$pageIndex) {
                        ForEach(-pageWindow...pageWindow,id:\.self) {index in
                            Group {
                                if let monthData=monthPage(index) {calendarGrid(monthData)}
                                else {Color.clear}
                            }.frame(maxHeight:.infinity,alignment:.top).tag(index)
                        }
                    }.tabViewStyle(.page(indexDisplayMode:.never)).frame(height:gridHeight)
                        .onChange(of:pageIndex) {_,index in
                            selected=nil
                            // Keep a window of pages around the visible month; re-centre near its edge.
                            if abs(index)>=pageWindow-3,let month=monthPage(index) {
                                anchorMonth=HijriYearMonth(year:month.year,month:month.month);pageIndex=0
                            }
                        }
                }
                if let selected {
                    Section(gregorian(selected.dateISO,full:true)) {
                        LabeledContent("Hijri day") {Text(selected.day.description).environment(\.layoutDirection,.leftToRight)}.font(.headline)
                        if selected.dateISO == model.religiousTodayISO {Label("Today",systemImage:"sun.horizon")}
                        if selected.observances.isEmpty {Text("No special observance")}
                        ForEach(selected.observances,id:\.self) {Text(LocalizedStringKey($0))}
                    }
                }
                if let anchor=model.settings.hijriAnchor,model.settings.hijriOffset != 0 {
                    Section {
                        LabeledContent("Active adjustment") {
                            Text("\(model.settings.hijriOffset) · \(anchor)").monospacedDigit().environment(\.layoutDirection,.leftToRight)
                        }.font(.footnote)
                        if anchor != page.monthKey {
                            Button("Reset active adjustment") {model.setCalendarOffset(pageMonthKey:anchor,value:0);selected=nil}
                                .accessibilityIdentifier("resetActiveHijriAdjustment")
                        }
                    }
                }
                Section {
                    Text("Umm al-Qura calendar").font(.footnote).foregroundStyle(.secondary)
                    Text("Local moon sighting may differ.").font(.footnote).foregroundStyle(.secondary)
                }
            } else {ContentUnavailableView("Calendar unavailable",systemImage:"calendar.badge.exclamationmark")}
        }.navigationTitle("Hijri calendar").navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if anchorMonth == nil,ProcessInfo.processInfo.arguments.contains("-uiTestingCalendarGeometry") {
                    anchorMonth=HijriYearMonth(year:1448,month:11)
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
                    }.frame(maxWidth:.infinity).frame(height:cellHeight)
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
                Color.clear.frame(height:cellHeight).allowsHitTesting(false).accessibilityHidden(true)
            }
        }.accessibilityElement(children:.contain).accessibilityIdentifier("calendarGrid").accessibilityValue(String(page.cells.count/7))
    }
    private func movePage(_ amount:Int) {
        withAnimation(reduceMotion ? nil : .easeInOut(duration:0.28)) {pageIndex+=amount}
    }
    private func setOffset(_ value:Int,page:CalendarPage) {
        // Pin the month being adjusted so it stays on screen if the adjustment moves "today".
        anchorMonth=HijriYearMonth(year:page.year,month:page.month);pageIndex=0
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
    @State private var showCalibration=false
    let reduceMotion:Bool
    private let cardShape=RoundedRectangle(cornerRadius:24,style:.continuous)
    var body:some View {
        ScrollView {
            VStack(spacing:16) {
                if let location=model.settings.location {
                    let bearing=model.engine.qiblaBearing(latitude:location.latitude,longitude:location.longitude)
                    let distance=model.engine.qiblaDistance(latitude:location.latitude,longitude:location.longitude)
                    let rotation=compass?.rotation ?? bearing
                    VStack(spacing:6) {
                        Text("Find your Qibla").font(.title2.bold())
                        Text("Lay your phone flat and turn until the Kaaba badge sits at the top.")
                            .font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }.padding(.horizontal,20)
                    QiblaCompassFace(pointerRotation:rotation,ringRotation:rotation-bearing,aligned:compass?.aligned == true,reduceMotion:reduceMotion)
                        .frame(maxWidth:360).aspectRatio(1,contentMode:.fit)
                        .accessibilityElement(children:.ignore)
                        .accessibilityLabel("Qibla compass")
                        .accessibilityValue(compass?.aligned == true ? "You're facing the Qibla" : (compass?.reliable == true ? "Following true heading" : "Heading unavailable or unreliable"))
                        .accessibilityIdentifier("qiblaCompass")
                    statusPill(bearing:bearing)
                    detailsCard(distance:distance)
                    if service.heading == nil {
                        Label {Text(LocalizedStringKey(service.headingMessage ?? "Compass unavailable. Use the true-north bearing above."))} icon:{Image(systemName:"info.circle")}
                            .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }
                } else {ContentUnavailableView("Choose your location",systemImage:"location")}
            }.padding(.horizontal,16).padding(.top,8).padding(.bottom,24).frame(maxWidth:560).frame(maxWidth:.infinity)
        }
        .background(Color(uiColor:.systemGroupedBackground))
        .navigationTitle("Qibla").navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.alignment,trigger:alignmentFeedback)
        .alert("Calibrating the compass",isPresented:$showCalibration) {
            Button("Okay",role:.cancel) {}
        } message: {Text("To calibrate your compass, move your phone smoothly in a figure 8 in the air several times, away from metal and magnets.")}
        .onAppear {service.startHeading();updateCompass()}
        .onDisappear {service.stopHeading();compass=nil;tracker=CompassAlignmentTracker()}
        // The alignment state is evaluated on every heading sample, with no dwell time.
        .task(id:"\(service.heading ?? -1)/\(service.accuracy ?? -1)/\(scenePhase)") {
            guard scenePhase == .active else {return}
            updateCompass()
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
    // Signed angle from the way you face to the Qibla: positive means turn right.
    private func relativeAngle(_ rotation:Double)->Double {
        var value=rotation.truncatingRemainder(dividingBy:360)
        if value>180 {value-=360} else if value < -180 {value+=360}
        return value
    }
    private func degreesText(_ value:Double)->String {String(format:"%.0f°",locale:Locale(identifier:"en_US_POSIX"),abs(value))}
    private func statusPill(bearing:Double)->some View {
        let aligned=compass?.aligned == true
        return VStack(spacing:2) {
            if aligned {
                Image(systemName:"checkmark.circle.fill").font(.system(size:28)).accessibilityHidden(true)
                Text("You're facing the Qibla").font(.headline)
            } else if let compass {
                let relative=relativeAngle(compass.rotation)
                Text(degreesText(relative)).font(.system(size:38,weight:.heavy,design:.rounded)).monospacedDigit()
                    .foregroundStyle(Color.accentColor)
                Text(LocalizedStringKey(relative>0 ? "Rotate right" : "Rotate left")).font(.subheadline.weight(.bold)).foregroundStyle(.secondary)
            } else {
                Text(degreesText(bearing)).font(.system(size:38,weight:.heavy,design:.rounded)).monospacedDigit()
                    .foregroundStyle(Color.accentColor)
                Text("Qibla from true north").font(.subheadline.weight(.bold)).foregroundStyle(.secondary)
            }
        }
        .environment(\.layoutDirection,.leftToRight)
        .frame(maxWidth:.infinity).frame(height:104)
        .foregroundStyle(aligned ? Color.white : Color.primary)
        .background(aligned ? Color.accentColor : Color(uiColor:.secondarySystemGroupedBackground),in:cardShape)
        .animation(reduceMotion ? nil : .easeInOut(duration:0.2),value:aligned)
        .accessibilityElement(children:.combine)
        .accessibilityIdentifier(aligned ? "qiblaAligned" : "qiblaDirection")
    }
    private var accuracy:(key:String,color:Color) {
        guard let value=service.accuracy,value>=0 else {return ("Not reliable",Color.red)}
        if value<=10 {return ("Strong",Color.accentColor)}
        if value<=15 {return ("Fair",Color.orange)}
        if value<=25 {return ("Weak",Color.red)}
        return ("Not reliable",Color.red)
    }
    private func detailsCard(distance:Double)->some View {
        VStack(alignment:.leading,spacing:10) {
            HStack(spacing:10) {
                Image(systemName:"mappin.and.ellipse").foregroundStyle(Color.accentColor).frame(width:24).accessibilityHidden(true)
                VStack(alignment:.leading,spacing:2) {
                    Text(model.locationDisplayName).font(.subheadline.weight(.semibold)).lineLimit(1)
                    HStack(spacing:4) {
                        Text("Distance to the Kaaba:")
                        Text(String(format:"%.0f",locale:Locale(identifier:"en_US_POSIX"),distance)).monospacedDigit()
                        Text("km")
                    }.font(.footnote).foregroundStyle(.secondary)
                }
                Spacer(minLength:0)
            }
            Divider()
            HStack(spacing:10) {
                Circle().fill(accuracy.color).frame(width:9,height:9).accessibilityHidden(true)
                HStack(spacing:4) {Text("Compass:");Text(LocalizedStringKey(accuracy.key))}.font(.subheadline).foregroundStyle(.secondary)
                Spacer(minLength:0)
                Button {showCalibration=true} label:{Label("Recalibrate",systemImage:"safari")}.buttonStyle(.borderless)
            }
        }
        .padding(16).frame(maxWidth:.infinity,alignment:.leading)
        .background(Color(uiColor:.secondarySystemGroupedBackground),in:cardShape)
    }
}

// Mirrors the Android compass: a warm glow, white face, dashed ring with N/E/S/W that turns with
// the phone, and a Kaaba badge that orbits the rim at the Qibla bearing. Everything uses the app
// accent colour; facing the Qibla strengthens the glow.
private struct QiblaCompassFace:View {
    let pointerRotation:Double
    let ringRotation:Double
    let aligned:Bool
    let reduceMotion:Bool
    private let glowColor=Color.accentColor
    private let faceColor=Color(uiColor:.secondarySystemGroupedBackground)
    private let letters=["N","E","S","W"]
    var body:some View {
        GeometryReader {proxy in
            let side=min(proxy.size.width,proxy.size.height)
            let face=side/2*0.82
            let glow=face*1.2
            let dial=face*0.75
            let badge=face*0.155
            ZStack {
                Circle().fill(RadialGradient(
                    gradient:Gradient(stops:[
                        .init(color:glowColor.opacity(0),location:0),
                        .init(color:glowColor.opacity(aligned ? 0.45 : 0.26),location:face/glow),
                        .init(color:glowColor.opacity(0),location:1)]),
                    center:.center,startRadius:0,endRadius:glow)).frame(width:glow*2,height:glow*2)
                Circle().fill(faceColor).frame(width:face*2,height:face*2)
                Circle().stroke(Color.secondary.opacity(0.18),lineWidth:1).frame(width:face*2,height:face*2)
                ZStack {
                    Circle().stroke(Color.secondary.opacity(0.30),style:StrokeStyle(lineWidth:1,dash:[3,3]))
                        .frame(width:dial*2,height:dial*2)
                    ForEach(0..<4,id:\.self) {index in
                        let angle=Double(index)*Double.pi/2
                        Text(letters[index]).font(.system(size:13,weight:.bold)).foregroundStyle(.secondary)
                            .padding(4).background(faceColor,in:Circle())
                            .offset(x:dial*CGFloat(sin(angle)),y:-dial*CGFloat(cos(angle)))
                    }
                }.rotationEffect(.degrees(ringRotation))
                Circle().fill(glowColor).frame(width:12,height:12)
                ZStack {
                    KaabaBadge(radius:badge,color:glowColor,face:faceColor)
                        .rotationEffect(.degrees(-pointerRotation)).offset(y:-face)
                }.rotationEffect(.degrees(pointerRotation))
            }
            .frame(width:proxy.size.width,height:proxy.size.height)
        }
        .environment(\.layoutDirection,.leftToRight)
        .animation(reduceMotion ? nil : .easeOut(duration:0.18),value:pointerRotation)
        .animation(reduceMotion ? nil : .easeInOut(duration:0.25),value:aligned)
    }
}
private struct KaabaBadge:View {
    let radius:CGFloat
    let color:Color
    let face:Color
    var body:some View {
        ZStack {
            Circle().fill(face).frame(width:(radius+3)*2,height:(radius+3)*2)
            Circle().fill(color).frame(width:radius*2,height:radius*2)
            Canvas {context,size in
                let s=size.width/43,cx=size.width/2,cy=size.height/2
                func p(_ x:CGFloat,_ y:CGFloat)->CGPoint {CGPoint(x:cx+x*s,y:cy+y*s)}
                func shape(_ points:[CGPoint])->Path {
                    var path=Path();path.move(to:points[0])
                    for point in points.dropFirst() {path.addLine(to:point)}
                    path.closeSubpath();return path
                }
                let belt=color.opacity(0.95)
                context.fill(shape([p(-10,-6),p(2,-2),p(2,11),p(-10,7)]),with:.color(.white))
                context.fill(shape([p(2,-2),p(10,-6),p(10,7),p(2,11)]),with:.color(Color.white.opacity(0.82)))
                context.fill(shape([p(-10,-6),p(-2,-10),p(10,-6),p(2,-2)]),with:.color(Color.white.opacity(0.68)))
                context.fill(shape([p(-10,-1.7),p(2,2.1),p(2,4.5),p(-10,0.8)]),with:.color(belt))
                context.fill(shape([p(2,2.1),p(10,-1.7),p(10,0.8),p(2,4.5)]),with:.color(belt))
                context.fill(shape([p(5.2,4.2),p(8.1,3),p(8.1,7.1),p(5.2,8.3)]),with:.color(belt))
            }.frame(width:radius*2,height:radius*2)
        }
    }
}
