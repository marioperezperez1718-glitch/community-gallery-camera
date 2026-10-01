package org.community.gallerycamerademo

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private var outputUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            text = "Gallery Camera Demo"
            textSize = 25f
        })

        status = TextView(this).apply {
            text = "Pulsa el botón para solicitar una imagen."
            textSize = 15f
            setPadding(0, dp(10), 0, dp(16))
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Solicitar foto"
            setOnClickListener { requestPhoto() }
        })

        preview = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        root.addView(preview, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(360)
        ))

        setContentView(root)
    }

    private fun requestPhoto() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "gallery_camera_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GalleryCameraDemo")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            Toast.makeText(this, "No se pudo crear la imagen de salida.", Toast.LENGTH_LONG).show()
            return
        }
        outputUri = uri

        val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            component = ComponentName(
                "org.community.gallerycamera",
                "org.community.gallerycamera.CaptureActivity"
            )
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("output", uri)
        }

        runCatching { startActivityForResult(capture, REQ_CAPTURE) }
            .onFailure {
                contentResolver.delete(uri, null, null)
                outputUri = null
                Toast.makeText(
                    this,
                    "Instala Gallery Camera y la Demo de la misma compilación.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAPTURE) return

        val uri = outputUri ?: return

        if (resultCode == RESULT_OK) {
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, this, null, null)
            }
            preview.setImageURI(null)
            preview.setImageURI(uri)
            status.text = "Resultado recibido correctamente."
        } else {
            contentResolver.delete(uri, null, null)
            status.text = "Solicitud cancelada."
        }
        outputUri = null
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_CAPTURE = 3001
    }
}
