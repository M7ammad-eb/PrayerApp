import SwiftUI
import SalatiCore

struct HijriView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var delta=0
    @State private var selected:CalendarCell?
    private var page: CalendarPage? { try? model.engine.calendar(dateISO:model.todayISO,monthDelta:Int32(delta),offset:Int32(model.settings.hijriAnchor == model.day?.hijriMonthKey ? model.settings.hijriOffset : 0)) }
    var body: some View {
        List {
            if let page {
                Section {
                    HStack {
                        Button {delta-=1} label: {Image(systemName:"chevron.backward").frame(minWidth:44,minHeight:44)}.accessibilityLabel("Previous month")
                        Spacer()
                        Text("\(model.arabic ? page.titleArabic : page.titleEnglish) \(page.year.description)").font(.title3.weight(.semibold))
                        Spacer()
                        Button {delta+=1} label: {Image(systemName:"chevron.forward").frame(minWidth:44,minHeight:44)}.accessibilityLabel("Next month")
                    }.buttonStyle(.borderless)
                    if typeSize.isAccessibilitySize {
                        ForEach(page.cells.filter { $0.inMonth },id:\.dateISO) { cell in
                            Button {selected=cell} label: {
                                VStack(alignment:.leading,spacing:6) {
                                    Text(cell.day.description).font(.headline.monospacedDigit())
                                    Text(cell.dateISO).font(.body.monospacedDigit())
                                    ForEach(cell.observances,id:\.self) {Text(LocalizedStringKey($0))}
                                }.frame(maxWidth:.infinity,minHeight:44,alignment:.leading)
                            }.buttonStyle(.plain)
                        }
                    } else {
                    LazyVGrid(columns:Array(repeating:GridItem(.flexible(),spacing:2),count:7),spacing:4) {
                        ForEach(0..<7) { index in Text(weekday(index)).font(.caption).foregroundStyle(.secondary).frame(maxWidth:.infinity) }
                        ForEach(page.cells,id:\.dateISO) { cell in
                            Button {selected=cell} label: {
                                VStack(spacing:3) {
                                    Text(cell.day.description).font(.body.monospacedDigit())
                                    Circle().fill(cell.observances.isEmpty ? .clear : Color.accentColor).frame(width:4,height:4)
                                }.frame(maxWidth:.infinity,minHeight:44)
                                    .foregroundStyle(cell.inMonth ? Color.primary : Color.secondary)
                                    .background(cell.dateISO == model.todayISO ? Color.accentColor.opacity(0.16) : .clear,in:RoundedRectangle(cornerRadius:12))
                            }.buttonStyle(.plain).accessibilityLabel("\(cell.day), \(cell.dateISO)")
                        }
                    }
                    }
                }
                if let selected {
                    Section(selected.dateISO) {
                        if selected.observances.isEmpty { Text("No special observance") }
                        ForEach(selected.observances,id:\.self) { Text(LocalizedStringKey($0)) }
                    }
                }
                Section {
                    Button("Today") {delta=0;selected=nil}
                    Text("Umm al-Qura calendar").font(.footnote).foregroundStyle(.secondary)
                    Text("Local moon sighting may differ. Hijri adjustments apply only to the selected current month.").font(.footnote).foregroundStyle(.secondary)
                }
            } else { ContentUnavailableView("Calendar unavailable",systemImage:"calendar.badge.exclamationmark") }
        }.navigationTitle("Hijri calendar")
    }
    func weekday(_ index:Int) -> String {
        let f=DateFormatter();f.locale=model.locale
        return f.veryShortStandaloneWeekdaySymbols[(index+6)%7]
    }
}
struct QiblaView: View {
    @EnvironmentObject var model: AppModel
    @Environment(\.accessibilityReduceMotion) var reduceMotion
    var body: some View { QiblaContent(model:model,service:model.locationService,reduceMotion:reduceMotion) }
}
struct QiblaContent: View {
    @ObservedObject var model: AppModel
    @ObservedObject var service: LocationService
    let reduceMotion: Bool
    var body: some View {
        ScrollView {
            VStack(spacing:28) {
                if let location=model.settings.location {
                    let bearing=model.engine.qiblaBearing(latitude:location.latitude,longitude:location.longitude)
                    let distance=model.engine.qiblaDistance(latitude:location.latitude,longitude:location.longitude)
                    ZStack {
                        Circle().stroke(Color.secondary.opacity(0.2),lineWidth:1)
                        Text("N").font(.caption.weight(.semibold)).offset(y:-100)
                        Image(systemName:"location.north.fill").font(.system(size:72,weight:.ultraLight))
                            .foregroundStyle(Color.accentColor).rotationEffect(.degrees(bearing-(service.heading ?? 0)))
                            .animation(reduceMotion ? nil : .easeOut(duration:0.25),value:service.heading)
                    }.frame(width:248,height:248).padding(.top,32).accessibilityHidden(true)
                    Text(String(format:"%.0f°",locale:Locale(identifier:"en_US_POSIX"),bearing)).font(.system(.largeTitle,design:.rounded)).monospacedDigit()
                    Text("Qibla from true north").foregroundStyle(.secondary)
                    Text(String(format:"%.0f km",locale:Locale(identifier:"en_US_POSIX"),distance)).font(.title3).monospacedDigit()
                    if service.heading == nil { Label("Compass unavailable. Use the true-north bearing above.",systemImage:"info.circle") }
                    else if (service.accuracy ?? 180)>20 { Label("Compass accuracy is low. Move away from metal and calibrate your device.",systemImage:"exclamationmark.triangle") }
                    if let message=service.message {Text(message).font(.footnote)}
                    Text("Keep your phone flat and away from magnets. Confirm direction with a reliable local reference.").font(.footnote).foregroundStyle(.secondary)
                } else { ContentUnavailableView("Choose your location",systemImage:"location") }
            }.padding(24).frame(maxWidth:560)
        }.navigationTitle("Qibla").onAppear {service.startHeading()}.onDisappear {service.stopHeading()}
    }
}
