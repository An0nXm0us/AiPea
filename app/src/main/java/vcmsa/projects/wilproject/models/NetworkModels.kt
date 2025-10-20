package vcmsa.projects.wilproject.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


class NetworkModels {


    @JsonClass(generateAdapter = true)
    data class GutendexResponse(
        val count: Int,
        val next: String?,
        val previous: String?,
        val results: List<BookNetwork>
    )


    @JsonClass(generateAdapter = true)
    data class BookNetwork(
        val id: Int,
        val title: String,
        val authors: List<AuthorNetwork>,
        val formats: Map<String, String>,
        @Json(name = "media_type") val mediaType: String
    ) {
       fun getDownloadUrl(): String? {
            val epubUrl = formats.entries.firstOrNull { it.key.contains("application/epub") }?.value
            if (epubUrl != null) return epubUrl

            val pdfUrl = formats.entries.firstOrNull { it.key.contains("application/pdf") }?.value
            if (pdfUrl != null) return pdfUrl

            return null
        }

        fun getDownloadExtension(): String {
            val formatsMap = formats.keys.map { it.lowercase()}
            return when {
                formatsMap.any { it.contains("application/epub") } -> ".epub"
                formatsMap.any { it.contains("application/pdf") } -> ".pdf"
                else -> ".book"
            }
        }
    }


    @JsonClass(generateAdapter = true)
    data class AuthorNetwork(
        val name: String
    )

}