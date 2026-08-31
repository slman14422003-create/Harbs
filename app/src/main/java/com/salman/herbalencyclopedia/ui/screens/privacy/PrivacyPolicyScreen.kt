package com.salman.herbalencyclopedia.ui.screens.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salman.herbalencyclopedia.ui.components.GlassIconButton
import com.salman.herbalencyclopedia.ui.components.GlassTopBar

private data class PolicySection(val title: String, val body: String)

private val sections = listOf(
    PolicySection(
        "المعلومات التي نجمعها",
        "لا يطلب التطبيق أي بيانات شخصية من المستخدم العادي (لا اسم، لا بريد إلكتروني، لا رقم هاتف، ولا أي معرّف لجهازك). التطبيق لا يتتبّعك ولا يجمع أي إحصاءات عن استخدامك."
    ),
    PolicySection(
        "بيانات الأعشاب المعروضة",
        "محتوى الموسوعة (أسماء الأعشاب، فوائدها، طريقة استخدامها، تحذيراتها، وصورها) هو محتوى توعوي عام من تدقيق المعالج الفيزيائي سلمان، يُعرض داخل التطبيق فقط لأغراض المعرفة العامة، ولا يُستخدم لأي غرض تسويقي ولا يُشارَك مع أي جهة خارجية."
    ),
    PolicySection(
        "ما يُحفظ على جهازك",
        "قائمة الأعشاب المفضّلة لديك وتفضيلات العرض (الوضع الليلي، حجم الخط، وضع الأداء) تُحفظ محلياً على جهازك فقط، ولا تُرسَل أو تُربَط باسمك أو بأي حساب. يمكنك مسح المفضّلة في أي وقت من الإعدادات."
    ),
    PolicySection(
        "التغييرات على هذه السياسة",
        "قد تُحدَّث هذه السياسة بين حين وآخر لتعكس تحسينات على التطبيق. يُنصح بمراجعتها بين فترة وأخرى."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { Text("سياسة الخصوصية") },
                navigationIcon = { GlassIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // إخلاء المسؤولية الطبية — أهم فقرة بالسياسة، لذلك تظهر أولاً وبشكل بارز.
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "إخلاء مسؤولية طبي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "هذه المعلومات من تدقيق المعالج الفيزيائي سلمان، وقد لا تكون دقيقة بنسبة 100%. " +
                                "يُفضّل استشارة طبيب مختص إن كنت تعاني من مرض ما، ولا يُغني محتوى هذا التطبيق " +
                                "عن التشخيص أو العلاج الطبي المتخصص بأي حال.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            items(sections) { section ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(section.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Text(
                    "© جميع الحقوق محفوظة — تطوير المعالج الفيزيائي سلمان",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }
    }
}
