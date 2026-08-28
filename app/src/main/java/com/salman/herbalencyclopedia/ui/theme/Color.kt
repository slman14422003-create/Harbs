package com.salman.herbalencyclopedia.ui.theme

import androidx.compose.ui.graphics.Color

// Herbal / botanical palette (default)
val LeafGreen80 = Color(0xFFB7DDB0)
val LeafGreen40 = Color(0xFF2E7D32)
val LeafGreenGrey80 = Color(0xFFC2CDC0)
val LeafGreenGrey40 = Color(0xFF52634E)
val Earth80 = Color(0xFFE6C599)
val Earth40 = Color(0xFF7A5230)

val SurfaceLight = Color(0xFFFBFDF7)
val SurfaceDark = Color(0xFF1A1C18)

/**
 * لوحة ألوان بديلة يختارها المستخدم يدويًا من شاشة الإعدادات عندما يكون
 * خيار "ألوان ديناميكية" (Material You) مُعطّلاً. كل لوحة تحمل ألوانها
 * الخاصة للوضعين الفاتح والداكن، بالإضافة إلى لون "معاينة" واحد يُستخدم
 * في شبكة الاختيار بالإعدادات.
 */
enum class ThemePalette(
    val label: String,
    val swatch: Color,
    val light40: Color,
    val light80: Color,
    val secondary40: Color,
    val secondary80: Color,
    val tertiary40: Color,
    val tertiary80: Color
) {
    LEAF(
        label = "أخضر (افتراضي)",
        swatch = LeafGreen40,
        light40 = LeafGreen40, light80 = LeafGreen80,
        secondary40 = LeafGreenGrey40, secondary80 = LeafGreenGrey80,
        tertiary40 = Earth40, tertiary80 = Earth80
    ),
    OCEAN(
        label = "أزرق",
        swatch = Color(0xFF1565C0),
        light40 = Color(0xFF1565C0), light80 = Color(0xFFA9C7FF),
        secondary40 = Color(0xFF4C607A), secondary80 = Color(0xFFB4C8E6),
        tertiary40 = Color(0xFF00695C), tertiary80 = Color(0xFF7FD8C7)
    ),
    VIOLET(
        label = "بنفسجي",
        swatch = Color(0xFF6A3EA1),
        light40 = Color(0xFF6A3EA1), light80 = Color(0xFFD6BBFF),
        secondary40 = Color(0xFF635573), secondary80 = Color(0xFFCCBEDC),
        tertiary40 = Color(0xFF7D5260), tertiary80 = Color(0xFFEFB8C8)
    ),
    SUNSET(
        label = "برتقالي",
        swatch = Color(0xFFC4570A),
        light40 = Color(0xFFC4570A), light80 = Color(0xFFFFB68C),
        secondary40 = Color(0xFF77574A), secondary80 = Color(0xFFE7BDAC),
        tertiary40 = Color(0xFF6B5D0F), tertiary80 = Color(0xFFD8C669)
    ),
    TEAL(
        label = "فيروزي",
        swatch = Color(0xFF00796B),
        light40 = Color(0xFF00796B), light80 = Color(0xFF80CBC4),
        secondary40 = Color(0xFF4A6361), secondary80 = Color(0xFFB1CCC9),
        tertiary40 = Color(0xFF456179), tertiary80 = Color(0xFFAECBEA)
    ),
    ROSE(
        label = "وردي",
        swatch = Color(0xFFB3264D),
        light40 = Color(0xFFB3264D), light80 = Color(0xFFFFB1C5),
        secondary40 = Color(0xFF77525A), secondary80 = Color(0xFFE7BDC5),
        tertiary40 = Color(0xFF785A2E), tertiary80 = Color(0xFFE9C18C)
    );

    companion object {
        fun fromId(id: String?): ThemePalette = entries.find { it.name == id } ?: LEAF
    }
}
