package co.geoluker.app.map

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.Closeable
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GeoPdfTileKey(
    val longEdgePixels: Int,
    val column: Int,
    val row: Int,
)

data class GeoPdfTile(
    val key: GeoPdfTileKey,
    val bitmap: Bitmap,
    val fullWidthPixels: Int,
    val fullHeightPixels: Int,
    val pixelLeft: Int,
    val pixelTop: Int,
)

data class GeoPdfTileGrid(
    val longEdgePixels: Int,
    val fullWidthPixels: Int,
    val fullHeightPixels: Int,
    val columns: Int,
    val rows: Int,
)

/** Renderiza únicamente los sectores visibles del GeoPDF y conserva una caché acotada. */
class GeoPdfTileRenderer(
    private val pdfFile: File,
    private val pageWidth: Int,
    private val pageHeight: Int,
) : Closeable {
    private val renderMutex = Mutex()
    private val cache = object : LruCache<GeoPdfTileKey, GeoPdfTile>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: GeoPdfTileKey, value: GeoPdfTile): Int = value.bitmap.allocationByteCount
    }

    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var page: PdfRenderer.Page? = null
    @Volatile private var closed = false

    fun grid(longEdgePixels: Int): GeoPdfTileGrid {
        val safeLongEdge = longEdgePixels.coerceIn(MIN_LONG_EDGE_PX, MAX_LONG_EDGE_PX)
        val pageLongEdge = max(pageWidth, pageHeight).coerceAtLeast(1)
        val factor = safeLongEdge.toFloat() / pageLongEdge.toFloat()
        val fullWidth = (pageWidth * factor).roundToInt().coerceAtLeast(1)
        val fullHeight = (pageHeight * factor).roundToInt().coerceAtLeast(1)
        return GeoPdfTileGrid(
            longEdgePixels = safeLongEdge,
            fullWidthPixels = fullWidth,
            fullHeightPixels = fullHeight,
            columns = ceil(fullWidth / TILE_SIZE_PX.toDouble()).toInt(),
            rows = ceil(fullHeight / TILE_SIZE_PX.toDouble()).toInt(),
        )
    }

    suspend fun render(key: GeoPdfTileKey): GeoPdfTile = withContext(Dispatchers.IO) {
        cache.get(key)?.let { return@withContext it }
        renderMutex.withLock {
            cache.get(key)?.let { return@withLock it }
            check(!closed) { "El renderizador del mapa ya fue cerrado." }
            val grid = grid(key.longEdgePixels)
            require(key.column in 0 until grid.columns && key.row in 0 until grid.rows) {
                "El mosaico solicitado está fuera del mapa."
            }

            val openedPage = obtainPage()
            val logicalLeft = key.column * TILE_SIZE_PX
            val logicalTop = key.row * TILE_SIZE_PX
            val logicalRight = minOf(logicalLeft + TILE_SIZE_PX, grid.fullWidthPixels)
            val logicalBottom = minOf(logicalTop + TILE_SIZE_PX, grid.fullHeightPixels)

            // PdfRenderer recorta cualquier glifo que atraviese el borde del bitmap.
            // El sangrado mantiene completo el texto y se solapa de forma imperceptible
            // con los mosaicos vecinos.
            val pixelLeft = maxOf(0, logicalLeft - TILE_BLEED_PX)
            val pixelTop = maxOf(0, logicalTop - TILE_BLEED_PX)
            val pixelRight = minOf(grid.fullWidthPixels, logicalRight + TILE_BLEED_PX)
            val pixelBottom = minOf(grid.fullHeightPixels, logicalBottom + TILE_BLEED_PX)
            val tileWidth = pixelRight - pixelLeft
            val tileHeight = pixelBottom - pixelTop
            val bitmap = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)

            val matrix = Matrix().apply {
                setScale(
                    grid.fullWidthPixels.toFloat() / openedPage.width.toFloat(),
                    grid.fullHeightPixels.toFloat() / openedPage.height.toFloat(),
                )
                postTranslate(-pixelLeft.toFloat(), -pixelTop.toFloat())
            }
            openedPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            GeoPdfTile(
                key = key,
                bitmap = bitmap,
                fullWidthPixels = grid.fullWidthPixels,
                fullHeightPixels = grid.fullHeightPixels,
                pixelLeft = pixelLeft,
                pixelTop = pixelTop,
            ).also { cache.put(key, it) }
        }
    }

    private fun obtainPage(): PdfRenderer.Page {
        page?.let { return it }
        val openedDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val openedRenderer = PdfRenderer(openedDescriptor)
        require(openedRenderer.pageCount == 1) { "El mapa oficial debe tener una sola página." }
        descriptor = openedDescriptor
        renderer = openedRenderer
        return openedRenderer.openPage(0).also { page = it }
    }

    override fun close() {
        if (closed) return
        closed = true
        page?.close()
        renderer?.close()
        descriptor?.close()
        page = null
        renderer = null
        descriptor = null
        cache.evictAll()
    }

    companion object {
        const val TILE_SIZE_PX = 512
        const val MIN_LONG_EDGE_PX = 2_048
        const val MAX_LONG_EDGE_PX = 16_384
        private const val TILE_BLEED_PX = 24
        private const val MAX_CACHE_BYTES = 48 * 1024 * 1024
    }
}
