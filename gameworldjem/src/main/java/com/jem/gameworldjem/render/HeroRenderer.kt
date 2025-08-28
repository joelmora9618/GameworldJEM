package com.jem.gameworldjem.render

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jem.gameworldjem.engine.Dir

/**
 * Dibuja el héroe en pantalla, escalado, anclando los pies.
 */
internal fun DrawScope.drawHero(
    heroSheet: ImageBitmap,
    frameIdx: Int,
    dir: Dir,
    heroFrameW: Int,
    heroFrameH: Int,
    heroMargin: Int,
    heroSpacing: Int,
    worldX: Float, // posición del jugador en unidades de mapa
    worldY: Float,
    camX: Float, camY: Float,
    scaleFactor: Float,
    dstW: Int, dstH: Int,
    footAnchor: Float = 0.85f
) {
    val heroRow = when (dir) {
        Dir.DOWN -> 0
        Dir.LEFT -> 2
        Dir.RIGHT -> 3
        Dir.UP -> 1
    }

    val sx = heroMargin + frameIdx * (heroFrameW + heroSpacing)
    val sy = heroMargin + heroRow * (heroFrameH + heroSpacing)

    val dstX = ((worldX - camX) * scaleFactor - dstW / 2f).toInt()
    val dstY = ((worldY - camY) * scaleFactor - dstH * footAnchor).toInt()

    drawImage(
        image = heroSheet,
        srcOffset = IntOffset(sx, sy),
        srcSize = IntSize(heroFrameW, heroFrameH),
        dstOffset = IntOffset(dstX, dstY),
        dstSize = IntSize(dstW, dstH)
    )
}
