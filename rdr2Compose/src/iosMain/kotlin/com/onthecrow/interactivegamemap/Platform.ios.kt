package com.onthecrow.interactivegamemap

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
@Suppress("unused")
class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val platformModule = module {
    }
}
