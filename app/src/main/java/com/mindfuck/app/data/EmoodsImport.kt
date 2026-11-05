package com.mindfuck.app.data

import java.io.InputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

/**
 * Reads an eMoods export (`export.emoods`: a ZIP of CSV files) and merges it into the diary
 * without deleting anything.
 *
 * eMoods scales are shifted by one compared to ours: 0 = not answered, 1 = none, 2 = mild,
 * 3 = moderate, 4 = severe (verified against a real export and the report printed from it).
 * Yes/no answers: 0 = not answered, 1 = no, 2 = yes. Medication TOOK: 2 = taken.
 */
object EmoodsImport {

    data class Entry(
        val date: LocalDate,
        val depression: Int?,
        val elevated: Int?,
        val irritability: Int?,
        val anxiety: Int?,
        val sleepHours: Float?,
        val psychotic: Boolean?,
        val therapy: Boolean?,
        val note: String,
    ) {
        val hasData: Boolean
            get() = depression != null || elevated != null || irritability != null || anxiety != null ||
                sleepHours != null || psychotic != null || therapy != null || note.isNotBlank()
    }

    data class Med(val id: String, val name: String, val dose: String)
    data class Intake(val date: LocalDate, val medId: String, val dose: String?)
    data class Data(val entries: List<Entry>, val meds: List<Med>, val intakes: List<Intake>)

    // ------------------------------------------------------------------ parsing

    fun read(input: InputStream): Data {
        val files = HashMap<String, String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                if (!e.isDirectory) files[e.name.substringAfterLast('/').lowercase()] = zip.readBytes().decodeToString()
            }
        }
        val entryCsv = files["entry.csv"] ?: throw IllegalArgumentException("Not an eMoods export: entry.csv is missing")
        return parse(
            entryCsv = entryCsv,
            medicationCsv = files["medication.csv"],
            entryMedicationCsv = files["entry_medication.csv"],
            timestampedNoteCsv = files["timestamped_note.csv"],
        )
    }

    fun parse(entryCsv: String, medicationCsv: String?, entryMedicationCsv: String?, timestampedNoteCsv: String?): Data {
        val entryRows = Csv.parse(entryCsv)
        val entryDateById = HashMap<String, LocalDate>()
        val extraNotes = HashMap<String, MutableList<String>>() // entry id -> notes

        timestampedNoteCsv?.let { Csv.parse(it) }?.forEach { r ->
            val note = r["NOTE"].orEmpty().trim()
            val entryId = r["ENTRY"].orEmpty()
            if (note.isNotEmpty() && entryId.isNotEmpty()) extraNotes.getOrPut(entryId) { mutableListOf() } += note
        }

        val entries = entryRows.mapNotNull { r ->
            val date = r.date() ?: return@mapNotNull null
            r["ID"]?.let { entryDateById[it] = date }
            val notes = listOfNotNull(r["NOTE"]?.trim()?.takeIf { it.isNotEmpty() }) +
                extraNotes[r["ID"]].orEmpty()
            Entry(
                date = date,
                depression = level(r["DEPRESSED"]),
                elevated = level(r["ELEVATED"]),
                irritability = level(r["IRRITABILITY"]),
                anxiety = level(r["ANXIETY"]),
                sleepHours = r["SLEEP"]?.trim()?.replace(',', '.')?.toFloatOrNull()?.takeIf { it in 0f..24f },
                psychotic = yesNo(r["PSYCHOTIC SYMPTOMS"]),
                therapy = yesNo(r["THERAPY"]),
                note = notes.joinToString("\n"),
            )
        }
            // one entry per day: if eMoods ever had two rows for a date, merge them in order
            .groupBy { it.date }
            .map { (_, list) -> list.reduce { a, b -> fill(a, b) } }
            .sortedBy { it.date }

        val meds = medicationCsv?.let { Csv.parse(it) }.orEmpty().mapNotNull { r ->
            val id = r["ID"] ?: return@mapNotNull null
            val name = r["NAME"].orEmpty().trim()
            if (name.isEmpty()) null else Med(id, name, dose(r["DOSE"], r["UNITS"]))
        }

        val intakes = entryMedicationCsv?.let { Csv.parse(it) }.orEmpty().mapNotNull { r ->
            if (r["TOOK"]?.trim() != "2") return@mapNotNull null
            val date = entryDateById[r["ENTRY"]] ?: return@mapNotNull null
            val medId = r["MEDICATION"] ?: return@mapNotNull null
            Intake(date, medId, dose(r["DOSETOOK"], r["UNITSTOOK"]).ifBlank { null })
        }.distinctBy { it.date to it.medId }

        return Data(entries, meds, intakes)
    }

    private fun Map<String, String>.date(): LocalDate? {
        val raw = (this["DATE (YYYY-MM-DD)"] ?: this["DATE"])?.trim() ?: return null
        return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }

    /** eMoods 1..4 -> ours 0..3; 0 / blank = not answered. */
    fun level(raw: String?): Int? = raw?.trim()?.toIntOrNull()?.takeIf { it in 1..4 }?.minus(1)

    /** eMoods 1 = no, 2 = yes; 0 / blank = not answered. */
    fun yesNo(raw: String?): Boolean? = when (raw?.trim()) {
        "1" -> false
        "2" -> true
        else -> null
    }

    /** "150.0" + "mg" -> "150 mg" */
    fun dose(amount: String?, units: String?): String {
        val a = amount?.trim().orEmpty()
        val number = a.toDoubleOrNull()
        val text = when {
            number == null -> a
            number % 1.0 == 0.0 -> number.toLong().toString()
            else -> number.toString()
        }
        return listOf(text, units?.trim().orEmpty()).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private fun fill(a: Entry, b: Entry) = a.copy(
        depression = a.depression ?: b.depression,
        elevated = a.elevated ?: b.elevated,
        irritability = a.irritability ?: b.irritability,
        anxiety = a.anxiety ?: b.anxiety,
        sleepHours = a.sleepHours ?: b.sleepHours,
        psychotic = a.psychotic ?: b.psychotic,
        therapy = a.therapy ?: b.therapy,
        note = mergeNotes(a.note, b.note),
    )

    // ------------------------------------------------------------------ merging

    data class Plan(
        val upserts: List<DayEntry>,
        val newMeds: List<Medication>,
        /** intake + our medication key: an existing id ("12") or "new:<index into newMeds>" */
        val intakes: List<Pair<Intake, String>>,
        val newDays: Int,
        val filledDays: Int,
        val unchangedDays: Int,
        val skippedEmptyDays: Int,
    ) {
        val summary: String
            get() = buildString {
                append("$newDays new day${if (newDays == 1) "" else "s"} added")
                if (filledDays > 0) append(", $filledDays existing day${if (filledDays == 1) "" else "s"} filled in")
                if (unchangedDays > 0) append(", $unchangedDays already up to date")
                if (newMeds.isNotEmpty()) append(", medications added: ${newMeds.joinToString { it.title }}")
                if (intakes.isNotEmpty()) append(", ${intakes.size} intake${if (intakes.size == 1) "" else "s"}")
                append(".")
            }
    }

    /**
     * Existing diary data always wins: a day you already logged keeps its values and only gets
     * fields that are still empty; notes are appended line by line if not already there.
     * Medications are matched by name (case-insensitive); unknown ones are created.
     */
    fun plan(
        data: Data,
        existingEntries: List<DayEntry>,
        existingMeds: List<Medication>,
        existingIntakes: List<MedIntake>,
    ): Plan {
        val byDay = existingEntries.associateBy { it.day }
        val upserts = ArrayList<DayEntry>()
        var newDays = 0
        var filled = 0
        var unchanged = 0
        var skipped = 0

        for (e in data.entries) {
            val day = e.date.toEpochDay()
            val current = byDay[day]
            if (current == null) {
                if (!e.hasData) { skipped++; continue }
                upserts += DayEntry(
                    day = day, sleepHours = e.sleepHours,
                    depression = e.depression, elevated = e.elevated,
                    irritability = e.irritability, anxiety = e.anxiety,
                    psychotic = e.psychotic, therapy = e.therapy, notes = e.note,
                )
                newDays++
            } else {
                val merged = current.copy(
                    sleepHours = current.sleepHours ?: e.sleepHours,
                    depression = current.depression ?: e.depression,
                    elevated = current.elevated ?: e.elevated,
                    irritability = current.irritability ?: e.irritability,
                    anxiety = current.anxiety ?: e.anxiety,
                    psychotic = current.psychotic ?: e.psychotic,
                    therapy = current.therapy ?: e.therapy,
                    notes = mergeNotes(current.notes, e.note),
                )
                if (merged != current) { upserts += merged; filled++ } else unchanged++
            }
        }

        // medications: eMoods id -> our id (existing) or a placeholder key for a new one
        val existingByName = existingMeds.associateBy { it.name.trim().lowercase() }
        val newMeds = ArrayList<Medication>()
        val medKey = HashMap<String, String>()
        val newByName = HashMap<String, String>()
        for (m in data.meds) {
            val name = m.name.lowercase()
            val match = existingByName[name]
            medKey[m.id] = when {
                match != null -> match.id.toString()
                // eMoods may list the same drug twice (e.g. re-added); create it only once
                name in newByName -> newByName.getValue(name)
                else -> "new:${newMeds.size}".also {
                    newByName[name] = it
                    newMeds += Medication(name = m.name, dose = m.dose)
                }
            }
        }

        val existingPairs = existingIntakes.map { it.day to it.medId.toString() }.toHashSet()
        val intakes = data.intakes.mapNotNull { i ->
            val key = medKey[i.medId] ?: return@mapNotNull null
            if ((i.date.toEpochDay() to key) in existingPairs) null else i to key
        }

        return Plan(upserts, newMeds, intakes, newDays, filled, unchanged, skipped)
    }

    /** Appends lines of [extra] that [base] does not contain yet. */
    fun mergeNotes(base: String, extra: String): String {
        if (extra.isBlank()) return base
        if (base.isBlank()) return extra
        val have = base.lines().map { it.trim() }.toHashSet()
        val add = extra.lines().map { it.trim() }.filter { it.isNotEmpty() && it !in have }
        return if (add.isEmpty()) base else base.trimEnd() + "\n" + add.joinToString("\n")
    }
}

/** Minimal RFC 4180 CSV reader: quoted fields, "" escapes, line breaks inside quotes. */
internal object Csv {
    fun parse(text: String): List<Map<String, String>> {
        val rows = rows(text.removePrefix("\uFEFF"))
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1)
            .filter { r -> r.any { it.isNotEmpty() } }
            .map { r -> header.indices.associate { i -> header[i] to r.getOrElse(i) { "" } } }
    }

    private fun rows(s: String): List<List<String>> {
        val out = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                quoted && c == '"' && i + 1 < s.length && s[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < s.length && s[i + 1] == '\n') i++
                    row += field.toString(); field.clear()
                    out += row; row = ArrayList()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); out += row }
        return out
    }
}
