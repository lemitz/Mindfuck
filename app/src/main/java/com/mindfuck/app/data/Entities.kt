package com.mindfuck.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged day. [day] is LocalDate.toEpochDay().
 * Levels are 0..3 (none/mild/moderate/severe); null means "not logged".
 */
@Entity(tableName = "day_entry")
data class DayEntry(
    @PrimaryKey val day: Long,
    val sleepHours: Float? = null,
    val depression: Int? = null,
    val elevated: Int? = null,
    val irritability: Int? = null,
    val anxiety: Int? = null,
    val psychotic: Boolean? = null,
    val therapy: Boolean? = null,
    val notes: String = "",
) {
    val levels: List<Int?> get() = listOf(depression, elevated, irritability, anxiety)
    val maxLevel: Int? get() = levels.filterNotNull().maxOrNull()
    val hasAnyData: Boolean
        get() = sleepHours != null || levels.any { it != null } || psychotic != null ||
            therapy != null || notes.isNotBlank()
}

@Entity(tableName = "medication")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dose: String = "",
    val active: Boolean = true,
) {
    val title: String get() = if (dose.isBlank()) name else "$name $dose"
}

/**
 * Presence of a row = medication taken that day.
 * [dose] is what was actually taken that day (snapshot of the default at check-off time,
 * editable per day). Null on rows from v1 = the medication's default dose.
 */
@Entity(tableName = "med_intake", primaryKeys = ["day", "medId"], indices = [Index("medId")])
data class MedIntake(val day: Long, val medId: Long, val dose: String? = null)

@Entity(tableName = "custom_tracker")
data class CustomTracker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: Int,
    val active: Boolean = true,
) {
    val isScale: Boolean get() = type == SCALE

    companion object {
        const val SCALE = 0
        const val YES_NO = 1
    }
}

/** Scale trackers store 0..3, yes/no trackers store 0/1. */
@Entity(tableName = "custom_value", primaryKeys = ["day", "trackerId"], indices = [Index("trackerId")])
data class CustomValue(val day: Long, val trackerId: Long, val value: Int)
