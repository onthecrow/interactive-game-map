package com.onthecrow.interactivegamemap.map.model

import kotlinx.serialization.Serializable

@Serializable
data class Locale(
    val en: Map<String, String>? = null,
)