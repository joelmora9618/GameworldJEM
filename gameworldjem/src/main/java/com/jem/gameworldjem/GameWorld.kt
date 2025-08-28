package com.jem.gameworldjem

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ────────────────────────────────────────────────────────────────────────────────
// MODELOS TILED
// ────────────────────────────────────────────────────────────────────────────────
data class TiledTilesetRef(val firstgid: Int, val source: String)
data class TiledObject(val id: Int, val x: Float, val y: Float, val width: Float, val height: Float, val visible: Boolean = true)
data class TiledChunk(val x: Int, val y: Int, val width: Int, val height: Int, val data: List<Int>)
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
data class TilesetBundle(val meta: ExternalTileset, val image: ImageBitmap, val firstgid: Int)

// ────────────────────────────────────────────────────────────────────────────────
// IO / PARSERS
// ────────────────────────────────────────────────────────────────────────────────
private fun Context.readAssetText(path: String) =
    assets.open(path).bufferedReader().use { it.readText() }

// Parser mínimo de .TSX (XML)
private fun parseTsx(tsx: String): ExternalTileset {
    fun attr(tag: String, name: String) =
        Regex("<$tag[^>]*$name=\"([^\"]+)\"").find(tsx)?.groupValues?.get(1)

    val name       = attr("tileset", "name") ?: "tileset"
    val tw         = attr("tileset", "tilewidth")?.toInt() ?: 32
    val th         = attr("tileset", "tileheight")?.toInt() ?: 32
    val count      = attr("tileset", "tilecount")?.toInt() ?: 0
    val cols       = attr("tileset", "columns")?.toInt() ?: 0
    val margin     = attr("tileset", "margin")?.toInt() ?: 0
    val spacing    = attr("tileset", "spacing")?.toInt() ?: 0
    val img        = attr("image", "source") ?: ""
    val imgW       = attr("image", "width")?.toInt() ?: 0
    val imgH       = attr("image", "height")?.toInt() ?: 0

    return ExternalTileset(name, img, imgW, imgH, tw, th, cols, count, spacing, margin)
}

private fun loadTiledMapFromAssets(
    ctx: Context,
    basePath: String,
    mapFile: String
): Pair<TiledMap, List<TilesetBundle>> {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val mapJson = ctx.readAssetText("$basePath/$mapFile")
    val map = moshi.adapter(TiledMap::class.java).fromJson(mapJson)!!

    val bundles = mutableListOf<TilesetBundle>()
    for (ref in map.tilesets.sortedBy { it.firstgid }) {
        val tsRel = if (ref.source.startsWith("$basePath/")) ref.source else "$basePath/${ref.source}"
        val tsText = ctx.readAssetText(tsRel)
        val isJson = tsRel.endsWith(".tsj", true) || tsRel.endsWith(".json", true)
        val ts = if (isJson) moshi.adapter(ExternalTileset::class.java).fromJson(tsText)!! else parseTsx(tsText)

        // normaliza la ruta a assets/tiled
        val imgFile = ts.image.substringAfterLast('/')
        val imgPath = "$basePath/$imgFile"
        val bmp = ctx.assets.open(imgPath).use { BitmapFactory.decodeStream(it) }
        val img = bmp.asImageBitmap()

        bundles += TilesetBundle(ts, img, ref.firstgid)
    }
    return map to bundles
}

// ────────────────────────────────────────────────────────────────────────────────
// UTILS
// ────────────────────────────────────────────────────────────────────────────────
private fun rectsIntersect(a: Rect, b: Rect) = a.overlaps(b)
private fun clamp(v: Float, minV: Float, maxV: Float) = max(minV, min(v, maxV))
private enum class Dir { DOWN, LEFT, RIGHT, UP }

// ────────────────────────────────────────────────────────────────────────────────
// API PÚBLICA DEL MÓDULO
// ────────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameWorldScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    mapPath: String = "tiled/demo_map.tmj",
    heroSheetPath: String = "tiled/hero_spritesheet.png",
    title: String = "Top-Down Demo"
) {
    val ctx = LocalContext.current

    // Carga mapa + tilesets
    val (map, tilesets) = remember(mapPath) {
        loadTiledMapFromAssets(ctx, basePath = "tiled", mapFile = mapPath.substringAfterLast('/'))
    }

    // Escala de render (con el primer tileset)
    val scaleFactor = remember(tilesets, map) {
        tilesets.firstOrNull()?.meta?.tilewidth?.toFloat()?.div(map.tilewidth.toFloat()) ?: 1f
    }.coerceAtLeast(1f)

    // Spritesheet del héroe
    val heroSheet: ImageBitmap = remember(heroSheetPath) {
        ctx.assets.open(heroSheetPath).use { BitmapFactory.decodeStream(it).asImageBitmap() }
    }

    // Atlas héroe (4x4) con margen/espaciado generosos
    val HERO_COLS = 4
    val HERO_ROWS = 4
    val HERO_MARGIN = 16
    val HERO_SPACING = 16
    val heroFrameW = (heroSheet.width - HERO_MARGIN * 2 - (HERO_COLS - 1) * HERO_SPACING) / HERO_COLS
    val heroFrameH = (heroSheet.height - HERO_MARGIN * 2 - (HERO_ROWS - 1) * HERO_SPACING) / HERO_ROWS

    // Zoom del héroe (alto deseado en tiles)
    val desiredTilesTall = 1.6f
    val heroScale = (map.tileheight * desiredTilesTall * scaleFactor) / heroFrameH.toFloat()
    val heroScaledW = (heroFrameW * heroScale).toInt()
    val heroScaledH = (heroFrameH * heroScale).toInt()

    // Mundo
    val tw = map.tilewidth
    val th = map.tileheight
    val worldW = map.width * tw
    val worldH = map.height * th

    val tileLayers = remember(map) { map.layers.filter { it.type == "tilelayer" && it.visible && (it.data != null || it.chunks != null) } }
    val collisions = remember(map) { map.layers.firstOrNull { it.type == "objectgroup" && it.name == "collision" }?.objects.orEmpty() }

    // Estado
    var playerPos by remember { mutableStateOf(Offset(worldW / 2f, worldH / 2f)) }
    var moving by remember { mutableStateOf(false) }
    var dir by remember { mutableStateOf(Dir.DOWN) }
    var frameIdx by remember { mutableStateOf(0) }
    var camera by remember { mutableStateOf(playerPos) }
    var input by remember { mutableStateOf(Offset.Zero) }

    // Loop
    LaunchedEffect(input, scaleFactor) {
        val speedOnScreenPx = 220f
        val speedWorld = speedOnScreenPx / scaleFactor
        val frameMs = 120L
        var elapsedMs = 0L
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastNanos == 0L) { lastNanos = now; return@withFrameNanos }
                val dt = (now - lastNanos) / 1_000_000f
                lastNanos = now

                if (moving) {
                    elapsedMs += dt.toLong()
                    if (elapsedMs >= frameMs) {
                        elapsedMs = 0
                        frameIdx = (frameIdx + 1) % HERO_COLS
                    }
                } else frameIdx = 0

                if (input.getDistance() > 0.1f) {
                    moving = true
                    val nx = input.x; val ny = input.y
                    dir = if (abs(nx) > abs(ny)) if (nx > 0) Dir.RIGHT else Dir.LEFT else if (ny > 0) Dir.DOWN else Dir.UP
                    val step = Offset(nx * (speedWorld * dt / 1000f), ny * (speedWorld * dt / 1000f))
                    val next = playerPos + step

                    val half = (min(tw, th) * 0.375f)
                    val nextRect = Rect(next.x - half, next.y - half, next.x + half, next.y + half)
                    val hit = collisions.any { o ->
                        if (!o.visible) false else rectsIntersect(nextRect, Rect(o.x, o.y, o.x + o.width, o.y + o.height))
                    }
                    if (!hit) {
                        playerPos = Offset(clamp(next.x, half, worldW - half), clamp(next.y, half, worldH - half))
                        camera = playerPos
                    }
                } else moving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        containerColor = Color(0xFF222428)
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF222428))
        ) {
            // Canvas mundo
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                val maxMag = 80f
                                val dx = clamp(drag.x, -maxMag, maxMag) / maxMag
                                val dy = clamp(drag.y, -maxMag, maxMag) / maxMag
                                input = Offset(dx, dy)
                            },
                            onDragEnd = { input = Offset.Zero },
                            onDragCancel = { input = Offset.Zero }
                        )
                    }
            ) {
                val viewWWorld = size.width / scaleFactor
                val viewHWorld = size.height / scaleFactor
                val camX = clamp(camera.x - viewWWorld / 2f, 0f, (worldW - viewWWorld).coerceAtLeast(0f))
                val camY = clamp(camera.y - viewHWorld / 2f, 0f, (worldH - viewHWorld).coerceAtLeast(0f))

                val startCol = (camX / tw).toInt().coerceAtLeast(0)
                val startRow = (camY / th).toInt().coerceAtLeast(0)
                val endCol = ((camX + viewWWorld) / tw).toInt().coerceAtMost(map.width - 1)
                val endRow = ((camY + viewHWorld) / th).toInt().coerceAtMost(map.height - 1)

                val FLIP_MASK = 0x1FFFFFFF

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

                    val cols = ((ts.image.width - margin * 2 + spacing) / (tW + spacing)).coerceAtLeast(1)
                    val rows = ((ts.image.height - margin * 2 + spacing) / (tH + spacing)).coerceAtLeast(1)
                    val total = cols * rows

                    val tileId = gid - ts.firstgid
                    if (tileId !in 0 until total) return

                    val c = tileId % cols
                    val r = tileId / cols
                    val sx = margin + c * (tW + spacing)
                    val sy = margin + r * (tH + spacing)

                    val dx = ((col * tw - camX) * scaleFactor).toInt()
                    val dy = ((row * th - camY) * scaleFactor).toInt()
                    val dw = (tw * scaleFactor).toInt()
                    val dh = (th * scaleFactor).toInt()

                    drawImage(ts.image, IntOffset(sx, sy), IntSize(tW, tH), IntOffset(dx, dy), IntSize(dw, dh))
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
                        val rr = max(startRow, ch.y)..min(endRow, ch.y + ch.height - 1)
                        val cr = max(startCol, ch.x)..min(endCol, ch.x + ch.width - 1)
                        for (row in rr) for (col in cr) {
                            val idx = (row - ch.y) * ch.width + (col - ch.x)
                            if (idx in ch.data.indices) drawTile(ch.data[idx], col, row)
                        }
                    }
                }

                // Personaje
                val heroRow = when (dir) { Dir.DOWN -> 0; Dir.LEFT -> 2; Dir.RIGHT -> 3; Dir.UP -> 1 }
                val sxHero = HERO_MARGIN + frameIdx * (heroFrameW + HERO_SPACING)
                val syHero = HERO_MARGIN + heroRow  * (heroFrameH + HERO_SPACING)
                val footAnchor = 0.85f
                val dstXpx = ((playerPos.x - camX) * scaleFactor - heroScaledW / 2f).toInt()
                val dstYpx = ((playerPos.y - camY) * scaleFactor - heroScaledH * footAnchor).toInt()

                drawImage(
                    image = heroSheet,
                    srcOffset = IntOffset(sxHero, syHero),
                    srcSize = IntSize(heroFrameW, heroFrameH),
                    dstOffset = IntOffset(dstXpx, dstYpx),
                    dstSize = IntSize(heroScaledW, heroScaledH)
                )
            }

            // D-pad opcional
            MovementPad(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                onDir = { dx, dy -> input = Offset(dx, dy) },
                onRelease = { input = Offset.Zero }
            )
        }
    }
}

@Composable
private fun MovementPad(
    modifier: Modifier = Modifier,
    onDir: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    Column(modifier) {
        Row(Modifier.height(48.dp)) {
            Spacer(Modifier.width(48.dp))
            ControlBtn("↑", { onDir(0f, -1f) }, onRelease)
            Spacer(Modifier.width(48.dp))
        }
        Row {
            ControlBtn("←", { onDir(-1f, 0f) }, onRelease)
            Spacer(Modifier.width(8.dp))
            ControlBtn("↓", { onDir(0f, 1f) }, onRelease)
            Spacer(Modifier.width(8.dp))
            ControlBtn("→", { onDir(1f, 0f) }, onRelease)
        }
    }
}

@Composable
private fun ControlBtn(label: String, onPress: () -> Unit, onRelease: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) onPress() else onRelease() }
    Button(
        onClick = {},
        interactionSource = interaction,
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
    ) { Text(label, color = Color.White) }
}
