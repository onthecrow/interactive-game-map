package com.onthecrow.interactivegamemap.network

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig

internal class AndroidRemoteConfigProvider : RemoteConfigProviderImpl() {

    // TODO to di
    private val remoteConfig = Firebase.remoteConfig

    override fun startBackgroundSync() {
        Firebase.remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    // todo
//                    configFields.forEach { configField ->
//                        if (configUpdate.updatedKeys.contains(configField)) {
//                            onConfigUpdated(configField)
//                        }
//                    }
                    onConfigUpdated()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                log("updates failed, ${error.code}: ${error.message}")
            }
        })
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                log("fetched")
            } else {
                // TODO implement retry logic
                log("fetch failed, ${task.exception?.message}")
            }
            // Falls back to the last activated values when offline
            onConfigUpdated()
        }
    }

    override fun getString(key: String): String {
        return Firebase.remoteConfig.getString(key)
    }
}
