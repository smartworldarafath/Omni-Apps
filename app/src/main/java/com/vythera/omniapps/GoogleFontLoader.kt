package com.vythera.omniapps

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val googleFontNames: Set<String> = linkedSetOf(
    "Open Sans", "Merriweather", "Playfair Display", "Lora", "Oswald",
    "Manrope", "Work Sans", "IBM Plex Sans", "Syne", "Libre Baskerville",
    "Poppins", "Nunito", "Montserrat", "DM Sans", "Lato", "Inter",
    "Ubuntu", "Raleway", "Quicksand", "Josefin Sans", "Exo 2", "Outfit",
    "Space Grotesk", "Plus Jakarta Sans", "Figtree", "Roboto Slab"
)

// In-memory cache: populated on successful download; consulted by fontFamilyFor()
val googleFontCache = ConcurrentHashMap<String, FontFamily>()

private val fontHttp = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .build()

/**
 * Downloads [name] from the Google Fonts CSS API, caches the TTF locally, and returns
 * a FontFamily backed by that file.  Returns FontFamily.Default on any failure.
 *
 * Safe to call from a coroutine on any dispatcher — all IO is moved to Dispatchers.IO.
 */
suspend fun loadGoogleFont(context: Context, name: String): FontFamily {
    googleFontCache[name]?.let { return it }

    return withContext(Dispatchers.IO) {
        val slug     = name.lowercase().replace(" ", "_")
        val fontDir  = File(context.filesDir, "gfonts")
        fontDir.mkdirs()
        val fontFile = File(fontDir, "$slug.ttf")

        // If a valid cached file exists, skip the download
        if (!fontFile.exists() || fontFile.length() < 1024L) {
            fontFile.delete()

            // Google Fonts CSS v1 API: User-Agent "Android" forces TTF (not WOFF2)
            val apiFamily = name.replace(" ", "+")
            val cssUrl    = "https://fonts.googleapis.com/css?family=$apiFamily:regular"

            val css: String = try {
                fontHttp.newCall(
                    Request.Builder().url(cssUrl)
                        .header("User-Agent", "Android")
                        .build()
                ).execute().use { it.body?.string() ?: "" }
            } catch (_: Exception) { return@withContext FontFamily.Default }

            // Extract the .ttf URL from the @font-face src line
            val ttfUrl = Regex("""url\(([^)]+\.ttf)\)""")
                .find(css)?.groupValues?.get(1)
                ?: return@withContext FontFamily.Default

            // Download the actual font file
            val downloaded = try {
                fontHttp.newCall(Request.Builder().url(ttfUrl).build())
                    .execute().use { resp ->
                        if (!resp.isSuccessful) return@withContext FontFamily.Default
                        resp.body?.byteStream()?.use { src ->
                            fontFile.outputStream().use { dst -> src.copyTo(dst) }
                        }
                        true
                    }
            } catch (_: Exception) { false }

            if (!downloaded || fontFile.length() < 1024L) {
                fontFile.delete()
                return@withContext FontFamily.Default
            }
        }

        try {
            FontFamily(Typeface.createFromFile(fontFile)).also { googleFontCache[name] = it }
        } catch (_: Exception) {
            fontFile.delete()
            FontFamily.Default
        }
    }
}
