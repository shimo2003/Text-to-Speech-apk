package com.example.util

object ArabicTextHelper {

    private val ARABIC_DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val TATWEEL_REGEX = Regex("\\u0640")
    private val ARABIC_CHAR_REGEX = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    /**
     * Removes diacritics (حركات التشكيل) from Arabic text.
     */
    fun removeDiacritics(text: String): String {
        return text.replace(ARABIC_DIACRITICS_REGEX, "").replace(TATWEEL_REGEX, "")
    }

    /**
     * Checks if the text has predominantly Arabic characters.
     */
    fun isArabicText(text: String): Boolean {
        if (text.isBlank()) return false
        val arabicCharCount = text.count { char ->
            ARABIC_CHAR_REGEX.matches(char.toString())
        }
        return arabicCharCount > (text.length / 4)
    }

    /**
     * Sample Arabic sentences with proper vocalization / tashkeel for testing Text-to-Speech
     */
    val SAMPLE_ARABIC_PHRASES = listOf(
        "مَرْحَبًا بِكُمْ فِي اسْتُودْيُو الصَّوْتِ الذَّكِيِّ.",
        "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللهِ وَبَرَكَاتُهُ.",
        "اللُّغَةُ الْعَرَبِيَّةُ لُغَةُ الْجَمَالِ وَالْفَصَاحَةِ.",
        "صَبَاحُ الْخَيْرِ، أَتَمَنَّى لَكَ يَوْمًا سَعِيدًا وَمُثْمِرًا.",
        "تَحْوِيلُ الصَّوْتِ إِلَى نَصٍّ وَالنَّصِّ إِلَى كَلَامٍ بَدِقَّةٍ عَالِيَةٍ.",
        "الْعِلْمُ نُورٌ وَالْجَهْلُ ظَلَامٌ."
    )

    val SAMPLE_ENGLISH_PHRASES = listOf(
        "Welcome to the Voice to Text and Speech Studio.",
        "Testing speech synthesis with high clarity and natural tone.",
        "Technology makes communication between languages seamless.",
        "Have a wonderful and productive day!"
    )
}
