package orllewin.rad

data class RadUIState(
    var stations: List<StationEntity> = mutableListOf(),
    var error: String? = null,
    var loading: Boolean = false
)