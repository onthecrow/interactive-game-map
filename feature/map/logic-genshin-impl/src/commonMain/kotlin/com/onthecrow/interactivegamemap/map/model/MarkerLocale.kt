package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

/** The names of the catalog in one language, from the site of the map. */
@Serializable
internal data class MarkerLocale(
    /** By the ids of the categories. */
    val categories: Map<String, String>,
    /** By the ids of the groups. */
    val groups: Map<String, String>,
    /** By the ids of the maps. */
    val maps: Map<Int, String>,
)
