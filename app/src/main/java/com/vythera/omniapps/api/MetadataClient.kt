package com.vythera.omniapps.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class AppEntry(
    val id          : String       = "",
    val source      : String       = "",
    val name        : String       = "",
    val summary     : String       = "",
    val icon        : String       = "",
    val stars       : Int          = 0,
    val version     : String       = "",
    val homepage    : String       = "",
    @SerializedName(value = "apkUrl", alternate = ["apk_url", "apk"])
    val apkUrl      : String       = "",
    val license     : String       = "",
    val categories  : List<String> = emptyList(),
    @SerializedName(value = "isLive", alternate = ["is_live"])
    val isLive      : Boolean      = false
)

data class BrowseResult(val apps: List<AppEntry>, val total: Int, val pages: Int)

data class MetaInfo(
    val total       : Int               = 0,
    val sources     : Map<String, Int>  = emptyMap(),
    val lastUpdated : String            = ""
)

class MetadataClient(private val context: Context, private val cdnBase: String) {

    private val gson  = Gson()
    private val prefs = context.getSharedPreferences("metadata_cache", Context.MODE_PRIVATE)

    private var indexCache     : List<AppEntry>? = null
    private var indexLoadedAt  : Long            = 0L

    private val CACHE_TTL = 60 * 60 * 1000L  // 1 hour

    // ── public ────────────────────────────────────────────────────────────

    suspend fun init() = withContext(Dispatchers.IO) {
        try { refreshIndex() } catch (e: Exception) {
            Log.w("MetadataClient", "init refresh failed: ${e.message}")
        }
    }

    suspend fun refresh() = refreshIndex()

    suspend fun search(query: String, source: String? = null): List<AppEntry> {
        val index = ensureIndex()
        val q     = query.lowercase().trim()
        return index.filter { app ->
            (source == null || app.source == source) &&
            (q.isEmpty() ||
                app.name.lowercase().contains(q) ||
                app.summary.lowercase().contains(q))
        }
    }

    suspend fun browseSource(source: String, page: Int = 1): BrowseResult =
        withContext(Dispatchers.IO) {
            val pageKey     = "source_${source}_p$page"
            val manifestKey = "manifest_$source"

            val pageJson     = cachedFetch(pageKey,     "$cdnBase/data/sources/$source/page-$page.json")
            val manifestJson = cachedFetch(manifestKey, "$cdnBase/data/sources/$source/manifest.json")

            val type : java.lang.reflect.Type = object : TypeToken<List<AppEntry>>() {}.type
            val apps : List<AppEntry> = try { gson.fromJson(pageJson, type) } catch (_: Exception) { emptyList() }

            val manifest = try { gson.fromJson(manifestJson, JsonObject::class.java) } catch (_: Exception) { null }
            val total    = manifest?.get("total")?.asInt ?: apps.size
            val pages    = manifest?.get("pages")?.asInt ?: 1

            BrowseResult(apps, total, pages)
        }

    suspend fun getDetail(appId: String): AppEntry? = withContext(Dispatchers.IO) {
        try {
            val parts = appId.split(":", limit = 2)
            if (parts.size != 2) return@withContext null
            val (src, pkg) = parts
            val json = cachedFetch("detail_${src}_$pkg", "$cdnBase/data/detail/$src/$pkg.json")
            gson.fromJson(json, AppEntry::class.java)
        } catch (_: Exception) { null }
    }

    suspend fun getMeta(): MetaInfo? = withContext(Dispatchers.IO) {
        try {
            val json = cachedFetch("meta", "$cdnBase/data/meta.json")
            gson.fromJson(json, MetaInfo::class.java)
        } catch (_: Exception) { null }
    }

    // ── private ───────────────────────────────────────────────────────────

    private suspend fun refreshIndex() = withContext(Dispatchers.IO) {
        val json : String = fetch("$cdnBase/data/index.json")
        saveCache("index", json)
        val type : java.lang.reflect.Type = object : TypeToken<List<AppEntry>>() {}.type
        indexCache    = gson.fromJson(json, type)
        indexLoadedAt = System.currentTimeMillis()
        Log.d("MetadataClient", "Index refreshed — ${indexCache?.size ?: 0} apps")
    }

    private suspend fun ensureIndex(): List<AppEntry> {
        val mem = indexCache
        if (mem != null && System.currentTimeMillis() - indexLoadedAt < CACHE_TTL) return mem

        val disk = loadCache("index")
        if (disk != null && isFresh("index")) {
            val type : java.lang.reflect.Type = object : TypeToken<List<AppEntry>>() {}.type
            return gson.fromJson<List<AppEntry>>(disk, type).also {
                indexCache    = it
                indexLoadedAt = System.currentTimeMillis()
            }
        }

        withContext(Dispatchers.IO) { refreshIndex() }
        return indexCache ?: emptyList()
    }

    private suspend fun cachedFetch(key: String, url: String): String =
        withContext(Dispatchers.IO) {
            if (isFresh(key)) loadCache(key)?.let { return@withContext it }
            fetch(url).also { saveCache(key, it) }
        }

    private fun fetch(url: String): String =
        URL(url).openStream().bufferedReader().use { it.readText() }

    private fun isFresh(key: String): Boolean =
        System.currentTimeMillis() - prefs.getLong("${key}_ts", 0L) < CACHE_TTL

    private fun saveCache(key: String, data: String) =
        prefs.edit().putString(key, data).putLong("${key}_ts", System.currentTimeMillis()).apply()

    private fun loadCache(key: String): String? = prefs.getString(key, null)
}
