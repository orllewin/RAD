package orllewin.rad

class StationEntity(
    val title: String,
    val website: String?,
    val streamUrl: String,
    val logoUrl: String?){

    fun hasLogoUrl(): Boolean = logoUrl != null
    fun hasWebsite(): Boolean = website != null && website.isNotEmpty()
}