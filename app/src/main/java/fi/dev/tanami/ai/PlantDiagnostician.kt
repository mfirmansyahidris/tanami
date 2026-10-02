package fi.dev.tanami.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import fi.dev.tanami.BuildConfig
import fi.dev.tanami.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlantDiagnostician(private val context: Context) {
    val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun analyze(imageUri: Uri, country: String, isEnglish: Boolean): String = withContext(Dispatchers.IO) {
        check(isConfigured) { context.getString(R.string.error_firebase_setup) }
        val bitmap = loadPreviewBitmap(imageUri)
        val prompt = content {
            image(bitmap)
            text(context.getString(if (isEnglish) R.string.ai_prompt_en else R.string.ai_prompt_id, country))
        }
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.TANAMI_AI_MODEL)
            model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
                ?: error(context.getString(R.string.error_empty_result))
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun loadPreviewBitmap(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: error(context.getString(R.string.error_photo_open))
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error(context.getString(R.string.error_photo_format))
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_EDGE || bounds.outHeight / sampleSize > MAX_IMAGE_EDGE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error(context.getString(R.string.error_photo_open))
    }

    private companion object {
        const val MAX_IMAGE_EDGE = 1280
    }
}
