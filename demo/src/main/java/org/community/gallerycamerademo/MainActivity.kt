package org.community.gallerycamerademo

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
        outputUri = savedInstanceState?.getString(KEY_OUTPUT_URI)?.let(Uri::parse)

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
        root.addView(
            preview,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)
            )
        )

        setContentView(root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outputUri?.let { outState.putString(KEY_OUTPUT_URI, it.toString()) }
        super.onSaveInstanceState(outState)
    }

    private fun requestPhoto() {
        val fileName = "capture_${System.currentTimeMillis()}.jpg"
        val uri = Uri.parse("content://${OutputProvider.AUTHORITY}/$fileName")
        outputUri = uri

        val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            component = ComponentName(
                "org.community.gallerycamera",
                "org.community.gallerycamera.CaptureActivity"
            )
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            clipData = ClipData.newRawUri("output", uri)
        }

        runCatching {
            startActivityForResult(capture, REQ_CAPTURE)
        }.onFailure { error ->
            outputUri = null
            Toast.makeText(
                this,
                "No se pudo abrir Gallery Camera: ${error.javaClass.simpleName}: ${error.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CAPTURE) return

        val uri = outputUri
        if (uri == null) {
            status.text = "No se encontró el URI de salida."
            return
        }

        if (resultCode == RESULT_OK) {
            preview.setImageURI(null)
            preview.setImageURI(uri)
            status.text = "Resultado recibido correctamente."
        } else {
            status.text = "Solicitud cancelada."
        }

        outputUri = null
    }

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQ_CAPTURE = 3001
        private const val KEY_OUTPUT_URI = "output_uri"
    }
}
