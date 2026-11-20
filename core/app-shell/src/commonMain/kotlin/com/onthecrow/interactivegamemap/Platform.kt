package com.onthecrow.interactivegamemap

import org.koin.core.module.Module

interface Platform {
    val name: String
    val platformModule: Module
}
