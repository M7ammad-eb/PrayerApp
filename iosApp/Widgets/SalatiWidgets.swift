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
    var isStaticPreview=false
    var next:SnapshotEvent? {snapshot?.nextEvent(after:date)}
    var currentID:String? {snapshot?.moment(at:date)?.currentEventID}
    var day:SnapshotDay? {snapshot?.day(at:date)}
    var schedule:[SnapshotEvent] {(snapshot?.displayEvents(at:date) ?? []).filter {showSunrise || !$0.id.hasSuffix(".SUNRISE")}}
    var tomorrowID:String? {snapshot?.moment(at:date)?.tomorrowEventID}
    var tomorrowEvent:SnapshotEvent? {schedule.first {$0.id == tomorrowID}}
    var previousEvent:SnapshotEvent? {
        guard let id=snapshot?.moment(at:date)?.previousEventID else {return nil}
        return snapshot?.events.first {$0.id == id}
    }
    var civilSchedule:[SnapshotEvent] {schedule.filter {$0.id != tomorrowID}}
    var nextIsTomorrow:Bool {next?.id == tomorrowID && tomorrowID != nil}
    var hijri:String? {
        guard let moment=snapshot?.moment(at:date) else {return nil}
        return snapshot?.language == "ar" ? moment.hijriArabic : moment.hijriEnglish
    }
    var arabic:Bool {snapshot?.language == "ar"}
    var locale:Locale {Locale(identifier:arabic ? "ar_SA@numbers=latn" : "en_US@numbers=latn")}
}
struct PrayerProvider:AppIntentTimelineProvider {
    func placeholder(in context:Context)->PrayerEntry {PrayerPreview.entry()}
    func snapshot(for configuration:PrayerWidgetOptions,in context:Context) async->PrayerEntry {
        entry(configuration,snapshot:context.isPreview ? PrayerPreview.entry().snapshot : ScheduleSnapshot.load(),date:Date())
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
            .environment(\.layoutDirection,entry.arabic ? .rightToLeft : .leftToRight)
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
                    if entry.nextIsTomorrow {(Text("Tomorrow")+Text(" · "+next.title+" · "+next.timeText)).font(.caption).monospacedDigit()}
                    else {Text(next.title+" · "+next.timeText).font(.caption).monospacedDigit()}
                case .accessoryCircular:
                    VStack(spacing:2) {if entry.nextIsTomorrow {Text("Tomorrow").font(.system(size:8))};Text(next.title).font(.caption2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.75);Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.caption.monospacedDigit()).minimumScaleFactor(0.6)}
                        .frame(maxWidth:.infinity,maxHeight:.infinity).widgetAccentable()
                default:
                    VStack(alignment:.leading,spacing:2) {
                        if entry.nextIsTomorrow {Text("Tomorrow").font(.caption2).foregroundStyle(.secondary)}
                        Text(next.title+" · "+next.timeText).font(.headline).lineLimit(1).minimumScaleFactor(0.7)
                        Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.body.monospacedDigit())
                    }
                }
            } else {empty}
        }
        .environment(\.locale,entry.locale)
        .environment(\.layoutDirection,entry.arabic ? .rightToLeft : .leftToRight)
    }
    private func small(_ next:SnapshotEvent)->some View {
        VStack(alignment:.leading,spacing:6) {
            if entry.showCity || entry.nextIsTomorrow {
                HStack(spacing:3) {if entry.showCity {city};Spacer(minLength:0);if entry.nextIsTomorrow {Text("Tomorrow").foregroundStyle(.blue)}}.font(.caption2).lineLimit(1).minimumScaleFactor(0.8)
            }
            Text(next.title).font(.title2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7).widgetAccentable()
            Text(next.timeText).font(.subheadline.weight(.medium)).monospacedDigit()
            Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.system(.title2,design:.rounded,weight:.light)).minimumScaleFactor(0.7)
            if entry.showDate,let hijri=entry.hijri {Text(hijri).font(.caption2).foregroundStyle(.secondary).lineLimit(2)}
        }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:.leading)
    }
    private func medium(_ next:SnapshotEvent)->some View {
        HStack(alignment:.center,spacing:14) {
            VStack(alignment:.leading,spacing:6) {
                if entry.showCity {city.font(.caption2)}
                Text(next.title).font(.title2.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7).widgetAccentable()
                HStack(spacing:4) {Text(next.timeText).monospacedDigit();if entry.nextIsTomorrow {Text("Tomorrow").foregroundStyle(.blue)}}.font(.caption)
                Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.system(.title2,design:.rounded,weight:.light)).minimumScaleFactor(0.7)
                if entry.showDate,let hijri=entry.hijri {Text(hijri).font(.caption2).foregroundStyle(.secondary).lineLimit(1)}
                previousContext
            }.frame(maxWidth:.infinity,alignment:.leading)
            Divider()
            VStack(spacing:3) {
                if entry.tomorrowID != nil || entry.previousEvent != nil {Text("Today").font(.system(size:9)).foregroundStyle(.secondary).frame(maxWidth:.infinity,alignment:.leading)}
                ForEach(entry.schedule) {event in scheduleRow(event,compact:true)}
            }.frame(maxWidth:.infinity)
        }.frame(maxWidth:.infinity,maxHeight:.infinity)
    }
    private func large(_ next:SnapshotEvent)->some View {
        VStack(alignment:.leading,spacing:10) {
            if entry.showCity {city.font(.subheadline)}
            HStack(alignment:.firstTextBaseline) {
                Text(next.title).font(.title2.weight(.semibold)).widgetAccentable()
                Spacer(minLength:8)
                Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.system(.title,design:.rounded,weight:.light)).minimumScaleFactor(0.6)
            }
            if entry.nextIsTomorrow {Text("Tomorrow").font(.caption.weight(.medium)).foregroundStyle(.blue)}
            previousContext
            if entry.showDate,let hijri=entry.hijri {
                Text(hijri).font(.caption).foregroundStyle(.secondary)
                if let iso=entry.day?.dateISO {Text(iso).font(.caption2).monospacedDigit().foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight)}
            }
            Divider()
            VStack(spacing:8) {ForEach(entry.schedule) {event in scheduleRow(event,compact:false)}}
        }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:.topLeading)
    }
    private var city:some View {Text(entry.snapshot?.city ?? "").foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.8)}
    private func scheduleRow(_ event:SnapshotEvent,compact:Bool)->some View {
        let isNext=event.id == entry.next?.id
        let isCurrent=event.id == entry.currentID
        return HStack(spacing:5) {
            Circle().fill(isNext ? Color.blue : (isCurrent ? Color.primary : Color.clear)).frame(width:4,height:4)
            VStack(alignment:.leading,spacing:0) {
                Text(event.title).lineLimit(1).minimumScaleFactor(0.7)
                if event.id == entry.tomorrowID {Text("Tomorrow").font(.system(size:compact ? 8 : 10)).foregroundStyle(.secondary)}
            }
            Spacer(minLength:3)
            Text(event.timeText).monospacedDigit().lineLimit(1).minimumScaleFactor(0.75)
        }.font(compact ? .caption2 : .subheadline).fontWeight(isNext || isCurrent ? .semibold : .regular)
            .foregroundStyle(isNext ? Color.blue : (isCurrent ? Color.primary : Color.secondary))
            .background(isNext ? Color.blue.opacity(0.06) : .clear,in:RoundedRectangle(cornerRadius:4))
            .accessibilityElement(children:.combine)
            .accessibilityValue(Text(LocalizedStringKey(isNext ? "Up next" : isCurrent ? "Current prayer" : "")))
    }
    @ViewBuilder private var previousContext:some View {
        if let previous=entry.previousEvent {
            HStack(spacing:4) {
                Circle().fill(Color.primary).frame(width:4,height:4)
                Text(previous.title).fontWeight(.semibold)
                Text("Previous day").foregroundStyle(.secondary)
            }.font(.caption2).lineLimit(1).minimumScaleFactor(0.8).accessibilityElement(children:.combine)
                .accessibilityValue(Text("Current prayer"))
        }
    }
    private func horizontal(_ next:SnapshotEvent,large:Bool)->some View {
        VStack(alignment:.leading,spacing:large ? 16 : 4) {
            if entry.showCity || entry.showDate {
                HStack(alignment:.firstTextBaseline,spacing:8) {
                    if entry.showCity {city}
                    Spacer(minLength:0)
                    if entry.showDate,let hijri=entry.hijri {Text(hijri).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.85)}
                }.font(large ? .caption : .system(size:10))
            }
            if large {Spacer(minLength:0)}
            HStack(alignment:.center,spacing:12) {
                VStack(alignment:.leading,spacing:3) {
                    HStack(spacing:5) {
                        Circle().fill(Color.blue).frame(width:5,height:5)
                        Text(next.title).font(large ? .title2.weight(.semibold) : .headline).lineLimit(1).minimumScaleFactor(0.8)
                        if entry.nextIsTomorrow {Text("Tomorrow").font(.caption2).foregroundStyle(.blue)}
                    }
                    Text(next.timeText).font(large ? .title3 : .subheadline).monospacedDigit()
                    if large {previousContext}
                }
                Spacer(minLength:4)
                Countdown(date:next.date,now:entry.date,previewReferenceDate:entry.isStaticPreview ? entry.date : nil).font(.system(large ? .largeTitle : .title2,design:.rounded,weight:.light))
                    .lineLimit(1).minimumScaleFactor(0.7).layoutPriority(1)
            }
            if large {Spacer(minLength:0);Divider()}
            if entry.tomorrowID != nil || entry.previousEvent != nil {Text("Today").font(.system(size:large ? 11 : 9)).foregroundStyle(.secondary)}
            HStack(alignment:.top,spacing:large ? 6 : 3) {
                ForEach(entry.civilSchedule) {event in horizontalCell(event,large:large)}
            }
            if let tomorrow=entry.tomorrowEvent {
                HStack(spacing:6) {
                    Circle().fill(Color.blue).frame(width:4,height:4)
                    Text("Tomorrow").foregroundStyle(.secondary)
                    Text(tomorrow.title).fontWeight(.semibold)
                    Spacer(minLength:4)
                    Text(tomorrow.timeText).fontWeight(.semibold).monospacedDigit()
                }.font(large ? .subheadline : .system(size:10)).foregroundStyle(.blue).accessibilityElement(children:.combine)
                    .accessibilityValue(Text("Up next"))
            } else if !large {previousContext}
            if large,entry.showDate,let iso=entry.day?.dateISO {
                Text(iso).font(.caption2).monospacedDigit().foregroundStyle(.secondary).environment(\.layoutDirection,.leftToRight)
            }
        }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:large ? .topLeading : .center)
    }
    private func horizontalCell(_ event:SnapshotEvent,large:Bool)->some View {
        let isNext=event.id == entry.next?.id,isCurrent=event.id == entry.currentID
        return VStack(spacing:large ? 8 : 2) {
            Text(event.title).font(large ? .caption.weight(.medium) : .system(size:10,weight:.medium)).lineLimit(1).minimumScaleFactor(0.9)
            Text(event.timeText).font(.system(size:large ? 12 : 10,weight:isNext || isCurrent ? .semibold : .regular)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.85)
            Circle().fill(isNext ? Color.blue : (isCurrent ? Color.primary : .clear)).frame(width:4,height:4)
        }.frame(maxWidth:.infinity).padding(.vertical,large ? 12 : 1)
            .foregroundStyle(isNext ? Color.blue : (isCurrent ? Color.primary : Color.secondary))
            .background(isNext ? Color.blue.opacity(0.08) : .clear,in:RoundedRectangle(cornerRadius:large ? 12 : 7))
            .accessibilityElement(children:.combine)
            .accessibilityValue(Text(LocalizedStringKey(isNext ? "Up next" : isCurrent ? "Current prayer" : "")))
    }
    @ViewBuilder private var empty:some View {
        if family == .accessoryInline {Text("Open Salati to refresh")}
        else {
            VStack(alignment:.leading,spacing:8) {
                Image(systemName:"moon.stars").font(.title3)
                Text("Open Salati to refresh").font(family == .accessoryCircular ? .caption2 : .headline)
                if family == .systemLarge || family == .systemMedium {
                    Text("Open the app to update your location and shared prayer schedule.").font(.caption).foregroundStyle(.secondary)
                }
            }.frame(maxWidth:.infinity,maxHeight:.infinity,alignment:.leading)
        }
    }
}

struct PrayerLiveActivity:Widget {
    var body:some WidgetConfiguration {
        ActivityConfiguration(for:PrayerActivityAttributes.self) {context in
            PrayerActivityCard(city:context.attributes.city,state:context.state,isExpired:context.isStale)
                .padding(16).activityBackgroundTint(Color.accentColor.opacity(0.08)).activitySystemActionForegroundColor(.primary)
        } dynamicIsland: {context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    PrayerActivityHeading(state:context.state).dynamicIsland(verticalPlacement:.belowIfTooWide)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    PrayerActivityRemaining(state:context.state,isExpired:context.isStale)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    PrayerActivityFooter(city:context.attributes.city,state:context.state,isExpired:context.isStale)
                }
            } compactLeading: {
                PrayerActivityCompactName(state:context.state)
            } compactTrailing: {
                PrayerActivityCompactTime(state:context.state,isExpired:context.isStale)
            } minimal: {
                PrayerActivityMinimalName(state:context.state)
            // Expanded regions sit beside the sensor and curved upper corners.
            // Keep their text within a shared safe inset without changing compact regions.
            }.contentMargins(.horizontal,24,for:.expanded)
                .contentMargins(.vertical,20,for:.expanded).keylineTint(Color.accentColor)
        }
    }
}
private func activityLocale(_ state:PrayerActivityAttributes.ContentState)->Locale {Locale(identifier:state.language == "ar" ? "ar_SA@numbers=latn" : "en_US@numbers=latn")}
private func activityDirection(_ state:PrayerActivityAttributes.ContentState)->LayoutDirection {state.language == "ar" ? .rightToLeft : .leftToRight}
struct PrayerActivityHeading:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {
        VStack(alignment:.leading,spacing:5) {
            Text(state.title).font(.headline).lineLimit(1).minimumScaleFactor(0.85)
            Text("At \(state.timeText)").font(.caption).monospacedDigit().foregroundStyle(.secondary)
        }.padding(.horizontal,2)
            .environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityRemaining:View {
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        VStack(alignment:.trailing,spacing:4) {
            if isExpired {Text(state.endMessage).font(.callout.weight(.medium)).multilineTextAlignment(.trailing)} else {
                Text("Remaining").font(.caption2).foregroundStyle(.secondary)
                Countdown(date:state.date).font(.system(.title2,design:.rounded,weight:.medium)).lineLimit(1).minimumScaleFactor(0.8)
            }
        }.padding(.horizontal,2).environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityFooter:View {
    let city:String
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        VStack(alignment:.leading,spacing:8) {
            if !city.isEmpty {Text(city).font(.caption).foregroundStyle(.secondary).lineLimit(1)}
            if !isExpired,let start=state.start,start<state.date {
                ProgressView(timerInterval:start...state.date,countsDown:false).tint(Color.accentColor).labelsHidden().accessibilityLabel("Prayer countdown progress")
            }
        }.padding(.horizontal,2).environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityCompactName:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {
        Text(state.title).font(.system(.caption,design:.rounded,weight:.semibold)).lineLimit(1).minimumScaleFactor(0.8)
            .padding(.horizontal,2).environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct PrayerActivityMinimalName:View {
    let state:PrayerActivityAttributes.ContentState
    var body:some View {Text(state.title).font(.system(size:9,weight:.semibold)).lineLimit(1).minimumScaleFactor(0.7).environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))}
}
struct PrayerActivityCompactTime:View {
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        Group {if isExpired {Text("0:00")} else {Countdown(date:state.date)}}
            .font(.system(.caption,design:.rounded,weight:.medium)).monospacedDigit()
            .frame(minWidth:44,idealWidth:52,maxWidth:62).lineLimit(1).minimumScaleFactor(0.8).padding(.horizontal,2)
            .environment(\.locale,Locale(identifier:"en_US_POSIX")).environment(\.layoutDirection,.leftToRight)
    }
}
struct PrayerActivityCard:View {
    let city:String
    let state:PrayerActivityAttributes.ContentState
    var isExpired=false
    var body:some View {
        VStack(alignment:.leading,spacing:12) {
            HStack(alignment:.center,spacing:16) {
                VStack(alignment:.leading,spacing:6) {
                    HStack(spacing:6) {Circle().fill(Color.accentColor).frame(width:5,height:5);Text(state.title).font(.title3.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.85)}
                    Text("At \(state.timeText)").font(.subheadline).monospacedDigit().foregroundStyle(.secondary)
                    if !city.isEmpty {Text(city).font(.caption).foregroundStyle(.secondary).lineLimit(1)}
                }.frame(maxWidth:.infinity,alignment:.leading)
                VStack(alignment:.trailing,spacing:5) {
                    if isExpired {Text(state.endMessage).font(.callout.weight(.semibold)).multilineTextAlignment(.trailing)} else {
                        Text("Remaining").font(.caption).foregroundStyle(.secondary)
                        Countdown(date:state.date).font(.system(.largeTitle,design:.rounded,weight:.medium)).lineLimit(1).minimumScaleFactor(0.75)
                    }
                }.frame(maxWidth:.infinity,alignment:.trailing)
            }
            if !isExpired,let start=state.start,start<state.date {
                ProgressView(timerInterval:start...state.date,countsDown:false).tint(Color.accentColor).labelsHidden().accessibilityLabel("Prayer countdown progress")
            }
        }.padding(.horizontal,2).environment(\.locale,activityLocale(state)).environment(\.layoutDirection,activityDirection(state))
    }
}
struct Countdown:View {
    let date:Date
    var now=Date()
    var previewReferenceDate:Date?=nil
    var body:some View {
        Group {
            if let reference=previewReferenceDate {Text(previewDuration(date.timeIntervalSince(reference)))}
            else if date>now {Text(timerInterval:now...date,countsDown:true)}
            else {Text("0:00")}
        }.monospacedDigit().environment(\.locale,Locale(identifier:"en_US_POSIX")).environment(\.layoutDirection,.leftToRight)
    }
    private func previewDuration(_ interval:TimeInterval)->String {
        let seconds=max(0,Int(interval)),hours=seconds/3600,minutes=(seconds%3600)/60
        if hours>0 {return String(format:"%d:%02d:%02d",locale:Locale(identifier:"en_US_POSIX"),hours,minutes,seconds%60)}
        return String(format:"%d:%02d",locale:Locale(identifier:"en_US_POSIX"),minutes,seconds%60)
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
        return PrayerEntry(date:fixture.date,snapshot:fixture.snapshot,isStaticPreview:true)
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
                        VStack(spacing:12) {HStack {PrayerActivityHeading(state:state);Spacer();PrayerActivityRemaining(state:state)};PrayerActivityFooter(city:entry.snapshot?.city ?? "",state:state)}
                    }
                }
            }.padding(16)
        }.navigationTitle("Presentation previews").navigationBarTitleDisplayMode(.inline)
    }
    @MainActor private func cleanActualActivities() async {
        for activity in Activity<PrayerActivityAttributes>.activities where activity.content.state.eventID.hasPrefix("salati.showcase.") {
            await activity.end(nil,dismissalPolicy:.immediate)
        }
    }
    @MainActor private func startActualActivity() async {
        guard ProcessInfo.processInfo.arguments.contains("-uiTestingShowcase") else {return}
        await cleanActualActivities()
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {actualActivityStatus="Live Activities are disabled";return}
        let start=Date(),date=start.addingTimeInterval(900)
        let state=PrayerActivityAttributes.ContentState(title:model.arabic ? "المغرب" : "Maghrib",date:date,eventID:"salati.showcase."+UUID().uuidString,language:model.settings.language,start:start,timeText:model.time(date.timeIntervalSince1970),endMessage:model.arabic ? "حان وقت الصلاة" : "It is time for prayer")
        do {
            _=try Activity<PrayerActivityAttributes>.request(attributes:PrayerActivityAttributes(city:model.locationDisplayName),content:ActivityContent(state:state,staleDate:date),pushType:nil)
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
                Text("Overnight scenarios use a fixed calculation date and a static remaining duration.").font(.caption).foregroundStyle(.secondary)
                content.padding(14).frame(maxWidth:.infinity,alignment:.leading).background(.thinMaterial,in:RoundedRectangle(cornerRadius:20))
                Text("System placement and Dynamic Island geometry require separate verification.").font(.footnote).foregroundStyle(.secondary)
            }.padding(16)
        }.navigationTitle(title).navigationBarTitleDisplayMode(.inline).accessibilityIdentifier("rendererPreviewDetail")
    }
}
#else
@main struct SalatiWidgetBundle:WidgetBundle {var body:some Widget {PrayerWidget();PrayerHorizontalWidget();PrayerAccessoryWidget();PrayerLiveActivity()}}
#endif
