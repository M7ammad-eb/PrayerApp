package com.salati.shared

import com.prayertimes.util.GeoUtils
import kotlin.math.abs

/** A religious date is a label, never a shift of the civil prayer schedule. */
object ReligiousDates {
    fun correspondence(civilDate: LocalDate, nowEpoch: Double, maghribEpoch: Double): LocalDate =
        if (nowEpoch >= maghribEpoch) civilDate.plusDays(1) else civilDate

    fun forSchedule(civilDateISO:String,nowEpoch:Double,maghribEpoch:Double,zone:String):String {
        val civil=LocalDate.parse(civilDateISO)
        return if(ZonedDateTime.fromEpochSeconds(nowEpoch.toLong(),ZoneId.of(zone)).toLocalDate() == civil)
            correspondence(civil,nowEpoch,maghribEpoch).toString() else civilDateISO
    }
    fun nextTransition(nowEpoch:Double,todayMaghrib:Double,tomorrowMaghrib:Double):Double =
        if(nowEpoch<todayMaghrib) todayMaghrib else tomorrowMaghrib
    fun adjusted(date: LocalDate, storedOffset: Int, anchor: String?): HijriDate =
        HijriDateCalculator.convertToHijri(date, HijriCalendar.effectiveAdjustment(storedOffset, anchor, date))
}

data class PlanEvent(val id: String, val prayer: String, val title: String, val epochSeconds: Double)
data class PlanPolicy(val prayer: String, val delivery: String, val reminderMinutes: Int)
data class PlannedDelivery(val id: String, val prayer: String, val title: String,
    val eventEpoch: Double, val triggerEpoch: Double, val kind: String)

/** API capacities are platform concerns. This plan preserves exact instants and stable identity. */
object SharedScheduling {
    fun plan(events: List<PlanEvent>, policies: List<PlanPolicy>, nowEpoch: Double,
             liveActivitiesEnabled: Boolean): List<PlannedDelivery> {
        val byPrayer = policies.associateBy { it.prayer }
        return events.distinctBy { it.id }.filter { it.prayer != "SUNRISE" && it.epochSeconds > nowEpoch }
            .flatMap { event ->
                val policy = byPrayer[event.prayer]
                val base = "salati.${event.id}"
                buildList {
                    if (policy?.delivery in setOf("alarm", "notification")) add(PlannedDelivery(
                        base, event.prayer, event.title, event.epochSeconds, event.epochSeconds, policy!!.delivery))
                    val reminder = policy?.reminderMinutes?.coerceIn(0, 60) ?: 0
                    if (reminder > 0 && event.epochSeconds - reminder * 60 > nowEpoch) add(PlannedDelivery(
                        "$base.reminder", event.prayer, event.title, event.epochSeconds,
                        event.epochSeconds - reminder * 60, "reminder"))
                    if (liveActivitiesEnabled) add(PlannedDelivery("$base.live", event.prayer, event.title,
                        event.epochSeconds, event.epochSeconds - 900, "live"))
                }
            }.sortedWith(compareBy<PlannedDelivery> { it.triggerEpoch }.thenBy { it.id })
    }
}

/** Android movement/fix thresholds plus a shared 15-minute routine acquisition throttle. */
object LocationAcceptance {
    const val movementMeters = 5000.0
    const val routineIntervalSeconds = 900.0
    fun usableFix(accuracyMeters: Double, fixEpoch: Double, nowEpoch: Double, explicit: Boolean): Boolean {
        val age = nowEpoch - fixEpoch
        return accuracyMeters.isFinite() && accuracyMeters >= 0 &&
            accuracyMeters <= (if (explicit) 250.0 else 500.0) &&
            age >= 0 && age <= (if (explicit) 900.0 else 7200.0)
    }
    fun shouldAcquire(lastEpoch: Double, nowEpoch: Double, isGps: Boolean, explicit: Boolean): Boolean =
        explicit || (isGps && nowEpoch - lastEpoch >= routineIntervalSeconds)

    fun meaningful(currentLatitude: Double, currentLongitude: Double, candidateLatitude: Double,
                   candidateLongitude: Double, zoneChanged: Boolean): Boolean = zoneChanged ||
        GeoUtils.haversineDistanceKm(currentLatitude, currentLongitude, candidateLatitude, candidateLongitude) * 1000 >= movementMeters

    fun accept(currentLatitude: Double, currentLongitude: Double, currentIsGps: Boolean,
               candidateLatitude: Double, candidateLongitude: Double, zoneChanged: Boolean,
               explicit: Boolean): Boolean = explicit || (currentIsGps && meaningful(currentLatitude,
        currentLongitude, candidateLatitude, candidateLongitude, zoneChanged))
}

/** The bundled cities5000 schema has no parent/admin relationship. Never infer one by proximity. */
object LocationLabels {
    fun readable(english: String, ascii: String, arabicName: String?, isArabic: Boolean): String =
        if (isArabic && !arabicName.isNullOrBlank()) arabicName else ascii.ifBlank { english }
}

data class CompassState(val rotation: Double, val reliable: Boolean, val aligned: Boolean,
                        val enteredAlignment: Boolean)
/** Geometry and hysteresis contain no locale or view direction. Clock supplied by the adapter. */
class CompassAlignmentTracker {
    private var rotation: Double? = null
    private var aligned = false
    private var candidateSince: Double? = null
    fun update(bearing: Double, heading: Double, accuracy: Double, nowEpoch: Double): CompassState {
        val reliable = bearing.isFinite() && heading.isFinite() && accuracy in 0.0..15.0
        val target = signed(bearing - heading)
        val continuous = rotation?.let { it + signed(target - it) } ?: target
        if (reliable) rotation = continuous
        val wasAligned = aligned
        if (!reliable || abs(target) > 8) { aligned = false; candidateSince = null }
        else if (!aligned && abs(target) <= 4) {
            val since = candidateSince ?: nowEpoch.also { candidateSince = it }
            if (nowEpoch - since >= 0.6) aligned = true
        } else if (!aligned) candidateSince = null
        return CompassState(rotation ?: continuous, reliable, aligned, aligned && !wasAligned)
    }
    private fun signed(angle: Double) = ((angle + 180) % 360 + 360) % 360 - 180
}

data class PresentationMoment(val epochSeconds: Double, val civilDateISO: String,
    val religiousDateISO: String, val hijriEnglish: String, val hijriArabic: String,
    val currentEventID: String?, val nextEventID: String?)

object SharedPresentation {
    fun timeline(days: List<PrayerDay>, nowEpoch: Double, validUntilEpoch: Double): List<PresentationMoment> {
        val events = days.flatMap { day -> day.events.map { day.dateISO + "." + it.id to it } }
            .sortedBy { it.second.epochSeconds }
        val boundaries = (listOf(nowEpoch) + days.flatMap { day ->
            listOf(day.civilStartEpoch, day.maghribEpoch, day.civilEndEpoch) + day.events.map { it.epochSeconds }
        }).filter { it >= nowEpoch && it < validUntilEpoch }.distinct().sorted()
        return boundaries.mapNotNull { instant ->
            val day = days.firstOrNull { instant >= it.civilStartEpoch && instant < it.civilEndEpoch } ?: return@mapNotNull null
            val sunset = instant >= day.maghribEpoch
            // Sunrise ends Fajr without becoming current; the next displayed event includes sunrise.
            val obligatory = events.filter { it.second.id != "SUNRISE" }
            val current=obligatory.lastOrNull { it.second.epochSeconds <= instant }
            val fajrEnded=current?.second?.id == "FAJR" && day.events.any { it.id == "SUNRISE" && it.epochSeconds<=instant }
            PresentationMoment(instant, day.dateISO, if (sunset) day.followingDateISO else day.dateISO,
                if (sunset) day.sunsetHijriEnglish else day.civilHijriEnglish,
                if (sunset) day.sunsetHijriArabic else day.civilHijriArabic,
                if(fajrEnded) null else current?.first,
                events.firstOrNull { it.second.epochSeconds > instant }?.first)
        }
    }
}
