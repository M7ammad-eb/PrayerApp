package com.salati.shared

data class CalculationDefaults(val method: String, val hanafi: Boolean, val fallback: Boolean)

/** Existing project time-zone method policy. Asr stays the existing standard default:
 * a geographic location is not evidence of a person's religious affiliation. */
object OnboardingDefaults {
    fun recommendation(zone: String): CalculationDefaults {
        val method = suggestCalculationMethod(zone)
        return CalculationDefaults(method.name, false, method == CalculationMethod.MUSLIM_WORLD_LEAGUE)
    }
    fun resolve(zone: String, currentMethod: String, currentHanafi: Boolean,
                methodOverridden: Boolean, asrOverridden: Boolean, completed: Boolean): CalculationDefaults {
        val defaults = recommendation(zone)
        return defaults.copy(method = if (completed || methodOverridden) currentMethod else defaults.method,
            hanafi = if (completed || asrOverridden) currentHanafi else defaults.hanafi)
    }
}
