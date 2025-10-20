package vcmsa.projects.wilproject.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass





@JsonClass(generateAdapter = true)
data class BookNetwork(
    val id: Int,
    val title: String,
    val authors: List<AuthorNetwork>,
    // Map of Mime Type/File Format -> URL
    val formats: Map<String, String>,
    @Json(name = "media_type") val mediaType: String
)


@JsonClass(generateAdapter = true)
data class AuthorNetwork(
    val name: String
)
