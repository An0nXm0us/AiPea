package vcmsa.projects.wilproject.models

data class Book(
    val id: Int,
    val title: String,
    val authors: List<Author>,
    val formats: Map<String, String>
) {

    fun getDownloadUrl(): String? {
        val epubUrl = formats.entries.firstOrNull { (key, _) ->
            key.contains("epub", ignoreCase = true)
        }?.value

        if (epubUrl != null) return epubUrl

        val pdfUrl = formats.entries.firstOrNull { (key, _) ->
            key.contains("pdf", ignoreCase = true)
        }?.value

        return pdfUrl
    }
    fun getDownloadExtension(): String? {
        // Priority 1: Check for EPUB
        val isEpub = formats.keys.any { key ->
            key.contains("epub", ignoreCase = true)
        }
        if (isEpub) return ".epub"

        // Priority 2: Check for PDF
        val isPdf = formats.keys.any { key ->
            key.contains("pdf", ignoreCase = true)
        }
        if (isPdf) return ".pdf"

        return null
    }
}
