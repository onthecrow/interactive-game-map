package com.onthecrow.interactivegamemap

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "RDR2, ${platform.name}!"
    }
}