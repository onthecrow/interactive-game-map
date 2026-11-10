package com.onthecrow.interactivegamemap.map

import com.onthecrow.interactivegamemap.map.model.MapsConfig
import kotlinx.coroutines.flow.Flow

/** The maps of the game: their tiles, camera, bounds and dungeons. */
interface MapsRepository {
    /** The maps once they are known, then again whenever they change: a new config, new data of the game. */
    fun maps(): Flow<MapsConfig>
}
