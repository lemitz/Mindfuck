package com.mindfuck.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.mindfuck.app.ui.calendar.CalendarScreen
import com.mindfuck.app.ui.graph.GraphScreen
import com.mindfuck.app.ui.log.LogScreen
import com.mindfuck.app.ui.report.ReportScreen
import com.mindfuck.app.ui.settings.SettingsScreen
import com.mindfuck.app.ui.theme.LocalMoodColors
import java.time.LocalDate

enum class Tab(val label: String, val icon: ImageVector) {
    LOG("Log Moods", Icons.AutoMirrored.Outlined.Assignment),
    CALENDAR("Calendar", Icons.Outlined.CalendarMonth),
    GRAPH("Graph", Icons.Outlined.BarChart),
    REPORT("Report", Icons.Outlined.PictureAsPdf),
}

private const val SETTINGS_MS = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    // `tab` is the single source of truth; the pager follows it and reports swipes back into it.
    var tab by rememberSaveable { mutableStateOf(Tab.LOG) }
    var day by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val pager = rememberPagerState(initialPage = tab.ordinal) { Tab.entries.size }

    // Swipe finished on a page -> select that tab.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            if (tab.ordinal != page) tab = Tab.entries[page]
        }
    }
    // Tab tapped (or changed in code) -> animate the pager there. Compare with where the pager is
    // heading, not where it last settled: a quick second tap must be able to reverse the motion.
    LaunchedEffect(tab) {
        if (pager.targetPage != tab.ordinal) pager.animateScrollToPage(tab.ordinal)
    }

    BackHandler(enabled = !settings && tab != Tab.LOG) { tab = Tab.LOG }

    // Main screen recedes a little while Settings slides over it.
    val recede by animateFloatAsState(
        targetValue = if (settings) 1f else 0f,
        animationSpec = tween(SETTINGS_MS, easing = FastOutSlowInEasing),
        label = "recede",
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -size.width * 0.12f * recede
                    alpha = 1f - 0.35f * recede
                },
        ) {
            MainScaffold(
                tab = tab,
                onTab = { tab = it },
                pager = pager,
                userScrollEnabled = !settings,
                onSettings = { settings = true },
                day = day,
                onDayChange = { day = it },
                onOpenDay = { day = it; tab = Tab.LOG },
            )
        }

        AnimatedVisibility(
            visible = settings,
            enter = slideInHorizontally(tween(SETTINGS_MS, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(SETTINGS_MS / 2)),
            exit = slideOutHorizontally(tween(SETTINGS_MS, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(SETTINGS_MS)),
        ) {
            BackHandler { settings = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    // swallow touches so nothing reaches the screen underneath
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                SettingsScreen(onBack = { settings = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    tab: Tab,
    onTab: (Tab) -> Unit,
    pager: androidx.compose.foundation.pager.PagerState,
    userScrollEnabled: Boolean,
    onSettings: () -> Unit,
    day: Long,
    onDayChange: (Long) -> Unit,
    onOpenDay: (Long) -> Unit,
) {
    val mood = LocalMoodColors.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mindfuck", fontWeight = FontWeight.Medium) },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = mood.topBar,
                    titleContentColor = mood.onTopBar,
                    actionIconContentColor = mood.onTopBar,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { onTab(t) },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            // Keep the neighbouring tabs composed: otherwise every switch throws one page away and
            // builds the next one from scratch in the middle of the swipe.
            beyondViewportPageCount = 1,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) { page ->
            when (Tab.entries[page]) {
                Tab.LOG -> LogScreen(day = day, onDayChange = onDayChange)
                Tab.CALENDAR -> CalendarScreen(onOpenDay = onOpenDay)
                Tab.GRAPH -> GraphScreen()
                Tab.REPORT -> ReportScreen()
            }
        }
    }
}
