package com.shutteranalyzer.data.local.database.converter

import androidx.room.TypeConverter

/**
 * Type converters for Room database.
 */
class Converters {

    /**
     * Converts a list of doubles to a comma-separated string for storage.
     */
    @TypeConverter
    fun fromDoubleList(values: List<Double>): String {
        return values.joinToString(",")
    }

    /**
     * Converts a comma-separated string back to a list of doubles.
     */
    @TypeConverter
    fun toDoubleList(value: String): List<Double> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { it.toDouble() }
    }
}
