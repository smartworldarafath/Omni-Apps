package com.vythera.omniapps

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import java.io.File
import kotlin.math.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size

private fun saveImageLocally(context: Context, uri: android.net.Uri, filename: String): String? {
    return try {
        val file = File(context.filesDir, "$filename.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file.absolutePath
    } catch (_: Exception) { null }
}

private fun saveBitmapLocally(context: Context, bmp: android.graphics.Bitmap, filename: String): String? = try {
    val file = File(context.filesDir, "$filename.jpg")
    file.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
    file.absolutePath
} catch (_: Exception) { null }

private fun cropBitmapFromLayer(
    source   : android.graphics.Bitmap,
    screenW  : Int,  screenH  : Int,
    userScale: Float, offsetX: Float, offsetY: Float,
    cropLeft : Float, cropTop: Float, cropW: Float, cropH: Float,
    circular : Boolean
): android.graphics.Bitmap {
    val bW = source.width.toFloat()
    val bH = source.height.toFloat()
    val fitScale = minOf(screenW / bW, screenH / bH)
    val totalScale = userScale * fitScale
    // graphicsLayer pivot = composable center → inverse mapping:
    // bitmapX = (screenX - screenW/2 - offsetX) / totalScale + bitmapW/2
    val bLeft = ((cropLeft  - screenW / 2f - offsetX) / totalScale + bW / 2f)
    val bTop  = ((cropTop   - screenH / 2f - offsetY) / totalScale + bH / 2f)
    val bCropW = cropW / totalScale
    val bCropH = cropH / totalScale
    val clL = bLeft.toInt().coerceIn(0, source.width - 1)
    val clT = bTop.toInt().coerceIn(0,  source.height - 1)
    val clW = bCropW.toInt().coerceIn(1, source.width  - clL)
    val clH = bCropH.toInt().coerceIn(1, source.height - clT)
    val extracted = android.graphics.Bitmap.createBitmap(source, clL, clT, clW, clH)
    val outW = if (circular) 512 else 1024; val outH = if (circular) 512 else 512
    val scaled = android.graphics.Bitmap.createScaledBitmap(extracted, outW, outH, true)
    return if (!circular) scaled else {
        val result = android.graphics.Bitmap.createBitmap(outW, outH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val paint  = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(outW / 2f, outH / 2f, outW / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        result
    }
}

@Composable
fun ImageCropDialog(
    uri      : android.net.Uri,
    circular : Boolean = true,
    onSave   : (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uri) {
        bmp = try {
            val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) { null }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val loadedBmp = bmp
        if (loadedBmp == null) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            return@Dialog
        }

        var scale   by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val cropR    = minOf(w, h) * 0.42f
            val rectW    = w * 0.88f
            val rectH    = rectW * (9f / 16f)
            val cropLeft = if (circular) (w - cropR * 2) / 2f else (w - rectW) / 2f
            val cropTop  = if (circular) (h - cropR * 2) / 2f else (h - rectH) / 2f
            val cropW    = if (circular) cropR * 2 else rectW
            val cropH    = if (circular) cropR * 2 else rectH

            // Image with pan + pinch-zoom
            Image(
                bitmap             = loadedBmp.asImageBitmap(),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale   = (scale * zoom).coerceIn(0.3f, 10f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
                    .graphicsLayer {
                        scaleX       = scale;  scaleY       = scale
                        translationX = offsetX; translationY = offsetY
                    }
            )

            // Dimmed overlay with transparent crop cutout
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(Color.Black.copy(alpha = 0.58f))
                if (circular) {
                    drawCircle(Color.Transparent, cropR, blendMode = BlendMode.Clear)
                    drawCircle(Color.White.copy(0.65f), cropR, style = Stroke(2.dp.toPx()))
                } else {
                    drawRect(Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(cropLeft, cropTop),
                        size    = Size(cropW, cropH),
                        blendMode = BlendMode.Clear)
                    drawRect(Color.White.copy(0.65f),
                        topLeft = androidx.compose.ui.geometry.Offset(cropLeft, cropTop),
                        size    = Size(cropW, cropH),
                        style   = Stroke(2.dp.toPx()))
                }
            }

            // Hint text
            Text(
                "Drag and pinch to position",
                color    = Color.White.copy(0.75f),
                style    = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp)
            )

            // Action buttons
            Row(
                modifier              = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.45f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Cancel") }
                Button(
                    onClick  = {
                        val cropped = cropBitmapFromLayer(
                            source    = loadedBmp,
                            screenW   = w.toInt(), screenH = h.toInt(),
                            userScale = scale, offsetX = offsetX, offsetY = offsetY,
                            cropLeft  = cropLeft, cropTop = cropTop,
                            cropW     = cropW,    cropH   = cropH,
                            circular  = circular
                        )
                        val filename = if (circular) "profile_avatar" else "profile_cover"
                        val path = saveBitmapLocally(context, cropped, filename)
                        if (path != null) onSave(path)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Crop & Save") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit) {
    TopAppBar(
        title          = {
            Text(
                title,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        colors   = TopAppBarDefaults.topAppBarColors(
            containerColor             = Color.Transparent,
            titleContentColor          = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.statusBarsPadding()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE SCREEN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(
    profile        : UserProfile,
    history        : List<HistoryItem>,
    favourites     : List<GitHubRepo>,
    installHistory : List<InstallHistoryEntry> = emptyList(),
    updates        : List<UpdateInfo>          = emptyList(),
    onSave         : (UserProfile) -> Unit,
    onAppClick     : (GitHubRepo) -> Unit,
    onCheckUpdates : () -> Unit                = {},
    onRollback     : (InstallHistoryEntry) -> Unit = {}
) {
    val t       = LocalTheme.current
    val context = LocalContext.current
    val s = LocalStrings.current

    var name     by remember { mutableStateOf(profile.name)     }
    var email    by remember { mutableStateOf(profile.email)    }
    var photoUri by remember { mutableStateOf(profile.photoUri) }
    var coverUri by remember { mutableStateOf(profile.coverUri) }
    var editing  by remember { mutableStateOf(false) }

    var cropAvatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var cropCoverUri  by remember { mutableStateOf<android.net.Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { cropAvatarUri = it }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { cropCoverUri = it }
    }

    // Show crop dialogs when URIs are set
    cropAvatarUri?.let { uri ->
        ImageCropDialog(
            uri      = uri,
            circular = true,
            onSave   = { path -> photoUri = path },
            onDismiss = { cropAvatarUri = null }
        )
    }
    cropCoverUri?.let { uri ->
        ImageCropDialog(
            uri      = uri,
            circular = false,
            onSave   = { path -> coverUri = path },
            onDismiss = { cropCoverUri = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.PROFILE)
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(s.profileTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = {
                    if (editing) {
                        onSave(UserProfile(name, "", email, photoUri, coverUri))
                        editing = false
                    } else editing = true
                }) {
                    Text(
                        if (editing) s.save else s.edit,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {

            // Cover + Avatar + Identity (avatar and name sit inside the cover, vertically centered left)
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    // Background: cover image or M3 Expressive gradient
                    if (coverUri.isNotEmpty()) {
                        AsyncImage(
                            model              = coverUri,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.38f)))
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.70f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.60f)
                                    )
                                )
                            )
                        )
                    }

                    // Avatar + identity Row, vertically centered on the left
                    Row(
                        modifier              = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .border(2.dp, Color.White.copy(0.50f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri.isNotEmpty()) {
                                AsyncImage(
                                    model              = photoUri,
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Person,
                                    null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            if (editing) {
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .background(Color.Black.copy(0.4f))
                                        .clickable {
                                            avatarPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        if (!editing) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    name.ifEmpty { s.guestUser },
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                if (email.isNotEmpty()) {
                                    Text(
                                        email,
                                        style    = MaterialTheme.typography.labelMedium,
                                        color    = Color.White.copy(0.80f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Change cover button (edit mode only)
                    if (editing) {
                        FilledTonalButton(
                            onClick   = {
                                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier  = Modifier.align(Alignment.TopEnd).padding(10.dp),
                            shape     = MaterialTheme.shapes.extraLarge,
                            colors    = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Black.copy(0.45f),
                                contentColor   = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Change Cover", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (editing) {
                    Column(
                        modifier            = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextField(
                            value            = name,
                            onValueChange    = { name = it },
                            modifier         = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                            placeholder      = { Text(s.displayName, style = MaterialTheme.typography.bodyMedium) },
                            singleLine       = true,
                            colors           = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        TextField(
                            value            = email,
                            onValueChange    = { email = it },
                            modifier         = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                            placeholder      = { Text(s.emailAddress, style = MaterialTheme.typography.bodyMedium) },
                            singleLine       = true,
                            colors           = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Support & Feedback
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.supportFeedbackTitle,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))

                    ElevatedCard(
                        shape     = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Send Feedback
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:narzo9990@gmail.com")
                                            putExtra(Intent.EXTRA_SUBJECT, "Omni Apps - Feedback")
                                            putExtra(Intent.EXTRA_TEXT, "Hi,\n\n")
                                        }
                                        try { context.startActivity(intent) } catch (_: Exception) {}
                                    }
                                    .padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.sendFeedback, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(s.sendFeedbackDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

                            // Support the Project
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/smartworldarafath/Omni-Apps")))
                                    }
                                    .padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Favorite, null, tint = RedDanger, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.supportProject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(s.supportProjectDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 14.dp))

                            // Share Omni
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Check out Omni Apps")
                                            putExtra(Intent.EXTRA_TEXT,
                                                "Omni Apps — open-source GitHub-powered Android app store.\nhttps://github.com/smartworldarafath/Omni-Apps")
                                        }
                                        context.startActivity(Intent.createChooser(share, "Share Omni Apps"))
                                    }
                                    .padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.shareOmni, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(s.shareOmniDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Favourites
            if (favourites.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(s.favourites, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                items(favourites.take(5)) { repo ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AppListTile(repo = repo, onClick = { onAppClick(repo) })
                    }
                }
            }

            // Updates Available
            if (updates.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "${s.updatesAvailable} (${updates.size})",
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onCheckUpdates) {
                                Text(s.refresh, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                items(updates) { upd ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Card(
                            shape  = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Update, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(upd.repoName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${upd.currentTag} → ${upd.latestTag}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Install History
            if (installHistory.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(s.installHistory, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(s.installHistoryHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(installHistory.sortedByDescending { it.installedAt }.take(15)) { entry ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Card(
                            onClick  = { onRollback(entry) },
                            shape    = MaterialTheme.shapes.extraLarge,
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.35f)),
                            border   = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(0.32f))
                        ) {
                            Row(
                                modifier              = Modifier.padding(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.repoName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(entry.tagName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatTimeAgo(entry.installedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                                }
                                Icon(Icons.Rounded.RestartAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Recently Viewed
            if (history.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(s.recentlyViewed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                items(history.take(5)) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Card(
                            shape  = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.25f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(0.25f))
                        ) {
                            AppListTile(repo = item.repo, onClick = { onAppClick(item.repo) })
                        }
                    }
                }
            }
        }
    }
    } // Box
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS SCREEN  — M3 Expressive surfaces
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    settings          : AppSettings,
    currentAccent     : Color?                    = null,
    useMonet          : Boolean                   = false,
    customTheme       : CustomThemeData           = CustomThemeData(),
    onSave            : (AppSettings) -> Unit,
    onAccentSelect    : (Color?) -> Unit          = {},
    onMonetToggle     : (Boolean) -> Unit         = {},
    onCustomThemeSave : (CustomThemeData) -> Unit = {}
) {
    val t       = LocalTheme.current
    val context = LocalContext.current
    val s = LocalStrings.current

    var themeMode            by remember { mutableStateOf(settings.themeMode)          }
    var amoled               by remember { mutableStateOf(settings.amoledBlack)        }
    var fontName             by remember { mutableStateOf(settings.fontName)           }
    var language             by remember { mutableStateOf(settings.language)           }
    var followSystemMonet    by remember { mutableStateOf(settings.followSystemMonet)  }
    var fontExpanded         by remember { mutableStateOf(false)                       }
    var customEditorExpanded by remember { mutableStateOf(false)                       }

    val fonts = listOf(
        // Instant system fonts
        "Default", "Serif", "Monospace", "Cursive",
        "Roboto Condensed", "Roboto Medium", "Roboto Light", "Roboto Thin", "Roboto Black",
        // Bundled fonts
        "Kilo", "Chococooky",
        // Top-rated Google Fonts (download on first use)
        "Open Sans", "Merriweather", "Playfair Display", "Lora", "Oswald",
        "Manrope", "Work Sans", "IBM Plex Sans", "Syne", "Libre Baskerville",
        // More Google Fonts
        "Poppins", "Nunito", "Montserrat", "DM Sans", "Lato", "Inter",
        "Ubuntu", "Raleway", "Quicksand", "Josefin Sans", "Exo 2", "Outfit",
        "Space Grotesk", "Plus Jakarta Sans", "Figtree", "Roboto Slab"
    )

    fun currentSettings(tok: String = settings.githubToken) =
        AppSettings(language, tok, settings.sortBy, fontName, themeMode, amoled, followSystemMonet)

    LaunchedEffect(themeMode, amoled, fontName, language, followSystemMonet) {
        onSave(currentSettings())
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenBackground(ScreenBg.SETTINGS)
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(s.settingsTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier            = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Appearance label
            Text(
                s.appearance,
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            // Theme mode — dropdown selector
            val themeOptions = listOf(
                "Light"   to "☀️  Light",
                "Dark"    to "🌙  Dark",
                "AMOLED"  to "🖤  AMOLED",
                "Minimal" to "◾  Minimal",
                "Sunset"  to "🌅  Sunset",
                "Custom"  to "🎨  Custom",
                "System"  to "📱  Follow System"
            )
            val selectedThemeLabel = themeOptions.firstOrNull { it.first == themeMode }?.second ?: themeMode
            var themeDropExpanded by remember { mutableStateOf(false) }

            Box {
                ElevatedCard(
                    onClick   = { themeDropExpanded = true },
                    shape     = MaterialTheme.shapes.large,
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(selectedThemeLabel, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Icon(
                            if (themeDropExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null, tint = MaterialTheme.colorScheme.primary.copy(0.70f)
                        )
                    }
                }
                DropdownMenu(
                    expanded         = themeDropExpanded,
                    onDismissRequest = { themeDropExpanded = false },
                    containerColor   = MaterialTheme.colorScheme.surface
                ) {
                    themeOptions.forEach { (mode, label) ->
                        val selected = themeMode == mode
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        label,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        color      = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                themeMode = mode
                                themeDropExpanded = false
                                // Opening Custom always re-opens the editor
                                if (mode == "Custom") customEditorExpanded = true
                            }
                        )
                    }
                }
            }

            // Custom theme — collapsed pill or full editor
            if (themeMode == "Custom") {
                if (customEditorExpanded) {
                    CustomThemeEditor(
                        initial   = customTheme,
                        onSave    = onCustomThemeSave,
                        onApplied = { customEditorExpanded = false }
                    )
                } else {
                    // Compact "applied" row shown after editor collapses
                    ElevatedCard(
                        shape     = MaterialTheme.shapes.large,
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Mini colour swatches from the applied theme
                                val ap = customTheme.toAppThemeColors()
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(ap.bgPrimary, ap.bgSurface, ap.accent, ap.accentContainer, ap.textPrimary).forEach { c ->
                                        Box(
                                            Modifier.size(14.dp).clip(CircleShape)
                                                .background(c)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                    }
                                }
                                Column {
                                    Text("Custom Theme", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Applied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                                }
                            }
                            TextButton(onClick = { customEditorExpanded = true }) {
                                Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Accent Color — collapsed row + expandable picker
            var accentPickerExpanded by remember { mutableStateOf(false) }
            val defaultAccentColor = MaterialTheme.colorScheme.primary
            var pendingAccent by remember(currentAccent) { mutableStateOf(currentAccent ?: defaultAccentColor) }
            var accentChanged by remember { mutableStateOf(false) }

            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .clickable { accentPickerExpanded = !accentPickerExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Accent Color",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pendingAccent)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(0.40f), CircleShape)
                        )
                    }
                    if (accentPickerExpanded) {
                        Column(
                            modifier            = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HsvColorWheel(
                                color         = pendingAccent,
                                onColorChange = {
                                    pendingAccent = it
                                    accentChanged = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick  = {
                                    onAccentSelect(pendingAccent)
                                    accentPickerExpanded = false
                                    accentChanged        = false
                                },
                                enabled  = accentChanged,
                                modifier = Modifier.fillMaxWidth(),
                                shape    = MaterialTheme.shapes.large
                            ) {
                                Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // AMOLED
            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.amoledOverride, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.amoledOverrideDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                    }
                    Switch(
                        checked         = amoled,
                        onCheckedChange = { amoled = it }
                    )
                }
            }

            // Monet / Material You
            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Material You (Monet)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Use dynamic color from wallpaper (Android 12+)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        Switch(
                            checked         = useMonet,
                            onCheckedChange = { onMonetToggle(it) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Follow System", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Auto-enable Monet when system supports it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        Switch(
                            checked         = followSystemMonet,
                            onCheckedChange = { followSystemMonet = it }
                        )
                    }
                }
            }

            // Font
            Text(
                s.fontSection,
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fontExpanded = !fontExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(fontName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Icon(
                            if (fontExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = fontExpanded) {
                        Column {
                            fonts.forEach { f ->
                                var ff by remember(f) { mutableStateOf(fontFamilyFor(f)) }
                                LaunchedEffect(f) {
                                    if (f in googleFontNames) ff = loadGoogleFont(context, f)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (fontName == f) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { fontName = f; fontExpanded = false; onSave(currentSettings()) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            f,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            color      = if (fontName == f) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (fontName == f) FontWeight.SemiBold else FontWeight.Normal,
                                            fontFamily = ff
                                        )
                                        Text(
                                            "The quick brown fox",
                                            style      = MaterialTheme.typography.labelSmall,
                                            color      = if (fontName == f) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                                                         else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            fontFamily = ff,
                                            maxLines   = 1
                                        )
                                    }
                                    if (fontName == f) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                if (f != fonts.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // Language
            Text(
                s.languageSection,
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            val supportedLanguages = listOf(
                "English", "Hindi", "Spanish", "French", "German", "Japanese",
                "Portuguese", "Italian", "Russian", "Chinese", "Korean",
                "Arabic", "Dutch", "Turkish", "Polish", "Swedish"
            )
            var langExpanded by remember { mutableStateOf(false) }

            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { langExpanded = !langExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(language, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Icon(
                            if (langExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = langExpanded) {
                        Column {
                            supportedLanguages.forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (language == lang) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            language = lang
                                            langExpanded = false
                                            onSave(currentSettings())
                                        }
                                        .padding(horizontal = 20.dp, vertical = 13.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        lang,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        color      = if (language == lang) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (language == lang) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (language == lang) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                if (lang != supportedLanguages.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // GitHub Token
            Text(
                s.githubSection,
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            var githubTokenInput by remember { mutableStateOf(settings.githubToken) }
            var isEditingToken   by remember { mutableStateOf(settings.githubToken.isEmpty()) }

            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter            = painterResource(R.drawable.github),
                            contentDescription = null,
                            modifier           = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                s.personalAccessToken,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                s.tokenRequired,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isEditingToken) {
                        TextField(
                            value         = githubTokenInput,
                            onValueChange = { githubTokenInput = it },
                            modifier      = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small),
                            placeholder   = { Text("ghp_xxxxxxxxxxxx", style = MaterialTheme.typography.bodyMedium) },
                            singleLine    = true,
                            visualTransformation = if (githubTokenInput.length > 4)
                                PasswordVisualTransformation() else VisualTransformation.None,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick  = {
                                    val mapped = when (fontName) {
                                        "Samsung One UI" -> "Default"
                                        "Sans Serif"     -> "SansSerif"
                                        else             -> fontName
                                    }
                                    onSave(currentSettings(githubTokenInput.trim()))
                                    isEditingToken = false
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape    = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.saveToken, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            if (settings.githubToken.isNotEmpty()) {
                                OutlinedButton(
                                    onClick  = { githubTokenInput = settings.githubToken; isEditingToken = false },
                                    shape    = MaterialTheme.shapes.medium,
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    } else {
                        // Saved — read-only display with pencil to re-edit
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "••••••••••••••••••••",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.primary.copy(0.60f),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                            IconButton(onClick = { isEditingToken = true }) {
                                Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary.copy(0.70f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // About
            Text(
                s.aboutSection,
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(s.versionLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("1.0.2", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary.copy(0.80f))
                }
            }
        }
    }
    } // Box
}

// ─────────────────────────────────────────────────────────────────────────────
// CUSTOM THEME EDITOR
// ─────────────────────────────────────────────────────────────────────────────
private class ColorSlot(
    val key        : String,
    val label      : String,
    val desc       : String,
    val getHex     : (CustomThemeData) -> String,
    val getFallback: (AppThemeColors)  -> Color,
    val setHex     : (CustomThemeData, String) -> CustomThemeData,
    val required   : Boolean = false
)

private val themeColorSlots = listOf(
    ColorSlot("accent",    "Accent / Primary", "Main brand & button color",
        { it.accentHex },    { it.accent },         { d, h -> d.copy(accentHex    = h) }, required = true),
    ColorSlot("bg",        "Background",        "App background color",
        { it.bgHex },        { it.bgPrimary },      { d, h -> d.copy(bgHex        = h) }),
    ColorSlot("surface",   "Surface",           "Cards and container color",
        { it.surfaceHex },   { it.bgSurface },      { d, h -> d.copy(surfaceHex   = h) }),
    ColorSlot("text",      "Text",              "Primary text color",
        { it.onSurfaceHex }, { it.textPrimary },    { d, h -> d.copy(onSurfaceHex = h) }),
    ColorSlot("secondary", "Secondary",         "Secondary accent color",
        { it.secondaryHex }, { it.accentAlt },      { d, h -> d.copy(secondaryHex = h) }),
    ColorSlot("tertiary",  "Tertiary",          "Tertiary accent color",
        { it.tertiaryHex },  { it.accentTertiary }, { d, h -> d.copy(tertiaryHex  = h) })
)

// ─────────────────────────────────────────────────────────────────────────────
// HSV COLOR WHEEL  — full circular picker (hue=angle, saturation=radius)
// ─────────────────────────────────────────────────────────────────────────────

private const val WHEEL_BMP_SIZE = 256

@Composable
private fun HsvColorWheel(
    color        : Color,
    onColorChange: (Color) -> Unit,
    modifier     : Modifier = Modifier
) {
    val initHsv = remember(color) {
        FloatArray(3).also { arr ->
            android.graphics.Color.colorToHSV(
                android.graphics.Color.rgb(
                    (color.red   * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue  * 255).toInt()
                ), arr
            )
        }
    }
    var hue        by remember(color) { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember(color) { mutableFloatStateOf(initHsv[1]) }
    var brightness by remember(color) { mutableFloatStateOf(initHsv[2]) }

    fun emit() = onColorChange(
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    )

    // Pre-built wheel bitmap: hue = angle, saturation = radius, brightness = 1
    val wheelBmp = remember {
        val bmp  = android.graphics.Bitmap.createBitmap(WHEEL_BMP_SIZE, WHEEL_BMP_SIZE, android.graphics.Bitmap.Config.ARGB_8888)
        val hsv  = FloatArray(3).also { it[2] = 1f }
        val cx   = WHEEL_BMP_SIZE / 2f; val cy = WHEEL_BMP_SIZE / 2f; val r = WHEEL_BMP_SIZE / 2f
        for (y in 0 until WHEEL_BMP_SIZE) {
            for (x in 0 until WHEEL_BMP_SIZE) {
                val dx   = x - cx; val dy = y - cy
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > r) { bmp.setPixel(x, y, android.graphics.Color.TRANSPARENT); continue }
                hsv[0] = (atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / PI.toFloat() + 360f) % 360f
                hsv[1] = dist / r
                bmp.setPixel(x, y, android.graphics.Color.HSVToColor(hsv))
            }
        }
        bmp
    }
    val wheelImage = remember(wheelBmp) { wheelBmp.asImageBitmap() }

    val t = LocalTheme.current

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Wheel ─────────────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cx = size.width / 2f; val cy = size.height / 2f
                            val dx = offset.x - cx;   val dy = offset.y - cy
                            val maxR = minOf(size.width, size.height) / 2f
                            hue = (atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / PI.toFloat() + 360f) % 360f
                            saturation = (sqrt(dx * dx + dy * dy) / maxR).coerceIn(0f, 1f)
                            emit()
                        },
                        onDrag = { change, _ ->
                            val cx = size.width / 2f; val cy = size.height / 2f
                            val dx = change.position.x - cx; val dy = change.position.y - cy
                            val maxR = minOf(size.width, size.height) / 2f
                            hue = (atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / PI.toFloat() + 360f) % 360f
                            saturation = (sqrt(dx * dx + dy * dy) / maxR).coerceIn(0f, 1f)
                            emit()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f; val cy = size.height / 2f
                        val dx = offset.x - cx;   val dy = offset.y - cy
                        val maxR = minOf(size.width, size.height) / 2f
                        if (sqrt(dx * dx + dy * dy) <= maxR) {
                            hue = (atan2(dy.toDouble(), dx.toDouble()).toFloat() * 180f / PI.toFloat() + 360f) % 360f
                            saturation = (sqrt(dx * dx + dy * dy) / maxR).coerceIn(0f, 1f)
                            emit()
                        }
                    }
                }
        ) {
            val wSize = IntSize(size.width.toInt(), size.height.toInt())
            // Draw the hue+saturation wheel
            drawImage(image = wheelImage, dstSize = wSize)
            // Darken overlay to simulate brightness < 1
            if (brightness < 1f) drawCircle(
                color  = Color.Black.copy(alpha = 1f - brightness),
                radius = size.minDimension / 2f
            )
            // White rim
            drawCircle(
                color  = Color.White.copy(alpha = 0.12f),
                radius = size.minDimension / 2f,
                style  = Stroke(width = 2.dp.toPx())
            )
            // Selector dot
            val maxR   = size.minDimension / 2f
            val ang    = hue * PI.toFloat() / 180f
            val sx     = size.width  / 2f + saturation * maxR * cos(ang)
            val sy     = size.height / 2f + saturation * maxR * sin(ang)
            val selPos = Offset(sx, sy)
            val selClr = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
            drawCircle(color = Color.White,              radius = 14.dp.toPx(), center = selPos)
            drawCircle(color = selClr,                   radius = 11.dp.toPx(), center = selPos)
            drawCircle(color = Color.White.copy(0.6f),   radius = 14.dp.toPx(), center = selPos, style = Stroke(2.dp.toPx()))
        }

        // ── Brightness slider ─────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Brightness", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val fullClr = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Brush.horizontalGradient(listOf(Color.Black, fullClr)))
                )
                Slider(
                    value         = brightness,
                    onValueChange = { brightness = it; emit() },
                    valueRange    = 0f..1f,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = SliderDefaults.colors(
                        thumbColor           = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))),
                        activeTrackColor     = Color.Transparent,
                        inactiveTrackColor   = Color.Transparent,
                        activeTickColor      = Color.Transparent,
                        inactiveTickColor    = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun CustomThemeEditor(
    initial   : CustomThemeData,
    onSave    : (CustomThemeData) -> Unit,
    onApplied : () -> Unit = {}
) {
    var d            by remember(initial) { mutableStateOf(initial) }
    var applied      by remember { mutableStateOf(false) }
    var expandedSlot by remember { mutableStateOf<String?>("accent") }

    LaunchedEffect(applied) {
        if (applied) { kotlinx.coroutines.delay(650); onApplied() }
    }

    val previewTheme = remember(d) { d.toAppThemeColors() }

    val slots = themeColorSlots

    ElevatedCard(
        shape     = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Custom Theme",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            // ── Live preview ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(previewTheme.bgPrimary)
                    .border(0.5.dp, previewTheme.borderVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(previewTheme.bgSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = previewTheme.textPrimary, maxLines = 1)
                                Text("by developer", style = MaterialTheme.typography.labelSmall, color = previewTheme.textSecondary, maxLines = 1)
                            }
                            Box(
                                modifier         = Modifier.height(22.dp).clip(RoundedCornerShape(11.dp)).background(previewTheme.accent).padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Install", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    // Mini nav dock
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(previewTheme.dockBg)
                            .padding(horizontal = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { i ->
                            Box(modifier = Modifier.size(if (i == 0) 6.dp else 5.dp).clip(CircleShape)
                                .background(if (i == 0) previewTheme.dockForeground else previewTheme.dockForeground.copy(alpha = 0.35f)))
                        }
                    }
                }
            }

            // ── Dark / Light toggle ───────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Appearance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(true to "Dark", false to "Light").forEach { (dark, label) ->
                        val selected = d.isDark == dark
                        FilterChip(
                            selected = selected,
                            onClick  = { d = d.copy(isDark = dark) },
                            label    = { Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Color slots ───────────────────────────────────────────────
            slots.forEach { slot ->
                val storedHex = slot.getHex(d)
                val effectiveColor = if (storedHex.isNotEmpty()) hexToColor(storedHex, slot.getFallback(previewTheme))
                                     else slot.getFallback(previewTheme)
                val isExpanded    = expandedSlot == slot.key
                val isCustomized  = storedHex.isNotEmpty() && !slot.required

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { expandedSlot = if (isExpanded) null else slot.key }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(effectiveColor)
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(slot.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                if (isCustomized) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                            Text(slot.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isCustomized) {
                            TextButton(
                                onClick        = { d = slot.setHex(d, "") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter   = expandVertically() + fadeIn(),
                        exit    = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            HsvColorWheel(
                                color         = effectiveColor,
                                onColorChange = { d = slot.setHex(d, it.toHex6()) },
                                modifier      = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (slot.key != slots.last().key) {
                        HorizontalDivider(
                            color    = MaterialTheme.colorScheme.outlineVariant.copy(0.4f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // ── Apply button ──────────────────────────────────────────────
            Button(
                onClick  = { if (!applied) { onSave(d); applied = true } },
                enabled  = !applied,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = MaterialTheme.shapes.large
            ) {
                Icon(if (applied) Icons.Rounded.Check else Icons.Rounded.Palette, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (applied) "Applied ✓" else "Apply Theme", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// GITHUB TOKEN ONBOARDING  (shown after splash when no token is saved)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GitHubTokenOnboarding(
    onSave : (String) -> Unit,
    onSkip : () -> Unit
) {
    val t          = LocalTheme.current
    var tokenInput by remember { mutableStateOf("") }

    Box(
        modifier         = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(20.dp)
        ) {
            // Icon
            FilledIconButton(
                onClick  = {},
                modifier = Modifier.size(76.dp)
            ) {
                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(36.dp))
            }

            Text(
                "GitHub Search",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center
            )

            Text(
                "To search apps directly on GitHub, add your Personal Access Token.\n\nYou can skip this and add it later in Settings.",
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
                lineHeight = 21.sp
            )

            // How to generate a token
            ElevatedCard(
                shape     = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors    = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Text(
                            "How to generate a token",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val steps = listOf(
                        "Open github.com → Sign in to your account",
                        "Go to Settings → Developer settings → Personal access tokens → Tokens (classic)",
                        "Click \"Generate new token (classic)\"",
                        "Add a note (e.g. \"Omni Apps\") and set an expiration",
                        "Under Scopes, check \"public_repo\" (no other scopes needed)",
                        "Click \"Generate token\" and copy it immediately"
                    )
                    steps.forEachIndexed { i, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment     = Alignment.Top
                        ) {
                            Box(
                                modifier         = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${i + 1}",
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                step,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier   = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            TextField(
                value         = tokenInput,
                onValueChange = { tokenInput = it },
                modifier      = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                placeholder   = { Text("ghp_xxxxxxxxxxxx", style = MaterialTheme.typography.bodyMedium) },
                singleLine    = true,
                visualTransformation = if (tokenInput.length > 4)
                    PasswordVisualTransformation() else VisualTransformation.None,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
                )
            )

            Button(
                onClick  = {
                    if (tokenInput.isNotBlank()) onSave(tokenInput.trim()) else onSkip()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = MaterialTheme.shapes.large
            ) {
                Text(
                    if (tokenInput.isNotBlank()) "Save & Continue" else "Continue",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onSkip) {
                Text("Skip for now", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

fun formatTimeAgo(ts: Long): String {
    val d = System.currentTimeMillis() - ts
    return when {
        d < 60_000     -> "Just now"
        d < 3_600_000  -> "${d / 60_000}m ago"
        d < 86_400_000 -> "${d / 3_600_000}h ago"
        else           -> "${d / 86_400_000}d ago"
    }
}
