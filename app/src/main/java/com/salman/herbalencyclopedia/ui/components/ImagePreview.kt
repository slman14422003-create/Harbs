package com.salman.herbalencyclopedia.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * معاينة صورة العشبة بحجم كامل الشاشة عند الضغط عليها (قابلة للتكبير
 * بإصبعين، بالسحب، وبالنقر المزدوج) — تُستخدم من [HerbThumbnail] عبر
 * [previewOnClick] وأيضاً من معاينة الصورة في شاشة إضافة/تعديل العشبة
 * الخاصة بالمطوّر (AdminEditHerbScreen)، لتفادي تكرار نفس منطق الحوار
 * في مكانين.
 *
 * تشمل: أنيميشن دخول/خروج سلس (تكبير + تلاشي)، مؤشر تحميل أثناء جلب
 * الصورة، وحدود سحب مبنية على مقاس الشاشة الفعلي بدل رقم ثابت.
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
            animationSpec = tween(durationMillis = 220, easing = EaseOutCubic),
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
        val maxOffsetXBase = with(density) { (configuration.screenWidthDp.dp / 2).toPx() }
        val maxOffsetYBase = with(density) { (configuration.screenHeightDp.dp / 2).toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f * entranceProgress))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        val maxOffsetX = maxOffsetXBase * (newScale - 1f)
                        val maxOffsetY = maxOffsetYBase * (newScale - 1f)
                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
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
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { imageState = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer(
                        scaleX = scale * (0.85f + 0.15f * entranceProgress),
                        scaleY = scale * (0.85f + 0.15f * entranceProgress),
                        translationX = offsetX,
                        translationY = offsetY,
                        alpha = entranceProgress
                    )
            )

            if (imageState is AsyncImagePainter.State.Loading) {
                CircularProgressIndicator(color = Color.White.copy(alpha = entranceProgress))
            }

            IconButton(
                onClick = { animateDismiss() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .graphicsLayer(alpha = entranceProgress)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق المعاينة", tint = Color.White)
            }
        }
    }
}

