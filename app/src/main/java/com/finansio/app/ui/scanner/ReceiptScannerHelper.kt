package com.finansio.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.finansio.app.data.receipt.OcrFragment
import com.finansio.app.data.receipt.OcrRowBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/** Pembantu ML Kit: membuka pemindai dokumen (kamera) dan membaca teks dari gambar struk. */
object ReceiptScannerHelper {

    fun getScannerOptions(): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1) // As agreed, 1 receipt per session
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
    }

    fun getScanner(): GmsDocumentScanner {
        return GmsDocumentScanning.getClient(getScannerOptions())
    }

    /**
     * Membaca teks struk dan menyusunnya ulang per baris menurut posisinya.
     * Teks mentah ML Kit dikelompokkan per kolom (semua harga di akhir), jadi tidak dipakai langsung.
     */
    suspend fun recognizeText(context: Context, imageUri: Uri): String {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val visionText = recognizer.process(inputImage).await()
        val lines = visionText.textBlocks.flatMap { it.lines }
        val fragments = lines.mapNotNull { line ->
            line.boundingBox?.let { box -> OcrFragment(line.text, box.left, box.top, box.right, box.bottom) }
        }
        if (fragments.isEmpty()) return visionText.text
        val rows = OcrRowBuilder.buildRows(fragments)
        // Baris tanpa posisi (jarang terjadi) ditaruh di akhir supaya tidak hilang.
        val unplaced = lines.filter { it.boundingBox == null }.joinToString("\n") { it.text }
        return if (unplaced.isBlank()) rows else "$rows\n$unplaced"
    }

    /**
     * Mengecilkan foto struk lalu mengubahnya menjadi JPEG base64 untuk dikirim ke model vision.
     * Dibatasi sekitar [MAX_PIXELS] piksel supaya tetap di bawah batas 4 MB Groq untuk gambar base64
     * dan tidak boros token, tetapi tulisan struk yang panjang masih terbaca.
     * Mengembalikan null bila gambar gagal dibaca (scan lalu memakai teks OCR saja).
     */
    suspend fun encodeImageForAi(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            // Turunkan resolusi kasar dulu saat decode supaya tidak kehabisan memori.
            var sample = 1
            while (bounds.outWidth.toLong() * bounds.outHeight / (sample.toLong() * sample) > MAX_PIXELS * 4) sample *= 2
            val decoded = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            val pixels = decoded.width.toLong() * decoded.height
            val bitmap = if (pixels > MAX_PIXELS) {
                val scale = sqrt(MAX_PIXELS.toDouble() / pixels)
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(32),
                    (decoded.height * scale).toInt().coerceAtLeast(32),
                    true
                ).also { if (it !== decoded) decoded.recycle() }
            } else decoded

            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
            bitmap.recycle()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private const val MAX_PIXELS = 2_000_000L
    private const val JPEG_QUALITY = 85
}
