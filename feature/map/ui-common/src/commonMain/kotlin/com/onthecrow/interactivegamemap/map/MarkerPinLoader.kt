package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.onthecrow.interactivegamemap.map.model.MapLocation
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private val DungeonBadgeColor = Color(0xFF2A2A2A)

// The badge is in the lower right part of a pin, like on the images of the Genshin map
private const val DUNGEON_BADGE_CENTER_X = 0.78f
private const val DUNGEON_BADGE_CENTER_Y = 0.7f
private const val DUNGEON_BADGE_RADIUS = 0.2f

// The side of a pictogram's box, of the width of its pin: it fills the middle of the round head or of the disc
private const val PICTOGRAM_SIZE = 0.84f

// So the pin under a pictogram wears in another place than the pictogram
private const val PIN_WEAR_SALT = 0x5EED

// The pin of the selected location: bigger, with a see-through layer of the highlight color over it
internal const val SELECTED_PIN_SCALE = 1.3f
private const val SELECTED_PIN_TINT_ALPHA = 0.35f

/**
 * An image of a pin, the pins with equal images share one bitmap. The size is in dp.
 * @param highlight the tint of the selected location's pin, which is also [SELECTED_PIN_SCALE] times bigger
 * @param pictogram drawn over [icon] in its head
 * @param anchorY where the location lies on the pin, of its height from the top
 */
internal data class MarkerPinImage(
    val icon: MarkerIcon,
    val hasDungeonBadge: Boolean,
    val width: Int,
    val height: Int,
    val highlight: Color? = null,
    val pictogram: MarkerIcon? = null,
    val anchorY: Float = 1f,
)

internal val MapLocation.pinImage: MarkerPinImage?
    get() {
        val pin = pin ?: return null
        val isInDungeon = dungeon != null
        return MarkerPinImage(
            icon = pin.dungeonIcon.takeIf { isInDungeon } ?: pin.icon,
            hasDungeonBadge = isInDungeon && pin.dungeonIcon == null,
            width = pin.width,
            height = pin.height,
            pictogram = pin.pictogram,
            anchorY = pin.anchorY,
        )
    }

/** The pin of the location, highlighted with [highlight] when it is the selected one. */
internal fun MapLocation.pinImage(selectedId: Int?, highlight: Color): MarkerPinImage? {
    return pinImage?.let { if (id == selectedId) it.copy(highlight = highlight) else it }
}

/** Must be used from one thread. */
internal class MarkerPinLoader(
    private val iconLoader: MarkerIconLoader,
) {
    private val images = mutableMapOf<MarkerPinImage, ImageBitmap>()

    suspend fun load(pin: MarkerPinImage, density: Float): ImageBitmap? {
        images[pin]?.let { return it }
        val source = iconLoader.load(pin.icon.url) ?: return null
        // A pin without its pictogram would say nothing of the category
        val pictogram = pin.pictogram?.let { iconLoader.load(it.url) ?: return null }
        val textures = PinTextures(
            pin = pin.icon.wear?.let { iconLoader.loadTexture(it) },
            pictogram = pin.pictogram?.wear?.let { iconLoader.loadTexture(it) },
        )
        return renderPin(source, pictogram, textures, pin, density).also { images[pin] = it }
    }

    /** The textures of the wear, a part without one is drawn unworn. */
    private class PinTextures(val pin: ImageBitmap?, val pictogram: ImageBitmap?)

    private fun renderPin(
        source: MarkerImage,
        pictogram: MarkerImage?,
        textures: PinTextures,
        pin: MarkerPinImage,
        density: Float,
    ): ImageBitmap {
        val scale = if (pin.highlight != null) density * SELECTED_PIN_SCALE else density
        val width = (pin.width * scale).roundToInt()
        val height = (pin.height * scale).roundToInt()
        val image = ImageBitmap(width, height)
        val size = Size(width.toFloat(), height.toFloat())
        val bounds = Rect(Offset.Zero, size)
        // The pictogram wears as in the lists, the pins of a color differ by the pictogram over them
        val seed = (pin.pictogram ?: pin.icon).url.hashCode()
        CanvasDrawScope().draw(Density(scale), LayoutDirection.Ltr, Canvas(image), size) {
            // The pin's wear rubs the pictogram as well: they are printed as one
            withWear(pin.icon.wear, textures.pin, bounds, seed xor PIN_WEAR_SALT) {
                drawMarkerImage(source, pin.icon, bounds)
                if (pictogram != null && pin.pictogram != null) {
                    val head = Rect(Offset(size.width / 2, size.width / 2), size.width * PICTOGRAM_SIZE / 2)
                    withWear(pin.pictogram.wear, textures.pictogram, head, seed) {
                        drawMarkerImage(pictogram, pin.pictogram, head)
                    }
                }
            }
            if (pin.hasDungeonBadge) {
                drawDungeonBadge()
            }
            pin.highlight?.let { highlight ->
                // Only over the pin itself, its transparent parts stay so
                drawRect(highlight.copy(alpha = SELECTED_PIN_TINT_ALPHA), blendMode = BlendMode.SrcAtop)
            }
        }
        return image
    }

    private fun DrawScope.drawDungeonBadge() {
        val center = Offset(size.width * DUNGEON_BADGE_CENTER_X, size.height * DUNGEON_BADGE_CENTER_Y)
        val radius = size.width * DUNGEON_BADGE_RADIUS
        drawCircle(DungeonBadgeColor, radius, center)
        // A stack of layers: a rhombus with a chevron under it
        drawPath(
            Path().apply {
                moveTo(center.x, center.y - radius * 0.6f)
                lineTo(center.x + radius * 0.6f, center.y - radius * 0.2f)
                lineTo(center.x, center.y + radius * 0.2f)
                lineTo(center.x - radius * 0.6f, center.y - radius * 0.2f)
                close()
            },
            Color.White,
        )
        drawPath(
            Path().apply {
                moveTo(center.x - radius * 0.6f, center.y + radius * 0.15f)
                lineTo(center.x, center.y + radius * 0.55f)
                lineTo(center.x + radius * 0.6f, center.y + radius * 0.15f)
            },
            Color.White,
            style = Stroke(width = radius * 0.18f),
        )
    }
}

/**
 * The finished loads of the pin images of [locations], null for an image which can't be loaded.
 * The pin of [selectedId] is highlighted with [highlight].
 */
@Composable
internal fun rememberMarkerPinImages(
    locations: List<MapLocation>,
    selectedId: Int?,
    highlight: Color,
): Map<MarkerPinImage, ImageBitmap?> {
    val pinLoader = koinInject<MarkerPinLoader>()
    val density = LocalDensity.current.density
    var images by remember { mutableStateOf(emptyMap<MarkerPinImage, ImageBitmap?>()) }

    LaunchedEffect(locations, selectedId, highlight) {
        locations.mapNotNullTo(mutableSetOf()) { it.pinImage(selectedId, highlight) }
            .filter { images[it] == null }
            .forEach { pin ->
                launch {
                    val image = pinLoader.load(pin, density)
                    images = images + (pin to image)
                }
            }
    }

    return images
}
