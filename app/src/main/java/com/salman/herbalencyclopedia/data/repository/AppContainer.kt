package com.salman.herbalencyclopedia.data.repository

import android.content.Context

/**
 * Minimal manual DI container. Kept intentionally simple (no Hilt/Koin)
 * to avoid extra annotation-processing build complexity for this project size.
 */
class AppContainer(context: Context) {
    val herbRepository: HerbRepository by lazy { HerbRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context.applicationContext)
    }
    val updateRepository: UpdateRepository by lazy { UpdateRepository() }
    val feedbackRepository: FeedbackRepository by lazy { FeedbackRepository() }
    // مزامنة "تعلّم سيمو الذاتي" بين الأجهزة عبر Firestore — انظر
    // SemoLearningRepository وAppViewModel.init للسلك الفعلي.
    val semoLearningRepository: SemoLearningRepository by lazy { SemoLearningRepository() }
}
