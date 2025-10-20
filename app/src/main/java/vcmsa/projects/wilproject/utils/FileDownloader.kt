package vcmsa.projects.wilproject.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileDownloader {

    private val client = OkHttpClient()


    suspend fun downloadAndSave(url: String, filenameBase: String, context: Context): Boolean {
        return try {
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download file: $response")

                val inputStream = response.body?.byteStream() ?: throw IOException("Empty response body")

                // Get the app-specific internal storage directory
                val internalDir = context.filesDir
                // Create a clean filename for the EPUB
                val filename = "${filenameBase.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.epub"
                val file = File(internalDir, filename)

                // Write the network stream to the local file
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                true // Success
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}