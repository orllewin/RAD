package orllewin.rad.androidauto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import orllewin.rad.StationsRepository
import orllewin.rad.dataStore
import orllewin.rad.Station

class AndroidAutoUseCase(val context: Context, val stationsRepository: StationsRepository) {

    val root = "root"
    val networkRoot = "network_root"
    val localRoot = "network_root"

    private val urlPrefKey = stringPreferencesKey("radio_feed_url")
    private var feedUrl = "https://orllewin.uk/orllewin_stations.json"
    private var prefs: Preferences = runBlocking { context.dataStore.data.first() }

    private val childMap = HashMap<String, List<MediaBrowserCompat.MediaItem>>()
    private val metadata = mutableListOf<MediaMetadataCompat>()

    init {
        feedUrl = prefs[urlPrefKey] ?: "https://orllewin.uk/orllewin_stations.json"
    }

    /**
     *
     * @param mediaId - the parent media id identifier
     * @param onChildren - response lamda
     * @return isSynchronous - true if root or children already exist in childMap, otherwise false: expect async operations
     */
    fun getChildren(mediaId: String, onChildren: (mediaId: String, children: List<MediaBrowserCompat.MediaItem>) -> Unit): Boolean{
        println("AndroidAutoUseCase: getChildren() mediaId: $mediaId")
        return when {
            childMap.containsKey(mediaId) -> {
                onChildren(mediaId, childMap[mediaId]!!)
                true
            }
            mediaId == root -> {
                getRoot(onChildren)
                true
            }
            mediaId == networkRoot -> {
                getAsync(mediaId, onChildren)
                false
            }
            else -> {
                getAsync(mediaId, onChildren)
                false
            }
        }
    }

    private fun getRoot(onChildren: (mediaId: String, children: List<MediaBrowserCompat.MediaItem>) -> Unit){
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val metadatas = mutableListOf<MediaMetadataCompat>()
        val remoteMetaData = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, networkRoot)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Radio Stations")
        }.build()
        metadatas.add(remoteMetaData)
        mediaItems.add(MediaBrowserCompat.MediaItem(remoteMetaData.description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE))

        onChildren(root, mediaItems)
    }

    private fun getAsync(parentMediaId: String, onChildren: (mediaId: String, children: List<MediaBrowserCompat.MediaItem>) -> Unit){
        stationsRepository.getStations(feedUrl){ stations, error ->
            if(error != null){
                //todo
                println("$error")
            }else {
                val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
                val metadatas = mutableListOf<MediaMetadataCompat>()
                stations.forEach { station ->
                    val metadata = MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, station.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, station.streamUrl)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri(station))
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, station.streamUrl)
                        .build()

                    metadatas.add(metadata)
                    mediaItems.add(
                        MediaBrowserCompat.MediaItem(
                            metadata.description,
                            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                        )
                    )
                }
                metadata.clear()
                metadata.addAll(metadatas)
                childMap[parentMediaId] = mediaItems
                onChildren(parentMediaId, mediaItems)
            }
        }
    }

    private fun artUri(station: Station): String {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(authority(context))
            .appendEncodedPath(Uri.parse(station.logoUrl).path)
            .appendQueryParameter("art_url", station.logoUrl)
            .build().toString()
    }

    private fun authority(context: Context): String{
        val applicationId = context.packageName
        return "${applicationId}.artwork_provider"
    }

    fun getMetadata(mediaId: String): MediaMetadataCompat?{
        return metadata.find { metadata  ->
            metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == mediaId
        }
    }
}