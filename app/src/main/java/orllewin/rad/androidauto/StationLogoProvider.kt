package orllewin.rad.androidauto

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import orllewin.rad.NOTIFICATION_ID
import orllewin.rad.R
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

class StationLogoProvider: ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? =  null
    override fun getType(uri: Uri): String = "image/png"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = -1
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = -1

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = this.context ?: return null

        if(uri.getQueryParameter("art_url") != null) {
            val url = uri.getQueryParameter("art_url")
            var file = File(context.cacheDir, "${Uri.parse(url).path}")
            if (!file.exists()) {

                val cacheFile = Glide.with(context)
                    .asFile()
                    .load(url)
                    .submit()
                    .get(10, TimeUnit.SECONDS)

                cacheFile.renameTo(file)
                file = cacheFile

            }

            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }else{
            //Local file
            val file = File("${uri.path}")
            if (!file.exists()) {
                throw FileNotFoundException(uri.path)
            }
            // Only allow access to files under cache path
            val cachePath = context.cacheDir.path
            if (!file.path.startsWith(cachePath)) {
                throw FileNotFoundException()
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}