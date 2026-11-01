package com.onthecrow.interactivegamemap.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

/** An image of a pin, the pins with equal images share one bitmap. The size is in dp. */
internal data class MarkerPinImage(
    val icon: MarkerIcon,
    val hasDungeonBadge: Boolean,
    val width: Int,
    val height: Int,
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
        )
    }

/** Must be used from one thread. */
internal class MarkerPinLoader(
    private val iconLoader: MarkerIconLoader,
) {
    private val images = mutableMapOf<MarkerPinImage, ImageBitmap>()

    suspend fun load(pin: MarkerPinImage, density: Float): ImageBitmap? {
        images[pin]?.let { return it }
        val source = iconLoader.load(pin.icon.url) ?: return null
        return renderPin(source, pin, density).also { images[pin] = it }
    }

    private fun renderPin(source: ImageBitmap, pin: MarkerPinImage, density: Float): ImageBitmap {
        val width = (pin.width * density).roundToInt()
        val height = (pin.height * density).roundToInt()
        val image = ImageBitmap(width, height)
        val canvas = Canvas(image)
        canvas.drawImageRect(
            image = source,
            srcOffset = IntOffset(pin.icon.x ?: 0, pin.icon.y ?: 0),
            srcSize = IntSize(pin.icon.width ?: source.width, pin.icon.height ?: source.height),
            dstSize = IntSize(width, height),
            paint = Paint(),
        )
        if (pin.hasDungeonBadge) {
            canvas.drawDungeonBadge(width, height)
        }
        return image
    }

    private fun Canvas.drawDungeonBadge(width: Int, height: Int) {
        val center = Offset(width * DUNGEON_BADGE_CENTER_X, height * DUNGEON_BADGE_CENTER_Y)
        val radius = width * DUNGEON_BADGE_RADIUS
        drawCircle(center, radius, Paint().apply { color = DungeonBadgeColor })
        // A stack of layers: a rhombus with a chevron under it
        drawPath(
            Path().apply {
                moveTo(center.x, center.y - radius * 0.6f)
                lineTo(center.x + radius * 0.6f, center.y - radius * 0.2f)
                lineTo(center.x, center.y + radius * 0.2f)
                lineTo(center.x - radius * 0.6f, center.y - radius * 0.2f)
                close()
            },
            Paint().apply { color = Color.White },
        )
        drawPath(
            Path().apply {
                moveTo(center.x - radius * 0.6f, center.y + radius * 0.15f)
                lineTo(center.x, center.y + radius * 0.55f)
                lineTo(center.x + radius * 0.6f, center.y + radius * 0.15f)
            },
            Paint().apply {
                color = Color.White
                style = PaintingStyle.Stroke
                strokeWidth = radius * 0.18f
            },
        )
    }
}

/** The finished loads of the pin images of [locations], null for an image which can't be loaded. */
@Composable
internal fun rememberMarkerPinImages(locations: List<MapLocation>): Map<MarkerPinImage, ImageBitmap?> {
    val pinLoader = koinInject<MarkerPinLoader>()
    val density = LocalDensity.current.density
    var images by remember { mutableStateOf(emptyMap<MarkerPinImage, ImageBitmap?>()) }

    LaunchedEffect(locations) {
        locations.mapNotNullTo(mutableSetOf()) { it.pinImage }
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
