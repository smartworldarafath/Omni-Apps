package com.vythera.omniapps

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun VideoSplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val onDone  by rememberUpdatedState(onFinished)

    // Safety fallback: skip if video hasn't ended within 6 seconds
    LaunchedEffect(Unit) {
        delay(6_000L)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash}")
                    setVideoURI(uri)
                    setOnCompletionListener { onDone() }
                    setOnErrorListener { _, _, _ -> onDone(); true }
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
