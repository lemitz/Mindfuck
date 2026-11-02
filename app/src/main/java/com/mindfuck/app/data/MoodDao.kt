package com.mindfuck.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    // ---- day entries
    @Query("SELECT * FROM day_entry WHERE day = :day")
    fun observeEntry(day: Long): Flow<DayEntry?>

    @Query("SELECT * FROM day_entry WHERE day = :day")
    suspend fun getEntry(day: Long): DayEntry?

    @Query("SELECT * FROM day_entry WHERE day BETWEEN :from AND :to ORDER BY day")
    fun observeEntries(from: Long, to: Long): Flow<List<DayEntry>>

    @Query("SELECT * FROM day_entry WHERE day BETWEEN :from AND :to ORDER BY day")
    suspend fun getEntries(from: Long, to: Long): List<DayEntry>

    @Query("SELECT * FROM day_entry ORDER BY day")
    suspend fun allEntries(): List<DayEntry>

    @Upsert
    suspend fun upsertEntry(entry: DayEntry)

    @Upsert
    suspend fun upsertEntries(entries: List<DayEntry>)

    // ---- medications
    @Query("SELECT * FROM medication ORDER BY id")
    fun observeMeds(): Flow<List<Medication>>

    @Query("SELECT * FROM medication ORDER BY id")
    suspend fun allMeds(): List<Medication>

    @Insert
    suspend fun insertMed(med: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeds(meds: List<Medication>)

    @Update
    suspend fun updateMed(med: Medication)

    @Query("DELETE FROM medication WHERE id = :id")
    suspend fun deleteMedRow(id: Long)

    @Query("DELETE FROM med_intake WHERE medId = :id")
    suspend fun deleteIntakesOf(id: Long)

    @Transaction
    suspend fun deleteMed(id: Long) {
        deleteIntakesOf(id)
        deleteMedRow(id)
    }

    // ---- intakes
    @Query("SELECT * FROM med_intake WHERE day BETWEEN :from AND :to")
    fun observeIntakes(from: Long, to: Long): Flow<List<MedIntake>>

    @Query("SELECT * FROM med_intake WHERE day BETWEEN :from AND :to")
    suspend fun getIntakes(from: Long, to: Long): List<MedIntake>

    @Query("SELECT * FROM med_intake")
    suspend fun allIntakes(): List<MedIntake>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIntake(intake: MedIntake)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIntakes(intakes: List<MedIntake>)

    @Query("DELETE FROM med_intake WHERE day = :day AND medId = :medId")
    suspend fun removeIntake(day: Long, medId: Long)

    @Query("UPDATE med_intake SET dose = :dose WHERE day = :day AND medId = :medId")
    suspend fun setIntakeDose(day: Long, medId: Long, dose: String?)

    /** Most recently used doses of a medication, newest first. */
    @Query("SELECT dose FROM med_intake WHERE medId = :medId AND dose IS NOT NULL AND dose != '' GROUP BY dose ORDER BY MAX(day) DESC LIMIT 6")
    suspend fun recentDoses(medId: Long): List<String>

    // ---- custom trackers
    @Query("SELECT * FROM custom_tracker ORDER BY id")
    fun observeTrackers(): Flow<List<CustomTracker>>

    @Query("SELECT * FROM custom_tracker ORDER BY id")
    suspend fun allTrackers(): List<CustomTracker>

    @Insert
    suspend fun insertTracker(tracker: CustomTracker): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackers(trackers: List<CustomTracker>)

    @Update
    suspend fun updateTracker(tracker: CustomTracker)

    @Query("DELETE FROM custom_tracker WHERE id = :id")
    suspend fun deleteTrackerRow(id: Long)

    @Query("DELETE FROM custom_value WHERE trackerId = :id")
    suspend fun deleteValuesOf(id: Long)

    @Transaction
    suspend fun deleteTracker(id: Long) {
        deleteValuesOf(id)
        deleteTrackerRow(id)
    }

    // ---- custom values
    @Query("SELECT * FROM custom_value WHERE day BETWEEN :from AND :to")
    fun observeValues(from: Long, to: Long): Flow<List<CustomValue>>

    @Query("SELECT * FROM custom_value WHERE day BETWEEN :from AND :to")
    suspend fun getValues(from: Long, to: Long): List<CustomValue>

    @Query("SELECT * FROM custom_value")
    suspend fun allValues(): List<CustomValue>

    @Upsert
    suspend fun upsertValue(value: CustomValue)

    @Upsert
    suspend fun upsertValues(values: List<CustomValue>)

    @Query("DELETE FROM custom_value WHERE day = :day AND trackerId = :trackerId")
    suspend fun clearValue(day: Long, trackerId: Long)

    // ---- wipe (used by backup restore)
    @Query("DELETE FROM day_entry") suspend fun wipeEntries()
    @Query("DELETE FROM medication") suspend fun wipeMeds()
    @Query("DELETE FROM med_intake") suspend fun wipeIntakes()
    @Query("DELETE FROM custom_tracker") suspend fun wipeTrackers()
    @Query("DELETE FROM custom_value") suspend fun wipeValues()

    @Transaction
    suspend fun replaceAll(
        entries: List<DayEntry>,
        meds: List<Medication>,
        intakes: List<MedIntake>,
        trackers: List<CustomTracker>,
        values: List<CustomValue>,
    ) {
        wipeValues(); wipeTrackers(); wipeIntakes(); wipeMeds(); wipeEntries()
        upsertEntries(entries)
        insertMeds(meds)
        addIntakes(intakes)
        insertTrackers(trackers)
        upsertValues(values)
    }
}
