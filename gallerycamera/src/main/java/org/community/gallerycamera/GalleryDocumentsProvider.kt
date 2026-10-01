package org.community.gallerycamera

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class GalleryDocumentsProvider : DocumentsProvider() {

    private val galleryDir: File
        get() = File(requireNotNull(context).filesDir, "gallery").apply { mkdirs() }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val columns = projection ?: ROOT_COLUMNS
        val cursor = MatrixCursor(columns)
        val row = cursor.newRow()

        for (column in columns) {
            row.add(
                when (column) {
                    DocumentsContract.Root.COLUMN_ROOT_ID -> ROOT_ID
                    DocumentsContract.Root.COLUMN_DOCUMENT_ID -> ROOT_DOCUMENT_ID
                    DocumentsContract.Root.COLUMN_TITLE -> "Gallery Camera"
                    DocumentsContract.Root.COLUMN_SUMMARY -> "Imágenes guardadas en Gallery Camera"
                    DocumentsContract.Root.COLUMN_FLAGS -> DocumentsContract.Root.FLAG_LOCAL_ONLY
                    DocumentsContract.Root.COLUMN_MIME_TYPES -> "image/*"
                    DocumentsContract.Root.COLUMN_AVAILABLE_BYTES -> galleryDir.usableSpace
                    DocumentsContract.Root.COLUMN_ICON -> android.R.drawable.ic_menu_gallery
                    else -> null
                }
            )
        }
        return cursor
    }

    override fun queryDocument(
        documentId: String,
        projection: Array<out String>?
    ): Cursor {
        val columns = projection ?: DOCUMENT_COLUMNS
        val cursor = MatrixCursor(columns)
        addDocumentRow(cursor, columns, documentId)
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        if (parentDocumentId != ROOT_DOCUMENT_ID) {
            throw FileNotFoundException("Unknown parent: $parentDocumentId")
        }

        val columns = projection ?: DOCUMENT_COLUMNS
        val cursor = MatrixCursor(columns)

        galleryDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { file ->
                addFileRow(cursor, columns, file)
            }

        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        if (mode.contains("w")) {
            throw FileNotFoundException("Read-only provider")
        }

        val file = fileForDocumentId(documentId)
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException(documentId)
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getDocumentType(documentId: String): String {
        return if (documentId == ROOT_DOCUMENT_ID) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            mimeFor(fileForDocumentId(documentId))
        }
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return parentDocumentId == ROOT_DOCUMENT_ID && documentId.startsWith(FILE_PREFIX)
    }

    private fun addDocumentRow(
        cursor: MatrixCursor,
        columns: Array<out String>,
        documentId: String
    ) {
        if (documentId == ROOT_DOCUMENT_ID) {
            val row = cursor.newRow()
            for (column in columns) {
                row.add(
                    when (column) {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID -> ROOT_DOCUMENT_ID
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME -> "Gallery Camera"
                        DocumentsContract.Document.COLUMN_MIME_TYPE -> DocumentsContract.Document.MIME_TYPE_DIR
                        DocumentsContract.Document.COLUMN_FLAGS -> DocumentsContract.Document.FLAG_DIR_PREFERS_GRID
                        DocumentsContract.Document.COLUMN_ICON -> android.R.drawable.ic_menu_gallery
                        else -> null
                    }
                )
            }
            return
        }

        addFileRow(cursor, columns, fileForDocumentId(documentId))
    }

    private fun addFileRow(
        cursor: MatrixCursor,
        columns: Array<out String>,
        file: File
    ) {
        if (!file.exists() || !file.isFile) return

        val row = cursor.newRow()
        for (column in columns) {
            row.add(
                when (column) {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID -> FILE_PREFIX + file.name
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME -> file.name
                    DocumentsContract.Document.COLUMN_MIME_TYPE -> mimeFor(file)
                    DocumentsContract.Document.COLUMN_SIZE -> file.length()
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED -> file.lastModified()
                    DocumentsContract.Document.COLUMN_FLAGS -> 0
                    else -> null
                }
            )
        }
    }

    private fun fileForDocumentId(documentId: String): File {
        if (!documentId.startsWith(FILE_PREFIX)) {
            throw FileNotFoundException(documentId)
        }

        val name = documentId.removePrefix(FILE_PREFIX)
        val safe = File(name).name
        if (safe != name || safe.isBlank()) {
            throw FileNotFoundException("Invalid document id")
        }

        return File(galleryDir, safe)
    }

    private fun mimeFor(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
    }

    companion object {
        const val AUTHORITY = "org.community.gallerycamera.documents"

        private const val ROOT_ID = "gallery"
        private const val ROOT_DOCUMENT_ID = "root"
        private const val FILE_PREFIX = "file:"

        private val ROOT_COLUMNS = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
            DocumentsContract.Root.COLUMN_ICON
        )

        private val DOCUMENT_COLUMNS = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )
    }
}
