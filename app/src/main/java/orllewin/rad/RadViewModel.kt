package orllewin.rad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RadViewModel @Inject constructor(val stationsRepository: StationsRepository): ViewModel() {

    var uiState by mutableStateOf(RadUIState(listOf(), null, true))
    private set

    fun getRadStations(feedUrl: String) {
        uiState.loading = true
        stationsRepository.getStations(feedUrl){ stations, error ->
            uiState = RadUIState(stations, error)
        }
    }
}