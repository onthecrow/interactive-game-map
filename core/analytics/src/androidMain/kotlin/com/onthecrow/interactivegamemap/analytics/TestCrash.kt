package com.onthecrow.interactivegamemap.analytics

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper

private const val EXTRA_TEST_CRASH = "test_crash"

// The map is open by then, the keys of the report are set
private const val TEST_CRASH_DELAY_MS = 5_000L

/** The crash asked for to see the crash reports reach Crashlytics, see docs/analytics-events.md. */
class TestCrashException : RuntimeException("A test crash, asked for to check that Crashlytics gets the reports")

/**
 * Crashes the app a few seconds after a launch with the extra `test_crash`, in a debuggable build only:
 * `adb shell am start -S -n <package>/<activity> --ez test_crash true`. A debug build sends the report when it's
 * built with `-PcrashlyticsInDebug`, on its next launch.
 */
fun Activity.crashOnTestRequest() {
    val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (!isDebuggable || !intent.getBooleanExtra(EXTRA_TEST_CRASH, false)) return
    Handler(Looper.getMainLooper()).postDelayed({ throw TestCrashException() }, TEST_CRASH_DELAY_MS)
}
