package com.restart.spacestationtracker.shared.passes

import kotlin.test.Test
import kotlin.test.assertEquals

class CompassLabelsTest {
    @Test
    fun translatesEachCompassPoint() {
        assertEquals("WNW", CompassLabels.localize("WNW", "en"))
        assertEquals("ONO", CompassLabels.localize("WNW", "fr"))
        assertEquals("NO", CompassLabels.localize("NE", "de"))
        assertEquals("ЗСЗ", CompassLabels.localize("WNW", "ru"))
        assertEquals("北東", CompassLabels.localize("NE", "ja"))
        assertEquals("شمال غرب", CompassLabels.localize("NW", "ar"))
    }

    @Test
    fun everyLanguageCoversAllSixteenPoints() {
        val codes = (0 until 16).map { compassPoint(it * 22.5) }
        listOf("de", "es", "fr", "pt", "ru", "ja", "zh", "ko", "ar", "hi", "id", "vi").forEach { language ->
            val labels = codes.map { CompassLabels.localize(it, language) }
            assertEquals(16, labels.toSet().size, "$language has duplicate or missing directions")
        }
    }

    @Test
    fun leavesUnknownTextAlone() {
        assertEquals("", CompassLabels.localize("", "de"))
        assertEquals("NE", CompassLabels.localize("NE", "xx"))
    }
}
