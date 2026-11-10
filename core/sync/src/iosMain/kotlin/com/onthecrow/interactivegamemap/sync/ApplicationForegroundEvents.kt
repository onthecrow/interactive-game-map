package com.onthecrow.interactivegamemap.sync

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObjectProtocol

/**
 * The launch, at once, and every return from the background. Not the becoming active: that comes after the
 * Control Center or a system dialog as well.
 */
internal class ApplicationForegroundEvents : AppForegroundEvents {
    private var observer: NSObjectProtocol? = null

    override fun register(onForeground: () -> Unit) {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> onForeground() }
        onForeground()
    }
}
