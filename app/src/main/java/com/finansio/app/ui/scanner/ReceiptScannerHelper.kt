package com.finansio.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.finansio.app.data.scanner.OcrLine
import com.finansio.app.data.scanner.ReceiptLayout
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

/** Pembantu ML Kit: membuka pemindai dokumen (kamera), membaca teks struk, dan menyiapkan foto untuk AI. */
object ReceiptScannerHelper {

    /** Sisi terpanjang foto yang dikirim ke AI. Cukup tajam untuk angka struk, tetap kecil untuk diunggah. */
    private const val MAX_IMAGE_SIDE = 1600
    private const val JPEG_QUALITY = 85

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
     * Membaca teks struk lalu menyusunnya per baris sesuai posisi di gambar, sehingga nama barang
     * dan harganya berada di baris yang sama (lihat [ReceiptLayout]).
     */
    suspend fun recognizeText(context: Context, imageUri: Uri): String {
        val inputImage = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val visionText = recognizer.process(inputImage).await()
            val lines = visionText.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    OcrLine(line.text, box.left, box.top, box.right, box.bottom)
                }
            }
            return if (lines.isEmpty()) visionText.text else ReceiptLayout.toText(lines)
        } finally {
            recognizer.close()
        }
    }

    /** Foto struk sebagai JPEG base64 (sudah diperkecil) untuk model vision. Null bila gagal dibaca. */
    suspend fun encodeImageForAi(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return@withContext null

            var sample = 1
            while (longest / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
                    .also { if (it != decoded) decoded.recycle() }
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
}
