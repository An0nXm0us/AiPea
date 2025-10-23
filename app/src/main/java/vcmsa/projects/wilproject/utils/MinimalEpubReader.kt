package vcmsa.projects.wilproject.utils

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object MinimalEpubReader {
    private const val TAG = "MinimalEpubReader"

    fun getEpubHtmlContent(epubFile: File, cacheDir: File): Pair<String, File>? {
        Log.d(TAG, "Starting EPUB processing for: ${epubFile.name}")

        val extractedDir = File(cacheDir, "epub_${epubFile.nameWithoutExtension}_${System.currentTimeMillis()}")
        if (!extractedDir.mkdirs()) {
            Log.e(TAG, "Failed to create extraction directory: ${extractedDir.absolutePath}")
            return null
        }

        if (!unzipEpub(epubFile, extractedDir)) {
            Log.e(TAG, "Failed to unzip EPUB file.")
            extractedDir.deleteRecursively()
            return null
        }
        Log.d(TAG, "EPUB successfully extracted to: ${extractedDir.absolutePath}")

        val containerFile = File(extractedDir, "META-INF/container.xml")
        val contentFilePath = findContentFilePath(containerFile)
        if (contentFilePath == null) {
            Log.e(TAG, "Could not find the content.opf file path.")
            extractedDir.deleteRecursively()
            return null
        }

        val epubRoot = containerFile.parentFile.parentFile
        val contentFile = File(epubRoot, contentFilePath)

        val combinedHtml = combineChapterHtml(contentFile, epubRoot)

        return if (combinedHtml != null) {
            Log.d(TAG, "Successfully combined all EPUB chapters.")
            Pair(combinedHtml, extractedDir)
        } else {
            Log.e(TAG, "Failed to combine chapter HTML.")
            extractedDir.deleteRecursively()
            null
        }
    }

    private fun unzipEpub(zipFile: File, destinationDir: File): Boolean {
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                var zipEntry: ZipEntry?
                val buffer = ByteArray(1024)
                while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                    val newFile = File(destinationDir, zipEntry!!.name)
                    if (zipEntry!!.isDirectory) {
                        newFile.mkdirs()
                        continue
                    }
                    newFile.parentFile?.mkdirs()

                    FileOutputStream(newFile).use { fileOutputStream ->
                        var count: Int
                        while (zipInputStream.read(buffer).also { count = it } != -1) {
                            fileOutputStream.write(buffer, 0, count)
                        }
                    }
                    zipInputStream.closeEntry()
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during EPUB extraction: ${e.message}", e)
            return false
        }
    }

    private fun findContentFilePath(containerFile: File): String? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(containerFile)

            val rootfiles = doc.getElementsByTagName("rootfile")
            if (rootfiles.length > 0) {
                rootfiles.item(0).attributes.getNamedItem("full-path")?.nodeValue
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding content file path: ${e.message}", e)
            null
        }
    }

    private fun combineChapterHtml(contentFile: File, epubRoot: File): String? {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(contentFile)

            val chapterIds = mutableListOf<String>()
            val itemMap = mutableMapOf<String, String>()

            val manifest = doc.getElementsByTagName("manifest").item(0)
            val items = manifest.childNodes
            for (i in 0 until items.length) {
                val item = items.item(i)
                if (item.nodeName == "item") {
                    val id = item.attributes.getNamedItem("id")?.nodeValue
                    val href = item.attributes.getNamedItem("href")?.nodeValue
                    if (id != null && href != null) {
                        itemMap[id] = href
                    }
                }
            }

            val spine = doc.getElementsByTagName("spine").item(0)
            val itemrefs = spine.childNodes
            for (i in 0 until itemrefs.length) {
                val ref = itemrefs.item(i)
                if (ref.nodeName == "itemref") {
                    val idRef = ref.attributes.getNamedItem("idref")?.nodeValue
                    if (idRef != null) {
                        chapterIds.add(idRef)
                    }
                }
            }

            val opfPath = contentFile.parentFile.relativeTo(epubRoot).path.replace(File.separatorChar, '/') + "/"

            val htmlBuilder = StringBuilder()

            htmlBuilder.append("""
                <html>
                <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: sans-serif;
                        line-height: 1.6;
                        padding: 10px;
                        word-wrap: break-word;
                    }
                    img, figure { max-width: 100%; height: auto; }
                    .app-highlight { background-color: yellow; border-radius: 3px; }
                </style>
                </head>
                <body>
            """.trimIndent())

            for (id in chapterIds) {
                val chapterPath = itemMap[id]
                if (chapterPath != null) {
                    val chapterFile = File(contentFile.parentFile, chapterPath)
                    if (chapterFile.exists()) {
                        Log.d(TAG, "Loading chapter: ${chapterFile.name}")

                        val chapterHtml = chapterFile.readText(Charsets.UTF_8)

                        val cleanedContent = chapterHtml
                            .substringAfter("<body", "")
                            .substringAfter(">")
                            .substringBeforeLast("</body>", "")
                            .trim()

                        val contentWithAssetsBase = cleanedContent
                            .replace("src=\"", "src=\"$opfPath")
                            .replace("src='", "src='$opfPath")

                        val chapterContentWithLinksBase = contentWithAssetsBase
                            .replace(Regex("href=\"(?!(#|http|mailto|tel):)(.*?)\"", RegexOption.IGNORE_CASE)) { matchResult ->
                                val originalHref = matchResult.groupValues[2]
                                "href=\"$opfPath$originalHref\""
                            }
                            .replace(Regex("href='(?!(#|http|mailto|tel):)(.*?)'", RegexOption.IGNORE_CASE)) { matchResult ->
                                val originalHref = matchResult.groupValues[2]
                                "href='$opfPath$originalHref'"
                            }

                        htmlBuilder.append("<div id='chapter-$id' class='epub-chapter-container'>")
                        htmlBuilder.append(chapterContentWithLinksBase)
                        htmlBuilder.append("</div>")

                        htmlBuilder.append("<br><hr style='border: 1px dashed #ccc;'><br>")
                    } else {
                        Log.w(TAG, "Chapter file not found: $chapterPath")
                    }
                }
            }

            htmlBuilder.append("</body></html>")
            return htmlBuilder.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error combining chapter HTML: ${e.message}", e)
            return null
        }
    }
}