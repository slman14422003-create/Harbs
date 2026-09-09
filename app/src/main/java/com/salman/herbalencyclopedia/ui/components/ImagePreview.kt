package com.salman.herbalencyclopedia.ui.components

import android.os.Build
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/**
 * معاينة صورة العشبة بحجم كامل الشاشة عند الضغط عليها — بتصميم دائري
 * مؤطّر يتماشى مع هوية التطبيق: خلفية مموّهة (Blur) من نفس الصورة
 * مغمورة بتدرّج من ألوان الثيم الحالي، وفوقها الصورة الحادة داخل إطار
 * دائري بحدود متدرّجة اللون. قابلة للتكبير بإصبعين، بالسحب، وبالنقر
 * المزدوج.
 *
 * تُستخدم من [HerbThumbnail] عبر [previewOnClick] وأيضاً من معاينة
 * الصورة في شاشة إضافة/تعديل العشبة الخاصة بالمطوّر
 * (AdminEditHerbScreen)، لتفادي تكرار نفس منطق الحوار في مكانين.
 */
@Composable
fun ImagePreviewDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var visible by remember { mutableStateOf(false) }
        var dismissing by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) { visible = true }

        val entranceProgress by animateFloatAsState(
            targetValue = if (visible && !dismissing) 1f else 0f,
            animationSpec = tween(durationMillis = 260, easing = EaseOutCubic),
            label = "imagePreviewEntrance",
            finishedListener = { value -> if (value == 0f && dismissing) onDismiss() }
        )

        fun animateDismiss() {
            if (!dismissing) {
                dismissing = true
                visible = false
            }
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val maxOffsetBase = with(density) { (configuration.screenWidthDp.dp / 2).toPx() }

        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val scrim = MaterialTheme.colorScheme.scrim

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f * entranceProgress)),
            contentAlignment = Alignment.Center
        ) {
            // خلفية مموّهة من نفس الصورة، مع تدرّج بألوان الثيم الحالي فوقها
            // بدل الأسود الصامت — يخلي المعاينة تحس متناسقة مع باقي التطبيق.
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.55f * entranceProgress)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(48.dp)
                        } else {
                            Modifier
                        }
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scrim.copy(alpha = 0.15f * entranceProgress),
                                scrim.copy(alpha = 0.85f * entranceProgress)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            val maxOffset = maxOffsetBase * (newScale - 1f)
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffset, maxOffset)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffset, maxOffset)
                            if (newScale <= 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (scale <= 1f) animateDismiss() },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // الإطار الدائري: حلقة تدرّج بلون الثيم، وداخلها الصورة
                // الحادة مقصوصة دائرياً.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f)
                        .graphicsLayer(
                            scaleX = scale * (0.85f + 0.15f * entranceProgress),
                            scaleY = scale * (0.85f + 0.15f * entranceProgress),
                            translationX = offsetX,
                            translationY = offsetY,
                            alpha = entranceProgress
                        )
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(primary, tertiary, primary)),
                            radius = size.minDimension / 2f - strokeWidth / 2f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onState = { imageState = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .clip(CircleShape)
                    )

                    if (imageState is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = primary
                        )
                    }
                }
            }

            IconButton(
                onClick = { animateDismiss() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(44.dp)
                    .graphicsLayer(alpha = entranceProgress)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.22f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق المعاينة", tint = Color.White)
            }
        }
    }
}
