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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlantDiagnostician(private val context: Context) {
    val isConfigured: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun analyze(imageUri: Uri, country: String): String = withContext(Dispatchers.IO) {
        check(isConfigured) { "Hubungkan proyek Firebase untuk memakai analisis foto." }
        val bitmap = loadPreviewBitmap(imageUri)
        val prompt = content {
            image(bitmap)
            text(
                    "Kamu membantu pekebun rumahan di $country, Asia Tenggara. Lokasi ini belum menyertakan kota atau ketinggian, " +
                    "jadi jangan mengasumsikan iklim mikro; bila kondisi suhu atau ketinggian menentukan, minta konteks tambahan. " +
                    "Amati foto tanaman dan jawab dalam bahasa Indonesia dengan bagian: " +
                    "Pengamatan visual, Kemungkinan penyebab (maksimal 3, jangan mengklaim diagnosis pasti), " +
                    "Cara memeriksa, Langkah awal yang aman, dan Kapan perlu diperiksa lagi. " +
                    "Pertimbangkan bahwa gejala dapat berasal dari air, cahaya, media, nutrisi, hama, atau penyakit. " +
                    "Jangan menyarankan dosis pestisida atau pupuk yang tidak ada pada label. " +
                    "Jika foto tidak cukup jelas, katakan apa yang perlu difoto ulang. " +
                    "Jawaban ringkas dan tidak melebihi 180 kata."
            )
        }
        try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.TANAMI_AI_MODEL)
            model.generateContent(prompt).text?.takeIf { it.isNotBlank() }
                ?: error("Gemini tidak memberikan hasil. Coba foto yang lebih jelas.")
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun loadPreviewBitmap(uri: Uri): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: error("Foto tidak dapat dibuka. Pilih foto lain.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Format foto tidak dapat dibaca. Pilih foto lain.")
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_EDGE || bounds.outHeight / sampleSize > MAX_IMAGE_EDGE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Foto tidak dapat dibuka. Pilih foto lain.")
    }

    private companion object {
        const val MAX_IMAGE_EDGE = 1280
    }
}
