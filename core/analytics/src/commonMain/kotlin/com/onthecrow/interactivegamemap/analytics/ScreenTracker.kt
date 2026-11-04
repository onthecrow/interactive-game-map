package com.onthecrow.interactivegamemap.analytics

/**
 * The screen the user sees: the one on top of the navigation while the app is in the foreground.
 * Each one shown is logged as screen_view and kept in the crash reports. Called on the main thread.
 */
class ScreenTracker(
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
) {

    private var topScreen: String? = null
    private var isForeground = false
    private var wasShown = false
    private val listeners = mutableListOf<(String?) -> Unit>()

    /** Null while the app is in the background. */
    var screen: String? = null
        private set

    fun onTopScreenChange(name: String) {
        topScreen = name
        update()
    }

    fun onForegroundChange(isForeground: Boolean) {
        this.isForeground = isForeground
        update()
    }

    /** [listener] gets the screen shown next, null for the background, before its screen_view. */
    fun addListener(listener: (screen: String?) -> Unit) {
        listeners += listener
    }

    private fun update() {
        val visible = topScreen.takeIf { isForeground }
        if (visible == screen) return
        val previous = screen ?: BACKGROUND.takeIf { wasShown }
        screen = visible
        crashReporter.setKeys(mapOf(KEY_SCREEN to (visible ?: BACKGROUND)))
        listeners.forEach { it(visible) }
        if (visible != null) {
            wasShown = true
            analytics.log(ScreenViewEvent(screenName = visible, previousScreen = previous))
        }
    }

    companion object {
        /** Where the user goes when no screen is shown. */
        const val BACKGROUND = "background"
    }
}
