package com.mindfuck.app.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.mindfuck.app.data.MoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** Generates reports into app storage and mirrors them to Downloads/Mindfuck. */
class ReportExporter(private val context: Context, private val repo: MoodRepository) {

    val dir: File get() = File(context.filesDir, "reports").apply { mkdirs() }

    suspend fun export(from: LocalDate, to: LocalDate, options: ReportOptions): File = withContext(Dispatchers.IO) {
        val range = repo.getRange(from.toEpochDay(), to.toEpochDay())
        val input = ReportInput(from, to, range, repo.allMeds(), repo.allTrackers())
        val file = File(dir, ReportFileNamer.next(context, dir))
        PdfReportGenerator(context).generate(input, options, file)
        runCatching { copyToDownloads(file) }
        file
    }

    fun list(): List<File> = dir.listFiles { f -> f.extension == "pdf" }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun delete(file: File) {
        file.delete()
    }

    private fun copyToDownloads(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Mindfuck")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    fun openIntent(file: File): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uriFor(file), "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    fun shareIntent(file: File): Intent = Intent.createChooser(
        Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uriFor(file))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        file.name,
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
