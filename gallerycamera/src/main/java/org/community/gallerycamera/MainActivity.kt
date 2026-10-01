package org.community.gallerycamera

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private val galleryDir by lazy { File(filesDir, "gallery").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Gallery Camera"
            textSize = 26f
        })

        root.addView(TextView(this).apply {
            text = "Importa imágenes, úsalas desde Gallery Camera o publícalas para que aparezcan en selectores de fotos de Android."
            textSize = 15f
            setPadding(0, dp(8), 0, dp(16))
        })

        root.addView(Button(this).apply {
            text = "Importar imágenes"
            setOnClickListener { openPicker() }
        })

        root.addView(Button(this).apply {
            text = "Publicar todo en galería del sistema"
            setOnClickListener { publishAllToMediaStore() }
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

        Toast.makeText(
            this,
            "$imported imagen(es) importada(s)",
            Toast.LENGTH_SHORT
        ).show()

        refresh()
    }

    private fun importUri(uri: Uri): Boolean = runCatching {
        val name = queryName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val dest = File(galleryDir, "${System.currentTimeMillis()}_$safe")

        contentResolver.openInputStream(uri)!!.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
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

    private fun publishAllToMediaStore() {
        val files = galleryDir.listFiles()
            ?.filter { it.isFile }
            .orEmpty()

        if (files.isEmpty()) {
            Toast.makeText(
                this,
                "No hay imágenes para publicar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        var published = 0
        var failed = 0

        files.forEach { file ->
            if (publishFile(file)) {
                published++
            } else {
                failed++
            }
        }

        val message = if (failed == 0) {
            "$published imagen(es) publicadas en Pictures/GalleryCamera"
        } else {
            "$published publicadas, $failed con error"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun publishFile(file: File): Boolean = runCatching {
        val mime = contentResolver.getType(GalleryShareProvider.uriFor(file))
            ?: when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "GC_${System.currentTimeMillis()}_${file.name}"
            )
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/GalleryCamera"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("No se pudo crear la entrada de MediaStore")

        try {
            file.inputStream().use { input ->
                contentResolver.openOutputStream(uri, "w")!!.use { output ->
                    input.copyTo(output)
                }
            }

            contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                },
                null,
                null
            )
        } catch (error: Throwable) {
            contentResolver.delete(uri, null, null)
            throw error
        }

        true
    }.getOrDefault(false)

    private fun refresh() {
        list.removeAllViews()

        val files = galleryDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        if (files.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Aún no hay imágenes."
                textSize = 16f
                setPadding(0, dp(24), 0, 0)
            })
            return
        }

        files.forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }

            val image = ImageView(this).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP

                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }

                setImageBitmap(
                    BitmapFactory.decodeFile(file.absolutePath, options)
                )

                setOnClickListener {
                    Toast.makeText(
                        this@MainActivity,
                        file.name,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            card.addView(
                image,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(220)
                )
            )

            card.addView(TextView(this).apply {
                text = file.name
                textSize = 13f
                setPadding(0, dp(6), 0, 0)
            })

            list.addView(card)
        }
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_PICK = 1001
    }
}
