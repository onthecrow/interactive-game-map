package com.onthecrow.interactivegamemap.map.mapper

import com.onthecrow.interactivegamemap.map.model.MarkerCatalog
import com.onthecrow.interactivegamemap.map.model.MarkerIcon
import com.onthecrow.interactivegamemap.map.model.MarkerPin

/**
 * Sets the url of every icon of the catalog, and of the textures of their wear, to [iconUrl] of it, once for the
 * icons with equal urls.
 */
internal suspend fun MarkerCatalog.withIconUrls(iconUrl: suspend (String) -> String): MarkerCatalog {
    val urls = mutableMapOf<String, String>()

    suspend fun MarkerIcon.withUrl(): MarkerIcon {
        return copy(
            url = urls[url] ?: iconUrl(url).also { urls[url] = it },
            wear = wear?.let { it.copy(texture = it.texture.withUrl()) },
        )
    }

    suspend fun MarkerPin.withUrls(): MarkerPin {
        return copy(
            icon = icon.withUrl(),
            pictogram = pictogram?.withUrl(),
            dungeonIcon = dungeonIcon?.withUrl(),
        )
    }

    return copy(
        categories = categories.mapValues { (_, category) ->
            category.copy(icon = category.icon.withUrl(), pin = category.pin.withUrls())
        },
    )
}
