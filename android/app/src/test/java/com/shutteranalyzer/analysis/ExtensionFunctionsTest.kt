package com.shutteranalyzer.analysis

import com.shutteranalyzer.analysis.model.median
import com.shutteranalyzer.analysis.model.percentile
import com.shutteranalyzer.analysis.model.stdDev
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for extension functions on List<Double>.
 */
class ExtensionFunctionsTest {

    @Test
    fun `median of odd list`() {
        val values = listOf(1.0, 3.0, 5.0, 7.0, 9.0)
        assertEquals(5.0, values.median(), 0.001)
    }

    @Test
    fun `median of even list`() {
        val values = listOf(1.0, 3.0, 5.0, 7.0)
        assertEquals(4.0, values.median(), 0.001)
    }

    @Test
    fun `median of single element`() {
        val values = listOf(42.0)
        assertEquals(42.0, values.median(), 0.001)
    }

    @Test
    fun `median of empty list`() {
        val values = emptyList<Double>()
        assertEquals(0.0, values.median(), 0.001)
    }

    @Test
    fun `percentile at 0`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(10.0, values.percentile(0), 0.001)
    }

    @Test
    fun `percentile at 100`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(50.0, values.percentile(100), 0.001)
    }

    @Test
    fun `percentile at 50`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(30.0, values.percentile(50), 0.001)
    }

    @Test
    fun `percentile at 25`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(20.0, values.percentile(25), 0.001)
    }

    @Test
    fun `stdDev of uniform values`() {
        val values = listOf(5.0, 5.0, 5.0, 5.0, 5.0)
        assertEquals(0.0, values.stdDev(), 0.001)
    }

    @Test
    fun `stdDev of varied values`() {
        // Values: 2, 4, 4, 4, 5, 5, 7, 9
        // Mean = 5
        // Variance = ((2-5)^2 + (4-5)^2 + (4-5)^2 + (4-5)^2 + (5-5)^2 + (5-5)^2 + (7-5)^2 + (9-5)^2) / 8
        //          = (9 + 1 + 1 + 1 + 0 + 0 + 4 + 16) / 8 = 32/8 = 4
        // StdDev = 2
        val values = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        assertEquals(2.0, values.stdDev(), 0.001)
    }

    @Test
    fun `stdDev of single element`() {
        val values = listOf(42.0)
        assertEquals(0.0, values.stdDev(), 0.001)
    }

    @Test
    fun `stdDev of empty list`() {
        val values = emptyList<Double>()
        assertEquals(0.0, values.stdDev(), 0.001)
    }
}
