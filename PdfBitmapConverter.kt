package com.example.pdfreader.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext


class PdfBitmapConverter(private val context: Context) {
    private var renderer: PdfRenderer? = null

    suspend fun pdfToBitmaps(contentUri: Uri): List<Bitmap> {
        return withContext(Dispatchers.IO) {
            renderer?.close()
            context.contentResolver.openFileDescriptor(
                contentUri,
                "r"
            )?.use {
                with(PdfRenderer(it)) {
                    renderer = this
                    val scale = 2.0f
                    return@withContext (0 until pageCount).map { pageIndex ->
                        async {
                            openPage(pageIndex).use { page ->
                                val bitmap = createScaledBitmap(page, scale)
                                val canvas = Canvas(bitmap).apply {
                                    drawColor(Color.WHITE)
                                    drawBitmap(bitmap, 0f, 0f, null)
                                }
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                                bitmap
                            }
                        }
                    }.awaitAll()
                }
            }
            return@withContext emptyList()
        }
    }
}

@SuppressLint("UseKtx")
private fun createScaledBitmap(page: PdfRenderer.Page, scale: Float): Bitmap {
    val width = (page.width * scale).toInt()
    val height = (page.height * scale).toInt()
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}
