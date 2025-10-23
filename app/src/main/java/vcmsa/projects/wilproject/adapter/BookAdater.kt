package vcmsa.projects.wilproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wilproject.R
import vcmsa.projects.wilproject.models.Book
import vcmsa.projects.wilproject.models.DownloadStatus

class BookAdapter(
    private val onBookClicked: (Book) -> Unit,
    private val onDownloadClicked: (Book) -> Unit,

    private val onPdfViewClicked: (Book, String) -> Unit,
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var books: List<Book> = emptyList()

    private var downloadStatuses: Map<Int, DownloadStatus> = emptyMap()

    private var localBookPaths: Map<Int, String> = emptyMap()

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.text_view_title)
        val author: TextView = itemView.findViewById(R.id.text_view_author)

        private val downloadButton: Button = itemView.findViewById(R.id.button_download)
        private val viewPdfButton: Button = itemView.findViewById(R.id.button_view_pdf)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val statusText: TextView = itemView.findViewById(R.id.text_view_status)

        fun bind(book: Book) {
            title.text = book.title
            val authorString = book.authors.joinToString { it.name }
            author.text = "Author(s): $authorString"

            itemView.setOnClickListener { onBookClicked(book) }

            val status = downloadStatuses[book.id] ?: DownloadStatus.NOT_STARTED
            val localPath = localBookPaths[book.id]

            if (localPath != null) {
                downloadButton.isVisible = false
                viewPdfButton.isVisible = true
                progressBar.isVisible = false
                statusText.isVisible = true
                statusText.text = "Downloaded"
                viewPdfButton.setOnClickListener { onPdfViewClicked(book, localPath) }
                return
            }

            when (status) {
                DownloadStatus.NOT_STARTED, DownloadStatus.FAILED -> {
                    val hasPdf = book.getDownloadUrl() != null
                    downloadButton.isVisible = hasPdf
                    viewPdfButton.isVisible = false
                    progressBar.isVisible = false
                    statusText.isVisible = !hasPdf || status == DownloadStatus.FAILED

                    if (!hasPdf) {
                        statusText.text = "Download link unavailable"
                    } else if (status == DownloadStatus.FAILED) {
                        statusText.text = "Failed! Try again."
                    } else {
                        statusText.text = ""
                    }
                    downloadButton.setOnClickListener { onDownloadClicked(book) }
                }
                DownloadStatus.PENDING, DownloadStatus.DOWNLOADING -> {
                    // Show progress while downloading
                    downloadButton.isVisible = false
                    viewPdfButton.isVisible = false
                    progressBar.isVisible = true
                    statusText.isVisible = true
                    statusText.text = if (status == DownloadStatus.PENDING) "Queued..." else "Downloading..."
                    downloadButton.setOnClickListener(null)
                }
                DownloadStatus.COMPLETE -> {
                    downloadButton.isVisible = false
                    viewPdfButton.isVisible = true
                    progressBar.isVisible = false
                    statusText.isVisible = true
                    statusText.text = "Downloaded (Path Pending)"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    fun updateDownloadStatuses(newStatuses: Map<Int, DownloadStatus>) {
        downloadStatuses = newStatuses
        notifyDataSetChanged()
    }


    fun updateLocalPaths(newPaths: Map<Int, String>) {
        localBookPaths = newPaths
        notifyDataSetChanged()
    }
}
