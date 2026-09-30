package com.mtc.crock.util

import com.mtc.crock.data.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ExtensionsTest {

    @Test
    fun testToPercentString() {
        assertEquals("50%", 50.toPercentString())
        assertEquals("100%", 100.toPercentString())
        assertEquals("0%", 0.toPercentString())
    }

    @Test
    fun testTemperatureConversion() {
        Locale.setDefault(Locale.US)
        // 0°C in Celsius -> "0.0°C"
        assertEquals("0.0°C", 0.0.toFormattedTemperature(TemperatureUnit.CELSIUS))
        // 0°C in Fahrenheit -> 32°F -> "32.0°F"
        assertEquals("32.0°F", 0.0.toFormattedTemperature(TemperatureUnit.FAHRENHEIT))
        // 100°C in Fahrenheit -> 212°F -> "212.0°F"
        assertEquals("212.0°F", 100.0.toFormattedTemperature(TemperatureUnit.FAHRENHEIT))
        // 22.22°C -> 72°F
        assertEquals("72°F", 22.2222.toFormattedIntTemperature(TemperatureUnit.FAHRENHEIT))
        assertEquals("22°C", 22.2222.toFormattedIntTemperature(TemperatureUnit.CELSIUS))
    }
}
