package com.vythera.omniapps

import android.os.Build
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

enum class VAppTab { HOME, SEARCH, INSTALLED, PROFILE, SETTINGS }

private const val Q_TRENDING     = "topic:android apk stars:>500"
private const val Q_MEDIA        = "topic:android media player stars:>100"
private const val Q_TOOLS        = "topic:android utility tool stars:>100"
private const val Q_GAMES        = "topic:android game emulator stars:>100"
private const val Q_BROWSERS     = "topic:android browser privacy stars:>50"
private const val Q_PRODUCTIVITY = "topic:android productivity notes stars:>50"
private const val Q_SECURITY     = "topic:android security stars:>50"
private const val Q_DEVTOOLS     = "topic:android developer-tools stars:>50"
private const val Q_PHOTO        = "topic:android photo video editor stars:>100"
private const val Q_MUSIC        = "topic:android music audio stars:>100"
private const val Q_FINANCE      = "topic:android finance banking stars:>100"
private const val Q_EDUCATION    = "topic:android education learning stars:>100"
private const val Q_FITNESS      = "topic:android fitness health workout stars:>100"
private const val Q_ART          = "topic:android art design creative stars:>100"
private const val Q_NEWS         = "topic:android news reader stars:>100"
private const val Q_SOCIAL       = "topic:android social network stars:>100"
private const val Q_CLOUD        = "topic:android cloud storage files stars:>100"
private const val Q_COOKING      = "topic:android cooking food recipe stars:>50"



@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun HomeScreen(viewModel: AppViewModel = viewModel()) {

    val state = viewModel.state   // ← must be FIRST

    val view         = LocalView.current
    val context      = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val homeListState = rememberLazyListState()

    var selectedTab   by remember { mutableStateOf(VAppTab.HOME) }
    var selectedRepo  by remember { mutableStateOf<GitHubRepo?>(null) }
    var showSeeAll    by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }
    var lastBackPress by remember { mutableLongStateOf(0L) }
    var dockVisible   by remember { mutableStateOf(true) }

    // ── Theme ─────────────────────────────────────────────────────────────
    val baseTheme: AppThemeColors = when {
        state.settings.themeMode == "Custom" -> state.customTheme.toAppThemeColors()
        state.settings.themeMode == "AMOLED" -> AmoledTheme
        state.settings.amoledBlack           -> AmoledTheme
        state.settings.themeMode == "Light"  -> LightTheme
        state.settings.themeMode == "Dark"   -> DarkTheme
        state.settings.themeMode == "Minimal"-> MinimalTheme
        state.settings.themeMode == "Sunset" -> SunsetTheme
        state.settings.themeMode == "System" -> if (isSystemDark) DarkTheme else LightTheme
        else                                  -> DarkTheme
    }

    // ── Full Monet palette extraction (Android 12+) ───────────────────
    // Each theme uses M3-correct tone mappings so Monet plays a structural
    // role (not just accent tinting).
    val monetPalette: Map<String, Color?> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        remember(view) {
            val ctx = view.context
            fun safe(id: Int): Color? = try {
                Color(ctx.resources.getColor(id, ctx.theme))
            } catch (_: Exception) { null }
            mapOf(
                "a1_100" to safe(android.R.color.system_accent1_100),
                "a1_200" to safe(android.R.color.system_accent1_200),
                "a1_300" to safe(android.R.color.system_accent1_300),
                "a1_600" to safe(android.R.color.system_accent1_600),
                "a1_700" to safe(android.R.color.system_accent1_700),
                "a1_900" to safe(android.R.color.system_accent1_900),
                "a2_200" to safe(android.R.color.system_accent2_200),
                "a2_600" to safe(android.R.color.system_accent2_600),
                "a3_100" to safe(android.R.color.system_accent3_100),
                "a3_200" to safe(android.R.color.system_accent3_200),
                "a3_600" to safe(android.R.color.system_accent3_600),
                "a3_700" to safe(android.R.color.system_accent3_700),
                "n1_50"  to safe(android.R.color.system_neutral1_50),
                "n1_100" to safe(android.R.color.system_neutral1_100),
                "n1_800" to safe(android.R.color.system_neutral1_800),
                "n1_900" to safe(android.R.color.system_neutral1_900),
            )
        }
    } else emptyMap()

    val useMonetEffective = state.useMonet ||
        (state.settings.followSystemMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val theme: AppThemeColors = run {
        val manualAccent = state.accentColor
        if (useMonetEffective && monetPalette.isNotEmpty()) {
            val dark = baseTheme.isDark
            val primary   = manualAccent ?: if (dark) monetPalette["a1_200"] else monetPalette["a1_600"]
            val secondary = if (dark) monetPalette["a2_200"] else monetPalette["a2_600"]
            val tertiary  = if (dark) monetPalette["a3_200"] else monetPalette["a3_600"]
            val tertiaryContainer = if (dark) monetPalette["a3_700"] else monetPalette["a3_100"]
            val container  = if (dark) monetPalette["a1_700"] else monetPalette["a1_100"]
            val onContainer= if (dark) monetPalette["a1_100"] else monetPalette["a1_900"]
            baseTheme.copy(
                accent                  = primary               ?: baseTheme.accent,
                accentAlt               = secondary             ?: baseTheme.accentAlt,
                accentContainer         = container             ?: baseTheme.accentContainer,
                onAccentContainer       = onContainer           ?: baseTheme.onAccentContainer,
                accentTertiary          = tertiary              ?: baseTheme.accentTertiary,
                accentTertiaryContainer = tertiaryContainer     ?: baseTheme.accentTertiaryContainer,
                dockForeground          = (primary              ?: baseTheme.accent),
                bgPrimary               = (if (dark) monetPalette["n1_900"] else monetPalette["n1_50"])  ?: baseTheme.bgPrimary,
                bgSurface               = (if (dark) monetPalette["n1_900"] else monetPalette["n1_100"]) ?: baseTheme.bgSurface,
                bgSurfaceAlt            = (if (dark) monetPalette["n1_800"] else monetPalette["n1_100"]) ?: baseTheme.bgSurfaceAlt,
                bgSurfaceHigh           = (if (dark) monetPalette["n1_800"] else monetPalette["n1_100"]) ?: baseTheme.bgSurfaceHigh
            )
        } else {
            val eff = manualAccent ?: baseTheme.accent
            baseTheme.copy(accent = eff, dockForeground = eff)
        }
    }
    var fontFamily by remember(state.settings.fontName) {
        mutableStateOf(fontFamilyFor(state.settings.fontName.ifEmpty { "Default" }))
    }
    LaunchedEffect(state.settings.fontName) {
        val name = state.settings.fontName.ifEmpty { "Default" }
        if (name in googleFontNames) {
            fontFamily = loadGoogleFont(context, name)
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !theme.isDark
        }
    }

    val installedSet = remember(state.installStates) {
        state.installStates.filter { e -> e.value.isInstalled }.keys
    }

    // ── Back ──────────────────────────────────────────────────────────────
    BackHandler {
        when {
            showCompare -> { showCompare = false; viewModel.setCompareTarget(null) }
            selectedRepo != null -> {
                viewModel.refreshInstall(selectedRepo!!.id)
                selectedRepo = null
            }
            showSeeAll -> showSeeAll = false
            selectedTab == VAppTab.SEARCH &&
                (state.platform != AppPlatform.ALL || state.selectedSubCategories.isNotEmpty()) -> {
                viewModel.clearSearchFilter()
            }
            selectedTab != VAppTab.HOME -> selectedTab = VAppTab.HOME
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPress < 2000) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    lastBackPress = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    LaunchedEffect(selectedRepo) {
        selectedRepo?.let {
            viewModel.fetchRelease(it)
            viewModel.fetchScreenshots(it)
        }
    }
    LaunchedEffect(selectedRepo, state.settings.language) {
        if (selectedRepo != null && state.settings.language != "English") {
            viewModel.translateDescription(selectedRepo!!)
        }
    }

    CompositionLocalProvider(LocalTheme provides theme, LocalStrings provides stringsForLanguage(state.settings.language)) {
        val m3Colors = if (theme.isDark) darkColorScheme(
            primary              = theme.accent,
            primaryContainer     = theme.accentContainer,
            onPrimaryContainer   = theme.onAccentContainer,
            secondary            = theme.accentAlt,
            tertiary             = theme.accentTertiary,
            tertiaryContainer    = theme.accentTertiaryContainer,
            surface              = theme.bgSurface,
            surfaceContainer     = theme.bgSurfaceAlt,
            surfaceContainerHigh = theme.bgSurfaceHigh,
            background           = theme.bgPrimary,
            onBackground         = theme.textPrimary,
            onSurface            = theme.textPrimary,
            onSurfaceVariant     = theme.textSecondary,
            outline              = theme.border,
            outlineVariant       = theme.borderVariant
        ) else lightColorScheme(
            primary              = theme.accent,
            primaryContainer     = theme.accentContainer,
            onPrimaryContainer   = theme.onAccentContainer,
            secondary            = theme.accentAlt,
            tertiary             = theme.accentTertiary,
            tertiaryContainer    = theme.accentTertiaryContainer,
            surface              = theme.bgSurface,
            surfaceContainer     = theme.bgSurfaceAlt,
            surfaceContainerHigh = theme.bgSurfaceHigh,
            background           = theme.bgPrimary,
            onBackground         = theme.textPrimary,
            onSurface            = theme.textPrimary,
            onSurfaceVariant     = theme.textSecondary,
            outline              = theme.border,
            outlineVariant       = theme.borderVariant
        )

        MaterialTheme(
            colorScheme = m3Colors,
            typography  = MaterialTheme.typography.run {
                copy(
                    displayLarge   = displayLarge.copy(fontFamily   = fontFamily),
                    displayMedium  = displayMedium.copy(fontFamily  = fontFamily),
                    displaySmall   = displaySmall.copy(fontFamily   = fontFamily),
                    headlineLarge  = headlineLarge.copy(fontFamily  = fontFamily),
                    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
                    headlineSmall  = headlineSmall.copy(fontFamily  = fontFamily),
                    titleLarge     = titleLarge.copy(fontFamily     = fontFamily),
                    titleMedium    = titleMedium.copy(fontFamily    = fontFamily),
                    titleSmall     = titleSmall.copy(fontFamily     = fontFamily),
                    bodyLarge      = bodyLarge.copy(fontFamily      = fontFamily),
                    bodyMedium     = bodyMedium.copy(fontFamily     = fontFamily),
                    bodySmall      = bodySmall.copy(fontFamily      = fontFamily),
                    labelLarge     = labelLarge.copy(fontFamily     = fontFamily),
                    labelMedium    = labelMedium.copy(fontFamily    = fontFamily),
                    labelSmall     = labelSmall.copy(fontFamily     = fontFamily)
                )
            }
        ) {
            // Hoist screen state so bottomBar can read it
            var lastRepo by remember { mutableStateOf<GitHubRepo?>(null) }
            if (selectedRepo != null) lastRepo = selectedRepo

            val currentScreen = when {
                showCompare -> "COMPARE"
                selectedRepo != null -> "DETAIL"
                showSeeAll -> "SEE_ALL"
                else -> "TABS"
            }

            Scaffold(
                containerColor      = Color.Transparent,
                contentWindowInsets = WindowInsets(0)
            ) { _ ->

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

                    AnimatedContent(
                        targetState  = currentScreen,
                        transitionSpec = {
                            val order   = listOf("TABS", "SEE_ALL", "DETAIL", "COMPARE")
                            val forward = order.indexOf(targetState) > order.indexOf(initialState)
                            if (forward) {
                                (slideInHorizontally(tween(320)) { it } + fadeIn(tween(250))) togetherWith
                                        (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(180)))
                            } else {
                                (slideInHorizontally(tween(320)) { -it } + fadeIn(tween(250))) togetherWith
                                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(180)))
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label    = "screen_nav"
                    ) { screen ->
                        val repo = lastRepo
                        when (screen) {
// ── Compare ───────────────────────────────────
                            "COMPARE" -> {
                                if (repo != null) {
                                    CompareScreen(
                                        leftRepo      = repo,
                                        rightRepo     = state.compareTargetRepo,
                                        searchResults = state.searchResults,
                                        onSearch      = { viewModel.onSearch(it) },
                                        onPickRight   = { viewModel.setCompareTarget(it) },
                                        onBack        = { showCompare = false; viewModel.setCompareTarget(null) }
                                    )
                                }
                            }
// ── Detail ────────────────────────────────────
                            "DETAIL" -> {
                                if (repo != null) {
                                    val installState = state.installStates[repo.id] ?: InstallState()
                                    AppDetailScreen(
                                        repo                  = repo,
                                        installState          = installState,
                                        isFavourite           = state.favourites.any { f -> f.id == repo.id },
                                        translatedDesc        = state.translatedDescriptions[repo.id],
                                        isTranslating         = state.isTranslating[repo.id] ?: false,
                                        translatedReleaseBody = state.translatedReleaseBodies[repo.id],
                                        isTranslatingRelease  = state.isTranslatingRelease[repo.id] ?: false,
                                        state                 = state,
                                        screenshots           = state.screenshots[repo.id] ?: emptyList(),
                                        onInstall             = { installState.apkAsset?.let { a -> viewModel.downloadAndInstall(repo, a) } },
                                        onDownloadOnly        = { installState.apkAsset?.let { a -> viewModel.downloadOnly(repo, a) } },
                                        onUninstall           = { viewModel.uninstall(repo) },
                                        onCancelDownload      = { viewModel.cancelDownload(repo) },
                                        onTranslate           = { viewModel.translateDescription(repo) },
                                        onTranslateRelease    = { viewModel.translateReleaseBody(repo) },
                                        onToggleFavourite     = { viewModel.toggleFavourite(repo) },
                                        onIgnoreVersion       = {
                                            installState.release?.tag_name?.let { viewModel.ignoreVersion(repo.id, it) }
                                        },
                                        onCompare             = { showCompare = true },
                                        onSelectRelease       = { rel -> viewModel.selectRelease(repo.id, rel) },
                                        onSelectAsset         = { asset -> viewModel.selectAsset(repo.id, asset) },
                                        onBack                = { viewModel.refreshInstall(repo.id); selectedRepo = null }
                                    )
                                }
                            }
// ── See All ───────────────────────────────────
                            "SEE_ALL" -> SeeAllScreen(
                                title         = state.seeAllTitle,
                                apps          = state.seeAllApps,
                                installed     = installedSet,
                                isLoading     = state.isLoadingSeeAll,
                                useTileColors = true,
                                onLoadMore    = { viewModel.loadMoreSeeAll() },
                                onAppClick    = { r -> viewModel.addToHistory(r); selectedRepo = r },
                                onBack        = { showSeeAll = false }
                            )
// ── Tabs ──────────────────────────────────────
                            else -> Box(modifier = Modifier.fillMaxSize()) {
                             AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    val toSearch   = targetState  == VAppTab.SEARCH
                                    val fromSearch = initialState == VAppTab.SEARCH
                                    when {
                                        // HOME → SEARCH: home drifts up, search rises from below
                                        toSearch   -> (slideInVertically(tween(420, easing = EaseOutQuart)) { it / 4 } + fadeIn(tween(360, easing = FastOutSlowInEasing))) togetherWith
                                                      (slideOutVertically(tween(380, easing = FastOutSlowInEasing)) { -it / 4 } + fadeOut(tween(300, easing = FastOutSlowInEasing)))
                                        // SEARCH → HOME: gentle reverse
                                        fromSearch -> (slideInVertically(tween(420, easing = EaseOutQuart)) { -it / 4 } + fadeIn(tween(360, easing = FastOutSlowInEasing))) togetherWith
                                                      (slideOutVertically(tween(380, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(300, easing = FastOutSlowInEasing)))
                                        else -> {
                                            val goingRight = targetState.ordinal > initialState.ordinal
                                            if (goingRight) {
                                                (slideInHorizontally(tween(280)) { it } + fadeIn(tween(200))) togetherWith
                                                        (slideOutHorizontally(tween(280)) { -it } + fadeOut(tween(160)))
                                            } else {
                                                (slideInHorizontally(tween(280)) { -it } + fadeIn(tween(200))) togetherWith
                                                        (slideOutHorizontally(tween(280)) { it } + fadeOut(tween(160)))
                                            }
                                        }
                                    }
                                },
                                label = "tab_switch"
                            ) { tab ->
                                when (tab) {
                                    VAppTab.HOME -> HomeTab(
                                        state          = state,
                                        viewModel      = viewModel,
                                        installed      = installedSet,
                                        listState      = homeListState,
                                        onAppClick     = { r -> viewModel.addToHistory(r); selectedRepo = r },
                                        onSeeAll       = { showSeeAll = true },
                                        onSearchClick  = { selectedTab = VAppTab.SEARCH },
                                        onScrollChange = { scrolling -> dockVisible = !scrolling },
                                        onProfileClick = { selectedTab = VAppTab.PROFILE}
                                    )
                                    VAppTab.SEARCH -> SearchScreen(
                                        query                 = state.searchQuery,
                                        results               = state.searchResults,
                                        platform              = state.platform,
                                        selectedSubCategories = state.selectedSubCategories,
                                        installed             = installedSet,
                                        suggestions           = state.trending.take(10),
                                        isSearching           = state.isSearching,
                                        onQueryChange         = { viewModel.onSearch(it) },
                                        onPlatformChange      = { viewModel.setPlatform(it) },
                                        onSubCategoryToggle   = { viewModel.toggleSubCategory(it) },
                                        onAppClick            = { r -> viewModel.addToHistory(r); selectedRepo = r },
                                        isFilterMenuOpen      = state.isFilterMenuOpen,
                                        activeSubMenuPlatform = state.activeSubMenuPlatform,
                                        onToggleFilterMenu    = { viewModel.toggleFilterMenu(it) },
                                        onSetSubMenuPlatform  = { viewModel.setSubMenuPlatform(it) }
                                    )
                                    VAppTab.INSTALLED -> InstalledScreen(
                                        installHistory    = state.installHistory,
                                        installStates     = state.installStates,
                                        updates           = state.updates,
                                        onAppClick        = { r -> selectedRepo = r },
                                        onCheckUpdates    = { viewModel.checkForUpdatesNow() },
                                        onUpdateAll       = { viewModel.updateAll() },
                                        onClearRemoved    = { viewModel.clearRemovedApps() },
                                        isCheckingUpdates = state.isCheckingUpdates
                                    )
                                    VAppTab.PROFILE -> ProfileScreen(
                                        profile         = state.profile,
                                        history         = state.history,
                                        favourites      = state.favourites,
                                        installHistory  = state.installHistory,
                                        updates         = state.updates,
                                        onSave          = { viewModel.updateProfile(it) },
                                        onAppClick      = { r -> selectedRepo = r },
                                        onCheckUpdates  = { viewModel.checkForUpdatesNow() },
                                        onRollback      = { entry -> viewModel.rollbackTo(entry) }
                                    )
                                    VAppTab.SETTINGS -> SettingsScreen(
                                        settings          = state.settings,
                                        currentAccent     = state.accentColor,
                                        useMonet          = state.useMonet,
                                        customTheme       = state.customTheme,
                                        onSave            = { viewModel.updateSettings(it) },
                                        onAccentSelect    = { viewModel.setAccentColor(it) },
                                        onMonetToggle     = { viewModel.setUseMonet(it) },
                                        onCustomThemeSave = { viewModel.setCustomTheme(it) }
                                    )
                                }
                            }
                            } // end Box(paddingValues)
                        }
                    }

                    // Reset dock visibility when sub-screens open
                    LaunchedEffect(selectedRepo, showSeeAll, showCompare) {
                        if (selectedRepo != null || showSeeAll || showCompare) dockVisible = true
                    }

                    // ── Floating dock overlay ──────────────────────────
                    AnimatedVisibility(
                        visible  = dockVisible && currentScreen == "TABS",
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter    = slideInVertically(tween(220)) { it } + fadeIn(tween(180)),
                        exit     = slideOutVertically(tween(180)) { it } + fadeOut(tween(140))
                    ) {
                        FloatingNavBar(
                            selectedTab = selectedTab,
                            updateCount = state.updates.size,
                            onTabSelect = { tab ->
                                selectedTab  = tab
                                selectedRepo = null
                                showSeeAll   = false
                                dockVisible  = true
                            }
                        )
                    }

                    // ── Self-update banner ─────────────────────────────
                    val selfUpdate = state.selfUpdateInfo
                    if (selfUpdate != null && !state.selfUpdateDismissed) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            ElevatedCard(
                                shape     = MaterialTheme.shapes.large,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                                colors    = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier  = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Update available: ${selfUpdate.latestVersion}",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        if (selfUpdate.changelog.isNotBlank()) {
                                            Text(
                                                selfUpdate.changelog.lines().firstOrNull()?.take(60) ?: "",
                                                color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                style    = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    FilledTonalButton(onClick = {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(selfUpdate.apkUrl)
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }) {
                                        Text("Update")
                                    }
                                    IconButton(onClick = { viewModel.dismissSelfUpdate() }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// HOME TAB
// ─────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    state          : UiState,
    viewModel      : AppViewModel,
    onProfileClick: () -> Unit,
    installed      : Set<Long>,
    listState      : LazyListState,
    onAppClick     : (GitHubRepo) -> Unit,
    onSeeAll       : () -> Unit,
    onSearchClick  : () -> Unit        = {},
    onScrollChange : (Boolean) -> Unit = {}
) {
    val theme = LocalTheme.current
    val strings = LocalStrings.current
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) isRefreshing = false
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            onScrollChange(true)
        } else {
            delay(300)
            onScrollChange(false)
        }
    }

    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 6
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd && !state.isLoadingMore && !state.isLoading && state.platform == AppPlatform.ALL) {
            viewModel.loadMoreTrending()
        }
    }

    fun openSeeAll(title: String, query: String) {
        viewModel.openSeeAll(title, query)
        onSeeAll()
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.bgPrimary)) {
        ScreenBackground(ScreenBg.HOME)
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) {
                isRefreshing = true; viewModel.loadAll()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            item(key = "discover_header") {
                val notifications = remember(state.updates, state.installHistory) {
                    buildList {
                        state.updates.take(5).forEach { u ->
                            add(AppNotification(
                                title = "Update available: ${u.repoName}",
                                body  = "${u.currentTag} → ${u.latestTag}",
                                type  = NotifType.UPDATE
                            ))
                        }
                        state.installHistory
                            .sortedByDescending { it.installedAt }
                            .take(4)
                            .forEach { entry ->
                                add(AppNotification(
                                    title = "Installed ${entry.repoName}",
                                    body  = "Version ${entry.tagName}",
                                    type  = NotifType.INSTALL
                                ))
                            }
                    }
                }
                DiscoverHeader(
                    profile         = state.profile,
                    notifications   = notifications,
                    notifsDismissed = state.notifsDismissed,
                    onClearAll      = { viewModel.clearNotifications() },
                    onProfileClick  = onProfileClick
                )
            }
            item(key = "search_bar") {
                HomeSearchBar(
                    onSearchClick = onSearchClick,
                    modifier      = Modifier.offset(y = (-8).dp)
                )
            }
            item(key = "source_chips") {
                HomeSourceChipsRow(
                    selectedSource = state.selectedSource,
                    onSourceSelect = { viewModel.setSourceFilter(it) },
                    modifier       = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

                if (state.error != null) {
                    item(key = "error") { ErrorPlaceholder(state.error) { viewModel.loadAll() } }
                } else if (state.platform != AppPlatform.ALL) {
                    if (state.platformApps.isEmpty()) {
                        item(key = "plat_load") { LoadingPlaceholder() }
                    } else {
                        item(key = "plat_grid") {
                            PlatformGrid(
                                platform = state.platform,
                                apps = state.platformApps,
                                installed = installed,
                                onAppClick = onAppClick
                            )
                        }
                    }
                } else if (state.isLoading) {
                    item(key = "loading") { LoadingPlaceholder() }
                } else {
                    // Hero banner, collections, sources — hidden when a source filter is active
                    item(key = "featured") {
                        AnimatedVisibility(
                            visible = state.selectedSource == null,
                            enter   = expandVertically(tween(420)) + fadeIn(tween(300, delayMillis = 80)),
                            exit    = slideOutVertically(tween(320)) { -it / 3 } + shrinkVertically(tween(360)) + fadeOut(tween(260))
                        ) {
                            val featuredPool = remember(
                                state.trending, state.fdroidApps, state.gitlabApps,
                                state.codebergApps, state.flathubApps, state.wingetApps
                            ) {
                                (state.trending + state.fdroidApps + state.gitlabApps +
                                 state.codebergApps + state.flathubApps + state.wingetApps)
                                    .filter { it.owner.avatar_url.isNotEmpty() }
                                    .shuffled()
                            }
                            FeaturedCard(apps = featuredPool, onAppClick = onAppClick)
                        }
                    }
                    item(key = "collections") {
                        AnimatedVisibility(
                            visible = state.selectedSource == null,
                            enter   = expandVertically(tween(400)) + fadeIn(tween(300, delayMillis = 40)),
                            exit    = slideOutVertically(tween(280)) { -it / 3 } + shrinkVertically(tween(330)) + fadeOut(tween(230))
                        ) {
                            CollectionsRow { collection ->
                                viewModel.openCollection(collection)
                                onSeeAll()
                            }
                        }
                    }
                    item(key = "sources") {
                        AnimatedVisibility(
                            visible = state.selectedSource == null,
                            enter   = expandVertically(tween(380)) + fadeIn(tween(280)),
                            exit    = slideOutVertically(tween(240)) { -it / 3 } + shrinkVertically(tween(300)) + fadeOut(tween(200))
                        ) {
                            SourcesRow(
                                gitlabCount   = state.gitlabApps.size,
                                codebergCount = state.codebergApps.size,
                                fdroidCount   = state.fdroidApps.size,
                                flathubCount  = state.flathubApps.size,
                                wingetCount   = state.wingetApps.size,
                                izzyCount     = state.izzyApps.size,
                                onSourceClick = { source ->
                                    viewModel.openSourceBrowse(source)
                                    onSeeAll()
                                }
                            )
                        }
                    }
                    // App cards filtered by the selected source chip
                    when (state.selectedSource) {
                        AppSource.FDROID -> item(key = "fdroid_apps") {
                            AppRow("F-Droid Apps", state.fdroidApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        AppSource.GITLAB -> item(key = "gitlab_apps") {
                            AppRow("GitLab Apps", state.gitlabApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        AppSource.CODEBERG -> item(key = "codeberg_apps") {
                            AppRow("Codeberg Apps", state.codebergApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        AppSource.FLATHUB -> item(key = "flathub_apps") {
                            AppRow("Flathub Apps", state.flathubApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        AppSource.WINGET -> item(key = "winget_apps") {
                            AppRow("Winget Apps", state.wingetApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        AppSource.IZZY -> item(key = "izzy_apps") {
                            AppRow("IzzyOnDroid Apps", state.izzyApps, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                        }
                        else -> {
                            // null (All Sources) or GITHUB — show all GitHub-backed rows
                            if (state.recommendations.isNotEmpty()) {
                                item(key = "recs") {
                                    AppRow(strings.sectionRecommended, state.recommendations, installed) { onAppClick(it) }
                                }
                            }
                            item(key = "r1") {
                                AppRow(strings.sectionTrending, state.trending, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r2") {
                                AppRow(strings.sectionMedia, state.media, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r3") {
                                AppRow(strings.sectionTools, state.tools, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r4") {
                                AppRow(strings.sectionGames, state.games, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r5") {
                                AppRow(strings.sectionBrowsers, state.browsers, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r6") {
                                AppRow(strings.sectionProductivity, state.productivity, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r7") {
                                AppRow(strings.sectionSecurity, state.security, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r8") {
                                AppRow(strings.sectionDevTools, state.devtools, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r9") {
                                AppRow(strings.sectionPhotoVideo, state.photoVideo, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r10") {
                                AppRow(strings.sectionMusic, state.music, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r11") {
                                AppRow(strings.sectionFinance, state.finance, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r12") {
                                AppRow(strings.sectionEducation, state.education, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r13") {
                                AppRow(strings.sectionFitness, state.fitness, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r14") {
                                AppRow(strings.sectionArtDesign, state.artDesign, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r15") {
                                AppRow(strings.sectionNews, state.news, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r16") {
                                AppRow(strings.sectionSocial, state.social, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r17") {
                                AppRow(strings.sectionCloudStorage, state.cloudStorage, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            item(key = "r18") {
                                AppRow(strings.sectionCooking, state.cooking, installed, refreshToken = state.refreshToken) { onAppClick(it) }
                            }
                            if (state.isLoadingMore) {
                                item(key = "load_more") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = theme.accent,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


    }
    } // Box
}
@Composable
fun LoadingPlaceholder() {
    val t = LocalTheme.current
    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = t.accent)
            Spacer(Modifier.height(14.dp))
            Text("Loading apps from all sources…", color = t.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun ErrorPlaceholder(message: String, onRetry: () -> Unit) {
    val t = LocalTheme.current
    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⚠️", fontSize = 44.sp)
            Text(message, color = t.textSecondary, fontSize = 14.sp)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = t.accent)) {
                Text("Try Again")
            }
        }
    }
}