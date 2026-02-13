package com.onthecrow.interactivegamemap

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Genshin, ${platform.name}!"
    }
}