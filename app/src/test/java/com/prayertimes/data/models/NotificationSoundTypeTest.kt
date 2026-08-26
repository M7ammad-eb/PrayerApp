package com.prayertimes.data.models

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.util.Locale

class NotificationSoundTypeTest {
    @Test
    fun selectableSoundsAreGroupedAndComplete() {
        val sounds = NotificationSoundType.selectableValues(isArabic = false)

        assertEquals(NotificationSoundType.ATHAN_DEFAULT, sounds.first())
        assertEquals("", NotificationSoundType.ATHAN_DEFAULT.subtitle)
        assertEquals(
            listOf(
                NotificationSoundType.ATHAN_EGYPT_ALALFI,
                NotificationSoundType.ATHAN_FAJR2_JORDAN_ALLALA,
                NotificationSoundType.ATHAN_FAJR1_KWAIT_ALAFASY
            ),
            sounds.drop(1).take(3)
        )
        assertEquals(
            listOf(
                NotificationSoundType.DEVICE_DEFAULT,
                NotificationSoundType.VIBRATE_ONLY,
                NotificationSoundType.SILENT
            ),
            sounds.takeLast(3)
        )
        assertEquals(NotificationSoundType.entries.toSet(), sounds.toSet())
        assertEquals(sounds.size, sounds.distinct().size)
    }

    @Test
    fun remainingAthansFollowTheSelectedLanguageAlphabet() {
        listOf(false to "en", true to "ar").forEach { (isArabic, languageTag) ->
            val regularAthans = NotificationSoundType.selectableValues(isArabic).drop(4).dropLast(3)
            val collator = Collator.getInstance(Locale.forLanguageTag(languageTag))
            val expected = regularAthans.sortedWith { first, second ->
                collator.compare(
                    first.localizedDisplayName(isArabic),
                    second.localizedDisplayName(isArabic)
                )
            }
            assertEquals(expected, regularAthans)
        }
    }
}
