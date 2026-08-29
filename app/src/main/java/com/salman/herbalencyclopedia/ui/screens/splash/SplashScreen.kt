package com.salman.herbalencyclopedia.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * شاشة البداية المخصصة داخل Compose (تظهر بعد شاشة النظام
 * Theme.HerbalEncyclopedia.Splash القصيرة). أُعيد تصميمها لتعطي طابعاً
 * نباتياً هادئاً وفخماً بدل الشاشة السابقة: خلفية متدرّجة بلون الهوية،
 * وميض عضوي خلف الأيقونة، أوراق زخرفية خافتة في الزوايا، وظهور متتابع
 * للعناصر بدل ظهورها دفعة واحدة.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var stage by remember { mutableStateOf(0) }

    val iconScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.6f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        stage = 1
        delay(250)
        stage = 2
        delay(1500)
        onFinished()
    }

    val brandGreen = Color(0xFF1B5E20)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(brandGreen, Color(0xFF2E7D32), MaterialTheme.colorScheme.surface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // زخرفة أوراق خافتة في الزوايا لإعطاء عمق نباتي بلا إلهاء عن المحتوى.
        Icon(
            imageVector = Icons.Filled.Spa,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-28).dp, y = 36.dp)
                .size(150.dp)
                .rotate(-18f)
        )
        Icon(
            imageVector = Icons.Filled.Spa,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = (-24).dp)
                .size(190.dp)
                .rotate(154f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // وميض عضوي متنفس خلف الشعار.
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(iconScale)
                        .background(Color.White.copy(alpha = glowAlpha * 0.25f), CircleShape)
                )
                AnimatedVisibility(
                    visible = stage >= 1,
                    enter = fadeIn(tween(550)) + scaleIn(tween(700, easing = FastOutSlowInEasing), initialScale = 0.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.80f))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Spa,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = brandGreen
                        )
                    }
                }
            }

            Spacer(Modifier.height(30.dp))

            AnimatedVisibility(
                visible = stage >= 2,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600, easing = FastOutSlowInEasing)) { it / 3 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "موسوعة الأعشاب",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "دليلك الطبيعي للمعرفة بالأعشاب",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            AnimatedVisibility(
                visible = stage >= 2,
                enter = fadeIn(tween(600, delayMillis = 200))
            ) {
                LoadingDots()
            }
        }

        AnimatedVisibility(
            visible = stage >= 2,
            enter = fadeIn(tween(700, delayMillis = 250)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
        ) {
            Text(
                text = "HERBAL ENCYCLOPEDIA",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.2.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun LoadingDots() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 160, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotAlpha$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.White.copy(alpha = alpha), CircleShape)
            )
        }
    }
}
