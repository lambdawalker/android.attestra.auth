package com.apexfission.android.auth.ui.onboarding.common

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailOneTimeCodeFieldTest {
    @Test
    fun normalizesPastedEmailCodes() {
        assertEquals("123456", normalizeEmailOneTimeCode("123456"))
        assertEquals("123456", normalizeEmailOneTimeCode("123 456"))
        assertEquals("123456", normalizeEmailOneTimeCode("12-34 abc 56"))
        assertEquals("001234", normalizeEmailOneTimeCode("00 1234 999"))
        assertEquals("", normalizeEmailOneTimeCode("no code"))
    }
}
