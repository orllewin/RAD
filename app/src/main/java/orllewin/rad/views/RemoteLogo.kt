package orllewin.rad.views

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class RemoteLogo {

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
}