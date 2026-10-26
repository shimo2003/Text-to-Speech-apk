package com.example.model

import java.util.Locale

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val locale: Locale,
    val isRtl: Boolean = false
) {
    companion object {
        val ARABIC_SA = SupportedLanguage(
            code = "ar-SA",
            name = "Arabic (Saudi Arabia / Standard)",
            nativeName = "العربية (السعودية / الفصحى)",
            flag = "🇸🇦",
            locale = Locale("ar", "SA"),
            isRtl = true
        )

        val ARABIC_EG = SupportedLanguage(
            code = "ar-EG",
            name = "Arabic (Egypt)",
            nativeName = "العربية (مصر)",
            flag = "🇪🇬",
            locale = Locale("ar", "EG"),
            isRtl = true
        )

        val ARABIC_AE = SupportedLanguage(
            code = "ar-AE",
            name = "Arabic (UAE)",
            nativeName = "العربية (الإمارات)",
            flag = "🇦🇪",
            locale = Locale("ar", "AE"),
            isRtl = true
        )

        val ARABIC_MA = SupportedLanguage(
            code = "ar-MA",
            name = "Arabic (Morocco)",
            nativeName = "العربية (المغرب)",
            flag = "🇲🇦",
            locale = Locale("ar", "MA"),
            isRtl = true
        )

        val ARABIC_GEN = SupportedLanguage(
            code = "ar",
            name = "Arabic (General)",
            nativeName = "العربية (عامة)",
            flag = "🌐",
            locale = Locale("ar"),
            isRtl = true
        )

        val ENGLISH_US = SupportedLanguage(
            code = "en-US",
            name = "English (US)",
            nativeName = "English (US)",
            flag = "🇺🇸",
            locale = Locale.US,
            isRtl = false
        )

        val ENGLISH_UK = SupportedLanguage(
            code = "en-GB",
            name = "English (UK)",
            nativeName = "English (UK)",
            flag = "🇬🇧",
            locale = Locale.UK,
            isRtl = false
        )

        val FRENCH_FR = SupportedLanguage(
            code = "fr-FR",
            name = "French (France)",
            nativeName = "Français",
            flag = "🇫🇷",
            locale = Locale.FRANCE,
            isRtl = false
        )

        val SPANISH_ES = SupportedLanguage(
            code = "es-ES",
            name = "Spanish (Spain)",
            nativeName = "Español",
            flag = "🇪🇸",
            locale = Locale("es", "ES"),
            isRtl = false
        )

        val GERMAN_DE = SupportedLanguage(
            code = "de-DE",
            name = "German (Germany)",
            nativeName = "Deutsch",
            flag = "🇩🇪",
            locale = Locale.GERMANY,
            isRtl = false
        )

        val ALL_LANGUAGES: List<SupportedLanguage> = listOf(
            ARABIC_SA,
            ARABIC_EG,
            ARABIC_AE,
            ARABIC_MA,
            ARABIC_GEN,
            ENGLISH_US,
            ENGLISH_UK,
            FRENCH_FR,
            SPANISH_ES,
            GERMAN_DE
        )

        fun findByCode(code: String): SupportedLanguage {
            return ALL_LANGUAGES.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: ALL_LANGUAGES.firstOrNull { it.locale.language.equals(code, ignoreCase = true) }
                ?: ARABIC_SA
        }
    }
}
