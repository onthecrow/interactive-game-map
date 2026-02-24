package com.onthecrow.interactivegamemap.network

import kotlinx.coroutines.flow.Flow

interface RemoteConfigProvider {
    fun getRemoteConfigFlow(): Flow<RemoteConfig>
}
