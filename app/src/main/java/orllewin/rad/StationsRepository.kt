package orllewin.rad

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.*
import okio.IOException
import javax.inject.Inject

class StationsRepository @Inject constructor(){

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().build()

    @JsonClass(generateAdapter = true)
    data class DefaultStations(
        val stations: List<RadStationDTO>?
    )

    @JsonClass(generateAdapter = true)
    data class RadStationDTO(
        val title: String?,
        val website: String?,
        val streamUrl: String?,
        val logoUrl: String?,
        val radImage: String?
    )

    fun getStations(feedUrl: String, onStations: (stations: List<StationEntity>, error: String?) -> Unit) = getRemoteStations(feedUrl, onStations)

    private fun getRemoteStations(feedUrl: String, onStations: (stations: List<StationEntity>, error: String?) -> Unit){
        val request = Request.Builder()
            .url(feedUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val jsonAdapter: JsonAdapter<DefaultStations> = moshi.adapter(DefaultStations::class.java)
                    val defaultStation: DefaultStations? = jsonAdapter.fromJson(response.body!!.string())

                    defaultStation?.let{
                        val stationsEntities = mutableListOf<StationEntity>()
                        defaultStation.stations?.forEachIndexed { index, stationDto ->
                            stationsEntities.add(
                                StationEntity(
                                title = stationDto.title ?: "",
                                website = stationDto.website,
                                streamUrl = stationDto.streamUrl ?: "",
                                logoUrl = stationDto.logoUrl
                            )
                            )
                        }

                        onStations(stationsEntities, null)
                    } ?: run{
                        onStations(listOf(), "Error parsing DefaultStations - null")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onStations(listOf(), e.toString())
            }
        })
    }
}