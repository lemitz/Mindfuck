package com.mindfuck.app.report

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Report language + extra sections on top of the base report (scale, day table, summary, last-day notes). */
data class ReportOptions(
    val sleep: Boolean = false,
    val yesNo: Boolean = false,
    val meds: Boolean = false,
    val custom: Boolean = false,
    val charts: Boolean = false,
    val allNotes: Boolean = false,
    val language: ReportLanguage = ReportLanguage.RU,
) {
    fun save(context: Context) {
        prefs(context).edit()
            .putBoolean("sleep", sleep).putBoolean("yesNo", yesNo).putBoolean("meds", meds)
            .putBoolean("custom", custom).putBoolean("charts", charts).putBoolean("allNotes", allNotes)
            .putString("language", language.name)
            .apply()
    }

    companion object {
        private fun prefs(context: Context) = context.getSharedPreferences("report_options", Context.MODE_PRIVATE)

        fun load(context: Context): ReportOptions = prefs(context).run {
            ReportOptions(
                sleep = getBoolean("sleep", false),
                yesNo = getBoolean("yesNo", false),
                meds = getBoolean("meds", false),
                custom = getBoolean("custom", false),
                charts = getBoolean("charts", false),
                allNotes = getBoolean("allNotes", false),
                language = ReportLanguage.entries.firstOrNull { it.name == getString("language", null) } ?: ReportLanguage.RU,
            )
        }
    }
}

/**
 * mindfuck_021126.pdf, then mindfuck_021126_v2.pdf, _v3 ... for reports made on the same day.
 * The counter never goes back, even if older files of that day were deleted.
 */
object ReportFileNamer {
    private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

    fun fileName(date: LocalDate, version: Int): String {
        val base = "mindfuck_${date.format(FMT)}"
        return if (version <= 1) "$base.pdf" else "${base}_v$version.pdf"
    }

    /** Picks the next free name for [date] and remembers it. */
    fun next(context: Context, dir: File, date: LocalDate = LocalDate.now()): String {
        val prefs = context.getSharedPreferences("report_names", Context.MODE_PRIVATE)
        val key = date.format(FMT)
        var version = prefs.getInt(key, 0) + 1
        while (File(dir, fileName(date, version)).exists()) version++
        prefs.edit().putInt(key, version).apply()
        return fileName(date, version)
    }
}
