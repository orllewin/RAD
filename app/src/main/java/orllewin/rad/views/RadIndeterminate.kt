package orllewin.rad.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

class RadIndeterminate{

    private val rad90Degrees = 1.57079633

    @Composable
    fun Indeterminate(radius: Dp, ballRadius: Dp){

        val degrees = angleTransition(0f, 360f, 1200)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val radians = degrees * (Math.PI * 2) / 360f
            val x = cos(radians - rad90Degrees) * (radius.toPx() + (ballRadius.toPx()/2)) + canvasWidth/2f
            val y = sin(radians - rad90Degrees) * (radius.toPx() + (ballRadius.toPx()/2)) + canvasHeight/2f

            drawCircle(Color.Black, ballRadius.toPx(), Offset(x.toFloat(), y.toFloat()))
        }
    }

    @Composable
    fun angleTransition(start: Float, end: Float, duration: Int): Float {
        val infiniteTransition = rememberInfiniteTransition()
        val scale: Float by infiniteTransition.animateFloat(
            initialValue = start,
            targetValue = end,
            animationSpec = infiniteRepeatable(
                animation = tween(duration),
                repeatMode = RepeatMode.Reverse
            )
        )

        return scale
    }
}