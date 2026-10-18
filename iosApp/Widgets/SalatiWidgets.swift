import WidgetKit
import SwiftUI
import ActivityKit

struct PrayerEntry: TimelineEntry {
    let date:Date
    let snapshot:ScheduleSnapshot?
    var next:SnapshotEvent? {snapshot?.nextEvent(after:date)}
}
struct Provider: TimelineProvider {
    func placeholder(in context:Context) -> PrayerEntry {PrayerEntry(date:Date(),snapshot:nil)}
    func getSnapshot(in context:Context,completion:@escaping (PrayerEntry)->Void) {completion(PrayerEntry(date:Date(),snapshot:ScheduleSnapshot.load()))}
    func getTimeline(in context:Context,completion:@escaping (Timeline<PrayerEntry>)->Void) {
        let now=Date(),snapshot=ScheduleSnapshot.load()
        var entries=[PrayerEntry(date:now,snapshot:snapshot)]
        if let snapshot {
            entries += snapshot.events.filter {$0.date>now}.map {PrayerEntry(date:$0.date,snapshot:snapshot)}
            entries.append(PrayerEntry(date:snapshot.validUntil,snapshot:snapshot))
        }
        completion(Timeline(entries:entries,policy:.after(min(snapshot?.validUntil ?? now.addingTimeInterval(3600),now.addingTimeInterval(6*3600)))))
    }
}
struct PrayerWidget: Widget {
    let kind="SalatiPrayer"
    var body:some WidgetConfiguration {
        StaticConfiguration(kind:kind,provider:Provider()) { entry in
            VStack(alignment:.leading,spacing:8) {
                if let next=entry.next,let snapshot=entry.snapshot,entry.date<snapshot.validUntil {
                    Text(snapshot.city).font(.caption).foregroundStyle(.secondary)
                    Text(next.title).font(.title2.weight(.semibold))
                    Text(timerInterval:entry.date...next.date,countsDown:true).font(.title.monospacedDigit()).environment(\.locale,Locale(identifier:"en_US_POSIX"))
                    Text(next.timeText).font(.caption)
                } else { Label("Open Salati to refresh",systemImage:"moon.stars").font(.headline) }
            }.frame(maxWidth:.infinity,alignment:.leading).containerBackground(.fill.tertiary,for:.widget)
                .environment(\.locale,Locale(identifier:entry.snapshot?.language == "ar" ? "ar_SA@numbers=latn" : "en_US@numbers=latn"))
                .environment(\.layoutDirection,entry.snapshot?.language == "ar" ? .rightToLeft : .leftToRight)
        }.configurationDisplayName("Next prayer").description("Your next prayer, calculated offline.").supportedFamilies([.systemSmall,.systemMedium])
    }
}
struct PrayerLiveActivity: Widget {
    var body:some WidgetConfiguration {
        ActivityConfiguration(for:PrayerActivityAttributes.self) { context in
            HStack {
                VStack(alignment:.leading) {Text(context.attributes.city).font(.caption);Text(context.state.title).font(.headline)}
                Spacer()
                Countdown(date:context.state.date)
            }.padding().activityBackgroundTint(Color.green.opacity(0.12))
        } dynamicIsland: {context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {Label(context.state.title,systemImage:"moon.stars")}
                DynamicIslandExpandedRegion(.trailing) {Countdown(date:context.state.date)}
                DynamicIslandExpandedRegion(.bottom) {Text(context.attributes.city).font(.caption)}
            } compactLeading: {Image(systemName:"moon.stars")}
            compactTrailing: {Countdown(date:context.state.date).frame(maxWidth:64)}
            minimal: {Image(systemName:"moon.stars")}
        }
    }
}
struct Countdown:View {
    let date:Date
    var body:some View {
        if date>Date() {Text(timerInterval:Date()...date,countsDown:true).monospacedDigit().environment(\.locale,Locale(identifier:"en_US_POSIX"))}
        else {Image(systemName:"checkmark")}
    }
}
@main struct SalatiWidgetBundle:WidgetBundle {var body:some Widget {PrayerWidget();PrayerLiveActivity()}}
