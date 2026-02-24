package com.onthecrow.interactivegamemap.network

import android.util.Log
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
                Log.e(javaClass.simpleName, error.message, error)
            }
        })
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoteConfig", "Initial Fetch Succeeded")
                onConfigUpdated()
            } else {
                // TODO implement retry logic
                Log.e("RemoteConfig", "Initial Fetch Failed")
            }
        }
    }

    override fun getString(key: String): String {
        return Firebase.remoteConfig.getString(key)
    }
}
