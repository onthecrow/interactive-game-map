package com.onthecrow.interactivegamemap.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private class RecordingTracker : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    var reportedUserId: String? = null

    override fun logEvent(name: String, params: Map<String, Any>) {
        events += name to params
    }

    override fun setUserProperty(name: String, value: String?) = Unit

    override fun setUserId(id: String) {
        reportedUserId = id
    }
}

private class RecordingCrashTracker : CrashReportTracker {
    val keys = mutableMapOf<String, Any>()
    var reportedUserId: String? = null

    override fun log(message: String) = Unit

    override fun setKeys(keys: Map<String, Any>) {
        this.keys += keys
    }

    override fun recordNonFatal(throwable: Throwable) = Unit

    override fun setUserId(id: String) {
        reportedUserId = id
    }
}

private class TestEvent(override val params: Map<String, Any?>) : AnalyticsEvent {
    override val name = "test"
}

class AnalyticsTest {
    private val tracker = RecordingTracker()
    private val analytics = AnalyticsImpl(listOf(tracker), userId = "id")
    private val crashTracker = RecordingCrashTracker()
    private val crashReporter = CrashReporterImpl(listOf(crashTracker), userId = "id")

    @Test
    fun `the reports go with the analytics id from the start`() {
        assertEquals("id", tracker.reportedUserId)
        assertEquals("id", crashTracker.reportedUserId)
    }

    @Test
    fun `keys get the values Firebase takes and an empty one for none`() {
        crashReporter.setKeys(mapOf("ready" to true, PARAM_MAP_ID to 2, "error" to "timeout"))
        crashReporter.setKeys(mapOf("error" to null))

        assertEquals<Map<String, Any>>(mapOf("ready" to "true", PARAM_MAP_ID to 2L, "error" to ""), crashTracker.keys)
    }

    @Test
    fun `values become the types Firebase takes`() {
        analytics.log(
            TestEvent(
                mapOf(
                    "flag" to true,
                    "int" to 3,
                    "float" to 0.5f,
                    "enum" to SearchType.MARKER_LIST,
                    "long" to "x".repeat(150),
                    "none" to null,
                )
            )
        )

        val params = tracker.events.single().second
        assertEquals(
            mapOf("flag" to "true", "int" to 3L, "float" to 0.5, "enum" to "marker_list", "long" to "x".repeat(100)),
            params,
        )
    }

    @Test
    fun `default parameters go with every event until they are removed`() {
        analytics.setDefaultParams(mapOf(PARAM_MAP_ID to 2))
        analytics.log(TestEvent(mapOf("a" to "b")))
        analytics.setDefaultParams(mapOf(PARAM_MAP_ID to null))
        analytics.log(TestEvent(emptyMap()))

        assertEquals(mapOf(PARAM_MAP_ID to 2L, "a" to "b"), tracker.events[0].second)
        assertEquals(emptyMap(), tracker.events[1].second)
    }

    @Test
    fun `screens are logged when they are shown`() {
        val screens = ScreenTracker(analytics, crashReporter)
        val shown = mutableListOf<String?>()
        screens.addListener { shown += it }

        screens.onTopScreenChange("map")
        screens.onForegroundChange(true)
        screens.onTopScreenChange("settings")
        screens.onForegroundChange(false)
        screens.onForegroundChange(true)
        screens.onTopScreenChange("settings")

        assertEquals(listOf("map", "settings", null, "settings"), shown)
        assertEquals("settings", crashTracker.keys[KEY_SCREEN])
        assertEquals(
            listOf(
                ScreenViewEvent("map", previousScreen = null),
                ScreenViewEvent("settings", previousScreen = "map"),
                ScreenViewEvent("settings", previousScreen = ScreenTracker.BACKGROUND),
            ).map { it.name to normalize(it.params) },
            tracker.events,
        )
    }

    @Test
    fun `the events fit the limits of Firebase`() {
        val events = listOf(
            ScreenViewEvent("map", "settings"),
            MapReadyEvent(MapLoadSource.MAP_SWITCH, 1.seconds, 2.seconds, 1, wasHidden = false),
            MapTimeEvent(3.seconds, 2.seconds, "settings", true, true, 10, 1, "timeout"),
            SearchEvent(SearchType.LINK, "dreamcatcher", 4),
            MarkerFoundChangeEvent(1, "o3", isFound = true, foundTotal = 5),
            SettingChangeEvent(AnalyticsSetting.HIDE_MARKERS_WHILE_MOVING, true),
        )
        val snakeCase = Regex("[a-z][a-z0-9_]*")

        events.forEach { event ->
            assertTrue(event.name.length <= 40 && snakeCase.matches(event.name), event.name)
            assertTrue(event.params.size + 1 <= 25, event.name)
            (event.params.keys + PARAM_MAP_ID).forEach { assertTrue(it.length <= 40 && snakeCase.matches(it), it) }
        }
        assertEquals(1500L, MapTimeEvent(1500.milliseconds, null, "", false, false, 0, 0, null).params["screen_ms"])
    }
}
