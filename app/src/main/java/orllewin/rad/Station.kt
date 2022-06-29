package orllewin.rad

import androidx.compose.ui.graphics.Color

class Station(
    val title: String,
    val website: String?,
    val streamUrl: String,
    val logoUrl: String?,
    val colour: Color? = null){

    fun hasLogoUrl(): Boolean = logoUrl != null
    fun hasWebsite(): Boolean = website != null && website.isNotEmpty()
}