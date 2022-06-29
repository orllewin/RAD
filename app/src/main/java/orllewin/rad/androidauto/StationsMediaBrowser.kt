package orllewin.rad.androidauto

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import orllewin.rad.StationsRepository
import javax.inject.Inject

/**
 * For debugging:
 * - On your phone go to Settings > All Apps > Android Auto: Additional Settings in the app
 * - In the app enable debug mode (ten presses on the version)
 * - In the app, in the newly available Develop Settings: scroll down and check 'Unknown Sources'
 * - In the overflow tap 'Start head unit server'
 * - On your computer execute SDK_LOCATION/platform-tools/./adb forward tcp:5277 tcp:5277
 * - On your computer execute SDK_LOCATION/extras/google/auto/./desktop-head-unit
 *      eg. /Users/fish/Library/Android/sdk/platform-tools/./adb forward tcp:5277 tcp:5277
 *      eg. /Users/fish/Library/Android/sdk/extras/google/auto/./desktop-head-unit
 *
 * If you don't have auto/desktop-head-unit you need to use the beta channel Android Studio,
 * you can switch back to stable once these tools are installed, they'll remain
 */

@AndroidEntryPoint
class StationsMediaBrowser: MediaBrowserServiceCompat() {

    @Inject
    lateinit var stationsRepository: StationsRepository

    lateinit var useCase: AndroidAutoUseCase

    private val radioAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val handleAudioFocus = true

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(applicationContext).build().apply {
            setAudioAttributes(radioAudioAttributes, handleAudioFocus)
            setHandleAudioBecomingNoisy(true)
        }
    }

    var stationsSession: MediaSessionCompat? = null

    var streamStart = 0L
    var pauseStart = 0L

    override fun onCreate() {
        super.onCreate()

        useCase = AndroidAutoUseCase(applicationContext, stationsRepository)

        stationsSession = MediaSessionCompat(applicationContext, "StationsMediaBrowser_Session").apply {
            setCallback(object: MediaSessionCompat.Callback() {

                override fun onStop() {
                    super.onStop()
                    setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                            .setActions(PlaybackStateCompat.ACTION_PLAY).build())
                    exoPlayer.pause()
                }

                override fun onPause() {
                    super.onPause()
                    setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, exoPlayer.contentPosition, 1.0f)
                            .setActions(PlaybackStateCompat.ACTION_PLAY).build())
                    exoPlayer.pause()
                    pauseStart = System.currentTimeMillis()
                }

                override fun onPlay() {
                    super.onPlay()

                    setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PLAYING, exoPlayer.contentPosition, 1.0f)
                            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build())
                    exoPlayer.play()
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPlayFromMediaId(mediaId, extras)
                    mediaId?.let{
                        println("AAUTO: onPlayFromMediaId(): $mediaId")
                        val dataSourceFactory = DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, "Radio"))
                        val mediaItem = MediaItem.fromUri(Uri.parse(mediaId))
                        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                        exoPlayer.setMediaSource(mediaSource)

                        stationsSession?.setMetadata(useCase.getMetadata(mediaId))
                        stationsSession?.controller?.transportControls?.play()

                        streamStart = System.currentTimeMillis()

                        setPlaybackState(
                            PlaybackStateCompat.Builder()
                                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE).build())

                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                }
            })
        }

        sessionToken = stationsSession?.sessionToken
    }
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val extras = Bundle()

        //Request grid for every type - browsable and playable:
        extras.putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        extras.putInt(
            MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
            MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        return BrowserRoot("root", extras)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        println("AndroidAuto onLoadChildren(): parentId: $parentId")
        val isSynchronous = useCase.getChildren(parentId){ id, children ->
            println("AndroidAuto onLoadChildren(): onChildren: $parentId returned ${children.size} children")
            result.sendResult(children.toMutableList())
        }

        if(!isSynchronous) result.detach()
    }


}