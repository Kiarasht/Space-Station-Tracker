package com.restart.spacestationtracker.shared.passes

import com.restart.spacestationtracker.shared.network.appLanguageCode
import com.restart.spacestationtracker.shared.network.wikipediaLanguage

/**
 * Compass directions in the reader's language. Passes and scheduled reminders keep the English codes
 * ("N", "WNW", ...) from [compassPoint] as stable data; they are translated only for display, here, so
 * Compose screens, Android notifications and iOS notifications all read the same way.
 */
object CompassLabels {
    private val codes = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")

    // Romance languages write East as E and West as O (oeste, ouest).
    private val romance = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO")

    private val labels: Map<String, List<String>> = mapOf(
        "de" to listOf("N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"),
        "es" to romance,
        "fr" to romance,
        "pt" to romance,
        "ru" to listOf("С", "ССВ", "СВ", "ВСВ", "В", "ВЮВ", "ЮВ", "ЮЮВ", "Ю", "ЮЮЗ", "ЮЗ", "ЗЮЗ", "З", "ЗСЗ", "СЗ", "ССЗ"),
        "ja" to listOf("北", "北北東", "北東", "東北東", "東", "東南東", "南東", "南南東", "南", "南南西", "南西", "西南西", "西", "西北西", "北西", "北北西"),
        "zh" to listOf("北", "北东北", "东北", "东东北", "东", "东东南", "东南", "南东南", "南", "南西南", "西南", "西西南", "西", "西西北", "西北", "北西北"),
        "ko" to listOf("북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동", "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서"),
        // Arabic has no common compass abbreviations, so directions are written out.
        "ar" to listOf(
            "شمال", "شمال شمال شرق", "شمال شرق", "شرق شمال شرق", "شرق", "شرق جنوب شرق", "جنوب شرق", "جنوب جنوب شرق",
            "جنوب", "جنوب جنوب غرب", "جنوب غرب", "غرب جنوب غرب", "غرب", "غرب شمال غرب", "شمال غرب", "شمال شمال غرب"
        ),
        "hi" to listOf("उ", "उ-उ-पू", "उ-पू", "पू-उ-पू", "पू", "पू-द-पू", "द-पू", "द-द-पू", "द", "द-द-प", "द-प", "प-द-प", "प", "प-उ-प", "उ-प", "उ-उ-प"),
        // Utara, Timur, Selatan, Barat, with Timur Laut, Tenggara, Barat Daya and Barat Laut between them.
        "id" to listOf("U", "UTL", "TL", "TTL", "T", "TTg", "Tg", "STg", "S", "SBD", "BD", "BBD", "B", "BBL", "BL", "UBL"),
        // Bắc, Đông, Nam, Tây.
        "vi" to listOf("B", "BĐB", "ĐB", "ĐĐB", "Đ", "ĐĐN", "ĐN", "NĐN", "N", "NTN", "TN", "TTN", "T", "TTB", "TB", "BTB")
    )

    /** [code] such as "WNW" in the app's language; unknown codes and English pass through unchanged. */
    fun localize(code: String): String = localize(code, wikipediaLanguage(appLanguageCode()))

    fun localize(code: String, language: String): String {
        val index = codes.indexOf(code.trim().uppercase())
        if (index < 0) return code
        return labels[language.lowercase()]?.get(index) ?: code
    }
}
