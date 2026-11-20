package com.onthecrow.interactivegamemap.map.common.di

import android.content.Context
import com.onthecrow.interactivegamemap.map.common.ContentFileStore
import kotlinx.io.files.Path
import org.koin.core.module.Module
import org.koin.dsl.module

// Kept by backups and not cleared by the system as a cache would be: the first load is the slow one
actual val contentStoreModule: Module = module {
    single { ContentFileStore(Path(get<Context>().filesDir.path, "content")) }
}
