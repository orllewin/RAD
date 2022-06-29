package orllewin.rad

data class RadUIState(
    var stations: List<Station> = mutableListOf(),
    var error: String? = null,
    var loading: Boolean = false
)