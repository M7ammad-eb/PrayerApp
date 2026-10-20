import WidgetKit
import SwiftUI
import ActivityKit
import AppIntents

struct PrayerWidgetOptions:WidgetConfigurationIntent {
    static var title:LocalizedStringResource="Prayer widget"
    static var description=IntentDescription("Choose the details shown on this widget.")
    @Parameter(title:"Show city",default:true) var showCity:Bool
    @Parameter(title:"Show date",default:true) var showDate:Bool
    @Parameter(title:"Show sunrise in schedule",default:true) var showSunrise:Bool
}
enum PrayerWidgetLayout:Equatable {case standard,horizontal}
struct PrayerEntry:TimelineEntry {
    let date:Date
    let snapshot:ScheduleSnapshot?
    var showCity=true
    var showDate=true
    var showSunrise=true
    var next:SnapshotEvent? {snapshot?.nextEvent(after:date)}
    var currentID:String? {snapshot?.moment(at:date)?.currentEventID}
    var day:SnapshotDay? {snapshot?.day(at:date)}
    var schedule:[SnapshotEvent] {(snapshot?.displayEvents(at:date) ?? []).filter {showSunrise || !$0.id.hasSuffix(".SUNRISE")}}
    var hijri:String? {
        guard let moment=snapshot?.moment(at:date) else {return nil}
        return snapshot?.language == "ar" ? moment.hijriArabic : moment.hijriEnglish
    }
    var arabic:Bool {snapshot?.language == "ar"}
    var locale:Locale {AppLanguage.locale(snapshot?.language ?? "en")}
    var direction:LayoutDirection {AppLanguage.isRightToLeft(snapshot?.language ?? "en") ? .rightToLeft : .leftToRight}
}
struct PrayerProvider:AppIntentTimelineProvider {
    func placeholder(in context:Context)->PrayerEntry {PrayerPreview.entry(arabic:AppLanguage.current == "ar")}
    func snapshot(for configuration:PrayerWidgetOptions,in context:Context) async->PrayerEntry {
        let saved=ScheduleSnapshot.load()
        let preview=saved.flatMap {$0.nextEvent(after:Date()) == nil ? nil:$0} ?? PrayerPreview.entry(arabic:AppLanguage.current == "ar").snapshot
        return entry(configuration,snapshot:context.isPreview ? preview:saved,date:Date())
    }
    func timeline(for configuration:PrayerWidgetOptions,in context:Context) async->Timeline<PrayerEntry> {
        let now=Date(),snapshot=ScheduleSnapshot.load()
        var boundaries=[now]
        if let snapshot {
            // Moments are calculated by shared domain rules at prayer, midnight and Maghrib boundaries.
            boundaries += (snapshot.moments.isEmpty ? snapshot.events.map(\.date) : snapshot.moments.map(\.date)).filter {$0>now && $0<snapshot.validUntil}
            if snapshot.validUntil>now {boundaries.append(snapshot.validUntil)}
        }
        let entries=Array(Set(boundaries)).sorted().map {entry(configuration,snapshot:snapshot,date:$0)}
        let expiry=snapshot?.validUntil ?? now.addingTimeInterval(3600)
        let refresh=expiry>now ? min(expiry,now.addingTimeInterval(6*3600)) : now.addingTimeInterval(3600)
        return Timeline(entries:entries,policy:.after(refresh))
    }
    private func entry(_ options:PrayerWidgetOptions,snapshot:ScheduleSnapshot?,date:Date)->PrayerEntry {
        PrayerEntry(date:date,snapshot:snapshot,showCity:options.showCity,showDate:options.showDate,showSunrise:options.showSunrise)
    }
}
struct PrayerWidget:Widget {
    let kind="SalatiPrayer"
    var body:some WidgetConfiguration {
        AppIntentConfiguration(kind:kind,intent:PrayerWidgetOptions.self,provider:PrayerProvider()) {entry in
            PrayerWidgetRoot(entry:entry)
                .containerBackground(.fill.tertiary,for:.widget)
        }.configurationDisplayName("Prayer times").description("Next prayer and your daily offline schedule.")
            .supportedFamilies([.systemSmall,.systemMedium,.systemLarge])
    }
}
struct PrayerHorizontalWidget:Widget {
    let kind="SalatiPrayerHorizontal"
    var body:some WidgetConfiguration {
        AppIntentConfiguration(kind:kind,intent:PrayerWidgetOptions.self,provider:PrayerProvider()) {entry in
            PrayerWidgetRoot(entry:entry,layout:.horizontal).containerBackground(.fill.tertiary,for:.widget)
        }.configurationDisplayName("Prayer times · Horizontal").description("Your next prayer above a horizontal daily schedule.")
            .supportedFamilies([.systemMedium,.systemLarge])
    }
}
struct PrayerAccessoryWidget:Widget {
    let kind="SalatiPrayerAccessory"
    var body:some WidgetConfiguration {
        AppIntentConfiguration(kind:kind,intent:PrayerWidgetOptions.self,provider:PrayerProvider()) {entry in
            PrayerWidgetRoot(entry:entry).containerBackground(.clear,for:.widget)
        }.configurationDisplayName("Prayer countdown").description("The next prayer on your Lock Screen.")
            .supportedFamilies([.accessoryInline,.accessoryCircular,.accessoryRectangular])
    }
}
private struct PrayerWidgetRoot:View {
    let entry:PrayerEntry
    var layout:PrayerWidgetLayout = .standard
    @Environment(\.widgetFamily) var family
    var body:some View {
        PrayerWidgetContent(entry:entry,family:family,layout:layout)
            .environment(\.locale,entry.locale)
            .environment(\.layoutDirection,entry.direction)
    }
}
struct PrayerWidgetContent:View {
    let entry:PrayerEntry
    let family:WidgetFamily
    var layout:PrayerWidgetLayout = .standard
    var body:some View {
        Group {
            if let next=entry.next {
                switch family {
                case .systemSmall:small(next)
                case .systemMedium:
                    if layout == .horizontal {horizontal(next,large:false)} else {medium(next)}
                case .systemLarge:
                    if layout == .horizontal {horizontal(next,large:true)} else {large(next)}
                case .accessoryInline:
                    Text(next.title+" · "+next.timeText).font(.caption).monospacedDigit()
                case .accessoryCircular:
                    VStack(spacing:3) {
                        Text(next.title).font(.caption2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.85)
                        Countdown(date:next.date,now:entry.date,width:64,alignment:.center)
                            .font(.caption.monospacedDigit())
                    }.frame(maxWidth:.infinity,maxHeight:.infinity).widgetAccentable()
                default:
                    VStack(alignment:.leading,spacing:3) {
                        Text(next.title+" · "+next.timeText).font(.headline).lineLimit(1).minimumScaleFactor(0.8)
                        Countdown(date:next.date,now:entry.date,width:116).font(.body)
                    }
                }
            } else {empty}
        }
        .environment(\.locale,entry.locale)
        .environment(\.layoutDirection,entry.direction)
    }
    private func small(_ next:SnapshotEvent)->some View {
        VStack(alignment:.leading,spacing:6) {
            if entry.showCity {city.font(.caption2)}
            Text(next.title).font(.title2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.85)
            Text(next.timeText).font(.headline).monospacedDigit().lineLimit(1).minimumScaleFactor(0.85)
            Countdown(date:next.date,now:entry.date,width:126).font(.system(.title2,design:.rounded,weight:.regular))
            if entry.showDate,let hijri=entry.hijri {Text(hijri).font(.caption2).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.85)}
        }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:.leading)
    }
    private func medium(_ next:SnapshotEvent)->some View {
        GeometryReader {geometry in
            let column=max(0,(geometry.size.width-23)/2)
            HStack(alignment:.center,spacing:11) {
                VStack(alignment:.leading,spacing:6) {
                    if entry.showCity {city.font(.caption2)}
                    Text(next.title).font(.title2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.85)
                    Text(next.timeText).font(.subheadline.weight(.medium)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.85)
                    Countdown(date:next.date,now:entry.date,width:column).font(.system(.title2,design:.rounded,weight:.regular))
                    if entry.showDate,let hijri=entry.hijri {Text(hijri).font(.caption2).foregroundStyle(.secondary).lineLimit(2)}
                }.frame(width:column,alignment:.leading)
                Divider()
                VStack(spacing:5) {ForEach(entry.schedule) {event in scheduleRow(event,compact:true)}}
                    .frame(width:column)
            }.frame(width:geometry.size.width,height:geometry.size.height)
        }
    }
    private func large(_ next:SnapshotEvent)->some View {
        GeometryReader {geometry in
            VStack(alignment:.leading,spacing:12) {
                if entry.showCity {city.font(.subheadline)}
                HStack(alignment:.firstTextBaseline,spacing:12) {
                    VStack(alignment:.leading,spacing:4) {
                        Text(next.title).font(.title2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.85)
                        Text(next.timeText).font(.subheadline).monospacedDigit()
                    }.frame(maxWidth:.infinity,alignment:.leading)
                    Countdown(date:next.date,now:entry.date,width:geometry.size.width*0.48,alignment:.trailing)
                        .font(.system(.title,design:.rounded,weight:.regular))
                }
                if entry.showDate,let hijri=entry.hijri {Text(hijri).font(.caption).foregroundStyle(.secondary).lineLimit(1)}
                Divider()
                VStack(spacing:10) {ForEach(entry.schedule) {event in scheduleRow(event,compact:false)}}
                Spacer(minLength:0)
            }.frame(width:geometry.size.width,height:geometry.size.height,alignment:.topLeading)
        }
    }
    private var city:some View {Text(entry.snapshot?.city ?? "").foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.85)}
    private func scheduleRow(_ event:SnapshotEvent,compact:Bool)->some View {
        let isNext=event.id == entry.next?.id,isCurrent=event.id == entry.currentID
        return HStack(spacing:4) {
            statusMark(isNext:isNext,isCurrent:isCurrent)
            Text(event.title).lineLimit(1).minimumScaleFactor(0.85)
            Spacer(minLength:2)
            Text(event.timeText).monospacedDigit().lineLimit(1).minimumScaleFactor(0.85)
        }.font(compact ? .caption2 : .subheadline).fontWeight(isNext || isCurrent ? .semibold : .regular)
            .foregroundStyle(isNext ? Color.blue : (isCurrent ? Color.primary : Color.secondary))
            .padding(.vertical,compact ? 1 : 3)
            .background(isNext ? Color.blue.opacity(0.08) : .clear,in:RoundedRectangle(cornerRadius:5))
            .accessibilityElement(children:.combine)
            .accessibilityValue(Text(LocalizedStringKey(isNext ? "Up next" : isCurrent ? "Current prayer" : "")))
    }
    private func statusMark(isNext:Bool,isCurrent:Bool)->some View {
        Group {
            if isNext {Image(systemName:"arrowtriangle.forward.fill").font(.system(size:5))}
            else {Circle().fill(isCurrent ? Color.primary : .clear).frame(width:4,height:4)}
        }.frame(width:5).accessibilityHidden(true)
    }
    private func horizontal(_ next:SnapshotEvent,large:Bool)->some View {
        GeometryReader {geometry in
            // WidgetKit archives dynamic Text with flexible width. Explicit columns keep
            // the timer from consuming the prayer's proposal in the installed widget.
            let timerWidth=geometry.size.width*0.46
            let identityWidth=max(0,geometry.size.width-timerWidth-12)
            let columnSpacing:CGFloat=large ? 5 : 3
            let columnWidth=max(0,(geometry.size.width-columnSpacing*CGFloat(max(0,entry.schedule.count-1)))/CGFloat(max(1,entry.schedule.count)))
            VStack(alignment:.leading,spacing:large ? 18 : 8) {
                if entry.showCity || entry.showDate {
                    HStack(alignment:.firstTextBaseline,spacing:8) {
                        if entry.showCity {city}
                        Spacer(minLength:0)
                        if entry.showDate,let hijri=entry.hijri {Text(hijri).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.85)}
                    }.font(.caption2)
                }
                if large {Spacer(minLength:0)}
                HStack(alignment:.center,spacing:12) {
                    VStack(alignment:.leading,spacing:large ? 7 : 3) {
                        Text(next.title).font(large ? .largeTitle.weight(.semibold) : .title3.weight(.semibold))
                            .lineLimit(1).minimumScaleFactor(0.85)
                        Text(next.timeText).font(large ? .title2 : .subheadline).monospacedDigit()
                            .lineLimit(1).minimumScaleFactor(0.85)
                    }.frame(width:identityWidth,alignment:.leading)
                    Countdown(date:next.date,now:entry.date,width:timerWidth,alignment:.trailing)
                        .font(.system(large ? .largeTitle : .title2,design:.rounded,weight:.regular))
                }
                if large {Spacer(minLength:0);Divider()}
                HStack(alignment:.top,spacing:columnSpacing) {
                    ForEach(entry.schedule) {event in horizontalCell(event,large:large).frame(width:columnWidth)}
                }
                if large {Spacer(minLength:0)}
            }.frame(width:geometry.size.width,height:geometry.size.height,alignment:.center)
        }
    }
    private func horizontalCell(_ event:SnapshotEvent,large:Bool)->some View {
        let isNext=event.id == entry.next?.id,isCurrent=event.id == entry.currentID
        return VStack(spacing:large ? 8 : 3) {
            Text(event.title).font(.caption2.weight(isNext || isCurrent ? .semibold : .regular))
                .lineLimit(1).minimumScaleFactor(0.85)
            Text(event.timeText).font(.system(size:large ? 12 : 11,weight:isNext || isCurrent ? .semibold : .regular))
                .monospacedDigit().lineLimit(2).minimumScaleFactor(0.85).multilineTextAlignment(.center)
                .frame(height:large ? 30 : 26,alignment:.top)
            statusMark(isNext:isNext,isCurrent:isCurrent).frame(height:5)
        }.frame(maxWidth:.infinity).padding(.vertical,large ? 10 : 3)
            .foregroundStyle(isNext ? Color.blue : (isCurrent ? Color.primary : Color.secondary))
            .background(isNext ? Color.blue.opacity(0.08) : .clear,in:RoundedRectangle(cornerRadius:large ? 10 : 6))
            .accessibilityElement(children:.combine)
            .accessibilityValue(Text(LocalizedStringKey(isNext ? "Up next" : isCurrent ? "Current prayer" : "")))
    }
    @ViewBuilder private var empty:some View {
        if family == .accessoryInline {Text("Open Salati to refresh")}
        else {
            VStack(alignment:.leading,spacing:8) {
                Image(systemName:"moon.stars").font(.title3)
                Text("Open Salati to refresh").font(family == .accessoryCircular ? .caption2 : .headline)
            }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:.leading)
        }
    }
}

struct PrayerLiveActivity:Widget {
    var body:some WidgetConfiguration {
        ActivityConfiguration(for:PrayerActivityAttributes.self) {context in
            PrayerActivityCard(city:context.attributes.city,state:context.state,isExpired:context.isStale || context.state.finished || context.state.date<=Date())
                .padding(16).activityBackgroundTint(Color.black.opacity(0.08)).activitySystemActionForegroundColor(.primary)
        } dynamicIsland: {context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.bottom) {
                    PrayerActivityExpanded(city:context.attributes.city,state:context.state,isExpired:context.isStale || context.state.finished || context.state.date<=Date())
                }
            } compactLeading: {
                PrayerActivityCompactName(state:context.state)
            } compactTrailing: {
                PrayerActivityCompactTime(state:context.state,isExpired:context.isStale || context.state.finished || context.state.date<=Date())
            } minimal: {
                PrayerActivityMinimalName(state:context.state)
            }.contentMargins(.horizontal,18,for:.expanded)
                .contentMargins(.vertical,12,for:.expanded).keylineTint(.clear)
        }
    }
}
private func activityLocale(_ state:PrayerActivityAttributes.ContentState)->Locale {AppLanguage.locale(state.language)}
private func activityDirection(_ state:PrayerActivityAttributes.ContentState)->LayoutDirection {AppLanguage.isRightToLeft(state.language) ? .rightToLeft : .leftToRight}
private func activityEndMessage(_ state:PrayerActivityAttributes.ContentState)->String {
    if !state.endMessage.trimmingCharacters(in:.whitespacesAndNewlines).isEmpty {return state.endMessage}
    return AppLanguage.text("It is time for prayer",language:state.language)
}
struct PrayerActivityHeading:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {
        VStack(alignment:.leading,spacing:3) {
            Text(state.title).font(.headline).lineLimit(1).minimumScaleFactor(0.85)
            Text(state.timeText).font(.subheadline).monospacedDigit().foregroundStyle(.secondary).lineLimit(1)
        }
    }
}
struct PrayerActivityRemaining:View {
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        Group {
            if isExpired {Text(activityEndMessage(state)).font(.subheadline.weight(.medium)).multilineTextAlignment(.trailing).fixedSize(horizontal:false,vertical:true)}
            else {Countdown(date:state.date,width:112,alignment:.trailing).font(.system(.title,design:.rounded,weight:.regular))}
        }.frame(width:112,alignment:.trailing)
            .environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityFooter:View {
    let city:String
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        Group {
            if !isExpired,let start=state.start,start<state.date {
                ProgressView(timerInterval:start...state.date,countsDown:false).tint(Color.accentColor)
                    .labelsHidden().accessibilityLabel("Prayer countdown progress")
                    .frame(height:4).clipShape(Capsule())
            }
        }
    }
}
private struct PrayerActivityCloseButton:View {
    let eventID:String
    var body:some View {
        Button(intent:DismissPrayerActivityIntent(eventID:eventID)) {
            Image(systemName:"xmark").font(.system(size:12,weight:.semibold))
                .frame(width:32,height:32).background(Color.secondary.opacity(0.12),in:Circle())
                .frame(width:44,height:44).contentShape(Rectangle())
        }.buttonStyle(.plain).accessibilityLabel("Close").accessibilityIdentifier("liveActivity.close")
    }
}
struct PrayerActivityExpanded:View {
    let city:String
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        VStack(alignment:.leading,spacing:8) {
            HStack(alignment:.center,spacing:8) {
                PrayerActivityHeading(state:state).frame(maxWidth:.infinity,alignment:.leading)
                PrayerActivityRemaining(state:state,isExpired:isExpired)
                PrayerActivityCloseButton(eventID:state.eventID)
            }
            if !city.isEmpty {Text(city).font(.caption).foregroundStyle(.secondary).lineLimit(1)}
            PrayerActivityFooter(city:city,state:state,isExpired:isExpired)
        }
        // The bottom region shares space with the sensor and rounded lower corners.
        // Keep its composition short and its animated track safely inside that area.
        .padding(.horizontal,4).padding(.bottom,10)
        .environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityCompactName:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {
        Text(state.title).font(.system(size:13,weight:.medium)).lineLimit(1).minimumScaleFactor(0.85)
            .environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityMinimalName:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {
        Text(state.title).font(.system(size:10,weight:.medium)).lineLimit(1).minimumScaleFactor(0.85)
            .environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityCompactTime:View {
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        Group {
            if isExpired {Text("0:00").frame(width:46,alignment:.trailing)}
            else {Countdown(date:state.date,width:46,alignment:.trailing)}
        }.font(.system(size:13,weight:.medium)).monospacedDigit()
            .environment(\.locale,Locale(identifier:"en_US_POSIX")).environment(\.layoutDirection,.leftToRight)
    }
}
struct PrayerActivityCard:View {
    let city:String
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        VStack(alignment:.leading,spacing:10) {
            HStack(alignment:.center,spacing:12) {
                if !city.isEmpty {Text(city).font(.caption).foregroundStyle(.secondary).lineLimit(1)}
                Spacer(minLength:0)
                PrayerActivityCloseButton(eventID:state.eventID)
            }
            HStack(alignment:.center,spacing:12) {
                PrayerActivityHeading(state:state).frame(maxWidth:.infinity,alignment:.leading)
                PrayerActivityRemaining(state:state,isExpired:isExpired)
            }
            PrayerActivityFooter(city:city,state:state,isExpired:isExpired)
        }.environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct Countdown:View {
    let date:Date
    var now=Date()
    var width:CGFloat=120
    var alignment:TextAlignment = .leading
    var body:some View {
        Group {
            if date>now {Text(timerInterval:now...date,countsDown:true)}
            else {Text("0:00")}
        }
        // Dynamic Text is horizontally flexible in an archived WidgetKit view.
        // A concrete width is required even when the ordinary SwiftUI preview fits.
        .monospacedDigit().lineLimit(1).minimumScaleFactor(0.85)
        .frame(width:width,alignment:alignment == .trailing ? .trailing : alignment == .center ? .center : .leading)
        .multilineTextAlignment(alignment)
        .environment(\.locale,Locale(identifier:"en_US_POSIX")).environment(\.layoutDirection,.leftToRight)
    }
}
// Illustrative gallery data is used only in WidgetKit's preview/placeholder context and
// the explicitly labeled UI-test showcase. Installed live widgets always read App Group data.
private enum PrayerPreview {
    static func entry(arabic:Bool=false)->PrayerEntry {
        let now=Date(),start=Calendar.current.startOfDay(for:now)
        let names=arabic ? ["الفجر","الشروق","الظهر","العصر","المغرب","العشاء"] : ["Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"]
        let ids=["FAJR","SUNRISE","DHUHR","ASR","MAGHRIB","ISHA"]
        let text=arabic ? ["5:12 ص","6:31 ص","12:18 م","3:42 م","6:04 م","7:34 م"] : ["5:12 AM","6:31 AM","12:18 PM","3:42 PM","6:04 PM","7:34 PM"]
        let events=ids.enumerated().map {index,id in
            let seconds=Double(index-4)*3600+872
            return SnapshotEvent(id:"2026-10-18."+id,title:names[index],date:now.addingTimeInterval(seconds),timeText:text[index])
        }
        let day=SnapshotDay(dateISO:"2026-10-18",start:start,end:start.addingTimeInterval(86400),hijriEnglish:"7 Jumada I 1448",hijriArabic:"7 جمادى الأولى 1448",events:events)
        let currentID=events[3].id,nextID=events[4].id
        var moment=SnapshotMoment(date:now,civilDateISO:day.dateISO,religiousDateISO:day.dateISO,hijriEnglish:day.hijriEnglish,hijriArabic:day.hijriArabic,currentEventID:currentID,nextEventID:nextID)
        moment.displayEventIDs=events.map(\.id)
        let snapshot=ScheduleSnapshot(city:arabic ? "المدينة المنورة" : "Madinah",events:events,validUntil:now.addingTimeInterval(86400),language:arabic ? "ar" : "en",use24Hour:false,days:[day],moments:[moment],zone:"Asia/Riyadh")
        return PrayerEntry(date:now,snapshot:snapshot)
    }
}
#if SALATI_APP
struct Phase2ShowcaseView:View {
    @EnvironmentObject var model:AppModel
    private var entry:PrayerEntry {PrayerPreview.entry(arabic:model.arabic)}
    @State private var actualActivityStatus:String?
    private var overnight:PrayerEntry? {scenario("afterIsha")}
    private var midnight:PrayerEntry? {scenario("afterMidnight")}
    private func scenario(_ phase:String)->PrayerEntry? {
        guard let fixture=model.showcaseSnapshot(phase:phase) else {return nil}
        return PrayerEntry(date:fixture.date,snapshot:fixture.snapshot)
    }
    var body:some View {
        ScrollView {
            VStack(alignment:.leading,spacing:20) {
                Text("Presentation previews").font(.headline)
                Text("Illustrative gallery data. These are app-rendered previews of the actual widget and activity views, not installed system surfaces.").font(.footnote).foregroundStyle(.secondary)
                if ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase") {
                    HStack {
                        Button("Start system Live Activity preview") {Task {await startActualActivity()}}.accessibilityIdentifier("actualActivity.start")
                        Button("Clean up preview") {Task {await cleanActualActivities();actualActivityStatus="Preview ended"}}.accessibilityIdentifier("actualActivity.cleanup")
                        Button("Inspect preview") {inspectActualActivities()}.accessibilityIdentifier("actualActivity.inspect")
                    }.buttonStyle(.bordered).font(.caption)
                    if let status=actualActivityStatus {Text(status).font(.footnote).accessibilityIdentifier("actualActivity.status")}
                }
                preview("Small widget",id:"preview.small") {PrayerWidgetContent(entry:entry,family:.systemSmall).frame(width:158,height:158)}
                preview("Medium widget",id:"preview.medium") {PrayerWidgetContent(entry:entry,family:.systemMedium).frame(height:140)}
                preview("Large widget",id:"preview.large") {PrayerWidgetContent(entry:entry,family:.systemLarge).frame(height:340)}
                preview("Horizontal · Medium",id:"preview.horizontalMedium") {PrayerWidgetContent(entry:entry,family:.systemMedium,layout:.horizontal).frame(height:140)}
                preview("Horizontal · Large",id:"preview.horizontalLarge") {PrayerWidgetContent(entry:entry,family:.systemLarge,layout:.horizontal).frame(height:340)}
                if let overnight {
                preview("After Isha · Standard",id:"preview.overnightStandard") {PrayerWidgetContent(entry:overnight,family:.systemMedium).frame(height:140)}
                preview("After Isha · Horizontal",id:"preview.overnightHorizontal") {PrayerWidgetContent(entry:overnight,family:.systemMedium,layout:.horizontal).frame(height:140)}
                }
                if let midnight {
                preview("After midnight · Standard",id:"preview.midnightStandard") {PrayerWidgetContent(entry:midnight,family:.systemMedium).frame(height:140)}
                preview("After midnight · Horizontal",id:"preview.midnightHorizontal") {PrayerWidgetContent(entry:midnight,family:.systemMedium,layout:.horizontal).frame(height:140)}
                }
                preview("Lock Screen · Inline",id:"preview.inline") {PrayerWidgetContent(entry:entry,family:.accessoryInline)}
                preview("Lock Screen · Circular",id:"preview.circular") {PrayerWidgetContent(entry:entry,family:.accessoryCircular).frame(width:72,height:72)}
                preview("Lock Screen · Rectangular",id:"preview.rectangular") {PrayerWidgetContent(entry:entry,family:.accessoryRectangular).frame(width:180,height:70)}
                if let next=entry.next {
                    let state=PrayerActivityAttributes.ContentState(title:next.title,date:next.date,eventID:next.id,language:model.settings.language,start:next.date.addingTimeInterval(-900),timeText:next.timeText,endMessage:model.arabic ? "حان وقت الصلاة" : "It is time for prayer")
                    preview("Live Activity · Lock Screen",id:"preview.activity") {PrayerActivityCard(city:entry.snapshot?.city ?? "",state:state)}
                    preview("Live Activity · Expired content",id:"preview.activityExpired") {PrayerActivityCard(city:entry.snapshot?.city ?? "",state:state,isExpired:true)}
                    preview("Dynamic Island · Compact content",id:"preview.compact") {
                        HStack(spacing:16) {PrayerActivityCompactName(state:state);Spacer(minLength:0);PrayerActivityCompactTime(state:state)}
                    }
                    preview("Dynamic Island · Minimal content",id:"preview.minimal") {PrayerActivityMinimalName(state:state).frame(width:36,height:24)}
                    preview("Dynamic Island · Expanded content",id:"preview.expanded") {
                        PrayerActivityExpanded(city:entry.snapshot?.city ?? "",state:state)
                    }
                }
            }.padding(16)
        }.navigationTitle("Presentation previews").navigationBarTitleDisplayMode(.inline)
    }
    @MainActor private func cleanActualActivities() async {
        for activity in Activity<PrayerActivityAttributes>.activities where activity.content.state.eventID.hasPrefix("salati.showcase.") {
            await activity.end(activity.content.state.finalContent,dismissalPolicy:.immediate)
        }
    }
    @MainActor private func inspectActualActivities() {
        let count=Activity<PrayerActivityAttributes>.activities.filter {
            $0.content.state.eventID.hasPrefix("salati.showcase.") && $0.activityState != .dismissed && $0.activityState != .ended
        }.count
        actualActivityStatus="Preview activities: \(count)"
    }
    @MainActor private func startActualActivity() async {
        guard ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase") else {return}
        await cleanActualActivities()
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {actualActivityStatus="Live Activities are disabled";return}
        let arguments=ProcessInfo.processInfo.arguments
        let durationIndex=arguments.firstIndex(of:"-uiTestingActivityDuration")
        let duration=durationIndex.flatMap {index in index+1<arguments.count ? Double(arguments[index+1]) : nil} ?? 900
        let start=Date(),date=start.addingTimeInterval(min(900,max(1,duration)))
        let state=PrayerActivityAttributes.ContentState(title:model.arabic ? "المغرب" : "Maghrib",date:date,eventID:"salati.showcase."+UUID().uuidString,language:model.settings.language,start:start,timeText:model.time(date.timeIntervalSince1970),endMessage:model.arabic ? "حان وقت الصلاة" : "It is time for prayer")
        do {
            _=try Activity<PrayerActivityAttributes>.request(attributes:PrayerActivityAttributes(city:model.locationDisplayName),content:ActivityContent(state:state,staleDate:date),pushType:nil)
            await model.scheduler.cleanupLiveActivities()
            actualActivityStatus="System Live Activity preview started"
        } catch {actualActivityStatus=error.localizedDescription}
    }
    private func preview<Content:View>(_ title:LocalizedStringKey,id:String,@ViewBuilder content:()->Content)->some View {
        VStack(alignment:.leading,spacing:8) {
            HStack {Text(title).font(.subheadline.weight(.semibold));Spacer();NavigationLink {RendererPreviewDetail(title:title,content:content())} label:{Image(systemName:"arrow.up.left.and.arrow.down.right")}.accessibilityLabel("Open preview").accessibilityIdentifier("open."+id)}
            content().padding(14).frame(maxWidth:.infinity,alignment:.leading).background(.thinMaterial,in:RoundedRectangle(cornerRadius:20))
        }.accessibilityElement(children:.contain).accessibilityIdentifier(id)
    }
}
private struct RendererPreviewDetail<Content:View>:View {
    let title:LocalizedStringKey
    let content:Content
    var body:some View {
        ScrollView {
            VStack(alignment:.leading,spacing:18) {
                Text("App-rendered preview").font(.caption.weight(.medium)).foregroundStyle(.secondary)
                Text("Shared calculation fixtures with live system timers.").font(.caption).foregroundStyle(.secondary)
                content.padding(14).frame(maxWidth:.infinity,alignment:.leading).background(.thinMaterial,in:RoundedRectangle(cornerRadius:20))
                Text("System placement and Dynamic Island geometry require separate verification.").font(.footnote).foregroundStyle(.secondary)
            }.padding(16)
        }.navigationTitle(title).navigationBarTitleDisplayMode(.inline).accessibilityIdentifier("rendererPreviewDetail")
    }
}
#else
@main struct SalatiWidgetBundle:WidgetBundle {var body:some Widget {PrayerWidget();PrayerHorizontalWidget();PrayerAccessoryWidget();PrayerLiveActivity()}}
#endif
