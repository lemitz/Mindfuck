package com.mindfuck.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DayBundle(
    val entry: DayEntry,
    /** medId -> dose stored on the intake (null = medication default) */
    val takenMeds: Map<Long, String?>,
    val custom: Map<Long, Int>,
)

/** Everything logged between [from] and [to] (epoch days, inclusive). */
data class RangeData(
    val from: Long,
    val to: Long,
    val entries: Map<Long, DayEntry>,
    /** day -> (medId -> stored dose, null = medication default) */
    val intakes: Map<Long, Map<Long, String?>>,
    val custom: Map<Long, Map<Long, Int>>,
) {
    fun entry(day: Long): DayEntry? = entries[day]
    fun taken(day: Long, medId: Long): Boolean = intakes[day]?.containsKey(medId) == true
    fun custom(day: Long, trackerId: Long): Int? = custom[day]?.get(trackerId)

    /** Dose actually taken: null = not taken, "" = taken but no dose known. */
    fun doseOn(day: Long, med: Medication): String? {
        val m = intakes[day] ?: return null
        if (!m.containsKey(med.id)) return null
        return m[med.id] ?: med.dose
    }

    /** A day counts as logged if anything at all was recorded for it. */
    fun isLogged(day: Long): Boolean =
        entries[day]?.hasAnyData == true || !intakes[day].isNullOrEmpty() || !custom[day].isNullOrEmpty()

    fun hasMed(medId: Long): Boolean = intakes.values.any { it.containsKey(medId) }

    companion object {
        fun of(from: Long, to: Long, e: List<DayEntry>, i: List<MedIntake>, v: List<CustomValue>) = RangeData(
            from = from,
            to = to,
            entries = e.associateBy { it.day },
            intakes = i.groupBy { it.day }.mapValues { (_, l) -> l.associate { it.medId to it.dose } },
            custom = v.groupBy { it.day }.mapValues { (_, l) -> l.associate { it.trackerId to it.value } },
        )
    }
}

class MoodRepository(private val db: AppDatabase) {
    val dao = db.dao()
    private val writeLock = Mutex()

    val meds: Flow<List<Medication>> = dao.observeMeds()
    val trackers: Flow<List<CustomTracker>> = dao.observeTrackers()

    fun observeDay(day: Long): Flow<DayBundle> = combine(
        dao.observeEntry(day), dao.observeIntakes(day, day), dao.observeValues(day, day),
    ) { e, i, v ->
        DayBundle(e ?: DayEntry(day), i.associate { it.medId to it.dose }, v.associate { it.trackerId to it.value })
    }

    fun observeRange(from: Long, to: Long): Flow<RangeData> = combine(
        dao.observeEntries(from, to), dao.observeIntakes(from, to), dao.observeValues(from, to),
    ) { e, i, v -> RangeData.of(from, to, e, i, v) }

    suspend fun getRange(from: Long, to: Long): RangeData =
        RangeData.of(from, to, dao.getEntries(from, to), dao.getIntakes(from, to), dao.getValues(from, to))

    suspend fun getEntry(day: Long): DayEntry? = dao.getEntry(day)

    suspend fun updateEntry(day: Long, transform: (DayEntry) -> DayEntry) = writeLock.withLock {
        val current = dao.getEntry(day) ?: DayEntry(day)
        val next = transform(current)
        if (next != current) dao.upsertEntry(next)
    }

    /** Checking a medication off stores the dose taken = its current default. */
    suspend fun setIntake(day: Long, med: Medication, taken: Boolean) {
        if (taken) dao.addIntake(MedIntake(day, med.id, med.dose.trim().ifBlank { null }))
        else dao.removeIntake(day, med.id)
    }

    suspend fun setIntakeDose(day: Long, medId: Long, dose: String) =
        dao.setIntakeDose(day, medId, dose.trim().ifBlank { null })

    suspend fun recentDoses(medId: Long): List<String> = dao.recentDoses(medId)

    suspend fun setCustom(day: Long, trackerId: Long, value: Int?) {
        if (value == null) dao.clearValue(day, trackerId) else dao.upsertValue(CustomValue(day, trackerId, value))
    }

    suspend fun addMed(name: String, dose: String) = dao.insertMed(Medication(name = name.trim(), dose = dose.trim()))
    suspend fun updateMed(med: Medication) = dao.updateMed(med)
    suspend fun deleteMed(id: Long) = dao.deleteMed(id)

    suspend fun addTracker(name: String, type: Int) = dao.insertTracker(CustomTracker(name = name.trim(), type = type))
    suspend fun updateTracker(t: CustomTracker) = dao.updateTracker(t)
    suspend fun deleteTracker(id: Long) = dao.deleteTracker(id)

    /**
     * Merges an eMoods export into the diary in one transaction: nothing is deleted, logged
     * values are never overwritten (see [EmoodsImport.plan]). Returns what was done.
     */
    suspend fun importEmoods(data: EmoodsImport.Data): EmoodsImport.Plan = writeLock.withLock {
        db.withTransaction {
            val plan = EmoodsImport.plan(data, dao.allEntries(), dao.allMeds(), dao.allIntakes())
            val newIds = plan.newMeds.map { dao.insertMed(it) }
            dao.upsertEntries(plan.upserts)
            dao.addIntakes(
                plan.intakes.map { (intake, key) ->
                    val medId = if (key.startsWith("new:")) newIds[key.removePrefix("new:").toInt()] else key.toLong()
                    MedIntake(intake.date.toEpochDay(), medId, intake.dose)
                },
            )
            plan
        }
    }

    suspend fun allMeds() = dao.allMeds()
    suspend fun allTrackers() = dao.allTrackers()
}
