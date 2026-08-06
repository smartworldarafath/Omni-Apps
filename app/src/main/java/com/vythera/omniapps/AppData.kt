package com.vythera.omniapps

import android.app.Application
import com.vythera.omniapps.api.AppEntry
import com.vythera.omniapps.api.MetadataManager
import com.vythera.omniapps.R
import kotlinx.coroutines.isActive
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File

enum class AppPlatform(val label: String, val emoji: String = "", val iconRes: Int? = null) {
    ALL("All", "⬡"), ANDROID("Android", iconRes = R.drawable.ic_android_logo),
    WINDOWS("Windows", iconRes = R.drawable.ic_windows_logo), LINUX("Linux", iconRes = R.drawable.ic_linux_logo), TV("TV", iconRes = R.drawable.ic_tv_logo),
    IOS("iOS", iconRes = R.drawable.ic_ios_logo)
}

enum class AppSource(val label: String, val colorHex: Long) {
    GITHUB   ("GitHub",      0xFF24292EL),
    GITLAB   ("GitLab",      0xFFFC6D26L),
    CODEBERG ("Codeberg",    0xFF2185D0L),
    FDROID   ("F-Droid",     0xFF1976D2L),
    IZZY     ("IzzyOnDroid", 0xFF0D47A1L),
    FLATHUB  ("Flathub",     0xFFEEEEEEL),
    WINGET   ("Winget",      0xFFFFD966L)
}

data class GitHubRepo(
    val id: Long = 0, val name: String = "", val full_name: String = "",
    val description: String? = null, val stargazers_count: Int = 0,
    val forks_count: Int = 0, val html_url: String = "",
    val owner: RepoOwner = RepoOwner(), val language: String? = null,
    val updated_at: String = "",
    val source: AppSource? = AppSource.GITHUB,
    val apkUrl: String = "",
    val cdnVersion: String = ""
)
data class RepoOwner(val login: String = "", val avatar_url: String = "")
data class SearchResponse(val items: List<GitHubRepo> = emptyList())
data class Release(
    val tag_name: String = "", val name: String = "",
    val assets: List<ReleaseAsset> = emptyList(),
    val published_at: String = "",
    val body: String = ""
)
data class ReleaseAsset(
    val name: String = "", val browser_download_url: String = "",
    val size: Long = 0, val content_type: String = ""
)

data class InstallState(
    val isLoadingRelease : Boolean = false,
    val release          : Release? = null,
    val releases         : List<Release> = emptyList(),
    val apkAsset         : ReleaseAsset? = null,
    val smartInstall     : SmartInstallResult? = null,
    val trustScore       : TrustScore? = null,
    val downloadProgress : Float? = null,
    val isInstalled      : Boolean = false,
    val packageName      : String? = null,
    val error            : String? = null,
    val downloadId       : Long? = null,
    val repo             : GitHubRepo? = null
)

data class UserProfile(
    val name     : String = "",
    val age      : String = "",
    val email    : String = "",
    val photoUri : String = "",
    val coverUri : String = ""
)
data class HistoryItem(val repo: GitHubRepo, val viewedAt: Long = System.currentTimeMillis())
data class AppSettings(
    val language          : String  = "English",
    val githubToken       : String  = "",
    val sortBy            : String  = "Stars",
    val fontName          : String  = "Default",
    val themeMode         : String  = "System",
    val amoledBlack       : Boolean = false,
    val followSystemMonet : Boolean = false
)

// User-editable custom theme — accent is required, extra fields override auto-derived colors
data class CustomThemeData(
    val accentHex      : String  = "#D0BCFF",
    val isDark         : Boolean = true,
    val bgHex          : String  = "",   // "" = auto-derived from accent
    val surfaceHex     : String  = "",
    val onSurfaceHex   : String  = "",
    val secondaryHex   : String  = "",
    val tertiaryHex    : String  = ""
)

data class InstallHistoryEntry(
    val repoId      : Long,
    val repoName    : String,
    val ownerLogin  : String,
    val tagName     : String,
    val apkPath     : String,
    val installedAt : Long   = System.currentTimeMillis(),
    val packageName : String = ""
)

data class UpdateInfo(
    val repoId     : Long,
    val repoName   : String,
    val currentTag : String,
    val latestTag  : String,
    val changelog  : String
)

data class SelfUpdateInfo(
    val latestVersion : String,
    val apkUrl        : String,
    val changelog     : String
)

data class ReadmeResponse(
    val content  : String = "",
    val encoding : String = ""
)

data class UiState(
    val refreshToken           : Int                     = 0,
    val trending               : List<GitHubRepo>        = emptyList(),
    val media                  : List<GitHubRepo>        = emptyList(),
    val tools                  : List<GitHubRepo>        = emptyList(),
    val games                  : List<GitHubRepo>        = emptyList(),
    val browsers               : List<GitHubRepo>        = emptyList(),
    val productivity           : List<GitHubRepo>        = emptyList(),
    val security               : List<GitHubRepo>        = emptyList(),
    val devtools               : List<GitHubRepo>        = emptyList(),
    val photoVideo             : List<GitHubRepo>        = emptyList(),
    val music                  : List<GitHubRepo>        = emptyList(),
    val finance                : List<GitHubRepo>        = emptyList(),
    val education              : List<GitHubRepo>        = emptyList(),
    val fitness                : List<GitHubRepo>        = emptyList(),
    val artDesign              : List<GitHubRepo>        = emptyList(),
    val news                   : List<GitHubRepo>        = emptyList(),
    val social                 : List<GitHubRepo>        = emptyList(),
    val cloudStorage           : List<GitHubRepo>        = emptyList(),
    val cooking                : List<GitHubRepo>        = emptyList(),
    val platformApps           : List<GitHubRepo>        = emptyList(),
    val searchResults          : List<GitHubRepo>        = emptyList(),
    val recommendations        : List<GitHubRepo>        = emptyList(),
    val seeAllTitle            : String                  = "",
    val seeAllQuery            : String                  = "",
    val seeAllApps             : List<GitHubRepo>        = emptyList(),
    val seeAllPage             : Int                     = 1,
    val seeAllSource           : String?                 = null,
    val isLoadingSeeAll        : Boolean                 = false,
    val isLoading              : Boolean                 = false,
    val isLoadingMore          : Boolean                 = false,
    val error                  : String?                 = null,
    val searchQuery            : String                  = "",
    val platform               : AppPlatform            = AppPlatform.ALL,
    val trendingPage           : Int                     = 1,
    val installStates          : Map<Long, InstallState> = emptyMap(),
    val profile                : UserProfile             = UserProfile(),
    val history                : List<HistoryItem>       = emptyList(),
    val settings               : AppSettings             = AppSettings(),
    val themeName              : ThemeName               = ThemeName.DARK,
    val accentColor            : Color?                  = null,
    val useMonet               : Boolean                 = false,
    val customTheme            : CustomThemeData         = CustomThemeData(),
    val categoryViewCounts     : Map<String, Int>        = emptyMap(),
    val translatedDescriptions  : Map<Long, String>       = emptyMap(),
    val isTranslating           : Map<Long, Boolean>      = emptyMap(),
    val translatedReleaseBodies : Map<Long, String>       = emptyMap(),
    val isTranslatingRelease    : Map<Long, Boolean>      = emptyMap(),
    val notifsDismissed         : Boolean                 = false,
    val favourites             : List<GitHubRepo>        = emptyList(),
    val githubUsername         : String                  = "",
    val installHistory         : List<InstallHistoryEntry> = emptyList(),
    val ignoredVersions        : Set<String>             = emptySet(),
    val updates                : List<UpdateInfo>        = emptyList(),
    val screenshots            : Map<Long, List<String>> = emptyMap(),
    val isCheckingUpdates      : Boolean                 = false,
    val compareTargetRepo      : GitHubRepo?             = null,
    val isSearching            : Boolean                 = false,
    val selectedSubCategories  : Set<String>             = emptySet(),
    val isFilterMenuOpen: Boolean = false,
    val activeSubMenuPlatform: AppPlatform? = null,
    val selectedSource  : AppSource?             = null,
    val gitlabApps      : List<GitHubRepo>        = emptyList(),
    val codebergApps    : List<GitHubRepo>        = emptyList(),
    val fdroidApps      : List<GitHubRepo>        = emptyList(),
    val izzyApps        : List<GitHubRepo>        = emptyList(),
    val flathubApps     : List<GitHubRepo>        = emptyList(),
    val wingetApps          : List<GitHubRepo>        = emptyList(),
    val selfUpdateInfo      : SelfUpdateInfo?         = null,
    val selfUpdateDismissed : Boolean                 = false,
)

interface GTranslateService {
    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String,
        @Query("sl") sl: String,
        @Query("tl") tl: String,
        @Query("dt") dt: String,
        @Query("q") q: String
    ): JsonArray
}
object GTranslateClient {
    val service: GTranslateService = Retrofit.Builder()
        .baseUrl("https://translate.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create()).build()
        .create(GTranslateService::class.java)
}

// ── GitHub API ────────────────────────────────────────────────────────────────
interface GitHubService {
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ReadmeResponse

    @GET("user/starred")
    suspend fun getStarredRepos(
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10
    ): List<Release>

    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Release
}

object RetrofitClient {
    @Volatile var authToken: String = ""
    private val httpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(3, 5, java.util.concurrent.TimeUnit.MINUTES))
        .addInterceptor { chain ->
            val req = chain.request().newBuilder().apply {
                if (authToken.isNotEmpty()) addHeader("Authorization", "Bearer $authToken")
                addHeader("Accept", "application/vnd.github+json")
            }.build()
            chain.proceed(req)
        }.build()

    val service: GitHubService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubService::class.java)
}

// ── GitLab API ────────────────────────────────────────────────────────────────
data class GitLabProject(
    val id: Int = 0, val name: String = "", val description: String? = null,
    val star_count: Int = 0, val forks_count: Int = 0, val web_url: String = "",
    val namespace: GitLabNamespace = GitLabNamespace(),
    val language: String? = null, val last_activity_at: String = ""
)
data class GitLabNamespace(val name: String = "", val avatar_url: String? = null)

fun GitLabProject.toUnifiedRepo() = GitHubRepo(
    id = id.toLong() + 9_000_000_000L,
    name = name, full_name = "${namespace.name}/$name",
    description = description, stargazers_count = star_count,
    forks_count = forks_count, html_url = web_url,
    owner = RepoOwner(
        namespace.name,
        namespace.avatar_url ?: "https://gitlab.com/uploads/-/system/group/avatar/$id/avatar.png"
    ),
    language = language, updated_at = last_activity_at,
    source = AppSource.GITLAB
)

interface GitLabService {
    @GET("projects")
    suspend fun searchProjects(
        @Query("search")     query     : String  = "android",
        @Query("order_by")   orderBy   : String  = "star_count",
        @Query("sort")       sort      : String  = "desc",
        @Query("per_page")   perPage   : Int     = 20,
        @Query("page")       page      : Int     = 1,
        @Query("visibility") visibility: String  = "public",
        @Query("topic")      topic     : String? = null
    ): List<GitLabProject>
}

object GitLabClient {
    val service: GitLabService = Retrofit.Builder()
        .baseUrl("https://gitlab.com/api/v4/")
        .client(OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .addHeader("Accept", "application/json").build())
            }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GitLabService::class.java)
}

// ── Codeberg API ──────────────────────────────────────────────────────────────
data class CodebergRepo(
    val id: Long = 0, val name: String = "", val description: String? = null,
    val stars_count: Int = 0, val forks_count: Int = 0, val html_url: String = "",
    val owner: CodebergOwner = CodebergOwner(),
    val language: String? = null, val updated: String = ""
)
data class CodebergOwner(val login: String = "", val avatar_url: String = "")
data class CodebergSearchResponse(val ok: Boolean = false, val data: List<CodebergRepo> = emptyList())

fun CodebergRepo.toUnifiedRepo() = GitHubRepo(
    id = id + 8_000_000_000L,
    name = name, full_name = "${owner.login}/$name",
    description = description, stargazers_count = stars_count,
    forks_count = forks_count, html_url = html_url,
    owner = RepoOwner(owner.login, owner.avatar_url),
    language = language, updated_at = updated,
    source = AppSource.CODEBERG
)

fun AppEntry.toGitHubRepo(): GitHubRepo {
    val appSource = when (source) {
        "fdroid"   -> AppSource.FDROID
        "gitlab"   -> AppSource.GITLAB
        "codeberg" -> AppSource.CODEBERG
        "flathub"  -> AppSource.FLATHUB
        "winget"   -> AppSource.WINGET
        "izzy"     -> AppSource.IZZY
        else       -> AppSource.GITHUB
    }
    val pkg = id.substringAfter(":")
    val repoId: Long = when (source) {
        "gitlab"   -> pkg.toLongOrNull()?.plus(9_000_000_000L)
                      ?: (kotlin.math.abs(id.hashCode()).toLong() + 9_000_000_000L)
        "codeberg" -> pkg.toLongOrNull()?.plus(8_000_000_000L)
                      ?: (kotlin.math.abs(id.hashCode()).toLong() + 8_000_000_000L)
        "fdroid"   -> kotlin.math.abs(id.hashCode()).toLong() + 7_000_000_000L
        "flathub"  -> kotlin.math.abs(id.hashCode()).toLong() + 6_000_000_000L
        "winget"   -> kotlin.math.abs(id.hashCode()).toLong() + 5_000_000_000L
        "github"   -> pkg.toLongOrNull() ?: kotlin.math.abs(id.hashCode()).toLong()
        "izzy"     -> pkg.toLongOrNull()?.plus(4_000_000_000L)
                      ?: (kotlin.math.abs(id.hashCode()).toLong() + 4_000_000_000L)
        else       -> kotlin.math.abs(id.hashCode()).toLong()
    }
    // GitHub and IzzyOnDroid CDN entries encode owner in the homepage URL
    val ghOwner = if (source == "github" || source == "izzy") {
        homepage.removePrefix("https://github.com/").substringBefore("/").takeIf { it.isNotBlank() }
    } else null
    val owner = when (source) {
        "gitlab", "codeberg" -> pkg.substringBefore("/").ifEmpty { source }
        "github", "izzy"     -> ghOwner ?: source
        else                 -> source
    }
    val fullName = when (source) {
        "github", "izzy" -> if (ghOwner != null) "$ghOwner/$name" else pkg
        else             -> pkg
    }
    val resolvedUrl = when {
        source == "fdroid" && !homepage.contains("f-droid.org") ->
            "https://f-droid.org/packages/$pkg"
        else -> homepage
    }
    return GitHubRepo(
        id               = repoId,
        name             = name,
        full_name        = fullName,
        description      = summary.ifEmpty { null },
        stargazers_count = stars,
        html_url         = resolvedUrl,
        owner            = RepoOwner(login = owner, avatar_url = icon),
        source           = appSource,
        apkUrl           = apkUrl,
        cdnVersion       = version
    )
}

interface CodebergService {
    @GET("repos/search")
    suspend fun searchRepos(
        @Query("q")     query : String  = "android",
        @Query("topic") topic : Boolean = false,
        @Query("sort")  sort  : String  = "stars",
        @Query("order") order : String  = "desc",
        @Query("limit") limit : Int     = 20,
        @Query("page")  page  : Int     = 1
    ): CodebergSearchResponse
}

object CodebergClient {
    val service: CodebergService = Retrofit.Builder()
        .baseUrl("https://codeberg.org/api/v1/")
        .client(OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "OmniApps/1.0")
                    .build())
            }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(CodebergService::class.java)
}

// ── Flathub API ───────────────────────────────────────────────────────────────
// All Flathub v2 endpoints return {"hits": [...]} — never a bare array.
// app_id is the canonical dotted ID (com.discordapp.Discord); `id` is underscore version.
data class FlathubApp(
    @com.google.gson.annotations.SerializedName("app_id")
    val appId    : String = "",
    val name     : String = "",
    val summary  : String = "",
    val icon     : String = "",
    @com.google.gson.annotations.SerializedName("installs_last_month")
    val installs : Int    = 0
)
data class FlathubResponse(val hits: List<FlathubApp>? = emptyList())
// Only send query — omitting filters avoids 422 from the backend's Filter object schema
data class FlathubSearchBody(val query: String, val hits_per_page: Int = 30)

fun FlathubApp.toUnifiedRepo(): GitHubRepo {
    val safeId   = appId   ?: ""
    val safeName = name    ?: ""
    val safeSum  = summary ?: ""
    val safeIcon = icon    ?: ""
    return GitHubRepo(
        id               = kotlin.math.abs(safeId.hashCode()).toLong() + 6_000_000_000L,
        name             = safeName.ifEmpty { safeId.substringAfterLast(".") },
        full_name        = safeId,
        description      = safeSum.ifEmpty { null },
        stargazers_count = installs / 100,
        html_url         = "https://flathub.org/apps/$safeId",
        owner            = RepoOwner(login = "flathub", avatar_url = safeIcon),
        source           = AppSource.FLATHUB
    )
}

interface FlathubService {
    @GET("collection/recently-added")
    suspend fun getPopular(): FlathubResponse

    @POST("search")
    suspend fun search(@Body body: FlathubSearchBody): FlathubResponse
}

object FlathubClient {
    val service: FlathubService = Retrofit.Builder()
        .baseUrl("https://flathub.org/api/v2/")
        .client(OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .addHeader("Accept", "application/json").build())
            }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(FlathubService::class.java)
}

// ── IzzyOnDroid live client ───────────────────────────────────────────────────
// Streams the F-Droid index-v1.json and returns the first N apps.
// The streaming parser skips the large "packages" object without loading it.
data class IzzyFDroidApp(
    val packageName : String  = "",
    val name        : String  = "",
    val summary     : String? = null,
    val icon        : String? = null,
    val sourceCode  : String? = null,
    val webSite     : String? = null
)

object IzzyOnDroidClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .addHeader("User-Agent", "OmniApps/1.0").build())
        }.build()

    @Volatile private var cache   : List<GitHubRepo>? = null
    @Volatile private var cacheAt : Long              = 0L
    private val CACHE_TTL = 6 * 60 * 60 * 1000L

    suspend fun getApps(limit: Int = 50): List<GitHubRepo> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val c = cache
            if (c != null && System.currentTimeMillis() - cacheAt < CACHE_TTL)
                return@withContext c.take(limit)
            try {
                val req  = okhttp3.Request.Builder()
                    .url("https://apt.izzysoft.de/fdroid/repo/index-v1.json").build()
                val resp = http.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body ?: return@withContext emptyList()
                val gson = Gson()
                val jr   = com.google.gson.stream.JsonReader(body.charStream())
                val apps = mutableListOf<IzzyFDroidApp>()
                jr.beginObject()
                while (jr.hasNext()) {
                    if (jr.nextName() == "apps") {
                        jr.beginArray()
                        while (jr.hasNext() && apps.size < limit) {
                            try { apps.add(gson.fromJson(jr, IzzyFDroidApp::class.java)) }
                            catch (_: Exception) { try { jr.skipValue() } catch (_: Exception) {} }
                        }
                        while (jr.hasNext()) { try { jr.skipValue() } catch (_: Exception) { break } }
                        jr.endArray()
                    } else {
                        try { jr.skipValue() } catch (_: Exception) {}
                    }
                }
                try { jr.endObject() } catch (_: Exception) {}
                body.close()
                val result = apps.filter { it.packageName.isNotEmpty() }.map { app ->
                    val icon = app.icon?.takeIf { it.isNotBlank() }
                        ?.let { "https://apt.izzysoft.de/fdroid/repo/$it" } ?: ""
                    val home = app.sourceCode?.takeIf { it.isNotBlank() }
                        ?: app.webSite?.takeIf { it.isNotBlank() }
                        ?: "https://apt.izzysoft.de/fdroid/index/apk/${app.packageName}"
                    GitHubRepo(
                        id               = kotlin.math.abs(app.packageName.hashCode()).toLong() + 4_000_000_000L,
                        name             = app.name.ifEmpty { app.packageName },
                        full_name        = app.packageName,
                        description      = app.summary?.takeIf { it.isNotBlank() },
                        stargazers_count = 0,
                        html_url         = home,
                        owner            = RepoOwner(
                            login      = app.packageName.substringAfterLast("."),
                            avatar_url = icon
                        ),
                        source           = AppSource.IZZY
                    )
                }
                cache   = result
                cacheAt = System.currentTimeMillis()
                result
            } catch (_: Exception) { emptyList() }
        }
}

// ── Preferences persistence ───────────────────────────────────────────────────
class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("omni_prefs", Context.MODE_PRIVATE)
    private val gson  = Gson()

    private val tokenPrefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "omni_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) { null }

    private fun saveToken(token: String) =
        tokenPrefs?.edit()?.putString("github_token", token)?.apply()
            ?: prefs.edit().putString("github_token_fallback", token).apply()

    private fun loadToken(): String =
        tokenPrefs?.getString("github_token", null)
            ?: prefs.getString("github_token_fallback", "") ?: ""

    fun saveLanguageCode(code: String) = prefs.edit().putString("user_language_code", code).apply()

    fun saveProfile(p: UserProfile) = prefs.edit().putString("profile", gson.toJson(p)).apply()
    fun loadProfile(): UserProfile  = fromJson("profile") ?: UserProfile()

    fun saveSettings(s: AppSettings) {
        saveToken(s.githubToken)
        prefs.edit().putString("settings", gson.toJson(s.copy(githubToken = ""))).apply()
    }
    fun loadSettings(): AppSettings {
        val s = fromJson<AppSettings>("settings")
            ?: fromJson<AppSettings>("app_settings_v2")
            ?: AppSettings()
        return s.copy(githubToken = loadToken())
    }

    fun saveTheme(t: ThemeName)  = prefs.edit().putString("theme", t.name).apply()
    fun loadTheme(): ThemeName   = try {
        ThemeName.valueOf(prefs.getString("theme", ThemeName.DARK.name)!!)
    } catch (_: Exception) { ThemeName.DARK }

    fun saveAccentColor(c: Color?) =
        prefs.edit().putString("accent", c?.value?.toString() ?: "").apply()
    fun loadAccentColor(): Color? = try {
        val s = prefs.getString("accent", "") ?: ""
        if (s.isEmpty()) null else Color(s.toULong())
    } catch (_: Exception) { null }

    fun saveSearchPlatform(p: AppPlatform) = prefs.edit().putString("search_platform", p.name).apply()
    fun loadSearchPlatform(): AppPlatform = try {
        AppPlatform.valueOf(prefs.getString("search_platform", AppPlatform.ALL.name)!!)
    } catch (_: Exception) { AppPlatform.ALL }

    fun saveSearchSubCategories(subs: Set<String>) = prefs.edit().putStringSet("search_subs", subs).apply()
    fun loadSearchSubCategories(): Set<String> = prefs.getStringSet("search_subs", emptySet()) ?: emptySet()

    fun saveUseMonet(v: Boolean) = prefs.edit().putBoolean("monet", v).apply()
    fun loadUseMonet(): Boolean  = prefs.getBoolean("monet", false)

    fun saveCustomTheme(d: CustomThemeData) =
        prefs.edit().putString("custom_theme", gson.toJson(d)).apply()
    fun loadCustomTheme(): CustomThemeData = fromJson("custom_theme") ?: CustomThemeData()

    fun saveHistory(h: List<HistoryItem>) =
        prefs.edit().putString("history", gson.toJson(h.take(50))).apply()
    fun loadHistory(): List<HistoryItem> {
        val json = prefs.getString("history", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<HistoryItem>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun saveCategoryViews(m: Map<String, Int>) =
        prefs.edit().putString("catviews", gson.toJson(m)).apply()
    fun loadCategoryViews(): Map<String, Int> {
        val json = prefs.getString("catviews", null) ?: return emptyMap()
        return try {
            gson.fromJson(json, object : TypeToken<Map<String, Int>>() {}.type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
    }

    private inline fun <reified T> fromJson(key: String): T? = try {
        val json = prefs.getString(key, null) ?: return null
        gson.fromJson(json, T::class.java)
    } catch (_: Exception) { null }

    fun saveInstallHistory(h: List<InstallHistoryEntry>) =
        prefs.edit().putString("install_history", gson.toJson(h.takeLast(60))).apply()
    fun loadInstallHistory(): List<InstallHistoryEntry> {
        val json = prefs.getString("install_history", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<InstallHistoryEntry>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun saveIgnoredVersions(v: Set<String>) =
        prefs.edit().putStringSet("ignored_versions", v).apply()
    fun loadIgnoredVersions(): Set<String> =
        prefs.getStringSet("ignored_versions", emptySet()) ?: emptySet()

    fun saveNotifsDismissed(v: Boolean) = prefs.edit().putBoolean("notifs_dismissed", v).apply()
    fun loadNotifsDismissed(): Boolean  = prefs.getBoolean("notifs_dismissed", false)

    fun saveUpdates(updates: List<UpdateInfo>) =
        prefs.edit().putString("cached_updates", gson.toJson(updates)).apply()
    fun loadUpdates(): List<UpdateInfo> {
        val json = prefs.getString("cached_updates", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<UpdateInfo>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun saveFavourites(favs: List<GitHubRepo>) =
        prefs.edit().putString("favourites", gson.toJson(favs)).apply()
    fun loadFavourites(): List<GitHubRepo> {
        val json = prefs.getString("favourites", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<GitHubRepo>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}

// ── Smart APK detection ───────────────────────────────────────────────────────
data class SmartInstallResult(
    val asset     : ReleaseAsset,
    val reason    : String,
    val isOptimal : Boolean = true
)

fun detectBestApk(assets: List<ReleaseAsset>): SmartInstallResult? {
    val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
    if (apks.isEmpty()) return null
    if (apks.size == 1) return SmartInstallResult(apks.first(), "Only available package")

    val deviceAbis = android.os.Build.SUPPORTED_ABIS.toList()
    val abiMap = mapOf(
        "arm64-v8a"   to listOf("arm64-v8a", "arm64", "aarch64"),
        "armeabi-v7a" to listOf("armeabi-v7a", "armeabi", "armv7"),
        "x86_64"      to listOf("x86_64", "x64"),
        "x86"         to listOf("x86", "i686")
    )

    apks.firstOrNull { it.name.contains("universal", ignoreCase = true) }
        ?.let { return SmartInstallResult(it, "Universal — works on all devices") }

    for (abi in deviceAbis) {
        val keywords = abiMap[abi] ?: continue
        for (kw in keywords) {
            apks.firstOrNull { it.name.contains(kw, ignoreCase = true) }
                ?.let { return SmartInstallResult(it, "Optimised for $abi (your device)") }
        }
    }

    val fallback = apks.maxByOrNull { it.size } ?: apks.first()
    return SmartInstallResult(fallback, "Default package", isOptimal = false)
}

// ── Trust Score ───────────────────────────────────────────────────────────────
data class TrustScore(
    val score           : Int,
    val daysSinceUpdate : Int,
    val releaseCount    : Int,
    val forks           : Int,
    val stars           : Int
) {
    val label: String get() = when {
        score >= 85 -> "Highly Trusted"
        score >= 65 -> "Trusted"
        score >= 45 -> "Moderate"
        score >= 25 -> "Low Trust"
        else        -> "Unverified"
    }
    val safeColor: Color get() = when {
        score >= 85 -> Color(0xFF1DB954.toInt())
        score >= 65 -> Color(0xFF4CAF50.toInt())
        score >= 45 -> Color(0xFFFF9800.toInt())
        score >= 25 -> Color(0xFFFF5722.toInt())
        else        -> Color(0xFF9E9E9E.toInt())
    }
}

fun calculateTrustScore(repo: GitHubRepo, releaseCount: Int): TrustScore {
    var score = 0

    score += when {
        repo.stargazers_count >= 10_000 -> 30
        repo.stargazers_count >= 1_000  -> 24
        repo.stargazers_count >= 500    -> 18
        repo.stargazers_count >= 100    -> 12
        repo.stargazers_count >= 10     -> 6
        else                            -> 0
    }

    val days = try {
        val sdf     = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        val updated = sdf.parse(repo.updated_at)?.time ?: 0L
        ((System.currentTimeMillis() - updated) / 86_400_000L).toInt()
    } catch (_: Exception) { 999 }
    score += when {
        days < 7   -> 25
        days < 30  -> 20
        days < 90  -> 14
        days < 180 -> 8
        days < 365 -> 3
        else       -> 0
    }

    score += when {
        repo.forks_count >= 1_000 -> 20
        repo.forks_count >= 200   -> 15
        repo.forks_count >= 50    -> 10
        repo.forks_count >= 10    -> 5
        repo.forks_count >= 1     -> 2
        else                      -> 0
    }

    score += when {
        releaseCount >= 10 -> 20
        releaseCount >= 5  -> 14
        releaseCount >= 2  -> 9
        releaseCount >= 1  -> 5
        else               -> 0
    }

    if (!repo.description.isNullOrEmpty()) score += 5

    return TrustScore(minOf(100, score), days, releaseCount, repo.forks_count, repo.stargazers_count)
}

// ── Curated collections ───────────────────────────────────────────────────────
// Added `subtitle` field — used by CollectionsRow in M3 Expressive tile layout
data class AppCollection(
    val emoji    : String,
    val title    : String,
    val query    : String,
    val subtitle : String = "",
    val iconRes:Int? = null
)

val COLLECTIONS = listOf(
    AppCollection("🔒", "Privacy Essentials",  "topic:android privacy",              "Essential privacy tools", R.drawable.ic_priv),
    AppCollection("🎵", "Best Media Apps",      "topic:android media player",         "The best for your media", R.drawable.ic_med),
    AppCollection("🛠",  "Root & Magisk Tools", "topic:android magisk root",           "Unlock your device", R.drawable.ic_mag),
    AppCollection("📖", "Reading & E-Books",    "topic:android ebook reader",          "Books and reading apps", R.drawable.ic_book),
    AppCollection("🌐", "Browsers",             "topic:android browser privacy",       "Open-source browsers", R.drawable.ic_brow),
    AppCollection("💬", "Messaging",            "topic:android messaging privacy",     "Private messaging apps", R.drawable.ic_mes),
    AppCollection("📸", "Camera & Gallery",     "topic:android camera",                "Camera and photo apps", R.drawable.ic_cam),
    AppCollection("🎮", "Emulators",            "topic:android emulator game",         "Game emulators", R.drawable.ic_emu),
    AppCollection("🔧", "Dev Tools",            "topic:android developer-tools",       "Tools for developers", R.drawable.ic_dev),
    AppCollection("☁️", "Sync & Backup",        "topic:android backup sync",           "Backup your data", R.drawable.ic_syc),
    AppCollection("🌐", "PWA Apps",          "topic:pwa progressive-web-app stars:>100",  "Progressive web apps", R.drawable.ic_pwa),
    AppCollection("🤖", "AI & ML Apps",       "topic:android machine-learning stars:>100", "AI-powered apps",R.drawable.ic_ai),
    AppCollection("🎨", "Customization",       "topic:android launcher theme stars:>50",    "Launchers & themes", R.drawable.ic_cus),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private var loadPage = 1

    fun openCollection(collection: AppCollection) {
        openSeeAll(collection.emoji + " " + collection.title, collection.query)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }

    private fun fuzzyMatch(text: String, query: String): Boolean {
        val t       = text.lowercase()
        val q       = query.lowercase()
        if (t.contains(q)) return true
        val words   = t.split(" ", "-", "_", ".")
        val allowed = maxOf(1, q.length / 4)
        return words.any { levenshtein(it, q) <= allowed } ||
                levenshtein(t.take(q.length + 2), q) <= allowed
    }
    fun toggleFilterMenu(isOpen: Boolean) {
        state = state.copy(isFilterMenuOpen = isOpen)
    }

    fun setSubMenuPlatform(platform: AppPlatform?) {
        state = state.copy(activeSubMenuPlatform = platform)
    }

    fun setSourceFilter(source: AppSource?) {
        state = state.copy(selectedSource = source)
    }

    fun openSourceBrowse(source: AppSource) {
        val cdnKey = when (source) {
            AppSource.FDROID   -> "fdroid"
            AppSource.GITLAB   -> "gitlab"
            AppSource.CODEBERG -> "codeberg"
            AppSource.FLATHUB  -> "flathub"
            AppSource.WINGET   -> "winget"
            AppSource.GITHUB   -> "github"
            AppSource.IZZY     -> "izzy"
            else               -> null
        }
        if (cdnKey != null) {
            // Show whatever we already loaded on the home screen instantly
            val preloaded = when (source) {
                AppSource.FDROID   -> state.fdroidApps
                AppSource.GITLAB   -> state.gitlabApps
                AppSource.CODEBERG -> state.codebergApps
                AppSource.FLATHUB  -> state.flathubApps
                AppSource.WINGET   -> state.wingetApps
                AppSource.IZZY     -> state.izzyApps
                AppSource.GITHUB   -> (state.trending + state.media + state.tools + state.games +
                                       state.browsers + state.productivity + state.security + state.devtools +
                                       state.photoVideo + state.music + state.finance + state.education +
                                       state.fitness + state.artDesign + state.news + state.social +
                                       state.cloudStorage + state.cooking).distinctBy { it.id }
                else               -> emptyList()
            }
            state = state.copy(
                seeAllTitle     = "${source.label} Apps",
                seeAllApps      = preloaded,
                seeAllQuery     = "",
                seeAllPage      = 1,
                seeAllSource    = cdnKey,
                isLoadingSeeAll = true
            )
            viewModelScope.launch {
                val mutex = Mutex()
                val seen  = preloaded.map { it.id }.toHashSet()

                suspend fun mergeSource(apps: List<GitHubRepo>) {
                    val fresh = apps.filter { seen.add(it.id) }
                    if (fresh.isEmpty()) return
                    state = state.copy(
                        seeAllApps      = (state.seeAllApps + fresh).sortedByDescending { it.stargazers_count },
                        isLoadingSeeAll = false
                    )
                }

                // CDN and live API fire simultaneously; first to return updates the UI
                val j1 = launch {
                    try {
                        val result = MetadataManager.get().browseSource(cdnKey, 1)
                        mutex.withLock { mergeSource(result.apps.map { it.toGitHubRepo() }) }
                    } catch (_: Exception) {}
                }
                val j2 = when (source) {
                    AppSource.GITLAB -> launch {
                        try {
                            val projects = GitLabClient.service.searchProjects(perPage = 30)
                            mutex.withLock { mergeSource(projects.map { it.toUnifiedRepo() }) }
                        } catch (_: Exception) {}
                    }
                    AppSource.CODEBERG -> launch {
                        try {
                            val repos = CodebergClient.service.searchRepos(query = "android", topic = true, limit = 30).data
                            mutex.withLock { mergeSource(repos.map { it.toUnifiedRepo() }) }
                        } catch (_: Exception) {}
                    }
                    // Flathub: live API as fallback
                    AppSource.FLATHUB -> launch {
                        try {
                            val apps = FlathubClient.service.getPopular().hits
                                ?.map { it.toUnifiedRepo() } ?: emptyList()
                            mutex.withLock { mergeSource(apps) }
                        } catch (_: Exception) {
                            // last-ditch: filter global CDN index
                            try {
                                val hits = MetadataManager.get().search("", source = "flathub")
                                    .take(50).map { it.toGitHubRepo() }
                                mutex.withLock { mergeSource(hits) }
                            } catch (_: Exception) {}
                        }
                    }
                    // Winget/F-Droid: global CDN index filtered by source
                    AppSource.WINGET, AppSource.FDROID -> launch {
                        try {
                            val hits = MetadataManager.get().search("", source = cdnKey)
                                .take(50).map { it.toGitHubRepo() }
                            mutex.withLock { mergeSource(hits) }
                        } catch (_: Exception) {}
                    }
                    // GitHub: live API fallback (uses user's token for 5000/hr)
                    AppSource.GITHUB -> launch {
                        try {
                            val items = RetrofitClient.service.searchRepos(
                                "topic:android stars:>100", perPage = 30
                            ).items.map { it.copy(source = AppSource.GITHUB) }
                            mutex.withLock { mergeSource(items) }
                        } catch (_: Exception) {}
                    }
                    // IzzyOnDroid: stream directly from F-Droid index (CDN fallback)
                    AppSource.IZZY -> launch {
                        try {
                            val apps = IzzyOnDroidClient.getApps(100)
                            mutex.withLock { mergeSource(apps) }
                        } catch (_: Exception) {}
                    }
                    else -> null
                }
                j1.join(); j2?.join()
                // Guarantee spinner always stops and falls back to preloaded if nothing arrived
                state = state.copy(
                    seeAllApps      = if (state.seeAllApps.isEmpty()) preloaded else state.seeAllApps,
                    isLoadingSeeAll = false
                )
            }
        } else {
            openSeeAll(source.label, "topic:android stars:>50")
        }
    }

    private fun loadMoreCdnSource(cdnKey: String) {
        if (state.isLoadingSeeAll) return
        val next = state.seeAllPage + 1
        viewModelScope.launch {
            state = state.copy(isLoadingSeeAll = true)
            try {
                val result = MetadataManager.get().browseSource(cdnKey, next)
                state = state.copy(
                    seeAllApps      = state.seeAllApps + result.apps.map { it.toGitHubRepo() },
                    seeAllPage      = next,
                    isLoadingSeeAll = false
                )
            } catch (_: Exception) {
                state = state.copy(isLoadingSeeAll = false)
            }
        }
    }

    fun toggleFavourite(repo: GitHubRepo) {
        val fav    = state.favourites
        val newFav = if (fav.any { it.id == repo.id }) fav.filter { it.id != repo.id }
        else listOf(repo) + fav
        state = state.copy(favourites = newFav)
        prefs.saveFavourites(newFav)
    }

    fun setGithubUsername(name: String) { state = state.copy(githubUsername = name) }

    private val ctx   = app.applicationContext
    private val prefs = PreferencesManager(ctx)
    private val downloadJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()
    private var searchJob  : kotlinx.coroutines.Job? = null
    private var loadJob    : kotlinx.coroutines.Job? = null
    private var platformJob: kotlinx.coroutines.Job? = null

    var state by mutableStateOf(UiState())
        private set

    init {
        val savedSettings = prefs.loadSettings()
        RetrofitClient.authToken = savedSettings.githubToken
        MetadataManager.init(ctx)
        state = state.copy(
            settings           = savedSettings,
            profile            = prefs.loadProfile(),
            history            = prefs.loadHistory(),
            favourites         = prefs.loadFavourites(),
            accentColor        = prefs.loadAccentColor(),
            useMonet           = prefs.loadUseMonet(),
            themeName          = prefs.loadTheme(),
            customTheme        = prefs.loadCustomTheme(),
            categoryViewCounts = prefs.loadCategoryViews(),
            installHistory     = prefs.loadInstallHistory(),
            ignoredVersions    = prefs.loadIgnoredVersions(),
            updates            = prefs.loadUpdates(),
            notifsDismissed    = prefs.loadNotifsDismissed(),
            platform           = prefs.loadSearchPlatform(),
            selectedSubCategories = prefs.loadSearchSubCategories()
        )
        viewModelScope.launch {
            try { MetadataManager.get().init() } catch (_: Exception) {}
        }
        reconstructInstallStatesFromHistory()
        loadAll()
        checkSelfUpdate()
    }

    private suspend fun <T> safeApi(block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        null
    }

    private val cache          = java.util.concurrent.ConcurrentHashMap<String, Pair<List<GitHubRepo>, Long>>()
    private val CACHE_MS       = 15 * 60 * 1000L   // 15 min: in-memory TTL (pull-to-refresh stays fresh)
    private val DISK_CACHE_MS  = 90 * 60 * 1000L   // 90 min: disk TTL (cold restarts skip API calls)
    private val searchCachePrefs = ctx.getSharedPreferences("gh_search_cache", Context.MODE_PRIVATE)
    private val cacheGson = Gson()

    init {
        // Warm the in-memory cache from disk so cold restarts skip the 19 GitHub API calls.
        val type: java.lang.reflect.Type = object : TypeToken<List<GitHubRepo>>() {}.type
        searchCachePrefs.all.keys
            .filter { !it.endsWith("_ts") }
            .forEach { key ->
                val ts = searchCachePrefs.getLong("${key}_ts", 0L)
                if (System.currentTimeMillis() - ts < DISK_CACHE_MS) {
                    val json = searchCachePrefs.getString(key, null) ?: return@forEach
                    try { cache[key] = cacheGson.fromJson<List<GitHubRepo>>(json, type) to ts }
                    catch (_: Exception) {}
                }
            }
    }

    private suspend fun searchCached(
        query   : String,
        perPage : Int = 20,
        page    : Int = 1
    ): SearchResponse? {
        val key    = "$query|$page|$perPage"
        val cached = cache[key]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_MS)
            return SearchResponse(cached.first)
        val result    = safeApi { RetrofitClient.service.searchRepos(query, perPage = perPage, page = page) }
            ?: return null
        val safeItems = try { result.items } catch (_: Throwable) { null } ?: emptyList()
        val now       = System.currentTimeMillis()
        cache[key]    = safeItems to now
        // Persist to disk so the next cold start skips this API call
        searchCachePrefs.edit()
            .putString(key, cacheGson.toJson(safeItems))
            .putLong("${key}_ts", now)
            .apply()
        return SearchResponse(safeItems)
    }

    fun loadAll() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(80)
            loadPage = (loadPage % 8) + 1
            state = state.copy(isLoading = true, error = null, trendingPage = 1, refreshToken = state.refreshToken + 1)

            // ── Phase 1: CDN sources — GitHub awaited, others fire-and-forget ──
            // Non-GitHub sources update state as they arrive; only GitHub blocks
            // because home-screen category rows depend on it.
            // IzzyOnDroid live fetch is intentionally excluded here — it downloads
            // a large JSON and is only loaded when the user opens Browse → Izzy.
            try {
                val c = MetadataManager.get()

                suspend fun cdnSource(key: String): List<GitHubRepo> =
                    try { c.browseSource(key, 1).apps.map { it.toGitHubRepo() } }
                    catch (_: Exception) { emptyList() }

                // Non-critical sources: update state independently as each finishes
                launch { val gl = cdnSource("gitlab"); if (isActive && gl.isNotEmpty()) state = state.copy(gitlabApps = gl) }
                launch { val cb = cdnSource("codeberg"); if (isActive && cb.isNotEmpty()) state = state.copy(codebergApps = cb) }
                launch { val fd = cdnSource("fdroid"); if (isActive && fd.isNotEmpty()) state = state.copy(fdroidApps = fd) }
                launch { val fh = cdnSource("flathub"); if (isActive && fh.isNotEmpty()) state = state.copy(flathubApps = fh) }
                launch { val wg = cdnSource("winget"); if (isActive && wg.isNotEmpty()) state = state.copy(wingetApps = wg) }
                launch { val iz = cdnSource("izzy"); if (isActive && iz.isNotEmpty()) state = state.copy(izzyApps = iz) }

                // GitHub is awaited — home screen category rows depend on it
                val ghEntries = try { c.browseSource("github", 1).apps }
                                catch (_: Exception) { emptyList<com.vythera.omniapps.api.AppEntry>() }
                val ghList = ghEntries.ifEmpty {
                    try { c.search("", source = "github").take(200) } catch (_: Exception) { emptyList() }
                }

                if (!isActive) { state = state.copy(isLoading = false); return@launch }

                fun List<com.vythera.omniapps.api.AppEntry>.toRows(vararg kws: String): List<GitHubRepo> {
                    val matched = filter { e ->
                        kws.any { k ->
                            e.categories.any { cat -> k in cat.lowercase() } ||
                            k in e.summary.lowercase() || k in e.name.lowercase()
                        }
                    }
                    return (if (matched.isNotEmpty()) matched else this).shuffled().take(20).map { it.toGitHubRepo() }
                }

                state = if (ghList.isNotEmpty()) {
                    state.copy(
                        isLoading    = false,
                        trending     = ghList.shuffled().take(20).map { it.toGitHubRepo() },
                        media        = ghList.toRows("media", "player", "video", "stream"),
                        tools        = ghList.toRows("tool", "utility", "manager"),
                        games        = ghList.toRows("game", "emulat"),
                        browsers     = ghList.toRows("browser"),
                        productivity = ghList.toRows("productivity", "note", "office", "task"),
                        security     = ghList.toRows("security", "privacy", "encrypt"),
                        devtools     = ghList.toRows("developer", "dev-tool", "terminal", "ssh"),
                        photoVideo   = ghList.toRows("photo", "camera", "gallery", "image"),
                        music        = ghList.toRows("music", "podcast", "radio"),
                        finance      = ghList.toRows("finance", "banking", "budget", "money"),
                        education    = ghList.toRows("education", "learn", "study", "language"),
                        fitness      = ghList.toRows("fitness", "health", "workout", "exercise"),
                        artDesign    = ghList.toRows("art", "design", "draw", "creative"),
                        news         = ghList.toRows("news", "rss", "feed", "reader"),
                        social       = ghList.toRows("social", "messag", "chat", "network"),
                        cloudStorage = ghList.toRows("cloud", "storage", "backup", "sync"),
                        cooking      = ghList.toRows("cooking", "food", "recipe")
                    )
                } else {
                    state.copy(isLoading = false)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false)
                if (e is kotlinx.coroutines.CancellationException) throw e
            }

            updateRecommendations()

            // ── Phase 2: Live API fallback for GitHub category rows ───────────
            // Only fires when CDN github source returned no data.
            if (state.trending.isEmpty() && isActive) {
                // Batch 1: primary rows
                try {
                    val tD = async { searchCached("topic:android apk stars:>100",          perPage = 20, page = loadPage) }
                    val mD = async { searchCached("topic:android media player stars:>50",  perPage = 20, page = loadPage) }
                    val uD = async { searchCached("topic:android utility tool stars:>50",  perPage = 20, page = loadPage) }
                    val gD = async { searchCached("topic:android game emulator stars:>50", perPage = 20, page = loadPage) }
                    val t = tD.await(); val m = mD.await(); val u = uD.await(); val g = gD.await()
                    if (!isActive) return@launch
                    state = state.copy(
                        trending = t?.items?.shuffled() ?: emptyList(),
                        media    = m?.items?.shuffled() ?: emptyList(),
                        tools    = u?.items?.shuffled() ?: emptyList(),
                        games    = g?.items?.shuffled() ?: emptyList()
                    )
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }

                updateRecommendations()

                // Batch 2: secondary categories
                if (!isActive) return@launch
                try {
                    val brD = async { searchCached("topic:android browser privacy stars:>50",    perPage = 20, page = loadPage) }
                    val prD = async { searchCached("topic:android productivity notes stars:>50", perPage = 20, page = loadPage) }
                    val seD = async { searchCached("topic:android security stars:>50",           perPage = 20, page = loadPage) }
                    val deD = async { searchCached("topic:android developer-tools stars:>50",    perPage = 20, page = loadPage) }
                    val br = brD.await(); val pr = prD.await(); val se = seD.await(); val de = deD.await()
                    if (!isActive) return@launch
                    state = state.copy(
                        browsers     = br?.items?.shuffled() ?: emptyList(),
                        productivity = pr?.items?.shuffled() ?: emptyList(),
                        security     = se?.items?.shuffled() ?: emptyList(),
                        devtools     = de?.items?.shuffled() ?: emptyList()
                    )
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }

                // Batch 3: entertainment / lifestyle
                if (!isActive) return@launch
                try {
                    val pvD = async { searchCached("topic:android photo video editor stars:>100", perPage = 20, page = loadPage) }
                    val muD = async { searchCached("topic:android music audio stars:>100",        perPage = 20, page = loadPage) }
                    val fiD = async { searchCached("topic:android finance banking stars:>100",    perPage = 20, page = loadPage) }
                    val edD = async { searchCached("topic:android education learning stars:>100", perPage = 20, page = loadPage) }
                    val pv = pvD.await(); val mu = muD.await(); val fi = fiD.await(); val ed = edD.await()
                    if (!isActive) return@launch
                    state = state.copy(
                        photoVideo = pv?.items?.shuffled() ?: emptyList(),
                        music      = mu?.items?.shuffled() ?: emptyList(),
                        finance    = fi?.items?.shuffled() ?: emptyList(),
                        education  = ed?.items?.shuffled() ?: emptyList()
                    )
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }

                // Batch 4: remaining rows
                if (!isActive) return@launch
                try {
                    val ftD = async { searchCached("topic:android fitness health workout stars:>100", perPage = 20, page = loadPage) }
                    val arD = async { searchCached("topic:android art design creative stars:>100",   perPage = 20, page = loadPage) }
                    val nwD = async { searchCached("topic:android news reader stars:>100",           perPage = 20, page = loadPage) }
                    val scD = async { searchCached("topic:android social network stars:>100",        perPage = 20, page = loadPage) }
                    val csD = async { searchCached("topic:android cloud storage files stars:>100",   perPage = 20, page = loadPage) }
                    val ckD = async { searchCached("topic:android cooking food recipe stars:>50",    perPage = 20, page = loadPage) }
                    val ft = ftD.await(); val ar = arD.await(); val nw = nwD.await()
                    val sc = scD.await(); val cs = csD.await(); val ck = ckD.await()
                    if (!isActive) return@launch
                    state = state.copy(
                        fitness      = ft?.items?.shuffled() ?: emptyList(),
                        artDesign    = ar?.items?.shuffled() ?: emptyList(),
                        news         = nw?.items?.shuffled() ?: emptyList(),
                        social       = sc?.items?.shuffled() ?: emptyList(),
                        cloudStorage = cs?.items?.shuffled() ?: emptyList(),
                        cooking      = ck?.items?.shuffled() ?: emptyList()
                    )
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            }

            updateRecommendations()
            checkForUpdatesNow()
        }
    }

    private fun updateRecommendations() {
        val counts      = state.categoryViewCounts
        val categoryMap = mapOf(
            "trending"    to state.trending,    "media"    to state.media,
            "tools"       to state.tools,       "games"    to state.games,
            "browsers"    to state.browsers,    "productivity" to state.productivity,
            "photo"       to state.photoVideo,  "music"    to state.music,
            "finance"     to state.finance,     "education" to state.education,
            "fitness"     to state.fitness,     "artDesign" to state.artDesign
        )
        val recs = if (counts.isEmpty()) {
            (state.trending + state.media).shuffled().take(20)
        } else {
            counts.entries.sortedByDescending { it.value }.take(4)
                .flatMap { categoryMap[it.key]?.take(8) ?: emptyList() }
                .distinctBy { it.id }.shuffled().take(20)
        }
        state = state.copy(recommendations = recs)
    }

    fun trackCategory(category: String) {
        val counts = state.categoryViewCounts.toMutableMap()
        counts[category] = (counts[category] ?: 0) + 1
        state = state.copy(categoryViewCounts = counts)
        prefs.saveCategoryViews(counts)
        updateRecommendations()
    }

    fun loadMoreTrending() {
        if (state.isLoadingMore) return
        val next = state.trendingPage + 1
        viewModelScope.launch {
            state = state.copy(isLoadingMore = true)
            try {
                val more = RetrofitClient.service.searchRepos("topic:android apk stars:>500", perPage = 20, page = next)
                state = state.copy(trending = state.trending + more.items, trendingPage = next, isLoadingMore = false)
            } catch (_: Exception) { state = state.copy(isLoadingMore = false) }
        }
    }

    fun setPlatform(p: AppPlatform) {
        val platformChanged = p != state.platform
        val newSubs = if (platformChanged) emptySet() else state.selectedSubCategories
        state = state.copy(platform = p, selectedSubCategories = newSubs)
        prefs.saveSearchPlatform(p)
        if (platformChanged) prefs.saveSearchSubCategories(emptySet())
        if (p != AppPlatform.ALL) loadPlatformApps(p)
        onSearch(state.searchQuery)
    }

    fun clearSearchFilter() {
        state = state.copy(platform = AppPlatform.ALL, selectedSubCategories = emptySet())
        prefs.saveSearchPlatform(AppPlatform.ALL)
        prefs.saveSearchSubCategories(emptySet())
        onSearch(state.searchQuery)
    }

    fun toggleSubCategory(sub: String) {
        val current = state.selectedSubCategories
        val updated = if (current.contains(sub)) current - sub else current + sub
        state = state.copy(selectedSubCategories = updated)
        prefs.saveSearchSubCategories(updated)
        onSearch(state.searchQuery)
    }

    private fun loadPlatformApps(platform: AppPlatform) {
        platformJob?.cancel()
        platformJob = viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, platformApps = emptyList())
            val q = when (platform) {
                AppPlatform.ANDROID -> "topic:android apk stars:>200"
                AppPlatform.WINDOWS -> "topic:windows stars:>200"
                AppPlatform.LINUX   -> "topic:linux stars:>200"
                AppPlatform.TV      -> "topic:android-tv stars:>30"
                AppPlatform.IOS     -> "topic:ios stars:>30"
                else                -> return@launch
            }
            try {
                val r = RetrofitClient.service.searchRepos(q, perPage = 30)
                state = state.copy(platformApps = r.items, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error     = "Could not load ${platform.label} apps. Check your connection."
                )
            }
        }
    }

    fun onSearch(q: String) {
        state = state.copy(searchQuery = q)
        if (q.isBlank() && state.selectedSubCategories.isEmpty() && state.platform == AppPlatform.ALL) {
            state = state.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        val platformQuery = when (state.platform) {
            AppPlatform.ANDROID -> "topic:android"
            AppPlatform.WINDOWS -> "topic:windows"
            AppPlatform.LINUX   -> "topic:linux"
            AppPlatform.TV      -> "topic:android-tv"
            AppPlatform.IOS     -> "topic:ios"
            else                -> ""
        }
        val subQueries = state.selectedSubCategories.joinToString(" ") { "topic:${it.lowercase().replace(" ", "-")}" }
        val finalQuery = listOf(q, platformQuery, subQueries).filter { it.isNotBlank() }.joinToString(" ")

        val allLoaded = (state.trending + state.media + state.tools + state.games +
                state.browsers + state.productivity + state.security + state.devtools +
                state.photoVideo + state.music + state.finance + state.education +
                state.fitness + state.artDesign + state.news + state.social +
                state.cloudStorage + state.cooking +
                state.gitlabApps + state.codebergApps +
                state.fdroidApps + state.izzyApps + state.flathubApps + state.wingetApps
        ).distinctBy { it.id }

        // Normalize: GitHub repos loaded from the API have source=null (Gson ignores Kotlin defaults)
        val allNormalized = allLoaded.map { if (it.source == null) it.copy(source = AppSource.GITHUB) else it }

        val localMatches = if (q.isNotBlank()) {
            val qt = q.trim().lowercase()
            allNormalized.filter { repo ->
                fuzzyMatch(repo.name, qt) ||
                        fuzzyMatch(repo.owner.login, qt) ||
                        (!repo.description.isNullOrEmpty() && fuzzyMatch(repo.description, qt))
            }.sortedByDescending { it.stargazers_count }
        } else {
            // No text — show loaded apps that match the active platform filter
            val platformSources: Set<AppSource>? = when (state.platform) {
                AppPlatform.ANDROID -> setOf(AppSource.GITHUB, AppSource.IZZY, AppSource.FDROID)
                AppPlatform.WINDOWS -> setOf(AppSource.WINGET)
                AppPlatform.LINUX   -> setOf(AppSource.FLATHUB, AppSource.CODEBERG, AppSource.GITLAB)
                AppPlatform.TV      -> setOf(AppSource.GITHUB, AppSource.IZZY)
                AppPlatform.IOS     -> emptySet()
                else                -> null   // ALL with no text → keep empty (default state)
            }
            if (platformSources != null)
                allNormalized.filter { it.source in platformSources }
                             .sortedByDescending { it.stargazers_count }
            else emptyList()
        }

        state = state.copy(searchResults = localMatches, isSearching = q.isNotBlank())

        if (q.isBlank()) return  // live API only fires with text; platform filter uses local data above

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(120)
            val mutex = Mutex()
            val seen  = localMatches.map { it.id }.toHashSet()

            // Merges new results into state; must be called inside mutex.withLock
            suspend fun merge(newItems: List<GitHubRepo>) {
                val fresh = newItems.filter { seen.add(it.id) }
                if (fresh.isEmpty()) return
                state = state.copy(
                    searchResults = (state.searchResults + fresh).sortedByDescending { it.stargazers_count }
                )
            }

            val qt = q.trim()

            // All searches fire simultaneously; each updates the UI as it finishes
            val j1 = launch {
                safeApi { RetrofitClient.service.searchRepos(finalQuery, perPage = 20) }
                    ?.items?.map { it.copy(source = AppSource.GITHUB) }
                    ?.let { mutex.withLock { merge(it) } }
            }
            val j2 = launch {
                safeApi { RetrofitClient.service.searchRepos("$finalQuery in:name", perPage = 10) }
                    ?.items?.map { it.copy(source = AppSource.GITHUB) }
                    ?.let { mutex.withLock { merge(it) } }
            }
            val j3 = launch {
                safeApi { GitLabClient.service.searchProjects(query = qt, perPage = 15) }
                    ?.map { it.toUnifiedRepo() }
                    ?.let { mutex.withLock { merge(it) } }
            }
            val j4 = launch {
                safeApi { CodebergClient.service.searchRepos(query = qt, limit = 15) }
                    ?.data?.map { it.toUnifiedRepo() }
                    ?.let { mutex.withLock { merge(it) } }
            }
            val j5 = launch {
                // F-Droid / Winget / GitLab / Codeberg from CDN index
                try {
                    val cdnHits = MetadataManager.get().search(qt).take(20).map { it.toGitHubRepo() }
                    mutex.withLock { merge(cdnHits) }
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            }
            val j6 = launch {
                // Flathub live search (not in CDN index)
                try {
                    val hits = FlathubClient.service.search(FlathubSearchBody(qt)).hits?.map { it.toUnifiedRepo() } ?: emptyList()
                    mutex.withLock { merge(hits) }
                } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e }
            }

            j1.join(); j2.join(); j3.join(); j4.join(); j5.join(); j6.join()
            state = state.copy(isSearching = false)
        }
    }

    fun openSeeAll(title: String, query: String) {
        state = state.copy(
            seeAllTitle     = title,
            seeAllQuery     = query,
            seeAllApps      = emptyList(),
            seeAllPage      = 1,
            seeAllSource    = null,
            isLoadingSeeAll = true
        )
        viewModelScope.launch {
            try {
                val r = RetrofitClient.service.searchRepos(query, perPage = 30, page = 1)
                state = state.copy(seeAllApps = r.items, isLoadingSeeAll = false)
            } catch (e: Exception) {
                state = state.copy(isLoadingSeeAll = false)
            }
        }
    }

    fun loadMoreSeeAll() {
        val cdnKey = state.seeAllSource
        if (cdnKey != null) { loadMoreCdnSource(cdnKey); return }
        if (state.isLoadingSeeAll || state.seeAllQuery.isEmpty()) return
        val next = state.seeAllPage + 1
        viewModelScope.launch {
            state = state.copy(isLoadingSeeAll = true)
            try {
                val r = RetrofitClient.service.searchRepos(state.seeAllQuery, perPage = 30, page = next)
                state = state.copy(
                    seeAllApps      = state.seeAllApps + r.items,
                    seeAllPage      = next,
                    isLoadingSeeAll = false
                )
            } catch (_: Exception) {
                state = state.copy(isLoadingSeeAll = false)
            }
        }
    }

    fun updateProfile(p: UserProfile) { state = state.copy(profile = p); prefs.saveProfile(p) }

    fun addToHistory(repo: GitHubRepo) {
        val filtered = state.history.filter { it.repo.id != repo.id }
        val newH     = listOf(HistoryItem(repo)) + filtered
        state = state.copy(history = newH); prefs.saveHistory(newH)
    }
    fun clearHistory() { state = state.copy(history = emptyList()); prefs.saveHistory(emptyList()) }

    fun setTheme(t: ThemeName)          { state = state.copy(themeName = t); prefs.saveTheme(t) }
    fun setAccentColor(c: Color?)       { state = state.copy(accentColor = c, useMonet = false); prefs.saveAccentColor(c); prefs.saveUseMonet(false) }
    fun setUseMonet(v: Boolean)         { state = state.copy(useMonet = v, accentColor = null); prefs.saveUseMonet(v); prefs.saveAccentColor(null) }
    fun setCustomTheme(d: CustomThemeData) { state = state.copy(customTheme = d); prefs.saveCustomTheme(d) }

    private fun allLoadedRepos() = (state.trending + state.media + state.tools + state.games +
        state.browsers + state.productivity + state.security + state.devtools +
        state.photoVideo + state.music + state.finance + state.education +
        state.fitness + state.artDesign + state.news + state.social +
        state.cloudStorage + state.cooking + state.gitlabApps + state.codebergApps +
        state.fdroidApps + state.izzyApps + state.flathubApps + state.wingetApps
    ).distinctBy { it.id }

    private fun reconstructInstallStatesFromHistory() {
        viewModelScope.launch {
            val pm      = ctx.packageManager
            val history = state.installHistory
            if (history.isEmpty()) return@launch
            // Keep only the latest install per repo
            val latest  = history.groupBy { it.repoId }.mapValues { (_, v) -> v.maxByOrNull { it.installedAt }!! }
            val newStates = state.installStates.toMutableMap()
            for ((repoId, entry) in latest) {
                if (newStates[repoId]?.repo != null) continue  // already have full data
                val isInstalled = if (entry.packageName.isNotEmpty()) {
                    try { pm.getPackageInfo(entry.packageName, 0); true }
                    catch (_: PackageManager.NameNotFoundException) { false }
                } else false
                val minRepo = GitHubRepo(
                    id        = repoId,
                    name      = entry.repoName,
                    full_name = "${entry.ownerLogin}/${entry.repoName}",
                    owner     = RepoOwner(login = entry.ownerLogin)
                )
                newStates[repoId] = (newStates[repoId] ?: InstallState()).copy(
                    isInstalled = isInstalled,
                    packageName = entry.packageName.takeIf { it.isNotEmpty() },
                    repo        = minRepo
                )
            }
            if (newStates.isNotEmpty()) state = state.copy(installStates = newStates)
        }
    }

    fun updateSettings(s: AppSettings)  {
        val languageChanged = s.language != state.settings.language
        state = state.copy(settings = s)
        if (languageChanged) state = state.copy(translatedDescriptions = emptyMap())
        RetrofitClient.authToken = s.githubToken
        prefs.saveSettings(s)
    }

    fun fetchRelease(repo: GitHubRepo) {
        if (state.installStates[repo.id]?.release != null) return
        viewModelScope.launch {
            // CDN-backed sources: build a synthetic release from CDN metadata instead of hitting GitHub API
            val isCdnSource = repo.source != null &&
                    repo.source != AppSource.GITHUB &&
                    repo.source != AppSource.IZZY
            if (isCdnSource) {
                val hasApk = repo.apkUrl.isNotBlank()
                val syntheticRelease = Release(
                    tag_name     = repo.cdnVersion.ifBlank { "Latest" },
                    name         = repo.name,
                    assets       = if (hasApk) listOf(ReleaseAsset(
                        name                 = "${repo.name}.apk",
                        browser_download_url = repo.apkUrl,
                        size                 = 0L,
                        content_type         = "application/vnd.android.package-archive"
                    )) else emptyList(),
                    published_at = repo.updated_at,
                    body         = ""
                )
                updateInstall(repo.id) {
                    copy(
                        isLoadingRelease = false,
                        release          = syntheticRelease,
                        apkAsset         = if (hasApk) syntheticRelease.assets.first() else null,
                        error            = null
                    )
                }
                return@launch
            }

            // GitHub / IzzyOnDroid: try CDN-cached releases first, fall back to direct API
            updateInstall(repo.id) { copy(isLoadingRelease = true, error = null) }
            try {
                var foundRelease : Release?      = null
                var foundApk     : ReleaseAsset? = null
                var allReleases  : List<Release> = emptyList()

                // 1. Try CDN-pre-cached releases (no token needed, populated by fetch_releases.py)
                try {
                    val cdnUrl = "https://smartworldarafath.github.io/appstore-metadata/data/releases/github" +
                            "/${repo.owner.login}/${repo.name}.json"
                    val cdnReleases: List<Release>? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val r = okhttp3.OkHttpClient().newCall(
                            okhttp3.Request.Builder().url(cdnUrl).build()
                        ).execute()
                        try {
                            if (!r.isSuccessful) null
                            else {
                                val body = r.body?.string()
                                if (body.isNullOrBlank()) null
                                else {
                                    val type = object : TypeToken<List<Release>>() {}.type
                                    val parsed: List<Release> = Gson().fromJson(body, type)
                                    parsed.takeIf { it.isNotEmpty() }
                                }
                            }
                        } finally { r.close() }
                    }
                    if (cdnReleases != null) {
                        allReleases  = cdnReleases
                        foundRelease = cdnReleases.first()
                        for (rel in cdnReleases) {
                            val apk = detectBestApk(rel.assets)?.asset
                            if (apk != null) { foundRelease = rel; foundApk = apk; break }
                        }
                    }
                } catch (_: Exception) {}

                // 2. Fall back to direct GitHub API if CDN didn't have this repo pre-cached
                if (foundRelease == null) {
                    try {
                        val latest   = RetrofitClient.service.getLatestRelease(repo.owner.login, repo.name)
                        foundRelease = latest
                        foundApk     = detectBestApk(latest.assets)?.asset
                    } catch (_: Exception) {}

                    try {
                        allReleases = RetrofitClient.service.getReleases(repo.owner.login, repo.name, 10)
                        if (foundApk == null) {
                            for (r in allReleases) {
                                if (foundRelease == null) foundRelease = r
                                val apk = detectBestApk(r.assets)?.asset
                                if (apk != null) { foundRelease = r; foundApk = apk; break }
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (foundRelease != null) {
                    val smart = detectBestApk(foundRelease.assets)
                    val trust = calculateTrustScore(repo, allReleases.size.coerceAtLeast(1))
                    updateInstall(repo.id) {
                        copy(
                            isLoadingRelease = false,
                            releases         = allReleases,
                            release          = foundRelease,
                            apkAsset         = smart?.asset ?: foundApk ?: foundRelease.assets.firstOrNull(),
                            smartInstall     = smart,
                            trustScore       = trust
                        )
                    }
                } else {
                    updateInstall(repo.id) {
                        copy(isLoadingRelease = false, releases = allReleases, error = "No releases found.")
                    }
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException)
                    updateInstall(repo.id) { copy(isLoadingRelease = false, error = "Could not load release info.") }
            }
        }
    }

    fun downloadAndInstall(repo: GitHubRepo, asset: ReleaseAsset) {
        val job = viewModelScope.launch {
            updateInstall(repo.id) { copy(downloadProgress = 0f, error = null, repo = repo) }
            val outFile = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "${repo.name}_${asset.name}")
            try {
                val dm  = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val req = DownloadManager.Request(Uri.parse(asset.browser_download_url))
                    .setTitle("Installing ${repo.name}").setDescription("Omni Apps")
                    .setDestinationUri(Uri.fromFile(outFile))
                    .setAllowedOverMetered(true).setAllowedOverRoaming(true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                val dlId = dm.enqueue(req)
                updateInstall(repo.id) { copy(downloadId = dlId) }
                while (true) {
                    val cur    = dm.query(DownloadManager.Query().setFilterById(dlId))
                    if (!cur.moveToFirst()) { cur.close(); break }
                    val status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done   = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total  = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cur.close()
                    if (total > 0) updateInstall(repo.id) { copy(downloadProgress = done.toFloat() / total) }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val pkg = try {
                                @Suppress("DEPRECATION")
                                ctx.packageManager.getPackageArchiveInfo(outFile.absolutePath, 0)?.packageName
                            } catch (_: Exception) { null }
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", outFile)
                            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/vnd.android.package-archive")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            updateInstall(repo.id) { copy(downloadProgress = null, downloadId = null, packageName = pkg) }
                            val tag = state.installStates[repo.id]?.release?.tag_name ?: "unknown"
                            recordInstall(repo, tag, outFile.absolutePath, pkg ?: "")
                            if (pkg != null) {
                                viewModelScope.launch {
                                    repeat(30) {
                                        delay(2000)
                                        if (installed(pkg)) { updateInstall(repo.id) { copy(isInstalled = true) }; return@launch }
                                    }
                                }
                            }
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            updateInstall(repo.id) { copy(downloadProgress = null, downloadId = null, error = "Download failed.") }
                            break
                        }
                        else -> delay(400)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                updateInstall(repo.id) { copy(downloadProgress = null, error = "Error: ${e.message}") }
            }
        }
        downloadJobs[repo.id] = job
    }

    fun downloadOnly(repo: GitHubRepo, asset: ReleaseAsset) {
        val job = viewModelScope.launch {
            updateInstall(repo.id) { copy(downloadProgress = 0f, error = null, repo = repo) }
            try {
                val dm  = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val req = DownloadManager.Request(Uri.parse(asset.browser_download_url))
                    .setTitle("Downloading ${repo.name}").setDescription("Saved to Downloads")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, asset.name)
                    .setAllowedOverMetered(true).setAllowedOverRoaming(true)
                val dlId = dm.enqueue(req)
                updateInstall(repo.id) { copy(downloadId = dlId) }
                while (true) {
                    val cur    = dm.query(DownloadManager.Query().setFilterById(dlId))
                    if (!cur.moveToFirst()) { cur.close(); break }
                    val status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done   = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total  = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cur.close()
                    if (total > 0) updateInstall(repo.id) { copy(downloadProgress = done.toFloat() / total) }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> { updateInstall(repo.id) { copy(downloadProgress = null, downloadId = null) }; break }
                        DownloadManager.STATUS_FAILED     -> { updateInstall(repo.id) { copy(downloadProgress = null, downloadId = null, error = "Download failed.") }; break }
                        else -> delay(400)
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                updateInstall(repo.id) { copy(downloadProgress = null, error = "Error: ${e.message}") }
            }
        }
        downloadJobs[repo.id] = job
    }

    fun cancelDownload(repo: GitHubRepo) {
        val dlId = state.installStates[repo.id]?.downloadId
        updateInstall(repo.id) { copy(downloadProgress = null, downloadId = null, error = null) }
        downloadJobs[repo.id]?.cancel()
        downloadJobs.remove(repo.id)
        if (dlId != null) {
            try { (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(dlId) }
            catch (_: Exception) {}
        }
    }

    fun uninstall(repo: GitHubRepo) {
        val installState = state.installStates[repo.id] ?: return
        val pkg          = installState.packageName
        if (pkg.isNullOrEmpty()) {
            val apkFile     = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles()?.firstOrNull { it.name.contains(repo.name, ignoreCase = true) }
            val detectedPkg = apkFile?.let {
                try { ctx.packageManager.getPackageArchiveInfo(it.absolutePath, 0)?.packageName }
                catch (_: Exception) { null }
            }
            if (detectedPkg != null) {
                updateInstall(repo.id) { copy(packageName = detectedPkg) }
                launchUninstall(repo.id, detectedPkg)
            }
            return
        }
        launchUninstall(repo.id, pkg)
    }

    private fun launchUninstall(repoId: Long, pkg: String) {
        try {
            ctx.startActivity(
                android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                    data = android.net.Uri.parse("package:$pkg")
                    putExtra(android.content.Intent.EXTRA_RETURN_RESULT, false)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            viewModelScope.launch {
                repeat(40) {
                    delay(2000)
                    try { ctx.packageManager.getPackageInfo(pkg, 0) }
                    catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                        updateInstall(repoId) { copy(isInstalled = false) }
                        return@launch
                    }
                }
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                updateInstall(repoId) { copy(error = "Could not launch uninstaller: ${e.message}") }
            }
        }
    }

    fun refreshInstall(id: Long) {
        val pkg = state.installStates[id]?.packageName ?: return
        updateInstall(id) { copy(isInstalled = installed(pkg)) }
    }

    fun translateDescription(repo: GitHubRepo) {
        val desc = repo.description ?: return
        val lang = when (state.settings.language) {
            "English"    -> "en"
            "Hindi"      -> "hi"
            "Spanish"    -> "es"
            "French"     -> "fr"
            "German"     -> "de"
            "Japanese"   -> "ja"
            "Portuguese" -> "pt"
            "Italian"    -> "it"
            "Russian"    -> "ru"
            "Chinese"    -> "zh"
            "Korean"     -> "ko"
            "Arabic"     -> "ar"
            "Dutch"      -> "nl"
            "Turkish"    -> "tr"
            "Polish"     -> "pl"
            "Swedish"    -> "sv"
            else         -> "en"
        }
        viewModelScope.launch {
            state = state.copy(isTranslating = state.isTranslating + (repo.id to true))
            try {
                val r = GTranslateClient.service.translate("gtx", "auto", lang, "t", desc)
                val translated = r[0].asJsonArray.joinToString("") { chunk ->
                    runCatching { chunk.asJsonArray[0].asString }.getOrDefault("")
                }
                state = state.copy(
                    translatedDescriptions = state.translatedDescriptions + (repo.id to translated),
                    isTranslating          = state.isTranslating + (repo.id to false)
                )
            } catch (_: Exception) {
                state = state.copy(isTranslating = state.isTranslating + (repo.id to false))
            }
        }
    }

    fun translateReleaseBody(repo: GitHubRepo) {
        val body = state.installStates[repo.id]?.release?.body?.takeIf { it.isNotBlank() } ?: return
        val lang = when (state.settings.language) {
            "English"    -> "en"
            "Hindi"      -> "hi"
            "Spanish"    -> "es"
            "French"     -> "fr"
            "German"     -> "de"
            "Japanese"   -> "ja"
            "Portuguese" -> "pt"
            "Italian"    -> "it"
            "Russian"    -> "ru"
            "Chinese"    -> "zh"
            "Korean"     -> "ko"
            "Arabic"     -> "ar"
            "Dutch"      -> "nl"
            "Turkish"    -> "tr"
            "Polish"     -> "pl"
            "Swedish"    -> "sv"
            else         -> "en"
        }
        viewModelScope.launch {
            state = state.copy(isTranslatingRelease = state.isTranslatingRelease + (repo.id to true))
            try {
                val r = GTranslateClient.service.translate("gtx", "auto", lang, "t", body.take(500))
                val translated = r[0].asJsonArray.joinToString("") { chunk ->
                    runCatching { chunk.asJsonArray[0].asString }.getOrDefault("")
                }
                state = state.copy(
                    translatedReleaseBodies = state.translatedReleaseBodies + (repo.id to translated),
                    isTranslatingRelease    = state.isTranslatingRelease + (repo.id to false)
                )
            } catch (_: Exception) {
                state = state.copy(isTranslatingRelease = state.isTranslatingRelease + (repo.id to false))
            }
        }
    }

    fun clearNotifications() {
        prefs.saveNotifsDismissed(true)
        state = state.copy(notifsDismissed = true)
    }

    fun selectRelease(repoId: Long, release: Release) {
        val bestApk = detectBestApk(release.assets)?.asset
            ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: release.assets.firstOrNull()
        updateInstall(repoId) { copy(release = release, apkAsset = bestApk) }
    }

    fun selectAsset(repoId: Long, asset: ReleaseAsset) {
        updateInstall(repoId) { copy(apkAsset = asset) }
    }

    private fun installed(pkg: String) = try {
        ctx.packageManager.getPackageInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun updateInstall(id: Long, block: InstallState.() -> InstallState) {
        val cur = state.installStates[id] ?: InstallState()
        state   = state.copy(installStates = state.installStates + (id to cur.block()))
    }

    fun fetchScreenshots(repo: GitHubRepo) {
        if (state.screenshots.containsKey(repo.id)) return
        viewModelScope.launch {
            try {
                val readme = RetrofitClient.service.getReadme(repo.owner.login, repo.name)
                val md     = if (readme.encoding == "base64") {
                    String(android.util.Base64.decode(readme.content.replace("\n", ""), android.util.Base64.DEFAULT))
                } else readme.content
                val regex = Regex(
                    """!\[[^\]]*\]\(([^)]+\.(?:png|jpg|jpeg|gif|webp))[^)]*\)""",
                    RegexOption.IGNORE_CASE
                )
                val urls = regex.findAll(md).map { it.groupValues[1] }
                    .map { url ->
                        if (url.startsWith("http")) url
                        else "https://raw.githubusercontent.com/${repo.full_name}/main/${url.trimStart('/', '.')}"
                    }.distinct().take(8).toList()
                state = state.copy(screenshots = state.screenshots + (repo.id to urls))
            } catch (_: Exception) {
                state = state.copy(screenshots = state.screenshots + (repo.id to emptyList()))
            }
        }
    }

    fun checkForUpdatesNow() {
        if (state.isCheckingUpdates) return
        viewModelScope.launch {
            state = state.copy(isCheckingUpdates = true)
            // One entry per repo — use the most recently installed version as the baseline
            val history = state.installHistory
                .groupBy { it.repoId }
                .mapValues { (_, v) -> v.maxByOrNull { it.installedAt }!! }
                .values.toList()
            val updates = mutableListOf<UpdateInfo>()

            for (entry in history) {
                try {
                    val r   = RetrofitClient.service.getLatestRelease(entry.ownerLogin, entry.repoName)
                    val key = "${entry.repoId}:${r.tag_name}"
                    if (r.tag_name != entry.tagName && key !in state.ignoredVersions)
                        updates.add(UpdateInfo(entry.repoId, entry.repoName, entry.tagName, r.tag_name, r.body))
                } catch (_: Exception) {}
            }

            val newUpdates = updates.distinctBy { it.repoId }
            val hasNewUpdates = newUpdates.any { n ->
                state.updates.none { o -> o.repoId == n.repoId && o.latestTag == n.latestTag }
            }
            val newDismissed = if (hasNewUpdates) false else state.notifsDismissed
            state = state.copy(
                updates           = newUpdates,
                isCheckingUpdates = false,
                notifsDismissed   = newDismissed
            )
            prefs.saveUpdates(newUpdates)
            if (hasNewUpdates) prefs.saveNotifsDismissed(false)
        }
    }

    fun updateAll() {
        if (state.updates.isEmpty()) return
        viewModelScope.launch {
            val history = state.installHistory
                .groupBy { it.repoId }
                .mapValues { (_, v) -> v.maxByOrNull { it.installedAt }!! }
            for (update in state.updates) {
                val entry = history[update.repoId] ?: continue
                try {
                    val release  = RetrofitClient.service.getLatestRelease(entry.ownerLogin, entry.repoName)
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                                   ?: continue
                    val repo = state.installStates[entry.repoId]?.repo ?: GitHubRepo(
                        id        = entry.repoId,
                        name      = entry.repoName,
                        full_name = "${entry.ownerLogin}/${entry.repoName}",
                        owner     = RepoOwner(login = entry.ownerLogin)
                    )
                    updateInstall(repo.id) { copy(release = release, apkAsset = apkAsset, repo = repo) }
                    downloadAndInstall(repo, apkAsset)
                } catch (_: Exception) {}
            }
        }
    }

    fun clearRemovedApps() {
        val history = state.installHistory
            .groupBy { it.repoId }
            .mapValues { (_, v) -> v.maxByOrNull { it.installedAt }!! }
        val removedIds = history.values
            .filter { entry ->
                val iState = state.installStates[entry.repoId]
                iState != null && !iState.isInstalled
            }
            .map { it.repoId }
            .toSet()
        if (removedIds.isEmpty()) return
        val pruned = state.installHistory.filter { it.repoId !in removedIds }
        state = state.copy(
            installHistory = pruned,
            installStates  = state.installStates.filter { it.key !in removedIds },
            updates        = state.updates.filter { it.repoId !in removedIds }
        )
        prefs.saveInstallHistory(pruned)
        prefs.saveUpdates(state.updates)
    }

    fun ignoreVersion(repoId: Long, tag: String) {
        val key     = "$repoId:$tag"
        val updated = state.ignoredVersions + key
        state = state.copy(
            ignoredVersions = updated,
            updates         = state.updates.filter { "${it.repoId}:${it.latestTag}" != key }
        )
        prefs.saveIgnoredVersions(updated)
    }

    fun recordInstall(repo: GitHubRepo, tagName: String, apkPath: String, packageName: String = "") {
        val entry    = InstallHistoryEntry(repo.id, repo.name, repo.owner.login, tagName, apkPath, packageName = packageName)
        val sameRepo = state.installHistory.filter { it.repoId == repo.id } + entry
        val others   = state.installHistory.filter { it.repoId != repo.id }
        if (sameRepo.size > 3) {
            sameRepo.dropLast(3).forEach { try { java.io.File(it.apkPath).delete() } catch (_: Exception) {} }
        }
        val pruned = others + sameRepo.takeLast(3)
        state = state.copy(installHistory = pruned)
        prefs.saveInstallHistory(pruned)
    }

    fun rollbackTo(entry: InstallHistoryEntry) {
        try {
            val file = java.io.File(entry.apkPath)
            if (!file.exists()) { state = state.copy(error = "APK file not found for rollback"); return }
            val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
            ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            state = state.copy(error = "Rollback failed: ${e.message}")
        }
    }

    fun syncStarredRepos() {
        if (state.settings.githubToken.isEmpty()) {
            state = state.copy(error = "Sign in with GitHub token first"); return
        }
        viewModelScope.launch {
            try {
                val starred = RetrofitClient.service.getStarredRepos(perPage = 50)
                val merged  = (starred + state.favourites).distinctBy { it.id }
                state = state.copy(favourites = merged)
                prefs.saveFavourites(merged)
            } catch (e: Exception) {
                state = state.copy(error = "Could not sync starred: ${e.message}")
            }
        }
    }

    fun setCompareTarget(repo: GitHubRepo?) { state = state.copy(compareTargetRepo = repo) }

    fun checkSelfUpdate() {
        val rawPrefs  = ctx.getSharedPreferences("omni_prefs", android.content.Context.MODE_PRIVATE)
        val lastCheck = rawPrefs.getLong("last_update_check", 0L)
        if (System.currentTimeMillis() - lastCheck < 6 * 60 * 60 * 1000L) return
        viewModelScope.launch {
            try {
                val release = RetrofitClient.service.getLatestRelease("smartworldarafath", "Omni-Apps")
                val latest  = release.tag_name.trimStart('v', 'V')
                val current = BuildConfig.VERSION_NAME
                if (isNewerVersion(latest, current)) {
                    val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    state = state.copy(
                        selfUpdateInfo = SelfUpdateInfo(
                            latestVersion = release.tag_name,
                            apkUrl        = apkAsset?.browser_download_url
                                ?: "https://github.com/smartworldarafath/Omni-Apps/releases/latest",
                            changelog     = release.body
                        )
                    )
                }
                rawPrefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply()
            } catch (_: Exception) {}
        }
    }

    fun dismissSelfUpdate() {
        state = state.copy(selfUpdateDismissed = true)
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val l = latest.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
