package org.community.gallerycamerademo

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

class OutputProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    private fun resolveFile(uri: Uri): File {
        val context = context ?: throw FileNotFoundException("Context unavailable")
        val rawName = uri.lastPathSegment ?: throw FileNotFoundException("Missing file name")
        val safeName = File(rawName).name
        if (safeName.isBlank()) throw FileNotFoundException("Invalid file name")

        val dir = File(context.filesDir, "captures").apply { mkdirs() }
        return File(dir, safeName)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = resolveFile(uri)

        val flags = when {
            mode.contains("w") -> {
                ParcelFileDescriptor.MODE_CREATE or
                    ParcelFileDescriptor.MODE_TRUNCATE or
                    ParcelFileDescriptor.MODE_READ_WRITE
            }
            else -> ParcelFileDescriptor.MODE_READ_ONLY
        }

        if (!file.exists() && !mode.contains("w")) {
            throw FileNotFoundException(file.absolutePath)
        }

        return ParcelFileDescriptor.open(file, flags)
    }

    override fun getType(uri: Uri): String = "image/jpeg"

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

        columns.forEach { column ->
            when (column) {
                OpenableColumns.DISPLAY_NAME -> row.add(file.name)
                OpenableColumns.SIZE -> row.add(if (file.exists()) file.length() else 0L)
                else -> row.add(null)
            }
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return if (resolveFile(uri).delete()) 1 else 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        const val AUTHORITY = "org.community.gallerycamerademo.output"
    }
}
