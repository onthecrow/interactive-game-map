package com.onthecrow.interactivegamemap.settings

import com.onthecrow.nomadrates.navigation.Destination
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDestination : Destination {
    override val screenName get() = "settings"
}
