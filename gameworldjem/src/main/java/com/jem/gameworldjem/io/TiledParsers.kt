package com.jem.gameworldjem.io

import com.jem.gameworldjem.tiled.model.ExternalTileset

// Parser mínimo de .TSX (XML). Ideal para tilesets externos en XML.
internal fun parseTsx(tsx: String): ExternalTileset {
    fun attr(tag: String, name: String) =
        Regex("<$tag[^>]*\\b$name=\"([^\"]+)\"").find(tsx)?.groupValues?.get(1)

    val name    = attr("tileset", "name") ?: "tileset"
    val tw      = attr("tileset", "tilewidth")?.toInt() ?: 32
    val th      = attr("tileset", "tileheight")?.toInt() ?: 32
    val count   = attr("tileset", "tilecount")?.toInt() ?: 0
    val cols    = attr("tileset", "columns")?.toInt() ?: 0
    val margin  = attr("tileset", "margin")?.toInt() ?: 0
    val spacing = attr("tileset", "spacing")?.toInt() ?: 0
    val img     = attr("image", "source") ?: ""
    val imgW    = attr("image", "width")?.toInt() ?: 0
    val imgH    = attr("image", "height")?.toInt() ?: 0

    return ExternalTileset(name, img, imgW, imgH, tw, th, cols, count, spacing, margin)
}
