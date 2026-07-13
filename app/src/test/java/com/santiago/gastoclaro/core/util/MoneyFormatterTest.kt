package com.santiago.gastoclaro.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun formatsMoneyInputWhileTyping() {
        assertEquals("1.500", formatMoneyInput("1500"))
        assertEquals("1.234.567,89", formatMoneyInput("1234567,89"))
        assertEquals("1.234,5", formatMoneyInput("1234,5"))
        assertEquals("0,", formatMoneyInput("0,"))
    }

    @Test
    fun parsesArgentinianAmounts() {
        assertEquals(150_050L, parseMoneyToCents("1.500,50"))
        assertEquals(2_345_000L, parseMoneyToCents("23.450"))
        assertEquals(150_000L, parseMoneyToCents("1500"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        assertNull(parseMoneyToCents(""))
        assertNull(parseMoneyToCents("hola"))
    }
}
