package orllewin.rad

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.audio.AudioAttributes

const val START_RAD_SERVICE = 111
const val MUTE_RAD_SERVICE = 112
const val UNMUTE_RAD_SERVICE = 113
const val STOP_RAD_SERVICE = 114

const val NOTIFICATION_ID = 1001

class RadService: Service() {

    companion object{

        fun getIntent(context: Context, radStation: StationEntity):Intent{
            return Intent(context, RadService::class.java).also { intent ->
                intent.putExtra("action", START_RAD_SERVICE)
                intent.putExtra("title", radStation.title)
                intent.putExtra("streamUrl", radStation.streamUrl)
                intent.putExtra("logoUrl", radStation.logoUrl)
            }
        }

        fun getStopIntent(context: Context):Intent{
            return Intent(context, RadService::class.java).also { intent ->
                intent.putExtra("action", STOP_RAD_SERVICE)
            }
        }

        fun radServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (RadService::class.java.name == service.service.className) {
                    if (service.foreground) {
                        return true
                    }
                }
            }
            return false
        }
    }
    private lateinit var notification: Notification
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var muteAction: NotificationCompat.Action
    private lateinit var unmuteAction: NotificationCompat.Action
    private lateinit var stopAction: NotificationCompat.Action

    private lateinit var  muteIntent: PendingIntent
    private lateinit var  unmuteIntent: PendingIntent
    private lateinit var  stopIntent: PendingIntent

    private var volume = 1.0f

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(uAmpAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        l("RadService onStartCommand()")
        intent?.run{
            when(getIntExtra("action", -1)){
                START_RAD_SERVICE -> initialise(getStringExtra("title"), getStringExtra("streamUrl"), getStringExtra("logoUrl"))
                MUTE_RAD_SERVICE -> mute()
                UNMUTE_RAD_SERVICE -> unmute()
                STOP_RAD_SERVICE -> stop()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initialise(title: String?, streamUrl: String?, logoUrl: String?) {
        println("Initialise RAD: $title")

        player.stop()

        val mediaSession = MediaSessionCompat(this, "RadService")

        val metadata =  MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "Radio")
            .build()

        mediaSession.setMetadata(metadata)

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken)
        mediaStyle.setShowActionsInCompactView(0, 1, 2)
        muteIntent = PendingIntent.getService(
            this,
            MUTE_RAD_SERVICE,
            Intent(this, RadService::class.java).also { intent ->
                intent.putExtra("action", MUTE_RAD_SERVICE)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        unmuteIntent = PendingIntent.getService(
            this,
            UNMUTE_RAD_SERVICE,
            Intent(this, RadService::class.java).also { intent ->
                intent.putExtra("action", UNMUTE_RAD_SERVICE)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        stopIntent = PendingIntent.getService(
            this,
            STOP_RAD_SERVICE,
            Intent(this, RadService::class.java).also { intent ->
                intent.putExtra("action", STOP_RAD_SERVICE)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntent: PendingIntent = Intent(this, RadComposeActivity::class.java).run {
            PendingIntent.getActivity(applicationContext, 0, this, PendingIntent.FLAG_IMMUTABLE)
        }

        val channelId = createNotificationChannel("rad_service", "RAD CHANNEL")

        notificationBuilder = NotificationCompat.Builder(this, channelId)

        muteAction = NotificationCompat.Action.Builder(IconCompat.createWithResource(applicationContext, R.drawable.vector_pause), "Mute", muteIntent).build()
        unmuteAction = NotificationCompat.Action.Builder(IconCompat.createWithResource(applicationContext, R.drawable.vector_play), "Unmute", unmuteIntent).build()
        stopAction = NotificationCompat.Action.Builder(IconCompat.createWithResource(applicationContext, R.drawable.vector_stop), "Stop", stopIntent).build()


        l("Asking exoPlayer to play...")
        val mediaItem = MediaItem.fromUri("$streamUrl")
        player.removeMediaItems(0, player.mediaItemCount)
        player.addMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        //not working
//        mediaSession.isActive = true
//        mediaSession.setPlaybackState(
//            PlaybackStateCompat.Builder()
//                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
//                .build())

        val request = ImageRequest.Builder(applicationContext)
            .data(logoUrl)
            .target(
                onStart = { placeholder ->
                    // Handle the placeholder drawable.
                },
                onSuccess = { result ->
                    // Handle the successful result.
                    notification = notificationBuilder
                        .setStyle(mediaStyle)
                        .setContentTitle("Radio")
                        .setContentText(title)
                        .setSmallIcon(R.drawable.vector_notification_icon)
                        .setLargeIcon(result.toBitmap())
                        .setContentIntent(pendingIntent)
                        .addAction(muteAction)
                        .addAction(stopAction)
                        .build()

                    startForeground(NOTIFICATION_ID, notification)
                },
                onError = { error ->
                    // Handle the error drawable.
                    notification = notificationBuilder
                        .setStyle(mediaStyle)
                        .setContentTitle("Radio")
                        .setContentText(title)
                        .setSmallIcon(R.drawable.vector_notification_icon)
                        .setContentIntent(pendingIntent)
                        .addAction(muteAction)
                        .addAction(stopAction)
                        .build()

                    startForeground(NOTIFICATION_ID, notification)
                }
            )
            .build()
        imageLoader.enqueue(request)
    }

    private fun mute(){
        volume = player.volume
        player.volume = 0f
        notification = notificationBuilder
            .clearActions()
            .addAction(unmuteAction)
            .addAction(stopAction)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun unmute(){
        player.volume = volume
        notification = notificationBuilder
            .clearActions()
            .addAction(muteAction)
            .addAction(stopAction)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stop(){
        player.stop()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.cancel(NOTIFICATION_ID)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun l(message: String) = println("RADService: $message")
}