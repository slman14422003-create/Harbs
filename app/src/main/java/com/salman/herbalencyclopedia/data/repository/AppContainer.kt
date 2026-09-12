package com.salman.herbalencyclopedia.data.repository

import android.content.Context
import com.salman.herbalencyclopedia.data.translate.TranslationRepository

/**
 * Minimal manual DI container. Kept intentionally simple (no Hilt/Koin)
 * to avoid extra annotation-processing build complexity for this project size.
 */
class AppContainer(context: Context) {
    // مرجع التطبيق العام (Application Context) — يُستخدم في AppViewModel
    // لاستدعاءات تحتاج Context خارج سياق واجهة مستخدم مباشرة (مثل التحقق
    // التلقائي الصامت من التحديثات عند بدء التطبيق، راجع
    // AppViewModel.checkForUpdateSilently)، بعكس بقية استدعاءات فحص/تحميل
    // التحديث اليدوية التي تستلم Context من الشاشة نفسها (LocalContext.current).
    val appContext: Context = context.applicationContext
    val herbRepository: HerbRepository by lazy { HerbRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository(context.applicationContext) }
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context.applicationContext)
    }
    val updateRepository: UpdateRepository by lazy { UpdateRepository() }
    val feedbackRepository: FeedbackRepository by lazy { FeedbackRepository() }
    // مزامنة "تعلّم سيمو الذاتي" بين الأجهزة عبر Firestore — انظر
    // SemoLearningRepository وAppViewModel.init للسلك الفعلي.
    val semoLearningRepository: SemoLearningRepository by lazy { SemoLearningRepository() }
    // ترجمة بيانات الأعشاب/التصنيفات/الخلطات إلى الإنجليزية عبر واجهة جوجل
    // المجانية، مع تخزين مؤقت دائم — راجع AppViewModel.translateHerbs وما شابه.
    val translationRepository: TranslationRepository by lazy {
        TranslationRepository(context.applicationContext)
    }
}

