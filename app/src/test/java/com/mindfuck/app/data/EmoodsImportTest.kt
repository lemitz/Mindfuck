package com.mindfuck.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Made-up eMoods data only, in the column layout of a real export. */
class EmoodsImportTest {

    private val entryCsv = """
        "ID","DATE (YYYY-MM-DD)","IRRITABILITY","ANXIETY","DEPRESSED","ELEVATED","SLEEP","PSYCHOTIC SYMPTOMS","NOTE","THERAPY","MENSTRUAL CYCLE","WEIGHT"
        "1","2026-07-05","4","3","2","1","8.0","1",,"2","0",""
        "2","2026-07-06","0","0","0","0","","0",,"0","0",""
        "3","2026-07-07","1","2","3","4","6.5","2","walked, ""fine""
        second line","1","0",""
    """.trimIndent()

    private val medicationCsv = """
        "ID","NAME","DOSE","UNITS","AMPM","VISIBLE","FREQUENCY","SORTORDER","EDITABLEUNTIL"
        "1","Testamine","150.0","mg","1","true","0","999","2026-10-19T21:06:20+0300"
        "2","Placebol","0.5","g","1","true","0","999",""
    """.trimIndent()

    private val entryMedicationCsv = """
        "ID","ENTRY","MEDICATION","TOOK","DOSETOOK","UNITSTOOK","DOSEPRESCRIBED","UNITSPRESCRIBED"
        "1","1","1","2","150.0","mg","150.0","mg"
        "2","3","1","2","200.0","mg","150.0","mg"
        "3","3","2","1","0.5","g","0.5","g"
    """.trimIndent()

    private fun data() = EmoodsImport.parse(entryCsv, medicationCsv, entryMedicationCsv, null)
    private fun day(d: Int) = LocalDate.of(2026, 7, d).toEpochDay()

    @Test fun scalesAreShiftedByOne() {
        val e = data().entries.first { it.date.dayOfMonth == 5 }
        assertEquals(3, e.irritability) // 4 = severe
        assertEquals(2, e.anxiety)      // 3 = moderate
        assertEquals(1, e.depression)   // 2 = mild
        assertEquals(0, e.elevated)     // 1 = none
        assertEquals(8f, e.sleepHours)
        assertEquals(false, e.psychotic) // 1 = no
        assertEquals(true, e.therapy)    // 2 = yes
    }

    @Test fun zeroMeansNotAnswered() {
        val e = data().entries.first { it.date.dayOfMonth == 6 }
        assertNull(e.depression); assertNull(e.sleepHours); assertNull(e.psychotic); assertNull(e.therapy)
        assertFalse(e.hasData)
    }

    @Test fun quotedNotesWithNewlines() {
        val e = data().entries.first { it.date.dayOfMonth == 7 }
        assertEquals("walked, \"fine\"\nsecond line", e.note)
    }

    @Test fun medsAndIntakes() {
        val d = data()
        assertEquals(listOf("150 mg", "0.5 g"), d.meds.map { it.dose })
        // TOOK=1 (not taken) is dropped; dose taken that day is kept
        assertEquals(listOf("150 mg", "200 mg"), d.intakes.map { it.dose })
    }

    @Test fun mergeNeverOverwritesAndNeverDeletes() {
        val existing = listOf(
            // already logged in Mindfuck: depression set, sleep empty, has a note
            DayEntry(day = day(5), depression = 3, sleepHours = null, notes = "my own note"),
            // a day eMoods does not know about must survive untouched
            DayEntry(day = day(20), anxiety = 1),
        )
        val plan = EmoodsImport.plan(data(), existing, emptyList(), emptyList())
        val byDay = plan.upserts.associateBy { it.day }

        val merged = byDay.getValue(day(5))
        assertEquals(3, merged.depression)    // ours kept (eMoods says mild)
        assertEquals(8f, merged.sleepHours)   // empty field filled
        assertEquals(3, merged.irritability)  // empty field filled
        assertEquals("my own note", merged.notes)

        assertTrue(byDay.containsKey(day(7)))      // new day added
        assertFalse(byDay.containsKey(day(6)))     // empty eMoods day skipped
        assertFalse(byDay.containsKey(day(20)))    // untouched, not rewritten
        assertEquals(1, plan.newDays); assertEquals(1, plan.filledDays); assertEquals(1, plan.skippedEmptyDays)
    }

    @Test fun medicationsMatchedByNameAndIntakesNotDuplicated() {
        val ours = Medication(id = 42, name = "testamine", dose = "150 mg")
        val plan = EmoodsImport.plan(
            data(), emptyList(), listOf(ours),
            existingIntakes = listOf(MedIntake(day(5), 42, "150 mg")),
        )
        assertEquals(listOf("Placebol"), plan.newMeds.map { it.name }) // Testamine matched, not duplicated
        assertEquals(listOf(day(7) to "42"), plan.intakes.map { (i, key) -> i.date.toEpochDay() to key })
    }

    @Test fun reimportIsANoOp() {
        val first = EmoodsImport.plan(data(), emptyList(), emptyList(), emptyList())
        val second = EmoodsImport.plan(data(), first.upserts, emptyList(), emptyList())
        assertEquals(0, second.newDays); assertEquals(0, second.filledDays); assertTrue(second.upserts.isEmpty())
    }

    @Test fun readsTheZipContainer() {
        val zip = ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { z ->
                for ((name, text) in listOf("entry.csv" to entryCsv, "medication.csv" to medicationCsv)) {
                    z.putNextEntry(ZipEntry(name)); z.write(text.toByteArray()); z.closeEntry()
                }
            }
        }.toByteArray()
        assertEquals(3, EmoodsImport.read(zip.inputStream()).entries.size)
    }

    @Test fun notesMergeLineByLine() {
        assertEquals("a\nb\nc", EmoodsImport.mergeNotes("a\nb", "b\nc"))
        assertEquals("a", EmoodsImport.mergeNotes("a", ""))
        assertEquals("x", EmoodsImport.mergeNotes("", "x"))
    }
}
