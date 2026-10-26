package com.voicestudio.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.voicestudio.app.model.SupportedLanguage
import com.voicestudio.app.util.ArabicTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Voice Studio", appName)
    }

    @Test
    fun `arabic text helper removes diacritics`() {
        val vocalized = "مَرْحَبًا بِكُمْ"
        val plain = ArabicTextHelper.removeDiacritics(vocalized)
        assertEquals("مرحبا بكم", plain)
    }

    @Test
    fun `arabic text detector recognizes arabic`() {
        val arabicText = "السلام عليكم ورحمة الله"
        assertTrue(ArabicTextHelper.isArabicText(arabicText))
    }

    @Test
    fun `arabic language lookup finds standard arabic`() {
        val lang = SupportedLanguage.findByCode("ar-SA")
        assertEquals("ar-SA", lang.code)
        assertTrue(lang.isRtl)
    }
}
