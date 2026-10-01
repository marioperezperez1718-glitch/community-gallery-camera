package org.community.gallerycamera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class CaptureActivity : Activity() {
    private lateinit var list: LinearLayout
    private val galleryDir by lazy { File(filesDir, "gallery").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Selecciona una imagen"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "La imagen elegida se devolverá a la aplicación de prueba solicitante."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        })
        root.addView(Button(this).apply {
            text = "Agregar imagen"
            setOnClickListener { openPicker() }
        })

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        setContentView(root)
        refresh()
    }

    private fun openPicker() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(i, REQ_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return

        var imported = 0
        val clip = data.clipData
        if (clip != null) {
            for (index in 0 until clip.itemCount) {
                if (importUri(clip.getItemAt(index).uri)) imported++
            }
        } else {
            data.data?.let { if (importUri(it)) imported++ }
        }
        Toast.makeText(this, "$imported imagen(es) agregada(s)", Toast.LENGTH_SHORT).show()
        refresh()
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
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }

    private fun refresh() {
        list.removeAllViews()
        val files = galleryDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()

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
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                setImageBitmap(BitmapFactory.decodeFile(file.absolutePath, opts))
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { returnSelected(file) }
            }
            list.addView(image, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            ))
        }
    }

    private fun returnSelected(file: File) {
        @Suppress("DEPRECATION")
        val outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT) as? Uri

        if (outputUri != null) {
            val result = runCatching {
                file.inputStream().use { input ->
                    contentResolver.openOutputStream(outputUri, "w")?.use { output ->
                        input.copyTo(output)
                    } ?: error("openOutputStream devolvió null")
                }
            }

            if (result.isFailure) {
                val error = result.exceptionOrNull()
                Toast.makeText(
                    this,
                    "No se pudo escribir la imagen: ${error?.javaClass?.simpleName}: ${error?.message}",
                    Toast.LENGTH_LONG
                ).show()
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            setResult(RESULT_OK)
            finish()
            return
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val thumb: Bitmap = ThumbnailUtils.extractThumbnail(bitmap, 320, 320)
        setResult(RESULT_OK, Intent().putExtra("data", thumb))
        finish()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_PICK = 2001
    }
}
