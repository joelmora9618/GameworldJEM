package com.jem.gameworldjem.tiled.model

import androidx.compose.ui.graphics.ImageBitmap

data class TiledTilesetRef(val firstgid: Int, val source: String)

data class TiledObject(
    val id: Int,
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val visible: Boolean = true
)

data class TiledChunk(
    val x: Int, val y: Int,
    val width: Int, val height: Int,
    val data: List<Int>
)

data class TiledLayer(
    val type: String,
    val name: String,
    val width: Int? = null,
    val height: Int? = null,
    val data: List<Int>? = null,
    val objects: List<TiledObject>? = null,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val chunks: List<TiledChunk>? = null
)

data class TiledMap(
    val width: Int,
    val height: Int,
    val tilewidth: Int,
    val tileheight: Int,
    val layers: List<TiledLayer>,
    val tilesets: List<TiledTilesetRef>
)

data class ExternalTileset(
    val name: String,
    val image: String,
    val imagewidth: Int,
    val imageheight: Int,
    val tilewidth: Int,
    val tileheight: Int,
    val columns: Int,
    val tilecount: Int,
    val spacing: Int? = 0,
    val margin: Int? = 0
)

// Agrupa TiledMap + lista de tilesets ya cargados
data class MapBundle(
    val map: TiledMap,
    val tilesets: List<TilesetBundle>
)

data class TilesetBundle(
    val meta: ExternalTileset,
    val image: ImageBitmap,
    val firstgid: Int
)
