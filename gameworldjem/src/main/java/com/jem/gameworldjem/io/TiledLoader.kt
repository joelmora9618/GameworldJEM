package com.jem.gameworldjem.io

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.jem.gameworldjem.tiled.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

internal fun Context.readAssetText(path: String): String =
    assets.open(path).bufferedReader().use { it.readText() }

/**
 * Carga un mapa .tmj (JSON) y todos sus tilesets externos (.tsj o .tsx),
 * devolviendo el mapa y los bundles de tilesets con sus bitmaps.
 *
 * @param basePath Carpeta base en assets (p.ej. "tiled")
 * @param mapFile  Archivo del mapa dentro de basePath (p.ej. "demo_map.tmj")
 */
internal fun loadTiledMapFromAssets(
    ctx: Context,
    basePath: String,
    mapFile: String
): MapBundle {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // 1) Mapa
    val mapJson = ctx.readAssetText("$basePath/$mapFile")
    val map = moshi.adapter(TiledMap::class.java).fromJson(mapJson)
        ?: error("No se pudo parsear $mapFile")

    // 2) Tilesets
    val bundles = mutableListOf<TilesetBundle>()
    val refs = map.tilesets.sortedBy { it.firstgid }

    for (ref in refs) {
        val tsRel = if (ref.source.startsWith("$basePath/")) ref.source else "$basePath/${ref.source}"
        val tsText = ctx.readAssetText(tsRel)

        val isJson = tsRel.endsWith(".tsj", true) || tsRel.endsWith(".json", true)
        val ts = if (isJson) {
            moshi.adapter(ExternalTileset::class.java).fromJson(tsText)
        } else {
            parseTsx(tsText)
        } ?: error("No se pudo parsear tileset: $tsRel")

        // Normaliza ruta de imagen: tomamos solo el archivo y lo buscamos en basePath
        val fileName = ts.image.substringAfterLast('/')
        val imgPath = "$basePath/$fileName"
        val img = ctx.assets.open(imgPath).use { BitmapFactory.decodeStream(it).asImageBitmap() }

        bundles += TilesetBundle(ts, img, ref.firstgid)
    }

    return MapBundle(map, bundles)
}
