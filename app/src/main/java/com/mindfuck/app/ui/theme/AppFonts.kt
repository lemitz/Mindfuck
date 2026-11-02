package com.mindfuck.app.ui.theme

import android.content.Context
import android.graphics.Typeface
import com.mindfuck.app.R
import java.io.File

/**
 * Realme "Real Choice" (variable font, wght 100..1000; 550 = Regular, 700 = Medium).
 * android.graphics Typefaces for Canvas drawing (charts, PDF). Compose uses [RealChoice].
 */
object AppFonts {
    const val REGULAR = 550
    const val MEDIUM = 700
    const val BOLD = 850

    private val cache = HashMap<Int, Typeface>()
    @Volatile private var file: File? = null

    private fun fontFile(context: Context): File {
        file?.let { return it }
        synchronized(this) {
            file?.let { return it }
            val f = File(context.applicationContext.noBackupFilesDir, "realchoice.ttf")
            if (!f.exists() || f.length() == 0L) {
                context.resources.openRawResource(R.font.realchoice).use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
            }
            file = f
            return f
        }
    }

    fun typeface(context: Context, weight: Int = REGULAR): Typeface = synchronized(cache) {
        cache.getOrPut(weight) {
            Typeface.Builder(fontFile(context))
                .setFontVariationSettings("'wght' $weight")
                .build()
        }
    }
}
