package com.example.myuni

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfTextItem(
    val text: String,
    val x: Float,
    val y: Float
)

data class PdfExtractionResult(
    val fileName: String,
    val pageCount: Int,
    val rawText: String,
    val formattedText: String,
    val textItems: List<PdfTextItem> = emptyList(),
    val errorMessage: String? = null,
)

object PdfExtractor {

    suspend fun extractText(context: Context, uri: Uri): PdfExtractionResult = withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri) ?: "فایل PDF"
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
            
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext PdfExtractionResult(fileName, 0, "", "", emptyList(), "امکان باز کردن فایل PDF وجود ندارد.")

            var rawText = ""
            val pageCount: Int
            val items = mutableListOf<PdfTextItem>()

            inputStream.use { stream ->
                PDDocument.load(stream).use { document ->
                    pageCount = document.numberOfPages
                    
                    // Strategy 1: Positional extraction (preserves table coordinate alignment)
                    val stripper = object : PDFTextStripper() {
                        init {
                            sortByPosition = true
                        }

                        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                            super.writeString(text)
                            val firstPos = textPositions.firstOrNull()
                            if (firstPos != null) {
                                val decodedPos = PersianFontDecoder.decodeAndNormalize(text)
                                items.add(PdfTextItem(text = decodedPos, x = firstPos.xDirAdj, y = firstPos.yDirAdj))
                            }
                        }
                    }
                    rawText = stripper.getText(document) ?: ""

                    // Strategy 2: Non-positional flow extraction fallback if positional returns blank
                    if (rawText.isBlank() && pageCount > 0) {
                        val flowStripper = PDFTextStripper().apply { sortByPosition = false }
                        rawText = flowStripper.getText(document) ?: ""
                    }
                }
            }

            // Step 1: Font decoding and Unicode normalization (handles Presentation Forms, PUA, Mojibake)
            var cleanedText = PersianFontDecoder.decodeAndNormalize(rawText)

            // Step 2: Unshape visual reversed RTL text if detected
            if (PersianFontDecoder.isReversedText(rawText)) {
                val unshaped = PersianFontDecoder.unshapeReversedText(rawText)
                if (unshaped.isNotBlank()) {
                    cleanedText = unshaped
                }
            }

            PdfExtractionResult(
                fileName = fileName,
                pageCount = pageCount,
                rawText = rawText,
                formattedText = cleanedText,
                textItems = items
            )
        } catch (e: Exception) {
            e.printStackTrace()
            PdfExtractionResult(
                fileName = fileName,
                pageCount = 0,
                rawText = "",
                formattedText = "",
                textItems = emptyList(),
                errorMessage = "خطا در پردازش فایل PDF: ${e.localizedMessage ?: "فایل معتبر یا قابل خواندن نیست."}"
            )
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            val path = uri.path
            val cut = path?.lastIndexOf('/') ?: -1
            if (cut != -1 && path != null) {
                result = path.substring(cut + 1)
            }
        }
        return result
    }

    fun normalizePersianText(text: String): String {
        return PersianFontDecoder.decodeAndNormalize(text)
    }

    fun reverseLines(text: String): String {
        return PersianFontDecoder.unshapeReversedText(text)
    }
}
