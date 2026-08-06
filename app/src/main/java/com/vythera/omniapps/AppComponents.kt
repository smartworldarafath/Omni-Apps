package com.vythera.omniapps

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR  — M3 CenterAlignedTopAppBar
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter            = painterResource(R.drawable.skpic),
                    contentDescription = "Logo",
                    modifier           = Modifier
                        .size(32.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Text(
                    "Omni Apps",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors   = TopAppBarDefaults.topAppBarColors(
            containerColor    = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.statusBarsPadding()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// NOTIFICATION TYPES
// ─────────────────────────────────────────────────────────────────────────────
enum class NotifType { UPDATE, INSTALL, UNINSTALL, INFO }
data class AppNotification(val title: String, val body: String, val type: NotifType)

// ─────────────────────────────────────────────────────────────────────────────
// ANALOG CLOCK  — isolated composable so only it recomposes every second
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnalogClock(accent: Color, modifier: Modifier = Modifier) {
    var clockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000L); clockMillis = System.currentTimeMillis() }
    }
    val cal    = java.util.Calendar.getInstance().apply { timeInMillis = clockMillis }
    val secF   = cal.get(java.util.Calendar.SECOND).toFloat()
    val minF   = cal.get(java.util.Calendar.MINUTE) + secF / 60f
    val hrF    = cal.get(java.util.Calendar.HOUR)   + minF / 60f
    val minDeg = minF * 6f
    val hrDeg  = hrF  * 30f
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f; val cy = size.height / 2f
        val center = Offset(cx, cy)
        drawCircle(color = accent.copy(alpha = 0.50f), radius = r * 0.92f, center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
        val hAng = java.lang.Math.toRadians(hrDeg.toDouble() - 90.0)
        val hLen = r * 0.52f
        drawLine(color = accent, strokeWidth = 11f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            start = Offset(cx - hLen * 0.18f * java.lang.Math.cos(hAng).toFloat(), cy - hLen * 0.18f * java.lang.Math.sin(hAng).toFloat()),
            end   = Offset(cx + hLen * java.lang.Math.cos(hAng).toFloat(), cy + hLen * java.lang.Math.sin(hAng).toFloat()))
        val mAng = java.lang.Math.toRadians(minDeg.toDouble() - 90.0)
        val mLen = r * 0.70f
        drawLine(color = Color.White, strokeWidth = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            start = Offset(cx - mLen * 0.14f * java.lang.Math.cos(mAng).toFloat(), cy - mLen * 0.14f * java.lang.Math.sin(mAng).toFloat()),
            end   = Offset(cx + mLen * java.lang.Math.cos(mAng).toFloat(), cy + mLen * java.lang.Math.sin(mAng).toFloat()))
        drawCircle(color = Color.White, radius = 4f, center = center)
        drawCircle(color = accent,      radius = 2.5f, center = center)
    }
}

// DISCOVER HEADER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DiscoverHeader(
    profile          : UserProfile,
    notifications    : List<AppNotification>,
    notifsDismissed  : Boolean = false,
    onClearAll       : () -> Unit = {},
    onProfileClick   : () -> Unit
) {
    val t = LocalTheme.current
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val timeGreeting = when (hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val headerBase = if (t.isDark) Color(
        red   = t.accent.red   * 0.18f,
        green = t.accent.green * 0.18f,
        blue  = t.accent.blue  * 0.18f,
        alpha = 1f
    ) else t.accentContainer
    val avatarBg      = t.accent.copy(alpha = 0.25f)
    val onHeader      = if (t.isDark) Color.White             else t.onAccentContainer
    val onHeaderMid   = if (t.isDark) Color.White.copy(0.72f) else t.onAccentContainer.copy(0.72f)
    val onHeaderSub   = if (t.isDark) Color.White.copy(0.52f) else t.onAccentContainer.copy(0.52f)
    var showNotif by remember { mutableStateOf(false) }
    val localNotifications = if (notifsDismissed) emptyList() else notifications
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(headerBase, headerBase.copy(alpha = 0.85f), t.bgPrimary),
                    startY = 0f,
                    endY   = 700f
                )
            )
            .statusBarsPadding()
    ) {
        AnalogClock(
            accent   = t.accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 45.dp)
                .size(110.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarBg)
                            .clickable {onProfileClick()},
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.photoUri.isNotEmpty()) {
                            AsyncImage(
                                model              = profile.photoUri,
                                contentDescription = null,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Rounded.Person, null, tint = t.onAccentContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        text = "$timeGreeting, ${profile.name.ifEmpty { "User" }} 👋",
                        fontSize   = 12.sp,
                        color      = onHeaderMid,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box {
                    BadgedBox(
                        badge = {
                            if (localNotifications.isNotEmpty()) {
                                Badge {
                                    Text(
                                        "${localNotifications.size.coerceAtMost(9)}${if (localNotifications.size > 9) "+" else ""}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        FilledIconButton(
                            onClick  = { showNotif = !showNotif },
                            modifier = Modifier.size(44.dp),
                            colors   = IconButtonDefaults.filledIconButtonColors(
                                containerColor = onHeader.copy(0.12f),
                                contentColor   = onHeaderMid
                            )
                        ) {
                            Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(20.dp))
                        }
                    }
                    DropdownMenu(
                        expanded         = showNotif,
                        onDismissRequest = { showNotif = false },
                        modifier         = Modifier.width(300.dp),
                        shape            = MaterialTheme.shapes.extraLarge,
                        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation  = 12.dp
                    ) {
                        // ── Gradient header ────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(MaterialTheme.colorScheme.primaryContainer.copy(0.55f), MaterialTheme.colorScheme.surfaceContainerHigh),
                                        startY = 0f, endY = 180f
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                    Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    if (localNotifications.isNotEmpty()) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "${localNotifications.size}",
                                                style    = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AnimatedVisibility(visible = localNotifications.isNotEmpty()) {
                                        TextButton(
                                            onClick        = { onClearAll(); showNotif = false },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Rounded.ClearAll, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Clear All", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    IconButton(onClick = { showNotif = false }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))

                        // ── Empty state ────────────────────────────────
                        if (localNotifications.isEmpty()) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier         = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(GreenOk.copy(0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Check, null, tint = GreenOk, modifier = Modifier.size(26.dp))
                                    }
                                    Text("You're all caught up!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("No new notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            // ── Notification items ─────────────────────
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                localNotifications.forEach { notif ->
                                    val (iconVec, iconColor) = when (notif.type) {
                                        NotifType.UPDATE    -> Icons.Rounded.SystemUpdate to MaterialTheme.colorScheme.primary
                                        NotifType.INSTALL   -> Icons.Rounded.Download     to GreenOk
                                        NotifType.UNINSTALL -> Icons.Rounded.Delete       to MaterialTheme.colorScheme.error
                                        NotifType.INFO      -> Icons.Rounded.Info         to MaterialTheme.colorScheme.secondary
                                    }
                                    Card(
                                        onClick  = { showNotif = false },
                                        shape    = MaterialTheme.shapes.large,
                                        colors   = CardDefaults.cardColors(containerColor = iconColor.copy(0.07f))
                                    ) {
                                        Row(
                                            modifier              = Modifier.padding(10.dp),
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            FilledTonalIconButton(
                                                onClick  = {},
                                                modifier = Modifier.size(36.dp),
                                                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                                                    containerColor = iconColor.copy(0.18f),
                                                    contentColor   = iconColor
                                                )
                                            ) {
                                                Icon(iconVec, null, modifier = Modifier.size(18.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(notif.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(notif.body,  style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Box(
                                                modifier = Modifier.size(6.dp).clip(CircleShape).background(iconColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text(
                "Discover",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = onHeader
            )
            Text(
                "Open source apps",
                style = MaterialTheme.typography.headlineSmall,
                color = onHeaderSub
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN BACKGROUNDS  —  M3 Expressive mesh-blob system
// Each screen gets a distinct radial-gradient layout that reacts to the current
// AppThemeColors (Monet / manual accent included automatically).
// ─────────────────────────────────────────────────────────────────────────────

enum class ScreenBg { HOME, SEARCH, INSTALLED, PROFILE, SETTINGS }

@Composable
fun ScreenBackground(screen: ScreenBg, modifier: Modifier = Modifier) {
    val t = LocalTheme.current
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter            = painterResource(R.drawable.saturn),
            contentDescription = null,
            alpha              = 0.07f,
            colorFilter        = ColorFilter.tint(t.accent, BlendMode.SrcAtop),
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxWidth(0.85f)
                .align(Alignment.BottomCenter)
                .offset(y = 40.dp)
        )
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                val w = size.width
                val h = size.height
                when (screen) {

                    ScreenBg.HOME -> {
                        // Primary blob — top-right (accent1 from wallpaper)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accent.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(w * 0.88f, h * 0.10f),
                                radius = w * 0.72f
                            )
                        )
                        // Secondary blob — bottom-left (accent2 from wallpaper)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset(w * 0.10f, h * 0.78f),
                                radius = w * 0.58f
                            )
                        )
                        // Tertiary blob — mid-center (accent3 from wallpaper)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(alpha = 0.09f), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.46f),
                                radius = w * 0.50f
                            )
                        )
                        // Container highlight — top-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentContainer.copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(w * 0.08f, h * 0.18f),
                                radius = w * 0.38f
                            )
                        )
                    }

                    ScreenBg.SEARCH -> {
                        // Primary spotlight — top-center (accent1)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accent.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(w * 0.50f, 0f),
                                radius = w * 0.75f
                            )
                        )
                        // Tertiary blob — bottom-right (accent3)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(alpha = 0.10f), Color.Transparent),
                                center = Offset(w * 0.88f, h * 0.85f),
                                radius = w * 0.46f
                            )
                        )
                        // Secondary blob — mid-left (accent2)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(alpha = 0.07f), Color.Transparent),
                                center = Offset(w * 0.08f, h * 0.52f),
                                radius = w * 0.38f
                            )
                        )
                    }

                    ScreenBg.INSTALLED -> {
                        // Diagonal sweep — upper-left corner (accent)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(t.accent.copy(0.22f), Color.Transparent),
                                start  = Offset(0f, 0f),
                                end    = Offset(w * 0.65f, h * 0.35f)
                            )
                        )
                        // Bold blob — top-right (accentAlt)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(0.24f), Color.Transparent),
                                center = Offset(w * 0.92f, h * 0.04f),
                                radius = w * 0.55f
                            )
                        )
                        // Mid-left blob (accentTertiary)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(0.20f), Color.Transparent),
                                center = Offset(w * 0.08f, h * 0.48f),
                                radius = w * 0.44f
                            )
                        )
                        // Mid-right blob (accent)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accent.copy(0.15f), Color.Transparent),
                                center = Offset(w * 0.88f, h * 0.52f),
                                radius = w * 0.36f
                            )
                        )
                        // Bottom-left sweep (accentContainer)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentContainer.copy(0.26f), Color.Transparent),
                                center = Offset(w * 0.10f, h * 0.88f),
                                radius = w * 0.52f
                            )
                        )
                        // Bottom-right blob (accentAlt)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(0.18f), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.90f),
                                radius = w * 0.42f
                            )
                        )
                        // Center accent dot
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(0.12f), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.55f),
                                radius = w * 0.30f
                            )
                        )
                    }

                    ScreenBg.PROFILE -> {
                        // Primary blob — top-center avatar zone (accent1)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accent.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.14f),
                                radius = w * 0.65f
                            )
                        )
                        // Tertiary blob — bottom-right (accent3)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(alpha = 0.11f), Color.Transparent),
                                center = Offset(w * 0.88f, h * 0.80f),
                                radius = w * 0.50f
                            )
                        )
                        // Secondary blob — bottom-left (accent2)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(alpha = 0.08f), Color.Transparent),
                                center = Offset(w * 0.10f, h * 0.76f),
                                radius = w * 0.42f
                            )
                        )
                    }

                    ScreenBg.SETTINGS -> {
                        // Primary blob — top-right (accent1)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accent.copy(alpha = 0.11f), Color.Transparent),
                                center = Offset(w * 0.88f, h * 0.07f),
                                radius = w * 0.50f
                            )
                        )
                        // Tertiary blob — mid-screen (accent3)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentTertiary.copy(alpha = 0.09f), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.45f),
                                radius = w * 0.44f
                            )
                        )
                        // Secondary blob — mid-left (accent2)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentAlt.copy(alpha = 0.07f), Color.Transparent),
                                center = Offset(w * 0.08f, h * 0.48f),
                                radius = w * 0.38f
                            )
                        )
                        // Container blob — bottom-center
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(t.accentContainer.copy(alpha = 0.06f), Color.Transparent),
                                center = Offset(w * 0.50f, h * 0.88f),
                                radius = w * 0.42f
                            )
                        )
                    }
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME SEARCH BAR  — M3 SearchBar (collapsed / inactive state)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSearchBar(
    onSearchClick : () -> Unit = {},
    modifier      : Modifier   = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query            = "",
                onQueryChange    = {},
                onSearch         = {},
                expanded         = false,
                onExpandedChange = { if (it) onSearchClick() },
                placeholder      = {
                    Text(
                        "Search GitHub, GitLab, F-Droid...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    FilledTonalIconButton(
                        onClick  = onSearchClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_filter_logo),
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        expanded         = false,
        onExpandedChange = { if (it) onSearchClick() },
        modifier         = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {}
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME SOURCE CHIPS  — M3 FilterChip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeSourceChipsRow(
    selectedSource : AppSource?,
    onSourceSelect : (AppSource?) -> Unit,
    modifier       : Modifier = Modifier
) {
    val chips = remember {
        listOf(
            Triple(null,               "All Sources",  R.drawable.all),
            Triple(AppSource.GITHUB,   "GitHub",       R.drawable.github),
            Triple(AppSource.FDROID,   "F-Droid",      R.drawable.fdroid),
            Triple(AppSource.GITLAB,   "GitLab",       R.drawable.gitlab),
            Triple(AppSource.CODEBERG, "Codeberg",     R.drawable.codeberg),
            Triple(AppSource.IZZY,     "IzzyOnDroid",  R.drawable.ic_izzy_logo),
            Triple(AppSource.FLATHUB,  "Flathub",      R.drawable.flathub),
            Triple(AppSource.WINGET,   "Winget",       R.drawable.winget)
        )
    }
    LazyRow(
        modifier              = modifier,
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = chips) { (src, label, iconRes) ->
            val selected = selectedSource == src
            FilterChip(
                selected    = selected,
                onClick     = { onSourceSelect(src) },
                label       = {
                    Text(
                        label,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Image(
                        painter            = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp)
                    )
                },
                shape    = CircleShape,
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURED CARD
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FeaturedCard(apps: List<GitHubRepo>, onAppClick: (GitHubRepo) -> Unit) {
    if (apps.isEmpty()) return
    val t          = LocalTheme.current
    val context    = LocalContext.current
    val featApps   = remember(apps) { apps.shuffled().take(5) }
    val pagerState = rememberPagerState(pageCount = { featApps.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(5500)
            val next = (pagerState.currentPage + 1) % featApps.size
            pagerState.animateScrollToPage(next, animationSpec = tween(900, easing = EaseInOutCubic))
        }
    }

    Column(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val repo = featApps[page]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onAppClick(repo) }
            ) {
                // Monet gradient: primary → dark/white depending on theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                )
                // Dark scrim for depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    // ── Left: text content ────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Transparent FEATURED badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    "FEATURED",
                                    fontSize      = 10.sp,
                                    fontWeight    = FontWeight.ExtraBold,
                                    color         = Color.White,
                                    letterSpacing = 1.sp,
                                    modifier      = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // App name — big bold
                            Text(
                                text       = repo.name,
                                fontSize   = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            // About / description
                            if (!repo.description.isNullOrEmpty()) {
                                Text(
                                    text       = repo.description,
                                    fontSize   = 12.sp,
                                    color      = Color.White.copy(alpha = 0.78f),
                                    maxLines   = 2,
                                    lineHeight = 16.sp,
                                    overflow   = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // M3 Expressive pill button — opens html_url in browser
                        val buttonLabel = when (repo.source) {
                            AppSource.GITHUB   -> "View on GitHub"
                            AppSource.GITLAB   -> "View on GitLab"
                            AppSource.FDROID   -> "View on F-Droid"
                            AppSource.CODEBERG -> "View on Codeberg"
                            AppSource.FLATHUB  -> "View on Flathub"
                            AppSource.WINGET   -> "View on Winget"
                            AppSource.IZZY     -> "View on IzzyOnDroid"
                            null               -> "View App"
                        }
                        Button(
                            onClick = {
                                if (repo.html_url.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.html_url))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            },
                            shape          = MaterialTheme.shapes.extraLarge,
                            colors         = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor   = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier       = Modifier.height(34.dp)
                        ) {
                            Text(buttonLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier           = Modifier.size(13.dp)
                            )
                        }
                    }

                    // ── Right: tilted decorative boxes ────────────────────────
                    Box(
                        modifier         = Modifier
                            .width(130.dp)
                            .fillMaxHeight()
                            .padding(end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Large dark box — behind, shifted down
                        Surface(
                            modifier = Modifier
                                .size(82.dp, 114.dp)
                                .offset(y = 16.dp)
                                .graphicsLayer { rotationZ = -12f },
                            shape = RoundedCornerShape(20.dp),
                            color = if (t.isDark) Color.Black.copy(alpha = 0.55f)
                                    else Color(0xFF1C1C1C).copy(alpha = 0.20f)
                        ) {}
                        // Small logo box — in front, shifted up
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .offset(y = (-16).dp)
                                .graphicsLayer { rotationZ = -12f },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            AsyncImage(
                                model = remember(repo.owner.avatar_url) {
                                    ImageRequest.Builder(context)
                                        .data(repo.owner.avatar_url.ifEmpty { null })
                                        .crossfade(300)
                                        .build()
                                },
                                contentDescription = null,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop,
                                error              = painterResource(R.drawable.ic_android_logo),
                                placeholder        = painterResource(R.drawable.ic_android_logo)
                            )
                        }
                    }
                }
            }
        }

        // Pager indicator dots
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(featApps.size) { i ->
                val sel = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (sel) 18.dp else 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

// DockItem removed — NavigationBarItem handles tab item rendering natively in M3

// ─────────────────────────────────────────────────────────────────────────────
// HERO BANNER  — unchanged logic, improved visual shape
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeroBanner(apps: List<GitHubRepo>, onAppClick: (GitHubRepo) -> Unit) {
    if (apps.isEmpty()) return
    val t       = LocalTheme.current
    val context = LocalContext.current

    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val bannerApps = remember(apps) { apps.shuffled().take(7) }
    val pagerState = rememberPagerState(pageCount = { bannerApps.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            val next = (pagerState.currentPage + 1) % bannerApps.size
            pagerState.animateScrollToPage(next, animationSpec = tween(1400, easing = EaseInOutCubic))
        }
    }

    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
        ) { page ->
            val repo = bannerApps[page]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.60f)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 2.dp,
                            bottomEnd = 32.dp
                        )
                    )   // M3 extraLarge shape
                    .clickable { onAppClick(repo) }
            ) {
                // Base surface colour (no vivid gradients — uses theme surface)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                // Owner avatar as subtle hero image
                AsyncImage(
                    model = remember(repo.owner.avatar_url) {
                        ImageRequest.Builder(context)
                            .data(repo.owner.avatar_url.ifEmpty { null })
                            .crossfade(400)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Scrim for text legibility
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background
                                ),
                                startY = 300f
                            )
                        )

                )
                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label   = {
                            Text(
                                "FEATURED",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        repo.name,
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    if (!repo.description.isNullOrEmpty()) {
                        Text(
                            repo.description,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = Color.White.copy(0.80f),
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(bannerApps.size) { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (selected) 24.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// M3 TAG  — M3 SuggestionChip for language / platform labels
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun M3Tag(
    label    : String,
    emoji    : String? = null,
    modifier : Modifier = Modifier
) {
    SuggestionChip(
        onClick  = {},
        label    = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (emoji != null) Text(emoji, style = MaterialTheme.typography.labelSmall)
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// APP CARD  — M3 ElevatedCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppCard(repo: GitHubRepo, isInstalled: Boolean = false, modifier: Modifier = Modifier.width(280.dp), onClick: () -> Unit) {
    val context   = LocalContext.current
    val platforms = remember(repo.id) { detectPlatformLabels(repo) }
    val imageModel = remember(repo.owner.avatar_url) {
        ImageRequest.Builder(context)
            .data(repo.owner.avatar_url.ifEmpty { null })
            .crossfade(300)
            .build()
    }

    ElevatedCard(
        onClick   = onClick,
        modifier  = modifier.height(120.dp),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model              = imageModel,
                contentDescription = null,
                modifier           = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale       = ContentScale.Crop,
                error              = painterResource(R.drawable.ic_android_logo),
                placeholder        = painterResource(R.drawable.ic_android_logo)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "@${repo.owner.login}",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    repo.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (!repo.description.isNullOrEmpty()) {
                    Text(
                        repo.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (repo.stargazers_count > 0) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Star, null, tint = StarGold, modifier = Modifier.size(12.dp))
                            Text(
                                formatStars(repo.stargazers_count),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (platform in platforms) {
                            if (platform.iconRes != null) {
                                Icon(
                                    painter            = painterResource(platform.iconRes),
                                    contentDescription = null,
                                    modifier           = Modifier.size(14.dp),
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(platform.emoji, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// APP LIST TILE  — M3 Card + ListItem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppListTile(
    repo          : GitHubRepo,
    isInstalled   : Boolean = false,
    tileIndex     : Int     = 0,
    useTileColors : Boolean = false,
    onClick       : () -> Unit
) {
    val context = LocalContext.current
    val tileColor = if (useTileColors) tileColors[tileIndex % tileColors.size] else null

    val glowColor = tileColor?.vivid()
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (glowColor != null) Modifier.glowBorder(glowColor, cornerRadius = 12.dp) else Modifier),
        shape    = MaterialTheme.shapes.large,
        colors   = CardDefaults.cardColors(
            containerColor = tileColor?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.surfaceContainer
        ),
        border   = if (glowColor != null) BorderStroke(1.dp, glowColor.copy(alpha = 0.55f)) else null
    ) {
        ListItem(
            headlineContent = {
                Text(
                    repo.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            },
            overlineContent = {
                Text(
                    "@${repo.owner.login}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            supportingContent = if (!repo.description.isNullOrEmpty() || repo.stargazers_count > 0 || isInstalled) ({
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!repo.description.isNullOrEmpty()) {
                        Text(
                            repo.description,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (repo.stargazers_count > 0) {
                            Icon(Icons.Rounded.Star, null, tint = StarGold, modifier = Modifier.size(11.dp))
                            Text(formatStars(repo.stargazers_count), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!repo.language.isNullOrEmpty()) {
                            Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(repo.language, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isInstalled) {
                            SuggestionChip(
                                onClick = {},
                                label   = { Text("Installed", style = MaterialTheme.typography.labelSmall) },
                                colors  = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = GreenOk.copy(alpha = 0.12f),
                                    labelColor     = GreenOk
                                )
                            )
                        }
                        if (repo.source != null && repo.source != AppSource.GITHUB) {
                            SourceBadge(source = repo.source)
                        }
                    }
                }
            }) else null,
            leadingContent = {
                AsyncImage(
                    model              = remember(repo.owner.avatar_url) {
                        ImageRequest.Builder(context)
                            .data(repo.owner.avatar_url.ifEmpty { null })
                            .crossfade(200)
                            .build()
                    },
                    contentDescription = null,
                    modifier           = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(tileColor?.copy(0.22f) ?: MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale       = ContentScale.Crop,
                    error              = painterResource(R.drawable.ic_android_logo),
                    placeholder        = painterResource(R.drawable.ic_android_logo)
                )
            },
            trailingContent = {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = tileColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP ROW — horizontal scroll of AppCards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppRow(
    title        : String,
    apps         : List<GitHubRepo>,
    installed    : Set<Long> = emptySet(),
    refreshToken : Int       = 0,
    onAppClick   : (GitHubRepo) -> Unit
) {
    val t = LocalTheme.current
    if (apps.isEmpty()) return

    val displayList  = remember(apps, refreshToken) { apps.shuffled() }
    val useInfinite  = displayList.size >= 4
    // Keep virtual count small (≤2000) so LazyRow position maths stay fast
    val virtualCount = if (useInfinite) (displayList.size * 80).coerceAtMost(2_000) else displayList.size
    val startIndex   = remember(displayList.size, useInfinite) {
        if (useInfinite) {
            val mid = virtualCount / 2
            mid - (mid % displayList.size)
        } else 0
    }
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            title,
            style    = MaterialTheme.typography.titleLarge,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            state                 = rowState,
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                count = virtualCount,
                key   = { idx -> "${displayList[idx % displayList.size].id}_$idx" }
            ) { index ->
                val repo = displayList[index % displayList.size]
                AppCard(
                    repo        = repo,
                    isInstalled = installed.contains(repo.id),
                    onClick     = { onAppClick(repo) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PLATFORM GRID — for filtered views
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlatformGrid(
    platform  : AppPlatform,
    apps      : List<GitHubRepo>,
    installed : Set<Long> = emptySet(),
    onAppClick: (GitHubRepo) -> Unit
) {
    val t = LocalTheme.current
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(platform.emoji, fontSize = 20.sp)
            Text(
                "${platform.label} Apps",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.forEachIndexed { index, repo ->
                AppListTile(
                    repo        = repo,
                    isInstalled = installed.contains(repo.id),
                    tileIndex   = index,
                    onClick     = { onAppClick(repo) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query                : String,
    results              : List<GitHubRepo>,
    platform             : AppPlatform,
    selectedSubCategories: Set<String>,
    installed            : Set<Long>        = emptySet(),
    suggestions          : List<GitHubRepo> = emptyList(),
    isSearching          : Boolean          = false,
    onQueryChange        : (String) -> Unit,
    onPlatformChange     : (AppPlatform) -> Unit,
    onSubCategoryToggle  : (String) -> Unit,
    onAppClick           : (GitHubRepo) -> Unit,
    isFilterMenuOpen     : Boolean,
    activeSubMenuPlatform: AppPlatform?,
    onToggleFilterMenu   : (Boolean) -> Unit,
    onSetSubMenuPlatform : (AppPlatform?) -> Unit
) {
    val t = LocalTheme.current
    val s = LocalStrings.current
    var selectedSource  by remember { mutableStateOf<AppSource?>(null) }
    // localPlatform seeds from ViewModel state so it persists across navigation
    var localPlatform   by remember(platform) { mutableStateOf(platform) }
    var screenEntered   by remember { mutableStateOf(false) }
    val focusRequester  = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Animate in on first composition, then open keyboard
    LaunchedEffect(Unit) {
        screenEntered = true
        delay(320) // wait for slide-in animation
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val displayResults = run {
        var list = results
        // Filter by source chip
        list = when {
            selectedSource == null -> list
            selectedSource == AppSource.GITHUB -> list.filter { it.source == null || it.source == AppSource.GITHUB }
            else -> list.filter { it.source == selectedSource }
        }
        // Platform filter — only re-filter by platform labels when query has text.
        // When blank, onSearch() already filtered by AppSource→platform, so double-filtering
        // would incorrectly drop repos whose name/description don't contain the keyword.
        if (localPlatform != AppPlatform.ALL && query.isNotBlank()) {
            list = list.filter { repo -> detectPlatformLabels(repo).contains(localPlatform) }
        }
        list
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.SEARCH)
    Column(modifier = Modifier.fillMaxSize()) {

        // ── TOP: search bar — slides down from above on enter ────────────────
        AnimatedVisibility(
            visible = screenEntered,
            enter   = slideInVertically(tween(380, easing = EaseOutCubic)) { -it } + fadeIn(tween(300))
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            val isFiltered = localPlatform != AppPlatform.ALL

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        Icons.Rounded.Search, null,
                        modifier = Modifier.size(20.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f)
                    )
                    Spacer(Modifier.width(10.dp))
                    TextField(
                        value         = query,
                        onValueChange = onQueryChange,
                        modifier      = Modifier.weight(1f).focusRequester(focusRequester),
                        singleLine    = true,
                        placeholder   = { Text(s.searchHint, style = MaterialTheme.typography.bodyMedium) },
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor     = Color.Transparent,
                            unfocusedContainerColor   = Color.Transparent,
                            focusedIndicatorColor     = Color.Transparent,
                            unfocusedIndicatorColor   = Color.Transparent,
                            focusedTextColor          = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor        = MaterialTheme.colorScheme.onSurface,
                            cursorColor               = MaterialTheme.colorScheme.primary,
                            focusedPlaceholderColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f)
                        )
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                    // Filter button — opens bottom sheet
                    FilledIconButton(
                        onClick   = { onToggleFilterMenu(true) },
                        modifier  = Modifier.padding(end = 6.dp).size(38.dp),
                        colors    = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_filter_logo), null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        } // AnimatedVisibility — search bar

        // ── BOTTOM: Bento grid results — rises from below on enter ────────────
        val gridState = rememberLazyGridState()
        LaunchedEffect(query, localPlatform, selectedSource) {
            gridState.scrollToItem(0)
        }
        AnimatedVisibility(
            visible = screenEntered,
            enter   = slideInVertically(tween(440, easing = EaseOutCubic)) { it / 2 } + fadeIn(tween(360))
        ) {
        // New random layout every time SearchScreen enters composition
        val bentoPattern = remember {
            buildList<Boolean> {
                var slots = 0
                while (slots < 80) {
                    when ((0..4).random()) {
                        0, 1 -> { add(true);  slots += 1 }            // full-width (2/5 chance)
                        else -> { add(false); add(false); slots += 2 } // two halves (3/5 chance)
                    }
                }
            }
        }
        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            state                 = gridState,
            contentPadding        = PaddingValues(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.fillMaxSize()
        ) {
            if (query.isBlank() && displayResults.isEmpty()) {
                // No text + no platform-filtered results → show trending suggestions
                if (suggestions.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            s.trendingApps,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp, top = 8.dp)
                        )
                    }
                    items(
                        count = suggestions.size,
                        key   = { suggestions[it].id },
                        span  = { idx -> if (idx < bentoPattern.size && bentoPattern[idx]) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
                    ) { idx ->
                        val repo = suggestions[idx]
                        AppCard(
                            repo        = repo,
                            isInstalled = installed.contains(repo.id),
                            modifier    = Modifier.fillMaxWidth(),
                            onClick     = { onAppClick(repo) }
                        )
                    }
                }
            } else if (query.isBlank()) {
                // No text but platform filter has results — show them directly
                items(
                    count = displayResults.size,
                    key   = { displayResults[it].id },
                    span  = { idx -> if (idx < bentoPattern.size && bentoPattern[idx]) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
                ) { idx ->
                    val repo = displayResults[idx]
                    AppCard(
                        repo        = repo,
                        isInstalled = installed.contains(repo.id),
                        modifier    = Modifier.fillMaxWidth(),
                        onClick     = { onAppClick(repo) }
                    )
                }
            } else {
                if (isSearching && displayResults.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                Text(s.searching, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(
                        count = displayResults.size,
                        key   = { displayResults[it].id },
                        span  = { idx -> if (idx < bentoPattern.size && bentoPattern[idx]) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
                    ) { idx ->
                        val repo = displayResults[idx]
                        AppCard(
                            repo        = repo,
                            isInstalled = installed.contains(repo.id),
                            modifier    = Modifier.fillMaxWidth(),
                            onClick     = { onAppClick(repo) }
                        )
                    }
                    if (displayResults.isNotEmpty() && isSearching) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                    Text(s.fetchingMore, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (displayResults.isEmpty() && !isSearching) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                                    val filterText = if (localPlatform != AppPlatform.ALL) " — ${localPlatform.label}" else ""
                                    Text("${s.noResultsFound}$filterText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    Text(s.tryDifferent, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
        } // AnimatedVisibility — bento grid
    }

    // ── Platform filter bottom sheet ─────────────────────────────────────────
    if (isFilterMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { onToggleFilterMenu(false) },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Filter by Platform",
                    style         = MaterialTheme.typography.labelMedium,
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier      = Modifier.padding(bottom = 10.dp)
                )
                val filterPlatforms = listOf(
                    AppPlatform.ALL,
                    AppPlatform.ANDROID,
                    AppPlatform.WINDOWS,
                    AppPlatform.LINUX,
                    AppPlatform.IOS,
                    AppPlatform.TV
                )
                filterPlatforms.forEach { p ->
                    val isSelected = localPlatform == p
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable {
                                localPlatform = p
                                onPlatformChange(p)
                                onToggleFilterMenu(false)
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (p == AppPlatform.ALL) {
                            Icon(
                                Icons.Rounded.Apps, null,
                                modifier = Modifier.size(22.dp),
                                tint     = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (p.iconRes != null) {
                            Icon(
                                painterResource(p.iconRes), null,
                                modifier = Modifier.size(22.dp),
                                tint     = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(p.emoji, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            if (p == AppPlatform.ALL) "All Platforms" else p.label,
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
    } // Box
} // SearchScreen


// ─────────────────────────────────────────────────────────────────────────────
// INSTALLED SCREEN
// ─────────────────────────────────────────────────────────────────────────────
private enum class UpdateCheckPhase { IDLE, CHECKING, FOUND, NOT_FOUND }

@Composable
fun InstalledScreen(
    installHistory    : List<InstallHistoryEntry>,
    installStates     : Map<Long, InstallState>,
    updates           : List<UpdateInfo>        = emptyList(),
    onAppClick        : (GitHubRepo) -> Unit,
    onCheckUpdates    : () -> Unit              = {},
    onUpdateAll       : () -> Unit              = {},
    onClearRemoved    : () -> Unit              = {},
    isCheckingUpdates : Boolean                 = false
) {
    val t = LocalTheme.current
    val s = LocalStrings.current

    // One card per app — latest install entry wins
    val entries = remember(installHistory) {
        installHistory
            .groupBy { it.repoId }
            .mapValues { (_, v) -> v.maxByOrNull { it.installedAt }!! }
            .values
            .sortedBy { it.repoName }
    }

    val updateCount  = updates.count { u -> entries.any { it.repoId == u.repoId } }
    val removedCount = entries.count { entry -> installStates[entry.repoId]?.isInstalled == false }

    var checkPhase by remember(updateCount) {
        mutableStateOf(if (updateCount > 0) UpdateCheckPhase.FOUND else UpdateCheckPhase.IDLE)
    }

    LaunchedEffect(isCheckingUpdates) {
        if (!isCheckingUpdates && checkPhase == UpdateCheckPhase.CHECKING) {
            checkPhase = if (updateCount > 0) UpdateCheckPhase.FOUND else UpdateCheckPhase.NOT_FOUND
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.INSTALLED)
    Column(modifier = Modifier.fillMaxSize()) {

        // Header — transparent so ScreenBackground blobs show through
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(s.navInstalled, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        when {
                            entries.isEmpty()   -> "No apps installed through Omni yet"
                            updateCount > 0     -> "$updateCount update${if (updateCount != 1) "s" else ""} available"
                            else                -> "${entries.size} app${if (entries.size != 1) "s" else ""} installed"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (updateCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (removedCount > 0) {
                    TextButton(onClick = onClearRemoved) {
                        Text(
                            "Clear removed ($removedCount)",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.InstallMobile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    Text(s.nothingInstalled, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        s.nothingInstalledDesc,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Check for Updates / Update All button
                item {
                    val btnLabel = when (checkPhase) {
                        UpdateCheckPhase.IDLE      -> s.checkForUpdates
                        UpdateCheckPhase.CHECKING  -> "Checking for updates..."
                        UpdateCheckPhase.FOUND     -> "Update All (${updateCount})"
                        UpdateCheckPhase.NOT_FOUND -> "No updates found"
                    }
                    val isEnabled = checkPhase == UpdateCheckPhase.IDLE || checkPhase == UpdateCheckPhase.FOUND
                    Button(
                        onClick = {
                            when (checkPhase) {
                                UpdateCheckPhase.IDLE -> {
                                    checkPhase = UpdateCheckPhase.CHECKING
                                    onCheckUpdates()
                                }
                                UpdateCheckPhase.FOUND -> onUpdateAll()
                                else -> {}
                            }
                        },
                        enabled  = isEnabled,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = MaterialTheme.shapes.large,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor        = MaterialTheme.colorScheme.primary,
                            contentColor          = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledContentColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        if (checkPhase == UpdateCheckPhase.CHECKING) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                color       = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                when (checkPhase) {
                                    UpdateCheckPhase.FOUND     -> Icons.Rounded.Update
                                    UpdateCheckPhase.NOT_FOUND -> Icons.Rounded.CheckCircle
                                    else                       -> Icons.Rounded.Refresh
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(btnLabel, fontWeight = FontWeight.Bold)
                    }
                }

                if (updateCount > 0) {
                    item {
                        ElevatedCard(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.NewReleases, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    "$updateCount update${if (updateCount != 1) "s" else ""} available",
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                items(items = entries, key = { it.repoId }) { entry ->
                    val iState  = installStates[entry.repoId]
                    val repo    = iState?.repo ?: GitHubRepo(
                        id        = entry.repoId,
                        name      = entry.repoName,
                        full_name = "${entry.ownerLogin}/${entry.repoName}",
                        owner     = RepoOwner(login = entry.ownerLogin)
                    )
                    val update  = updates.firstOrNull { it.repoId == entry.repoId }
                    val isStillInstalled = iState?.isInstalled == true

                    InstalledAppCard(
                        repo        = repo,
                        entry       = entry,
                        isInstalled = isStillInstalled,
                        update      = update,
                        onClick     = { onAppClick(repo) },
                        onUpdate    = { onAppClick(repo) }
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun InstalledAppCard(
    repo        : GitHubRepo,
    entry       : InstallHistoryEntry,
    isInstalled : Boolean,
    update      : UpdateInfo?,
    onClick     : () -> Unit,
    onUpdate    : () -> Unit = {}
) {
    val s = LocalStrings.current

    ElevatedCard(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (update != null) 4.dp else 2.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (update != null)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model              = repo.owner.avatar_url.ifEmpty { null },
                    contentDescription = null,
                    modifier           = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    error              = painterResource(R.drawable.ic_android_logo),
                    placeholder        = painterResource(R.drawable.ic_android_logo)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        repo.name,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (update != null) MaterialTheme.colorScheme.onPrimaryContainer
                                     else MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        repo.owner.login,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = if (update != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    update != null -> Button(
                        onClick        = onUpdate,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape          = MaterialTheme.shapes.large,
                        colors         = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.Update, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Update", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    isInstalled -> FilledTonalButton(
                        onClick        = {},
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(s.installedStatus, style = MaterialTheme.typography.labelSmall)
                    }
                    else -> OutlinedButton(
                        onClick        = {},
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(s.removedStatus, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "v${entry.tagName.trimStart('v', 'V')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (update != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (update != null) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null,
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp))
                    Text(
                        update.latestTag,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.weight(1f))
                val fmt = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                Text(
                    fmt.format(java.util.Date(entry.installedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (update != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APP DETAIL SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    repo                  : GitHubRepo,
    installState          : InstallState,
    isFavourite           : Boolean,
    translatedDesc        : String?,
    isTranslating         : Boolean,
    translatedReleaseBody : String?  = null,
    isTranslatingRelease  : Boolean  = false,
    state                 : UiState,
    screenshots           : List<String>  = emptyList(),
    onInstall             : () -> Unit,
    onDownloadOnly        : () -> Unit,
    onUninstall           : () -> Unit,
    onCancelDownload      : () -> Unit,
    onTranslate           : () -> Unit,
    onTranslateRelease    : () -> Unit = {},
    onToggleFavourite     : () -> Unit,
    onIgnoreVersion       : () -> Unit    = {},
    onCompare             : () -> Unit    = {},
    onSelectRelease       : (Release) -> Unit = {},
    onSelectAsset         : (ReleaseAsset) -> Unit = {},
    onBack                : () -> Unit
) {
    val t       = LocalTheme.current
    val context = LocalContext.current
    val s = LocalStrings.current
    var screenshotFullscreen by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.HOME)
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title          = {
                Text(
                    repo.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = {
                    val shareText = "${repo.name} by @${repo.owner.login}\n\n${repo.description ?: ""}\n\n${repo.html_url}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, null))
                }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggleFavourite) {
                    Icon(
                        imageVector        = if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint               = if (isFavourite) RedDanger else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors   = TopAppBarDefaults.topAppBarColors(
                containerColor             = Color.Transparent,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.statusBarsPadding()
        )

        LazyColumn(
            contentPadding      = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // App icon + info header card (overlaps gradient)
            item {
                ElevatedCard(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                    shape     = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors    = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Square rounded icon box
                        Box(
                            modifier         = Modifier
                                .size(80.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model              = ImageRequest.Builder(LocalContext.current)
                                    .data(repo.owner.avatar_url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier           = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.large),
                                contentScale       = ContentScale.Crop
                            )
                        }
                        // Name + stats
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "@${repo.owner.login}",
                                style    = MaterialTheme.typography.labelLarge,
                                color    = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                repo.name,
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis
                            )
                            if (installState.isInstalled) {
                                Spacer(Modifier.height(4.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label   = {
                                        Text(s.installedBadge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    },
                                    colors  = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = GreenOk.copy(0.15f),
                                        labelColor     = GreenOk
                                    )
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Stars | Forks | Language
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, null, tint = StarGold, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    formatStars(repo.stargazers_count),
                                    style      = MaterialTheme.typography.labelLarge,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                VerticalDivider(modifier = Modifier.height(14.dp).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                Icon(Icons.Rounded.ForkRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(formatStars(repo.forks_count), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!repo.language.isNullOrBlank()) {
                                    VerticalDivider(modifier = Modifier.height(14.dp).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(repo.language, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // About section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        s.about,
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        shape  = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val displayedDesc = translatedDesc ?: (repo.description ?: s.noDescAvailable)
                            Text(
                                displayedDesc,
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            if (repo.description != null) {
                                FilledTonalButton(
                                    onClick  = { if (!isTranslating) onTranslate() },
                                    enabled  = !isTranslating,
                                    shape    = MaterialTheme.shapes.large,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isTranslating) {
                                        CircularProgressIndicator(
                                            modifier    = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(15.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (translatedDesc != null) s.translatedRedo
                                        else "${s.translateTo} ${state.settings.language}",
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            // Screenshots inside About
                            if (screenshots.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    s.screenshots,
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(screenshots) { index, url ->
                                        AsyncImage(
                                            model              = url,
                                            contentDescription = null,
                                            modifier           = Modifier
                                                .width(140.dp)
                                                .height(240.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                .clickable { screenshotFullscreen = index },
                                            contentScale       = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Release section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    when {
                        installState.isLoadingRelease -> {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color       = MaterialTheme.colorScheme.primary
                                )
                                Text(s.checkingRelease, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        installState.error != null -> {
                            Text(installState.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                        installState.release != null -> {
                            Text(
                                s.release,
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                modifier   = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                            Card(
                                shape  = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier              = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        SuggestionChip(
                                            onClick = {},
                                            label   = {
                                                Text(
                                                    installState.release.tag_name.ifBlank { "Latest" },
                                                    style      = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                labelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            border = null
                                        )
                                        if (installState.apkAsset != null) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    formatBytes(installState.apkAsset.size),
                                                    style      = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color      = MaterialTheme.colorScheme.primary
                                                )
                                                Text(s.stableRelease, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    if (installState.release.body.isNotBlank()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            s.whatsNew,
                                            style      = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        val displayedBody = translatedReleaseBody ?: installState.release.body
                                        Text(
                                            displayedBody.take(500) +
                                                    if (displayedBody.length > 500) "…" else "",
                                            style      = MaterialTheme.typography.bodySmall,
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 19.sp
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick  = { if (!isTranslatingRelease) onTranslateRelease() },
                                            enabled  = !isTranslatingRelease,
                                            shape    = MaterialTheme.shapes.large,
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            if (isTranslatingRelease) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Rounded.Translate, null, modifier = Modifier.size(15.dp))
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (translatedReleaseBody != null) "Retranslate notes"
                                                else "Translate notes",
                                                style      = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    TextButton(
                                        onClick = { onIgnoreVersion() },
                                        shape   = MaterialTheme.shapes.small
                                    ) {
                                        Icon(
                                            Icons.Rounded.VisibilityOff, null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(s.skipVersion, style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (installState.apkAsset == null) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(s.noApkFound, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Release / Asset selectors  — two dropdowns side by side
            val allApkAssets = installState.release?.assets
                ?.filter { it.name.endsWith(".apk", ignoreCase = true) } ?: emptyList()
            val allOtherAssets: List<ReleaseAsset> = if (allApkAssets.isEmpty())
                installState.release?.assets?.filter { !it.name.endsWith(".apk", ignoreCase = true) } ?: emptyList()
            else emptyList()
            if (installState.releases.size > 1 || allApkAssets.isNotEmpty() || allOtherAssets.isNotEmpty()) {
                item {
                    var showReleaseMenu by remember { mutableStateOf(false) }
                    var showAssetMenu   by remember { mutableStateOf(false) }
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Release picker
                        if (installState.releases.size > 1) {
                            Box(modifier = Modifier.weight(1f)) {
                                ElevatedCard(
                                    onClick   = { showReleaseMenu = true },
                                    shape     = MaterialTheme.shapes.large,
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                    colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier  = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Tag, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            installState.release?.tag_name?.ifBlank { "Release" } ?: "Release",
                                            style      = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurface,
                                            maxLines   = 1,
                                            overflow   = TextOverflow.Ellipsis,
                                            modifier   = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                DropdownMenu(
                                    expanded         = showReleaseMenu,
                                    onDismissRequest = { showReleaseMenu = false },
                                    containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    installState.releases.forEach { rel ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(rel.tag_name.ifBlank { "Release" }, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                    if (rel.published_at.isNotBlank()) {
                                                        Text(rel.published_at.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            },
                                            leadingIcon = {
                                                if (installState.release?.tag_name == rel.tag_name) {
                                                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = { onSelectRelease(rel); showReleaseMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        // Non-APK asset picker — shown when there are no APKs
                        if (allOtherAssets.isNotEmpty()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ElevatedCard(
                                    onClick   = { if (allOtherAssets.size > 1) showAssetMenu = true },
                                    shape     = MaterialTheme.shapes.large,
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                    colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier  = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                installState.apkAsset?.name?.let { if (it.length > 20) it.take(17) + "…" else it } ?: "Select file",
                                                style      = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color      = MaterialTheme.colorScheme.onSurface,
                                                maxLines   = 1,
                                                overflow   = TextOverflow.Ellipsis
                                            )
                                            installState.apkAsset?.let { a ->
                                                if (a.size > 0) Text(formatBytes(a.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (allOtherAssets.size > 1)
                                            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (allOtherAssets.size > 1) {
                                    DropdownMenu(
                                        expanded         = showAssetMenu,
                                        onDismissRequest = { showAssetMenu = false },
                                        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        allOtherAssets.forEach { asset ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(asset.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                        if (asset.size > 0) Text(formatBytes(asset.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                leadingIcon = {
                                                    if (installState.apkAsset?.name == asset.name)
                                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                },
                                                onClick = { onSelectAsset(asset); showAssetMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // APK asset picker — always visible when APKs available
                        if (allApkAssets.isNotEmpty()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ElevatedCard(
                                    onClick   = { if (allApkAssets.size > 1) showAssetMenu = true },
                                    shape     = MaterialTheme.shapes.large,
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                    colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    modifier  = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                installState.apkAsset?.name?.let { if (it.length > 20) it.take(17) + "…" else it } ?: "Select APK",
                                                style      = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color      = MaterialTheme.colorScheme.onSurface,
                                                maxLines   = 1,
                                                overflow   = TextOverflow.Ellipsis
                                            )
                                            installState.apkAsset?.let { a ->
                                                val abi = detectAssetAbi(a.name)
                                                if (abi != null) Text(abi, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        if (allApkAssets.size > 1)
                                            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (allApkAssets.size > 1) {
                                    DropdownMenu(
                                        expanded         = showAssetMenu,
                                        onDismissRequest = { showAssetMenu = false },
                                        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        allApkAssets.forEach { asset ->
                                            val abi = detectAssetAbi(asset.name)
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(asset.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            if (abi != null) Text(abi, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                            if (asset.size > 0) Text(formatBytes(asset.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                },
                                                leadingIcon = {
                                                    if (installState.apkAsset?.name == asset.name)
                                                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                },
                                                onClick = { onSelectAsset(asset); showAssetMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons (includes download progress when active)
            item {
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val canInstall = installState.apkAsset != null &&
                            installState.apkAsset!!.name.endsWith(".apk", ignoreCase = true) &&
                            installState.downloadProgress == null &&
                            !installState.isInstalled
                    val canDownloadNonApk = allApkAssets.isEmpty() &&
                            installState.apkAsset != null &&
                            installState.downloadProgress == null

                    if (installState.downloadProgress != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.downloading, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${(installState.downloadProgress * 100).toInt()}%",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress   = { installState.downloadProgress },
                                    modifier   = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.small),
                                    color      = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedDanger.copy(0.12f))
                                        .clickable { onCancelDownload() },
                                    Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Close, "Cancel",
                                        tint     = RedDanger,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else if (canInstall) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick  = onDownloadOnly,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape    = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.download, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick  = onInstall,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape    = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.InstallMobile, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.install, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (canDownloadNonApk) {
                        Button(
                            onClick  = onDownloadOnly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.download, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }

                    val viewUrl = when {
                        repo.html_url.isNotBlank() -> repo.html_url
                        repo.source == AppSource.FDROID   -> "https://f-droid.org/packages/${repo.full_name}/"
                        repo.source == AppSource.FLATHUB  -> "https://flathub.org/apps/${repo.full_name}"
                        repo.source == AppSource.WINGET   -> "https://winget.run/pkg/${repo.full_name}"
                        else -> ""
                    }
                    if (viewUrl.isNotBlank()) {
                        val sourceLabel = when (repo.source) {
                            AppSource.GITLAB   -> "GitLab"
                            AppSource.CODEBERG -> "Codeberg"
                            AppSource.FDROID   -> "F-Droid"
                            AppSource.IZZY     -> "IzzyOnDroid"
                            AppSource.FLATHUB  -> "Flathub"
                            AppSource.WINGET   -> "Winget"
                            else               -> "GitHub"
                        }
                        FilledTonalButton(
                            onClick  = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(viewUrl)))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View on $sourceLabel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick  = onCompare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.CompareArrows, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compare with another app", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                    }

                    if (installState.isInstalled) {
                        Button(
                            onClick  = onUninstall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = MaterialTheme.shapes.large,
                            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.uninstall, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        if (installState.apkAsset != null) {
                            OutlinedButton(
                                onClick  = onInstall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape    = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reinstall / Update", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // About the Author
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "About the Author",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    ElevatedCard(
                        shape     = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model              = repo.owner.avatar_url,
                                contentDescription = null,
                                modifier           = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape),
                                contentScale       = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    repo.owner.login,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                                val devLabel = when (repo.source) {
                                    AppSource.GITLAB   -> "GitLab Developer"
                                    AppSource.CODEBERG -> "Codeberg Developer"
                                    AppSource.FDROID   -> "F-Droid Developer"
                                    AppSource.IZZY     -> "IzzyOnDroid Developer"
                                    AppSource.FLATHUB  -> "Flathub Developer"
                                    AppSource.WINGET   -> "Winget Developer"
                                    else               -> "GitHub Developer"
                                }
                                Text(devLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${repo.owner.login}"))
                                    )
                                }
                            ) {
                                Icon(Icons.Rounded.OpenInBrowser, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Omni Trust Score
            installState.trustScore?.let { trust ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "Omni Trust Score",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        ElevatedCard(
                            shape     = MaterialTheme.shapes.extraLarge,
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                TrustScoreBar(trust)
                            }
                        }
                    }
                }
            }

            // Smart Install recommendation
            installState.smartInstall?.let { smart ->
                item {
                    val smartColor = if (smart.isOptimal) GreenOk else StarGold
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        OutlinedCard(
                            shape  = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.outlinedCardColors(containerColor = smartColor.copy(0.08f)),
                            border = BorderStroke(1.dp, smartColor.copy(0.28f))
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = {},
                                    colors  = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = smartColor.copy(0.18f),
                                        contentColor   = smartColor
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(
                                        "Best package for your device",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        smart.reason,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = smartColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Developer Mode
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "Developer Mode",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    ElevatedCard(
                        shape     = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val baseUrl = when {
                                repo.html_url.isNotBlank() -> repo.html_url
                                repo.source == AppSource.FDROID  -> "https://f-droid.org/packages/${repo.full_name}/"
                                repo.source == AppSource.FLATHUB -> "https://flathub.org/apps/${repo.full_name}"
                                else -> ""
                            }
                            listOf(
                                Triple(Icons.Rounded.Code,        "View Source Code", baseUrl),
                                Triple(Icons.Rounded.BugReport,   "Open Issues",      if (baseUrl.isNotBlank()) "$baseUrl/issues" else ""),
                                Triple(Icons.Rounded.Description, "License",          if (baseUrl.isNotBlank()) "$baseUrl/blob/main/LICENSE" else ""),
                                Triple(Icons.Rounded.History,     "Release History",  if (baseUrl.isNotBlank()) "$baseUrl/releases" else "")
                            ).forEachIndexed { i, (icon, label, url) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (url.isNotBlank()) Modifier.clickable {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            } else Modifier
                                        )
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    FilledTonalIconButton(
                                        onClick  = {},
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(icon, null, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        label,
                                        style    = MaterialTheme.typography.bodyMedium,
                                        color    = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.OpenInBrowser, null,
                                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (i < 3) HorizontalDivider(
                                    color    = MaterialTheme.colorScheme.outlineVariant.copy(0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    screenshotFullscreen?.let { idx ->
        FullScreenImageViewer(
            urls         = screenshots,
            initialIndex = idx,
            onDismiss    = { screenshotFullscreen = null }
        )
    }
    } // Column
    } // Box

// ─────────────────────────────────────────────────────────────────────────────
// SEE ALL SCREEN
// ─────────────────────────────────────────────────────────────────────────────
private fun Color.vivid(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
        ), hsv
    )
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f)))
}

private fun Modifier.glowBorder(color: Color, cornerRadius: Dp, glowRadius: Dp = 7.dp): Modifier =
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            paint.asFrameworkPaint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2.dp.toPx()
                this.color = color.copy(alpha = 0.55f).toArgb()
                maskFilter = android.graphics.BlurMaskFilter(
                    glowRadius.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            val r = cornerRadius.toPx()
            canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
    }

private val tileColors = listOf(
    Color(0xFF0891B2), Color(0xFF6D28D9), Color(0xFF065F46), Color(0xFFB45309),
    Color(0xFF1D4ED8), Color(0xFF9333EA), Color(0xFFDB2777), Color(0xFFDC2626),
    Color(0xFF0D9488), Color(0xFF7C3AED), Color(0xFF059669), Color(0xFF0EA5E9),
    Color(0xFFEA580C), Color(0xFF16A34A), Color(0xFF7C3AED), Color(0xFFC026D3)
)

@Composable
fun SeeAllScreen(
    title         : String,
    apps          : List<GitHubRepo>,
    installed     : Set<Long>    = emptySet(),
    isLoading     : Boolean,
    useTileColors : Boolean      = false,
    onLoadMore    : () -> Unit,
    onAppClick    : (GitHubRepo) -> Unit,
    onBack        : () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.SEARCH)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        when {
            apps.isEmpty() && isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            apps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⚠️", style = MaterialTheme.typography.displaySmall)
                        Text("Could not load apps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onLoadMore) { Text("Retry") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items = apps) { index, repo ->
                        AppListTile(
                            repo           = repo,
                            isInstalled    = installed.contains(repo.id),
                            tileIndex      = index,
                            useTileColors  = useTileColors,
                            onClick        = { onAppClick(repo) }
                        )
                    }
                    item {
                        if (isLoading) {
                            Box(
                                modifier        = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }
                        } else {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Spacer(Modifier.height(110.dp))
                        }
                    }
                }
            }
        }
    }
    } // Box
}

// ─────────────────────────────────────────────────────────────────────────────
// TRUST SCORE BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrustScoreBar(trust: TrustScore) {
    val color = trust.safeColor

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Omni Trust Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        trust.label,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(color.copy(0.12f))
                        .border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${trust.score}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress   = { trust.score / 100f },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.small),
                color      = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrustChip(
                    label = if (trust.daysSinceUpdate < 90) "Active" else "Inactive",
                    ok    = trust.daysSinceUpdate < 90
                )
                TrustChip(
                    label = if (trust.stars >= 100) "${formatStars(trust.stars)} stars" else "Low stars",
                    ok    = trust.stars >= 100
                )
                TrustChip(
                    label = if (trust.releaseCount > 0) "${trust.releaseCount} releases" else "No releases",
                    ok    = trust.releaseCount > 0
                )
            }
        }
}

@Composable
fun TrustChip(label: String, ok: Boolean) {
    SuggestionChip(
        onClick = {},
        label   = {
            Text(
                "${if (ok) "✓" else "✗"} $label",
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (ok) GreenOk.copy(0.12f) else RedDanger.copy(0.12f),
            labelColor     = if (ok) GreenOk else RedDanger
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled      = true,
            borderColor  = if (ok) GreenOk.copy(0.3f) else RedDanger.copy(0.3f),
            borderWidth  = 0.5.dp
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SOURCES ROW  — horizontal browsable source tiles
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SourcesRow(
    gitlabCount   : Int,
    codebergCount : Int,
    fdroidCount   : Int,
    flathubCount  : Int,
    wingetCount   : Int,
    izzyCount     : Int,
    onSourceClick : (AppSource) -> Unit
) {
    val t = LocalTheme.current
    val sources = listOf(
        Triple(AppSource.GITHUB,   R.drawable.github,      -1),
        Triple(AppSource.FDROID,   R.drawable.fdroid,      fdroidCount),
        Triple(AppSource.GITLAB,   R.drawable.gitlab,      gitlabCount),
        Triple(AppSource.CODEBERG, R.drawable.codeberg,    codebergCount),
        Triple(AppSource.IZZY,     R.drawable.ic_izzy_logo, izzyCount),
        Triple(AppSource.FLATHUB,  R.drawable.flathub,     flathubCount),
        Triple(AppSource.WINGET,   R.drawable.winget,      wingetCount)
    )

    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            "Browse by Source",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = sources) { (source, emoji, count) ->
                val base = Color(source.colorHex)
                val darkEnd  = Color(base.red * 0.45f, base.green * 0.45f, base.blue * 0.45f)
                val lightEnd = Color(
                    (base.red   + 0.30f).coerceAtMost(1f),
                    (base.green + 0.30f).coerceAtMost(1f),
                    (base.blue  + 0.30f).coerceAtMost(1f)
                )
                val countText = when {
                    count < 0  -> "Open Source"
                    count == 0 -> "Browse"
                    else       -> "$count apps"
                }
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(96.dp)   // ← AppComponents.kt ~line 2996: adjust Browse-by-Source tile height here
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(lightEnd, darkEnd)))
                        .clickable { onSourceClick(source) }
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 10.dp, top = 10.dp, end = 8.dp),
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter            = painterResource(emoji),
                                contentDescription = null,
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(source.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(countText,    fontSize = 11.sp, color = Color.White.copy(0.75f), maxLines = 1)
                        }
                    }
                    Box(
                        modifier         = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SOURCE BADGE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SourceBadge(source: AppSource?, modifier: Modifier = Modifier) {
    val s = source ?: return
    if (s == AppSource.GITHUB) return
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(4.dp),
        color    = Color(s.colorHex)
    ) {
        Text(
            s.label,
            fontSize   = 8.sp,
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FULL-SCREEN IMAGE VIEWER
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    urls         : List<String>,
    initialIndex : Int,
    onDismiss    : () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties       = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false
        )
    ) {
        val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { urls.size })
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model              = urls[page],
                    contentDescription = null,
                    modifier           = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentScale       = ContentScale.Fit
                )
            }

            // Page indicator
            Row(
                modifier              = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(urls.size) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(4.dp)
                            .width(if (i == pagerState.currentPage) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i == pagerState.currentPage) Color.White else Color.White.copy(
                                    0.4f
                                )
                            )
                    )
                }
            }

            // Close button
            Surface(
                modifier  = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
                shape = CircleShape,
                color = Color.White.copy(0.15f)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    "Close",
                    tint     = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTIONS ROW  — horizontal LazyRow of vertical cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CollectionsRow(onCollectionClick: (AppCollection) -> Unit) {
    val t = LocalTheme.current

    // Per-collection: gradient colors + emoji tilt (°)
    val configs = remember {
        listOf(
            listOf(Color(0xFF0891B2), Color(0xFF083344)) to 12f,
            listOf(Color(0xFF6D28D9), Color(0xFF1E1035)) to -18f,
            listOf(Color(0xFF065F46), Color(0xFF032B20)) to  22f,
            listOf(Color(0xFFB45309), Color(0xFF1C0A00)) to  -8f,
            listOf(Color(0xFF1D4ED8), Color(0xFF1E3A5F)) to -15f,
            listOf(Color(0xFF9333EA), Color(0xFF2E1065)) to  10f,
            listOf(Color(0xFFDB2777), Color(0xFF500724)) to -22f,
            listOf(Color(0xFFDC2626), Color(0xFF450A0A)) to  16f,
            listOf(Color(0xFF0D9488), Color(0xFF042F2E)) to -10f,
            listOf(Color(0xFF7C3AED), Color(0xFF1E1B4B)) to   8f,
            listOf(Color(0xFF059669), Color(0xFF022C22)) to -14f,
            listOf(Color(0xFF0EA5E9), Color(0xFF0C4A6E)) to  18f,
            listOf(Color(0xFFEA580C), Color(0xFF431407)) to  -6f,
        )
    }

    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            "Curated Collections",
            style    = MaterialTheme.typography.titleLarge,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count = COLLECTIONS.size) { index ->
                val col = COLLECTIONS[index]
                val (gradient, tilt) = configs.getOrElse(index) {
                    listOf(Color(0xFF6D28D9), Color(0xFF1E1035)) to 0f
                }
                CollectionCard(
                    col           = col,
                    gradient      = gradient,
                    emojiRotation = tilt,
                    decorStyle    = index % 4,
                    onClick       = { onCollectionClick(col) }
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(
    col           : AppCollection,
    gradient      : List<Color>,
    emojiRotation : Float,
    decorStyle    : Int,
    onClick       : () -> Unit
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(185.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient))
            .clickable { onClick() }
    ) {
        // Decorative background shape, varies per card
        when (decorStyle) {
            0 -> Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-24).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.08f))
            )
            1 -> Box(
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-28).dp)
                    .graphicsLayer { rotationZ = 30f }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.07f))
            )
            2 -> {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-8).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.09f))
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 32.dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.06f))
                )
            }
            // 3 → clean gradient, no decoration
        }

        // Content: emoji + text stacked from the top
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (col.iconRes != null) {
                // Offscreen layer + BlendMode.Screen makes black PNG pixels transparent
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter            = painterResource(col.iconRes),
                        contentDescription = null,
                        modifier           = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                rotationZ = emojiRotation
                                blendMode = BlendMode.Screen
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        col.emoji,
                        fontSize = 26.sp,
                        modifier = Modifier.graphicsLayer { rotationZ = emojiRotation }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                col.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                maxLines   = 2,
                lineHeight = 16.sp,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                col.subtitle,
                fontSize   = 10.sp,
                color      = Color.White.copy(0.65f),
                maxLines   = 2,
                lineHeight = 13.sp,
                overflow   = TextOverflow.Ellipsis
            )
        }
        // Arrow circle anchored to bottom-right of card
        Box(
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.22f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREENSHOTS PAGER
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ScreenshotsPager(urls: List<String>) {
    if (urls.isEmpty()) return
    val t          = LocalTheme.current
    val pagerState = rememberPagerState(pageCount = { urls.size })
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }

    Column {
        HorizontalPager(
            state       = pagerState,
            modifier    = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp),
            pageSpacing = 8.dp
        ) { page ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { fullscreenIndex = page },
                shape    = MaterialTheme.shapes.large,
                color    = MaterialTheme.colorScheme.surface
            ) {
                AsyncImage(
                    model              = urls[page],
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit
                )
            }
        }
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(urls.size) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (i == pagerState.currentPage) 16.dp else 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }

    fullscreenIndex?.let { idx ->
        FullScreenImageViewer(
            urls         = urls,
            initialIndex = idx,
            onDismiss    = { fullscreenIndex = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FLOATING DOCK  —  pill-shaped, icon+text row, pill-fill highlight
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FloatingNavBar(
    selectedTab  : VAppTab,
    onTabSelect  : (VAppTab) -> Unit,
    updateCount  : Int = 0
) {
    val s     = LocalStrings.current
    val items = listOf(
        Triple(VAppTab.HOME,      Icons.Rounded.Home,     s.navHome),
        Triple(VAppTab.INSTALLED, Icons.Rounded.Download, s.navInstalled),
        Triple(VAppTab.PROFILE,   Icons.Rounded.Person,   s.navProfile),
        Triple(VAppTab.SETTINGS,  Icons.Rounded.Settings, s.navSettings)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(32.dp),
            color           = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation  = 6.dp
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { (tab, icon, label) ->
                    val selected  = selectedTab == tab
                    val hasUpdate = tab == VAppTab.INSTALLED && updateCount > 0
                    val interactionSource = remember { MutableInteractionSource() }
                    val tabWeight by animateFloatAsState(
                        targetValue = if (selected) 1.8f else 1f,
                        animationSpec = tween(220),
                        label = "tabWeight"
                    )

                    Box(
                        modifier         = Modifier
                            .weight(tabWeight)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication        = null
                            ) { onTabSelect(tab) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BadgedBox(badge = {
                                if (hasUpdate) Badge { Text("$updateCount", style = MaterialTheme.typography.labelSmall) }
                            }) {
                                Icon(
                                    imageVector        = icon,
                                    contentDescription = label,
                                    modifier           = Modifier.size(22.dp),
                                    tint               = if (selected) MaterialTheme.colorScheme.onPrimary
                                                         else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = selected,
                                enter   = expandHorizontally(tween(220)) + fadeIn(tween(160)),
                                exit    = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
                            ) {
                                Text(
                                    label,
                                    style      = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimary,
                                    maxLines   = 1,
                                    softWrap   = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────
fun detectAssetAbi(name: String): String? {
    val l = name.lowercase()
    return when {
        "arm64-v8a" in l || "aarch64" in l -> "arm64-v8a"
        "armeabi-v7a" in l || "armv7" in l || "arm32" in l -> "armeabi-v7a"
        "x86_64" in l || "_x64-" in l -> "x86_64"
        "x86" in l -> "x86"
        "universal" in l -> "universal"
        else -> null
    }
}

fun detectPlatformLabels(repo: GitHubRepo): List<AppPlatform> {
    val text = "${repo.name} ${repo.description ?: ""}".lowercase()
    val lang = repo.language?.lowercase() ?: ""
    val platforms = mutableListOf<AppPlatform>()

    // Android — source is the most reliable signal
    val isAndroid = repo.source == AppSource.FDROID ||
        repo.source == AppSource.IZZY ||
        "android" in text || "apk" in text ||
        lang == "kotlin" || lang == "java" || lang == "dart"
    if (isAndroid) platforms.add(AppPlatform.ANDROID)

    // Windows — Winget packages are Windows-only
    val isWindows = repo.source == AppSource.WINGET ||
        "windows" in text || "win32" in text || "winforms" in text || "uwp" in text ||
        lang == "c#" || lang == "autohotkey" || lang == "powershell" || lang == "visual basic .net"
    if (isWindows) platforms.add(AppPlatform.WINDOWS)

    // Linux — Flathub packages are desktop-Linux-first
    val isLinux = repo.source == AppSource.FLATHUB ||
        "linux" in text || "debian" in text || "ubuntu" in text ||
        "flatpak" in text || "snap" in text || "gtk" in text || "kde" in text
    if (isLinux) platforms.add(AppPlatform.LINUX)

    // iOS / macOS
    val isIOS = "ios" in text || "iphone" in text || "ipad" in text ||
        "macos" in text || "mac os" in text || "swiftui" in text ||
        lang == "swift" || lang == "objective-c"
    if (isIOS) platforms.add(AppPlatform.IOS)

    // TV
    val isTV = "android tv" in text || " tv " in text || "firetv" in text ||
        "fire tv" in text || "leanback" in text || "television" in text
    if (isTV) platforms.add(AppPlatform.TV)

    return platforms.distinct()
}


fun formatStars(count: Int): String =
    if (count >= 1000) "${count / 1000}.${(count % 1000) / 100}k" else count.toString()

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
    bytes >= 1_000     -> "${bytes / 1_000} KB"
    else               -> "$bytes B"
}