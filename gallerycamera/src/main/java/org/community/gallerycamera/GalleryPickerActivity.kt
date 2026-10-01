package org.community.gallerycamera

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class GalleryPickerActivity : Activity() {

    private lateinit var list: LinearLayout
    private val galleryDir by lazy { File(filesDir, "gallery").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Elegir desde Gallery Camera"
            textSize = 24f
        })

        root.addView(TextView(this).apply {
            text = "Selecciona una imagen para devolverla a la aplicación solicitante."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        })

        root.addView(Button(this).apply {
            text = "Agregar imagen"
            setOnClickListener { importImage() }
        })

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        refresh()
    }

    private fun importImage() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(picker, REQ_IMPORT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK) return

        val uri = data?.data ?: return
        if (importUri(uri)) {
            Toast.makeText(this, "Imagen agregada", Toast.LENGTH_SHORT).show()
            refresh()
        } else {
            Toast.makeText(this, "No se pudo importar la imagen", Toast.LENGTH_LONG).show()
        }
    }

    private fun importUri(uri: Uri): Boolean = runCatching {
        val name = queryName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(galleryDir, "${System.currentTimeMillis()}_$safe")

        contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        true
    }.getOrDefault(false)

    private fun queryName(uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun refresh() {
        list.removeAllViews()

        val files = galleryDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No hay imágenes. Pulsa Agregar imagen."
                textSize = 16f
                setPadding(0, dp(24), 0, 0)
            })
            return
        }

        files.forEach { file ->
            val image = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(0, dp(10), 0, dp(10))
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath, options))
                setOnClickListener { returnFile(file) }
            }

            list.addView(
                image,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(220)
                )
            )
        }
    }

    private fun returnFile(file: File) {
        val uri = GalleryShareProvider.uriFor(file)

        val result = Intent().apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("image", uri)
        }

        setResult(RESULT_OK, result)
        finish()
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_IMPORT = 4101
    }
}
