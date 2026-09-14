package com.salman.herbalencyclopedia.ui.screens.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.salman.herbalencyclopedia.data.ai.AiConfig
import com.salman.herbalencyclopedia.data.model.Category
import com.salman.herbalencyclopedia.data.model.Herb

// نسخة "public" — راجع تعليق AdminListScreen.kt (مجلد ui/screens/admin) للتفاصيل
// الكاملة. نفس التوقيع تماماً (بنفس القيم الافتراضية) حتى يبقى استدعاء هذه
// الدالة في HerbalNavGraph.kt مطابقاً حرفياً بين النكهتين.
@Composable
fun AdminToolsScreen(
    categories: List<Category>,
    herbs: List<Herb>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateCategory: (String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteCategory: (String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteAllHerbs: ((Boolean, String?) -> Unit) -> Unit,
    onDeleteAllData: ((Boolean, String?) -> Unit) -> Unit,
    onTestConnection: ((Boolean, String?) -> Unit) -> Unit,
    onClearFavorites: () -> Unit,
    onRestoreBackup: (String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateSettingsClick: () -> Unit,
    aiSimilarityThreshold: Float = AiConfig.defaultSimilarityThreshold.toFloat(),
    aiSearchThreshold: Float = AiConfig.defaultSearchThreshold.toFloat(),
    aiExtraStopWords: Set<String> = emptySet(),
    onSetAiSimilarityThreshold: (Float) -> Unit = {},
    onSetAiSearchThreshold: (Float) -> Unit = {},
    onSetAiExtraStopWords: (Set<String>) -> Unit = {},
    onResetAiSettings: () -> Unit = {},
    onOpenTrainedCases: () -> Unit = {},
    onOpenSemoLearning: () -> Unit = {},
    aiOnlineEnabled: Boolean = AiConfig.defaultOnlineEnabled,
    aiOnlineApiKey: String = "",
    aiOnlineModel: String = AiConfig.defaultOnlineModel,
    aiOnlineBaseUrl: String = AiConfig.defaultOnlineBaseUrl,
    onSetAiOnlineEnabled: (Boolean) -> Unit = {},
    onSetAiOnlineApiKey: (String) -> Unit = {},
    onSetAiOnlineModel: (String) -> Unit = {},
    onSetAiOnlineBaseUrl: (String) -> Unit = {},
    onResetAiOnlineSettings: () -> Unit = {},
    deviceStatsLoading: Boolean = false,
    deviceStatsTotal: Long? = null,
    deviceStatsActive7d: Long? = null,
    deviceStatsError: String? = null,
    onRefreshDeviceStats: () -> Unit = {}
) {
    LaunchedEffect(Unit) { onBack() }
}
