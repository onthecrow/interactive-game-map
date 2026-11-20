package com.onthecrow.interactivegamemap.sync

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * The start of the process's lifecycle: the first activity seen at the launch and after every return from the
 * background. An activity recreated, like on a change of the language, doesn't count: the process stops only some
 * time after its last activity.
 */
internal class ProcessForegroundEvents : AppForegroundEvents {
    override fun register(onForeground: () -> Unit) {
        val observe = {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = onForeground()
            })
        }
        // The lifecycle takes observers on the main thread only
        if (Looper.myLooper() == Looper.getMainLooper()) observe() else Handler(Looper.getMainLooper()).post(observe)
    }
}
