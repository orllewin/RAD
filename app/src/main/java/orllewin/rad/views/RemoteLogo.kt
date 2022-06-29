package orllewin.rad.views

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class RemoteLogo {

    val grayScaleMatrix = ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    @SuppressLint("NotConstructor")
    @Composable
    fun RemoteLogo(imageUrl: String?, title: String?, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clickable { onClick() }
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,

                onSuccess = {
                    println("Loading image: $imageUrl")
                }
            )
        }
    }

    @Composable
    fun RemoteLogoFill(imageUrl: String?, title: String?) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,

                onSuccess = {
                    println("Loading image: $imageUrl")
                }
            )
        }
    }

    @Composable
    fun RemoteLogoRound(imageUrl: String?, title: String?, size: Dp) {
        Box(
            modifier = Modifier
                .size(size),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.clip(CircleShape),
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply {
                    //setToSaturation(0.0f)
                }),
                onSuccess = {
                    println("Loading image: $imageUrl")
                }
            )
        }
    }
}