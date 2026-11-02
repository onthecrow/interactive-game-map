package com.onthecrow.nomadrates.navigation

import androidx.navigation3.runtime.NavKey

interface Destination : NavKey {
    /** The name of the screen in the analytics. */
    val screenName: String
}
