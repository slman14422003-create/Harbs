package com.salman.herbalencyclopedia.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salman.herbalencyclopedia.data.ai.HerbAssistant
import com.salman.herbalencyclopedia.data.model.AppUpdateConfig
import com.salman.herbalencyclopedia.data.model.AppUpdateInfo
import com.salman.herbalencyclopedia.data.model.Blend
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Feedback
import com.salman.herbalencyclopedia.data.model.Herb
import com.salman.herbalencyclopedia.data.repository.AppContainer
import com.salman.herbalencyclopedia.data.repository.HerbRepository
import com.salman.herbalencyclopedia.data.update.UpdateDownloadService
import com.salman.herbalencyclopedia.data.update.UpdateDownloadState
import com.salman.herbalencyclopedia.data.update.UpdateDownloadStatus
import com.salman.herbalencyclopedia.ui.util.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val herbs: List<Herb> = emptyList(),
    val categories: List<Category> = emptyList(),
    val blends: List<Blend> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/** State of an in-app "check for updates" action (see [AppViewModel.checkForUpdate]). */
sealed class UpdateCheckState {
    data object Idle : UpdateCheckState()
    data object Checking : UpdateCheckState()
    data object UpToDate : UpdateCheckState()
    data class Available(val info: AppUpdateInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

class AppViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = container.preferencesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** لغة واجهة التطبيق الحالية (راجع [AppLanguage] وSettingsScreen). */
    val appLanguage: StateFlow<AppLanguage> = container.preferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ARABIC)

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { container.preferencesRepository.setAppLanguage(language) }
    }

    var isLoggedIn by mutableStateOf(container.authRepository.isAdmin)
        private set
    var isAdmin by mutableStateOf(container.authRepository.isAdmin)
        private set

    // ---------------------------------------------------------------------
    // Update check -> direct APK download hand-off
    // ---------------------------------------------------------------------

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    // التحميل الفعلي الآن يتم بخدمة أمامية حقيقية (UpdateDownloadService)
    // تعمل بالخلفية حتى لو أُغلقت شاشة التطبيق — راجع توثيق
    // UpdateDownloadStatus لماذا هذا التغيير ضروري. الحالة هنا مجرد
    // انعكاس مباشر لما تكتبه الخدمة، بلا أي تغيير بمنطق الواجهة.
    val downloadState: StateFlow<UpdateDownloadState> = UpdateDownloadStatus.state

    private val _updateConfig = MutableStateFlow(AppUpdateConfig())
    val updateConfigState: StateFlow<AppUpdateConfig> = _updateConfig.asStateFlow()


    /** Reads the app's own installed version and checks the configured GitHub repo for a newer release. */
    fun checkForUpdate(context: Context) {
        if (_updateState.value == UpdateCheckState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            val pkgInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val versionName = pkgInfo?.versionName ?: "0.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                (pkgInfo?.longVersionCode ?: 0L).toInt()
            } else {
                @Suppress("DEPRECATION") (pkgInfo?.versionCode ?: 0)
            }
            val result = runCatching { container.updateRepository.checkForUpdate(versionCode, versionName) }
            _updateState.value = result.fold(
                onSuccess = { info -> if (info != null) UpdateCheckState.Available(info) else UpdateCheckState.UpToDate },
                onFailure = { e -> UpdateCheckState.Error(e.localizedMessage ?: "تعذّر التحقق من التحديثات") }
            )
        }
    }

    // Remembers the last update info so downloadUpdate()/installUpdate() (which
    // aren't passed the info again from the UI on every call) can re-use it,
    // e.g. on retry or when there's no direct .apk asset to download.
    private var lastUpdateInfo: AppUpdateInfo? = null

    /**
     * يبدأ تحميل ملف التحديث (.apk) بالخلفية عبر [UpdateDownloadService] — خدمة
     * أمامية حقيقية تستمر حتى لو أغلق المستخدم شاشة التطبيق أو خرج منه تماماً،
     * وتُظهر إشعار تقدّم بهوية التطبيق. الحالة (نسبة التحميل، الجاهزية،
     * الفشل) تصل تلقائياً عبر [downloadState] (المرتبط بـ [UpdateDownloadStatus]
     * المشتركة)، فلا حاجة لانتظار نتيجة من هنا مباشرة.
     *
     * إن لم يكن لهذا الإصدار ملف .apk مرفق (صفحة إصدار فقط)، لا شيء نحمّله
     * داخل التطبيق، فنفتح صفحة الإصدار بالمتصفح مباشرة بدلاً من ذلك، كما كان
     * سابقاً.
     */
    fun downloadUpdate(context: Context, info: AppUpdateInfo) {
        lastUpdateInfo = info
        val apkUrl = info.apkUrl
        if (apkUrl == null) {
            val opened = openInBrowser(context, info.releasePageUrl)
            UpdateDownloadStatus.update(
                if (opened) UpdateDownloadState.ReadyToInstall
                else UpdateDownloadState.Failed("تعذّر فتح رابط التحميل")
            )
            return
        }

        val candidates = container.updateRepository.downloadCandidates(
            apkUrl, info.useProxyFallback, info.customProxyBaseUrl
        )
        UpdateDownloadService.start(context, candidates, info.versionName)
    }

    /**
     * يوقف تحميل التحديث الجاري (زر "إلغاء" بشاشة الإعدادات) — يُنهي الخدمة
     * الأمامية، يقطع الاتصال الجاري فوراً، ويحذف الملف الجزئي المُنزَّل.
     */
    fun cancelDownload(context: Context) {
        UpdateDownloadService.cancel(context)
    }

    /**
     * Hands the already-downloaded .apk to the system package installer via a FileProvider
     * content:// Uri. On Android 8+ this also makes sure "install unknown apps" is allowed for
     * this app first — if not, it opens that settings screen and the user just taps the
     * install button again once they've granted it.
     *
     * الملف نفسه أصبح يأتي من [UpdateDownloadStatus] المشتركة بدل متغيّر محلي،
     * لأن التحميل الفعلي صار يتم بخدمة منفصلة (UpdateDownloadService) قد تكون
     * أنهت التحميل بينما كانت شاشة الإعدادات مغلقة.
     */
    fun installUpdate(context: Context) {
        val file = UpdateDownloadStatus.downloadedApk
        if (file == null || !file.exists()) {
            // Nothing was downloaded in-app (e.g. the release had no .apk asset) - fall back
            // to whatever link we have.
            lastUpdateInfo?.let { openInBrowser(context, it.apkUrl ?: it.releasePageUrl) }
            return
        }
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settingsIntent) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { UpdateDownloadStatus.update(UpdateDownloadState.Failed("تعذّر فتح مثبّت التطبيقات")) }
    }

    /** Opens a URL (the GitHub release page, when there's no .apk asset to download in-app) in the browser. */
    private fun openInBrowser(context: Context, url: String): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }

    /** Resets the update flow back to its initial state (e.g. after a dismissal). */
    fun resetUpdateFlow(context: Context) {
        cancelDownload(context)
        _updateState.value = UpdateCheckState.Idle
        UpdateDownloadStatus.reset()
    }

    /** Loads the current admin-editable update settings, for [AdminUpdateScreen]. */
    fun loadUpdateConfig() {
        viewModelScope.launch {
            _updateConfig.value = runCatching { container.updateRepository.fetchConfig() }
                .getOrDefault(AppUpdateConfig())
        }
    }

    /** Saves admin-editable update settings (GitHub repo, override link/notes, mandatory-update threshold). */
    fun saveUpdateConfig(config: AppUpdateConfig, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            runCatching { container.updateRepository.saveConfig(config) }
                .onSuccess {
                    _updateConfig.value = config
                    onResult(true, "تم حفظ إعدادات التحديث")
                }
                .onFailure { onResult(false, it.localizedMessage ?: "حدث خطأ أثناء الحفظ") }
        }
    }

    // نتيجة زر "تحقق الآن بهذه الإعدادات" في لوحة الإدارة — منفصلة تماماً عن
    // [updateState] الخاص بشاشة المستخدم العادية، حتى لا يختلط اختبار الأدمن
    // لإعدادات لم تُحفَظ بعد مع حالة التحقق الحقيقية التي يراها المستخدمون.
    private val _adminUpdateTestState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val adminUpdateTestState: StateFlow<UpdateCheckState> = _adminUpdateTestState.asStateFlow()

    /**
     * يختبر [config] كما هو مكتوب في حقول شاشة الإدارة الآن مباشرة — قبل
     * حفظه وبصرف النظر عمّا هو محفوظ فعلياً في Firestore — فيعرف الأدمن فوراً
     * إن كانت هذه الإعدادات (المستودع، الرابط المخصّص...) تعمل فعلاً، دون
     * الحاجة للحفظ ثم الخروج لشاشة المستخدم العادية للتأكد.
     */
    fun testUpdateConfig(context: Context, config: AppUpdateConfig) {
        if (_adminUpdateTestState.value == UpdateCheckState.Checking) return
        viewModelScope.launch {
            _adminUpdateTestState.value = UpdateCheckState.Checking
            val pkgInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val versionName = pkgInfo?.versionName ?: "0.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= 28) {
                (pkgInfo?.longVersionCode ?: 0L).toInt()
            } else {
                @Suppress("DEPRECATION") (pkgInfo?.versionCode ?: 0)
            }
            val result = runCatching {
                container.updateRepository.checkForUpdate(config, versionCode, versionName)
            }
            _adminUpdateTestState.value = result.fold(
                onSuccess = { info -> if (info != null) UpdateCheckState.Available(info) else UpdateCheckState.UpToDate },
                onFailure = { e -> UpdateCheckState.Error(e.localizedMessage ?: "تعذّر التحقق من التحديثات") }
            )
        }
    }

    /** يعيد نتيجة اختبار الإعدادات في لوحة الإدارة إلى الحالة الأولية (مثلاً عند فتح الشاشة من جديد). */
    fun resetAdminUpdateTest() {
        _adminUpdateTestState.value = UpdateCheckState.Idle
    }

    init {
        // Live sync: stay subscribed to Firestore for as long as the app is alive, so
        // any change - made here, from another device, or from the web admin panel -
        // is reflected immediately without needing a manual refresh.
        viewModelScope.launch {
            combine(
                container.herbRepository.observeCategories(),
                container.herbRepository.observeHerbs(),
                container.herbRepository.observeBlends(),
                container.preferencesRepository.appLanguage
            ) { categories, herbs, blends, language ->
                // عند اختيار الإنجليزية، تُترجَم بيانات الأعشاب/التصنيفات/
                // الخلطات هنا في نقطة مركزية واحدة (بدل كل شاشة على حدة)،
                // فتصل مُترجَمة تلقائياً لكل شاشة تعرضها (الرئيسية، كل
                // الأعشاب، البحث، المفضلة، التصنيفات، التفاصيل، الخلطات...)
                // دون أي تعديل إضافي في تلك الشاشات. راجع translateHerbs/
                // translateCategories/translateBlends أدناه.
                UiState(
                    herbs = translateHerbs(herbs, language),
                    categories = translateCategories(categories, language),
                    blends = translateBlends(blends, language),
                    isLoading = false,
                    error = null
                )
            }
                .catch { e ->
                    // Keep whatever data is already on screen (e.g. from the offline
                    // cache) and only surface the error, instead of wiping the list.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = HerbRepository.describeError(e)
                    )
                }
                .collect { state -> _uiState.value = state }
        }

        // عند تغيير لغة التطبيق (وليس أول قراءة عند بدء التشغيل، لذا drop(1))،
        // نظهر مؤشر تحميل صغير فوراً (نفس مؤشر "سحب للتحديث" — راجع
        // isRefreshing في HomeScreen) بدل ترك الشاشة تبدو متجمّدة طوال مدة
        // ترجمة كل الأعشاب. الحالة النهائية المُترجَمة تصل لاحقاً من كتلة
        // combine أعلاه وتُطفئ isLoading تلقائياً.
        viewModelScope.launch {
            container.preferencesRepository.appLanguage.drop(1).collect {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
        }

        // ── مزامنة "تعلّم سيمو الذاتي" بين الأجهزة ───────────────────────
        // كانت [PreferencesRepository.aiAutoLearnedExamples] مصدراً محلياً
        // بحتاً (DataStore على هذا الجهاز فقط): أي حالة يتعلّمها سيمو من
        // تقييم 👍 هنا لا يستفيد منها أي تثبيت آخر للتطبيق إطلاقاً. هذا
        // المستمع يبقى مفتوحاً طوال حياة العملية (كمستمعي Firestore أعلاه)
        // ليدمج، عند كل تغيير محلي أو وصول شبكي جديد، القائمة المحلية مع
        // النسخة المشتركة الحيّة من SemoLearningRepository (انظر توثيقها
        // وتوثيق [HerbAssistant.mergeLearnedExamples])، ثم يحفظ الناتج
        // المدموج محلياً — فيصل تلقائياً لـ AiConfig عبر نفس مسار
        // HerbalNavGraph الحالي دون أي تعديل عليه. فشل الشبكة هنا لا يوقف
        // شيئاً: [SemoLearningRepository.observeSharedLearnedExamples] يبتلع
        // أخطاءه بنفسه ويستمر سيمو بالعمل بآخر بيانات محلية معروفة.
        viewModelScope.launch {
            combine(
                container.preferencesRepository.aiAutoLearnedExamples,
                container.semoLearningRepository.observeSharedLearnedExamples()
            ) { local, shared -> local to shared }
                .catch { /* المزامنة انتهازية فقط؛ لا تُسقِط التطبيق أو تُعطّل سيمو محلياً. */ }
                .collect { (local, shared) ->
                    val merged = HerbAssistant.mergeLearnedExamples(local, shared)
                    if (merged !== local) {
                        container.preferencesRepository.setAiAutoLearnedExamples(merged)
                    }
                }
        }
    }

    /**
     * يترجم قوائم الأعشاب/التصنيفات/الخلطات بالتوازي (كل حقل نصي طلب شبكة
     * منفصل عبر [com.salman.herbalencyclopedia.data.translate.TranslationRepository]
     * المُخزِّن مؤقتاً) عند اختيار الإنجليزية فقط؛ في الوضع العربي تُعاد
     * القوائم كما هي دون أي طلب شبكة إضافي.
     */
    private suspend fun translateHerbs(herbs: List<Herb>, language: AppLanguage): List<Herb> {
        if (language == AppLanguage.ARABIC || herbs.isEmpty()) return herbs
        val translator = container.translationRepository
        return coroutineScope {
            herbs.map { herb ->
                async {
                    herb.copy(
                        name = translator.translate(herb.name, "en"),
                        benefits = translator.translate(herb.benefits, "en"),
                        usage = translator.translate(herb.usage, "en"),
                        warnings = translator.translate(herb.warnings, "en"),
                        harms = translator.translate(herb.harms, "en"),
                        notes = translator.translate(herb.notes, "en")
                    )
                }
            }.awaitAll()
        }
    }

    private suspend fun translateCategories(categories: List<Category>, language: AppLanguage): List<Category> {
        if (language == AppLanguage.ARABIC || categories.isEmpty()) return categories
        val translator = container.translationRepository
        return coroutineScope {
            categories.map { category ->
                async { category.copy(name = translator.translate(category.name, "en")) }
            }.awaitAll()
        }
    }

    private suspend fun translateBlends(blends: List<Blend>, language: AppLanguage): List<Blend> {
        if (language == AppLanguage.ARABIC || blends.isEmpty()) return blends
        val translator = container.translationRepository
        return coroutineScope {
            blends.map { blend ->
                async {
                    blend.copy(
                        name = translator.translate(blend.name, "en"),
                        benefits = translator.translate(blend.benefits, "en"),
                        usage = translator.translate(blend.usage, "en"),
                        warnings = translator.translate(blend.warnings, "en"),
                        notes = translator.translate(blend.notes, "en")
                    )
                }
            }.awaitAll()
        }
    }

    /**
     * يُستدعى بعد تسجيل تقييم 👍 على رد قابل للتعلّم في شاشة سيمو (انظر
     * SemoAssistantScreen.rateMessage): يرفع نفس الحالة إلى المجموعة
     * المشتركة على Firestore بلا انتظار (fire-and-forget) — الحفظ المحلي
     * عبر [HerbAssistant.recordFeedback] يحدث مستقلاً عن هذا الاستدعاء
     * ولا ينتظره، فتبقى الشاشة سريعة الاستجابة حتى بلا إنترنت.
     */
    fun contributeSemoLearning(question: String, response: String) {
        viewModelScope.launch { container.semoLearningRepository.contribute(question, response) }
    }

    /**
     * يُستدعى بعد تسجيل تقييم 👎: يزيد صوتاً سلبياً على نفس الحالة في
     * المجموعة المشتركة إن كانت قد شُوركت شبكياً أصلاً من هذا الجهاز أو
     * جهاز آخر (انظر [com.salman.herbalencyclopedia.data.repository.SemoLearningRepository.demote]
     * لسبب عدم الحذف المباشر).
     */
    fun demoteSemoLearning(question: String) {
        viewModelScope.launch { container.semoLearningRepository.demote(question) }
    }

    /** Manual retry: forces a real server round-trip to confirm connectivity and clear any error. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                container.herbRepository.fetchCategories(fromServer = true)
                container.herbRepository.fetchHerbs(fromServer = true)
                container.herbRepository.fetchBlends(fromServer = true)
                // The live listeners above already keep uiState in sync with these
                // results; this call's job is just to confirm connectivity and
                // surface a clear error if it fails.
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = HerbRepository.describeError(e)
                )
            }
        }
    }

    fun toggleFavorite(herbId: String) {
        viewModelScope.launch { container.preferencesRepository.toggleFavorite(herbId) }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = container.authRepository.login(email, password)
            isLoggedIn = result.success
            isAdmin = result.isAdmin
            onResult(result.success, result.message)
        }
    }


    fun logout() {
        container.authRepository.logout()
        isLoggedIn = false
        isAdmin = false
    }

    // Writes below don't call refresh(): the live Firestore listeners in init{}
    // pick up every change automatically (instantly from the local cache, then
    // reconciled with the server), so an extra manual fetch would just be a
    // redundant round-trip and could momentarily race with the listener.

    fun addHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.addHerb(herb)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun updateHerb(herb: Herb, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.updateHerb(herb)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun deleteHerb(id: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.deleteHerb(id)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }
    // ---------------------------------------------------------------------
    // Blends ("الخلطات") — admin-only writes, same live-sync model as herbs.
    // ---------------------------------------------------------------------

    fun addBlend(blend: Blend, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.addBlend(blend)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun updateBlend(blend: Blend, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                container.herbRepository.updateBlend(blend)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun deleteBlend(id: String, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                container.herbRepository.deleteBlend(id)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    // ---------------------------------------------------------------------
    // Feedback ("ملاحظات المستخدمين") — anyone can send, only the admin
    // account can read (enforced by firestore.rules), surfaced in
    // AdminFeedbackScreen under Settings.
    // ---------------------------------------------------------------------

    private val _feedbackList = MutableStateFlow<List<Feedback>>(emptyList())
    val feedbackList: StateFlow<List<Feedback>> = _feedbackList.asStateFlow()

    private val _feedbackLoading = MutableStateFlow(false)
    val feedbackLoading: StateFlow<Boolean> = _feedbackLoading.asStateFlow()

    private val _feedbackError = MutableStateFlow<String?>(null)
    val feedbackError: StateFlow<String?> = _feedbackError.asStateFlow()

    private var feedbackJob: Job? = null

    /** Sending is open to everyone — no admin/login check here on purpose. */
    fun submitFeedback(
        targetType: String,
        targetId: String,
        targetName: String,
        message: String,
        senderName: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                container.feedbackRepository.submitFeedback(targetType, targetId, targetName, message, senderName)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    /** Starts (once) the admin-only live listener backing [feedbackList]. Safe to call every time the screen opens. */
    fun loadFeedback() {
        if (feedbackJob?.isActive == true) return
        _feedbackLoading.value = true
        feedbackJob = viewModelScope.launch {
            container.feedbackRepository.observeFeedback()
                .catch { e ->
                    _feedbackLoading.value = false
                    _feedbackError.value = HerbRepository.describeError(e)
                }
                .collect { list ->
                    _feedbackLoading.value = false
                    _feedbackError.value = null
                    _feedbackList.value = list
                }
        }
    }

    fun deleteFeedback(id: String, onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                container.feedbackRepository.deleteFeedback(id)
                onDone(true, null)
            } catch (e: Exception) {
                onDone(false, HerbRepository.describeError(e))
            }
        }
    }

    fun addCategory(name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.addCategory(name) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteCategory(id: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteCategory(id) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    // ── تعديل (إعادة تسمية) تصنيف موجود ──────────────────────────────────
    // كانت شاشة أدوات الإدارة (AdminToolsScreen) تسمح فقط بإضافة تصنيف جديد
    // أو حذف تصنيف موجود، رغم أن HerbRepository.updateCategory كانت موجودة
    // فعلاً في طبقة البيانات بلا أي واجهة تستدعيها إطلاقاً — فلم يكن هناك أي
    // طريق للمسؤول لتصحيح اسم تصنيف أُدخل خطأً سوى حذفه وإضافته من جديد
    // (فيفقد كل الأعشاب المرتبطة به تصنيفها). هذه الدالة تكمل الوصلة الناقصة.
    fun updateCategory(id: String, name: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.updateCategory(id, name) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteAllHerbs(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteAllHerbs() }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun deleteAllData(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { runCatching { container.herbRepository.deleteAllData() }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun clearFavorites() { viewModelScope.launch { container.preferencesRepository.clearFavorites() } }

    fun restoreBackup(json: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch { runCatching { container.herbRepository.restoreBackup(json) }.onSuccess { onResult(true, null) }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

    fun testConnection(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch { runCatching { container.herbRepository.testConnection() }.onSuccess { onResult(true, "الاتصال يعمل بشكل طبيعي") }.onFailure { onResult(false, HerbRepository.describeError(it)) } }
    }

}
