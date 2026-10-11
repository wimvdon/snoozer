package io.github.impca201.snoozer.ui.home

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Material 3 Expressive style shapes, built with androidx.graphics.shapes (the library the
 * Material shape set is made of). Polygons are centred on (0, 0) with radius 1.
 */
object ExpressiveShapes {
    /** Friendly 9-sided "cookie": the resting (not snoozing) toggle. */
    val Cookie: RoundedPolygon =
        RoundedPolygon.star(
            numVerticesPerRadius = 9,
            radius = 1f,
            innerRadius = 0.86f,
            rounding = CornerRounding(radius = 0.32f, smoothing = 0.6f),
        )

    /** Soft 8-petal flower: the sleepy, snoozing toggle. */
    val Flower: RoundedPolygon =
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.72f,
            rounding = CornerRounding(radius = 0.36f, smoothing = 0.9f),
        )

    val CookieToFlower: Morph = Morph(Cookie, Flower)

    /** Degrees between identical orientations of [Cookie]; used to settle rotation nicely. */
    const val COOKIE_SYMMETRY_DEGREES = 360f / 9
}

/**
 * A [Shape] that draws [morph] at [progress] (0 = start, 1 = end), rotated by [rotation] degrees
 * around the centre and scaled to fill the layout bounds.
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f))
        val matrix =
            Matrix().apply {
                setRotate(rotation)
                postScale(size.width / 2f, size.height / 2f)
                postTranslate(size.width / 2f, size.height / 2f)
            }
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}
