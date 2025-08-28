package com.jem.gameworldjem.engine

import androidx.compose.ui.geometry.Rect
import kotlin.math.max
import kotlin.math.min

internal fun rectsIntersect(a: Rect, b: Rect) = a.overlaps(b)
internal fun clamp(v: Float, minV: Float, maxV: Float) = max(minV, min(v, maxV))

internal enum class Dir { DOWN, LEFT, RIGHT, UP }
