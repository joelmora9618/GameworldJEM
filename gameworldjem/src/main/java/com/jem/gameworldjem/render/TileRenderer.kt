package com.jem.gameworldjem.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jem.gameworldjem.tiled.model.*

private const val FLIP_MASK = 0x1FFFFFFF

/**
 * Dibuja todas las capas de tiles visibles usando el/los tilesets.
 * Calcula el viewport a partir del tamaño del Canvas y la cámara en “unidades de mapa”.
 */
internal fun DrawScope.drawTiledLayers(
    map: TiledMap,
    tilesets: List<TilesetBundle>,
    tileLayers: List<TiledLayer>,
    camX: Float,
    camY: Float,
    scaleFactor: Float
) {
    val tw = map.tilewidth
    val th = map.tileheight
    val worldW = map.width * tw
    val worldH = map.height * th

    val viewWWorld = size.width / scaleFactor
    val viewHWorld = size.height / scaleFactor

    val startCol = (camX / tw).toInt().coerceAtLeast(0)
    val startRow = (camY / th).toInt().coerceAtLeast(0)
    val endCol = ((camX + viewWWorld) / tw).toInt().coerceAtMost(map.width - 1)
    val endRow = ((camY + viewHWorld) / th).toInt().coerceAtMost(map.height - 1)

    fun resolveTs(gid: Int): TilesetBundle? {
        if (gid <= 0) return null
        var chosen: TilesetBundle? = null
        for (ts in tilesets) if (gid >= ts.firstgid) chosen = ts else break
        return chosen
    }

    fun drawTile(gidRaw: Int, col: Int, row: Int) {
        val gid = gidRaw and FLIP_MASK
        if (gid == 0) return

        val ts = resolveTs(gid) ?: return
        val m = ts.meta
        val spacing = m.spacing ?: 0
        val margin = m.margin ?: 0
        val tW = m.tilewidth
        val tH = m.tileheight

        // Derivar columnas/filas reales a partir del PNG
        val colsImg = ((ts.image.width - margin * 2 + spacing) / (tW + spacing)).coerceAtLeast(1)
        val rowsImg = ((ts.image.height - margin * 2 + spacing) / (tH + spacing)).coerceAtLeast(1)
        val total = colsImg * rowsImg

        val tileId = gid - ts.firstgid
        if (tileId !in 0 until total) return

        val atlasCol = tileId % colsImg
        val atlasRow = tileId / colsImg
        val sx = margin + atlasCol * (tW + spacing)
        val sy = margin + atlasRow * (tH + spacing)

        val dstLeftPx = ((col * tw - camX) * scaleFactor).toInt()
        val dstTopPx = ((row * th - camY) * scaleFactor).toInt()
        val dstW = (tw * scaleFactor).toInt()
        val dstH = (th * scaleFactor).toInt()

        drawImage(
            image = ts.image,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(tW, tH),
            dstOffset = IntOffset(dstLeftPx, dstTopPx),
            dstSize = IntSize(dstW, dstH)
        )
    }

    tileLayers.forEach { layer ->
        layer.data?.let { data ->
            for (row in startRow..endRow) {
                for (col in startCol..endCol) {
                    val idx = row * map.width + col
                    if (idx in data.indices) drawTile(data[idx], col, row)
                }
            }
        }
        layer.chunks?.forEach { ch ->
            val rowRange = maxOf(startRow, ch.y)..minOf(endRow, ch.y + ch.height - 1)
            val colRange = maxOf(startCol, ch.x)..minOf(endCol, ch.x + ch.width - 1)
            for (row in rowRange) for (col in colRange) {
                val localRow = row - ch.y
                val localCol = col - ch.x
                val idx = localRow * ch.width + localCol
                if (idx in ch.data.indices) drawTile(ch.data[idx], col, row)
            }
        }
    }
}
