package com.mindfuck.app

import android.app.Application
import com.mindfuck.app.data.AppDatabase
import com.mindfuck.app.data.MoodRepository
import com.mindfuck.app.ui.theme.AppFonts
import com.mindfuck.app.ui.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MindfuckApp : Application() {
    /**
     * Writes go here so they survive leaving a screen mid-edit. A single-lane dispatcher keeps
     * them in submission order (e.g. fast typing in the notes field never saves an older text last).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: MoodRepository by lazy { MoodRepository(database) }

    override fun onCreate() {
        super.onCreate()
        ThemeSettings.init(this)
        // Warm up the font file + DB off the main thread so the first frame is not blocked.
        CoroutineScope(Dispatchers.IO).launch {
            AppFonts.typeface(this@MindfuckApp)
            AppFonts.typeface(this@MindfuckApp, AppFonts.BOLD)
            database.openHelper.writableDatabase
        }
    }

    fun write(block: suspend MoodRepository.() -> Unit) {
        appScope.launch { repository.block() }
    }
}
