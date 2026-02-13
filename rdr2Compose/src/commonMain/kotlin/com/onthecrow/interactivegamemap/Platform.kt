package com.onthecrow.interactivegamemap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform