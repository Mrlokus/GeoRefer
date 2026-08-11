package co.georefer.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSNumber
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlin.math.max
import kotlin.math.roundToInt

class GeoPdfRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    init {
        PDFBoxResourceLoader.init(appContext)
    }

    fun load(uri: Uri): GeoPdfMap {
        validateFileSize(uri)
        val displayName = displayName(uri)
        val reference = readReference(uri)
        val bitmap = renderFirstPage(uri)
        return GeoPdfMap(
            displayName = displayName,
            bitmap = bitmap,
            reference = reference,
        )
    }

    private fun validateFileSize(uri: Uri) {
        resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
            val size = descriptor.statSize
            if (size > MAX_FILE_BYTES) {
                error("El GeoPDF supera el límite de 250 MB.")
            }
        } ?: error("Android no pudo abrir el archivo seleccionado.")
    }

    private fun renderFirstPage(uri: Uri): Bitmap {
        val descriptor = resolver.openFileDescriptor(uri, "r")
            ?: error("Android no pudo abrir el archivo seleccionado.")

        descriptor.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                if (renderer.pageCount != 1) {
                    error("Por ahora Georefer admite GeoPDF de una sola página.")
                }
                renderer.openPage(0).use { page ->
                    val longestEdge = max(page.width, page.height).coerceAtLeast(1)
                    val scale = MAX_RENDER_EDGE_PX.toFloat() / longestEdge.toFloat()
                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(Color.WHITE)
                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                        )
                    }
                }
            }
        }
    }

    private fun readReference(uri: Uri): GeoPdfReference {
        val input = resolver.openInputStream(uri)
            ?: error("Android no pudo leer los metadatos del archivo.")

        input.use { stream ->
            PDDocument.load(stream).use { document ->
                if (document.numberOfPages != 1) {
                    error("Por ahora Georefer admite GeoPDF de una sola página.")
                }

                val page = document.getPage(0)
                val viewports = page.cosObject.dictionaryArray("VP")
                    ?: error("El PDF no contiene un viewport geográfico.")
                val viewport = viewports.firstDictionary()
                    ?: error("El viewport geográfico del PDF no es válido.")
                val measure = viewport.dictionary("Measure")
                    ?: error("El PDF no contiene parámetros de georreferenciación.")

                val subtype = measure.getNameAsString(COSName.getPDFName("Subtype"))
                if (subtype != "GEO") {
                    error("El archivo seleccionado no es un GeoPDF compatible.")
                }

                val viewportBox = viewport.numberArray("BBox")
                    ?: error("El GeoPDF no contiene los límites del mapa.")
                val localPoints = measure.numberArray("LPTS")
                    ?: error("El GeoPDF no contiene puntos locales de control.")
                val geographicPoints = measure.numberArray("GPTS")
                    ?: error("El GeoPDF no contiene coordenadas geográficas.")

                if (
                    viewportBox.size != 4 ||
                    localPoints.size < 8 ||
                    geographicPoints.size < 8 ||
                    localPoints.size != geographicPoints.size ||
                    localPoints.size % 2 != 0
                ) {
                    error("Los puntos de georreferenciación del GeoPDF no son válidos.")
                }

                val controls = localPoints.indices.step(2).map { index ->
                    GeoControlPoint(
                        localX = localPoints[index],
                        localY = localPoints[index + 1],
                        latitude = geographicPoints[index],
                        longitude = geographicPoints[index + 1],
                    )
                }

                return GeoPdfReference(
                    pageWidthPoints = page.mediaBox.width.toDouble(),
                    pageHeightPoints = page.mediaBox.height.toDouble(),
                    viewportBox = viewportBox,
                    controlPoints = controls,
                )
            }
        }
    }

    private fun displayName(uri: Uri): String {
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment ?: "Mapa GeoPDF"
    }

    private fun COSDictionary.dictionary(name: String): COSDictionary? =
        getDictionaryObject(COSName.getPDFName(name)) as? COSDictionary

    private fun COSDictionary.dictionaryArray(name: String): COSArray? =
        getDictionaryObject(COSName.getPDFName(name)) as? COSArray

    private fun COSDictionary.numberArray(name: String): List<Double>? =
        (getDictionaryObject(COSName.getPDFName(name)) as? COSArray)?.let { array ->
            (0 until array.size()).mapNotNull { index ->
                (array.getObject(index) as? COSNumber)?.doubleValue()
            }.takeIf { it.size == array.size() }
        }

    private fun COSArray.firstDictionary(): COSDictionary? =
        if (size() > 0) getObject(0) as? COSDictionary else null

    private companion object {
        const val MAX_FILE_BYTES = 250L * 1024L * 1024L
        const val MAX_RENDER_EDGE_PX = 2400
    }
}
