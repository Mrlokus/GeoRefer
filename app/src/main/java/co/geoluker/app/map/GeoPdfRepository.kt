package co.geoluker.app.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import co.geoluker.app.R
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSArray
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.cos.COSNumber
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class GeoPdfRepository(context: Context) {
    private val appContext = context.applicationContext

    init {
        PDFBoxResourceLoader.init(appContext)
    }

    fun loadBundled(): GeoPdfMap {
        val localPdf = copyBundledPdfToCache()
        val preview = renderBundledMap(localPdf)
        return GeoPdfMap(
            displayName = "Luker Agrícola",
            bitmap = preview,
            reference = readBundledReference(),
            tileRenderer = GeoPdfTileRenderer(localPdf, preview.width, preview.height),
        )
    }

    @SuppressLint("Recycle")
    private fun renderBundledMap(localPdf: File): Bitmap {
        return ParcelFileDescriptor.open(localPdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount == 1) { "El mapa oficial debe tener una sola página." }
                renderer.openPage(0).use { page ->
                    val longestEdge = max(page.width, page.height).coerceAtLeast(1)
                    val scale = MAX_RENDER_EDGE_PX.toFloat() / longestEdge.toFloat()
                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }

    /**
     * PdfRenderer solo recibe un descriptor de archivo completo. El descriptor devuelto por
     * openRawResourceFd apunta a una región dentro del APK, por lo que algunos dispositivos
     * intentan interpretar el APK como si fuera el PDF. La copia privada evita esa diferencia
     * entre fabricantes y nunca queda expuesta al almacenamiento compartido.
     */
    private fun copyBundledPdfToCache(): File {
        val target = File(appContext.cacheDir, BUNDLED_CACHE_NAME)
        val temporary = File(appContext.cacheDir, "$BUNDLED_CACHE_NAME.tmp")
        appContext.resources.openRawResource(R.raw.luker_map).use { input ->
            temporary.outputStream().buffered().use { output -> input.copyTo(output) }
        }
        if (target.exists() && !target.delete()) {
            temporary.delete()
            error("No fue posible actualizar la copia local del mapa oficial.")
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            error("No fue posible preparar el mapa oficial para visualizarlo.")
        }
        return target
    }

    private fun readBundledReference(): GeoPdfReference =
        appContext.resources.openRawResource(R.raw.luker_map).use { stream ->
            PDDocument.load(stream).use { document ->
                require(document.numberOfPages == 1) { "El mapa oficial debe tener una sola página." }
                val page = document.getPage(0)
                val viewport = page.cosObject.dictionaryArray("VP")?.firstDictionary()
                    ?: error("El mapa oficial no contiene un viewport geográfico.")
                val measure = viewport.dictionary("Measure")
                    ?: error("El mapa oficial no contiene georreferenciación.")

                require(measure.getNameAsString(COSName.getPDFName("Subtype")) == "GEO") {
                    "El mapa oficial no es un GeoPDF compatible."
                }

                val viewportBox = viewport.numberArray("BBox")
                    ?: error("El mapa oficial no contiene límites.")
                val localPoints = measure.numberArray("LPTS")
                    ?: error("El mapa oficial no contiene puntos locales.")
                val geographicPoints = measure.numberArray("GPTS")
                    ?: error("El mapa oficial no contiene coordenadas geográficas.")

                require(
                    viewportBox.size == 4 &&
                        localPoints.size >= 8 &&
                        localPoints.size == geographicPoints.size &&
                        localPoints.size % 2 == 0,
                ) { "La georreferenciación del mapa oficial no es válida." }

                GeoPdfReference(
                    pageWidthPoints = page.mediaBox.width.toDouble(),
                    pageHeightPoints = page.mediaBox.height.toDouble(),
                    viewportBox = viewportBox,
                    controlPoints = localPoints.indices.step(2).map { index ->
                        GeoControlPoint(
                            localX = localPoints[index],
                            localY = localPoints[index + 1],
                            latitude = geographicPoints[index],
                            longitude = geographicPoints[index + 1],
                        )
                    },
                )
            }
        }

    private fun COSDictionary.dictionary(name: String): COSDictionary? =
        getDictionaryObject(COSName.getPDFName(name)) as? COSDictionary

    private fun COSDictionary.dictionaryArray(name: String): COSArray? =
        getDictionaryObject(COSName.getPDFName(name)) as? COSArray

    @Suppress("DEPRECATION")
    private fun COSDictionary.numberArray(name: String): List<Double>? =
        (getDictionaryObject(COSName.getPDFName(name)) as? COSArray)?.let { array ->
            (0 until array.size()).mapNotNull { index ->
                (array.getObject(index) as? COSNumber)?.doubleValue()
            }.takeIf { it.size == array.size() }
        }

    private fun COSArray.firstDictionary(): COSDictionary? =
        if (size() > 0) getObject(0) as? COSDictionary else null

    private companion object {
        const val MAX_RENDER_EDGE_PX = 2048
        const val BUNDLED_CACHE_NAME = "geoluker_official_map.pdf"
    }
}
