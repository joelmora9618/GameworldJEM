package com.jem.gameworldjem.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.jem.gameworldjem.engine.Dir
import com.jem.gameworldjem.engine.clamp
import com.jem.gameworldjem.engine.rectsIntersect
import com.jem.gameworldjem.io.loadTiledMapFromAssets
import com.jem.gameworldjem.render.drawHero
import com.jem.gameworldjem.render.drawTiledLayers
import com.jem.gameworldjem.tiled.model.TiledLayer
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.min

/**
 * API pública: pantalla del mundo top-down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameWorldScreen(
    modifier: Modifier = Modifier,
    title: String = "Top-Down Demo",
    onBack: (() -> Unit)? = null,
    basePath: String = "tiled",
    mapFile: String = "demo_map.tmj",
    heroSheetPath: String = "tiled/hero_spritesheet.png",
    desiredTilesTall: Float = 1.6f // zoom del héroe
) {
    val ctx = LocalContext.current

    // Map + tilesets
    val bundle = remember(mapFile, basePath) {
        loadTiledMapFromAssets(ctx, basePath, mapFile)
    }
    val map = bundle.map
    val tilesets = bundle.tilesets

    // Escala del tileset con respecto al mapa (usar primer tileset)
    val scaleFactor = remember(bundle) {
        tilesets.firstOrNull()?.meta?.tilewidth?.toFloat()?.div(map.tilewidth.toFloat()) ?: 1f
    }.coerceAtLeast(1f)

    // Hero sheet
    val heroSheet: ImageBitmap = remember(heroSheetPath) {
        ctx.assets.open(heroSheetPath).use { BitmapFactory.decodeStream(it).asImageBitmap() }
    }

    // Atlas 4x4 con margen/spacing holgados
    val HERO_COLS = 4
    val HERO_ROWS = 4
    val HERO_MARGIN = 16
    val HERO_SPACING = 16
    val heroFrameW = (heroSheet.width  - HERO_MARGIN * 2 - (HERO_COLS - 1) * HERO_SPACING) / HERO_COLS
    val heroFrameH = (heroSheet.height - HERO_MARGIN * 2 - (HERO_ROWS - 1) * HERO_SPACING) / HERO_ROWS

    // Zoom del héroe a pixeles pantalla
    val heroScale = (map.tileheight * desiredTilesTall * scaleFactor) / heroFrameH.toFloat()
    val heroScaledW = (heroFrameW * heroScale).toInt()
    val heroScaledH = (heroFrameH * heroScale).toInt()

    // Mundo (unidades mapa)
    val tw = map.tilewidth
    val th = map.tileheight
    val worldW = map.width * tw
    val worldH = map.height * th

    // Capas útiles
    val tileLayers: List<TiledLayer> = remember(map) {
        map.layers.filter { it.type == "tilelayer" && it.visible && (it.data != null || it.chunks != null) }
    }
    val collisions = remember(map) {
        map.layers.firstOrNull { it.type == "objectgroup" && it.name == "collision" }?.objects.orEmpty()
    }

    // Estado jugador/cámara
    var playerPos by remember { mutableStateOf(Offset(worldW / 2f, worldH / 2f)) }
    var moving by remember { mutableStateOf(false) }
    var dir by remember { mutableStateOf(Dir.DOWN) }
    var frameIdx by remember { mutableStateOf(0) }
    var camera by remember { mutableStateOf(playerPos) }
    var input by remember { mutableStateOf(Offset.Zero) }

    // Loop de juego (anim/mov)
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

                // anim
                if (moving) {
                    elapsedMs += dt.toLong()
                    if (elapsedMs >= frameMs) { elapsedMs = 0; frameIdx = (frameIdx + 1) % HERO_COLS }
                } else frameIdx = 0

                // move
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
                        playerPos = Offset(
                            x = clamp(next.x, 0f + half, worldW - half),
                            y = clamp(next.y, 0f + half, worldH - half)
                        )
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
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
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
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, drag ->
                                change.consume()
                                val maxMag = 80f
                                val dx = (drag.x.coerceIn(-maxMag, maxMag)) / maxMag
                                val dy = (drag.y.coerceIn(-maxMag, maxMag)) / maxMag
                                input = Offset(dx, dy)
                            },
                            onDragEnd = { input = Offset.Zero },
                            onDragCancel = { input = Offset.Zero }
                        )
                    }
            ) {
                // ======= ÚNICO CAMBIO IMPORTANTE PARA CENTRAR =======
                // Convertimos la cámara (en el centro del viewport) a top-left
                val viewWWorld = size.width / scaleFactor
                val viewHWorld = size.height / scaleFactor
                val camLeft = clamp(
                    camera.x - viewWWorld / 2f,
                    0f,
                    (worldW - viewWWorld).coerceAtLeast(0f)
                )
                val camTop = clamp(
                    camera.y - viewHWorld / 2f,
                    0f,
                    (worldH - viewHWorld).coerceAtLeast(0f)
                )
                // =====================================================

                // Tiles
                drawTiledLayers(
                    map = map,
                    tilesets = tilesets,
                    tileLayers = tileLayers,
                    camX = camLeft,   // ← usar top-left
                    camY = camTop,    // ← usar top-left
                    scaleFactor = scaleFactor
                )

                // Héroe (usa la misma cámara top-left)
                drawHero(
                    heroSheet = heroSheet,
                    frameIdx = frameIdx,
                    dir = dir,
                    heroFrameW = heroFrameW,
                    heroFrameH = heroFrameH,
                    heroMargin = HERO_MARGIN,
                    heroSpacing = HERO_SPACING,
                    worldX = playerPos.x,
                    worldY = playerPos.y,
                    camX = camLeft,   // ← usar top-left
                    camY = camTop,    // ← usar top-left
                    scaleFactor = scaleFactor,
                    dstW = heroScaledW, dstH = heroScaledH
                )
            }

            // D-pad
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
