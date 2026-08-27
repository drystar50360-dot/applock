package com.userapp.applock

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayCountdownService : Service() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val totalSeconds = 20
    private val holdMillisToUnlock = 3000L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage = intent?.getStringExtra("target_package") ?: return START_NOT_STICKY
        if (composeView != null) return START_NOT_STICKY
        showOverlay(targetPackage)
        return START_NOT_STICKY
    }

    private fun showOverlay(targetPackage: String) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        lifecycleOwner = owner

        val view = ComposeView(this)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)

        view.setContent {
            OverlayContent(
                totalSeconds = totalSeconds,
                holdMillisToUnlock = holdMillisToUnlock,
                onFinished = { launchTargetNow(targetPackage) },
                onEmergency = { launchTargetNow(targetPackage) }
            )
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(view, params)
        composeView = view
    }

    private fun launchTargetNow(targetPackage: String) {
        PrefsManager.setAllowUntil(this, targetPackage, System.currentTimeMillis() + 5_000)
        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent?.let { startActivity(it) }
        removeOverlay()
        stopSelf()
    }

    private fun removeOverlay() {
        composeView?.let { windowManager?.removeView(it) }
        composeView = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
private fun OverlayContent(
    totalSeconds: Int,
    holdMillisToUnlock: Long,
    onFinished: () -> Unit,
    onEmergency: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var holdProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A081E))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size((140 * scale).dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFA29BFE), Color(0xFF6C5CE7), Color(0xFF4834D4))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = secondsLeft.toString(),
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "숨을 천천히 들이쉬고 내쉬세요",
                color = Color(0xFFC8C8E0),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            var released = false
                            var triggered = false
                            coroutineScope {
                                launch {
                                    released = tryAwaitRelease()
                                }
                                launch {
                                    val startTime = System.currentTimeMillis()
                                    while (!released && !triggered) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        holdProgress = (elapsed.toFloat() / holdMillisToUnlock).coerceIn(0f, 1f)
                                        if (elapsed >= holdMillisToUnlock) {
                                            triggered = true
                                            onEmergency()
                                        }
                                        delay(30)
                                    }
                                }
                            }
                            if (holdProgress < 1f) holdProgress = 0f
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                drawCircle(
                    color = Color(0xFF25254A),
                    radius = size.minDimension / 2
                )
                drawArc(
                    color = Color(0xFFFF8FA3),
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                )
            }
            Text(
                text = "긴급\n실행",
                color = Color(0xFF8A8AA8),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
