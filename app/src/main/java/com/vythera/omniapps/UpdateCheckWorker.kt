package com.vythera.omniapps

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vythera.omniapps.R

class UpdateCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs   = PreferencesManager(applicationContext)
        val history = prefs.loadInstallHistory().distinctBy { it.repoId }
        val ignored = prefs.loadIgnoredVersions()
        val token   = prefs.loadSettings().githubToken
        if (token.isNotEmpty()) RetrofitClient.authToken = token

        val available = mutableListOf<Pair<String, String>>()
        for (entry in history) {
            try {
                val release = RetrofitClient.service.getLatestRelease(entry.ownerLogin, entry.repoName)
                val key = "${entry.repoId}:${release.tag_name}"
                if (release.tag_name != entry.tagName && key !in ignored) {
                    available.add(entry.repoName to release.tag_name)
                }
            } catch (_: Exception) {}
        }

        if (available.isNotEmpty()) showNotification(available)
        return Result.success()
    }

    private fun showNotification(updates: List<Pair<String, String>>) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = "${updates.size} app update${if (updates.size > 1) "s" else ""} available"
        val text  = updates.take(3).joinToString(", ") { "${it.first} → ${it.second}" }

        val notif = NotificationCompat.Builder(applicationContext, "omni_updates")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val mgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(2024, notif)
    }
}