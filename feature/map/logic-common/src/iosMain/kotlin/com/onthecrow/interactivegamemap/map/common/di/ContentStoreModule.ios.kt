package com.onthecrow.interactivegamemap.map.common.di

import com.onthecrow.interactivegamemap.map.common.ContentFileStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// Application Support: the app's own files, which the system doesn't clear as it does the caches
@OptIn(ExperimentalForeignApi::class)
actual val contentStoreModule: Module = module {
    single {
        val url = NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        ContentFileStore(Path(requireNotNull(url?.path), "content"))
    }
}
