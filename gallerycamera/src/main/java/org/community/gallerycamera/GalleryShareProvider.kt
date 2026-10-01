package org.community.gallerycamera

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class GalleryShareProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun galleryDir(): File =
        File(requireNotNull(context).filesDir, "gallery").apply { mkdirs() }

    private fun resolveFile(uri: Uri): File {
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "file") {
            throw FileNotFoundException("Invalid URI")
        }

        val name = segments[1]
        val safe = File(name).name
        if (safe != name || safe.isBlank()) {
            throw FileNotFoundException("Invalid file")
        }

        val file = File(galleryDir(), safe)
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException(name)
        }
        return file
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode.contains("w")) {
            throw FileNotFoundException("Read-only provider")
        }
        return ParcelFileDescriptor.open(resolveFile(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        val file = resolveFile(uri)
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/*"
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val file = resolveFile(uri)
        val columns = projection ?: arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        )
        val cursor = MatrixCursor(columns)
        val row = cursor.newRow()

        for (column in columns) {
            row.add(
                when (column) {
                    OpenableColumns.DISPLAY_NAME -> file.name
                    OpenableColumns.SIZE -> file.length()
                    else -> null
                }
            )
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY = "org.community.gallerycamera.shared"

        fun uriFor(file: File): Uri =
            Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath("file")
                .appendPath(file.name)
                .build()
    }
}
