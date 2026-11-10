package com.onthecrow.interactivegamemap.map.data

import com.onthecrow.interactivegamemap.map.common.UpdateGuard
import com.onthecrow.interactivegamemap.map.hoyolab.ChangelogEntryDto
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabApi
import com.onthecrow.interactivegamemap.map.hoyolab.HoyolabConfigProvider
import com.onthecrow.interactivegamemap.map.hoyolab.MapInfoDto
import com.onthecrow.interactivegamemap.settings.AppLanguageProvider
import com.onthecrow.interactivegamemap.sync.SyncCheck
import com.onthecrow.interactivegamemap.sync.SyncOutcome
import com.onthecrow.interactivegamemap.sync.SyncTask
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

// Teyvat's points are most of the download
private const val TEYVAT_MAP_ID = 2
private const val TEYVAT_POINTS_BYTES = 23_000_000L
private const val OTHER_POINTS_BYTES = 600_000L

/** What the check found for the update: everything, or the names in a new language only. */
internal data class HoyolabUpdatePlan(
    val full: Boolean,
    val language: String,
    val maps: List<MapInfoDto>,
)

/**
 * Keeps HoYoLAB's map on the device. The check reads the changelog and the tiles' version of every map, a few
 * kilobytes; the update downloads the points of every map, their underground places and the names in the app's
 * language. Once in [com.onthecrow.interactivegamemap.map.hoyolab.HoyolabContentConfig.fullRefreshDays] it
 * downloads all of it anyway: the changelog may not tell of removed points.
 */
@OptIn(ExperimentalTime::class)
internal class HoyolabSyncTask(
    private val api: HoyolabApi,
    private val content: GenshinContent,
    private val config: HoyolabConfigProvider,
    private val languageProvider: AppLanguageProvider,
    private val clock: Clock = Clock.System,
) : SyncTask {

    // The names of the categories come in the app's language
    override val triggers: Flow<Unit> = languageProvider.language.drop(1).map { }

    override suspend fun localVersion(): String? = content.meta()?.version

    override suspend fun check(): SyncCheck {
        val config = config.config.value
        val meta = content.meta()
        if (!config.enabled) {
            // Turned off: the data there is stays, without any the app can only say it failed
            return if (meta != null) SyncCheck.UpToDate else throw IllegalStateException("The source is turned off")
        }
        val language = languageProvider.currentLanguage
        val (maps, changelog) = coroutineScope {
            val maps = config.mapIds.map { async { api.mapInfo(it, language) } }
            // One changelog for all the maps, whichever map is asked
            val changelog = async { api.changelog(config.mapIds.first()) }
            maps.awaitAll() to changelog.await()
        }
        val version = sourceVersion(maps, changelog)
        val age = clock.now().toEpochMilliseconds() - (meta?.fetchedAtEpochMillis ?: 0)
        return when {
            meta == null || meta.version != version || age > config.fullRefreshDays.days.inWholeMilliseconds ->
                SyncCheck.UpdateAvailable(version, HoyolabUpdatePlan(full = true, language, maps))
            language !in meta.languages ->
                SyncCheck.UpdateAvailable("$version/$language", HoyolabUpdatePlan(full = false, language, maps))
            else -> SyncCheck.UpToDate
        }
    }

    override suspend fun update(check: SyncCheck.UpdateAvailable, progress: (Float?) -> Unit): SyncOutcome {
        val plan = check.payload as HoyolabUpdatePlan
        val steps = Steps(plan, progress)
        val labels = SnapshotLabels(
            language = plan.language,
            groups = api.labelTree(plan.maps.first().id, plan.language),
            mapNames = plan.maps.associate { map ->
                map.id to if (map.id == TEYVAT_MAP_ID) teyvatName(plan.language) else map.name
            },
        )
        steps.done(LABELS_WEIGHT)
        val meta = content.meta()
        if (!plan.full && meta != null) {
            content.saveLabels(labels, meta.copy(languages = meta.languages + plan.language))
            return SyncOutcome.Updated
        }

        // Teyvat first: it's most of the bar
        val maps = plan.maps.sortedBy { it.id != TEYVAT_MAP_ID }
        val points = maps.associate { map ->
            val weight = steps.pointsWeight(map.id)
            val expected = if (map.id == TEYVAT_MAP_ID) TEYVAT_POINTS_BYTES else OTHER_POINTS_BYTES
            val mapPoints = api.points(map.id, expected) { steps.within(weight, it) }
            steps.done(weight)
            map.id to mapPoints
        }
        val groups = maps.flatMap { map ->
            api.pointGroups(map.id).map { SnapshotGroup(it.id, map.id, it.floors) }.also { steps.done(GROUPS_WEIGHT) }
        }
        val snapshot = GenshinSnapshot(
            version = check.remoteVersion,
            maps = plan.maps.map { SnapshotMap(it.id, it.detail) },
            points = snapshotPoints(points),
            groups = groups,
        )
        UpdateGuard.rejectReason(snapshot.points.size, meta?.pointCount)?.let { return SyncOutcome.Rejected(it) }
        content.save(
            snapshot = snapshot,
            labels = labels,
            meta = SnapshotMeta(
                version = check.remoteVersion,
                fetchedAtEpochMillis = clock.now().toEpochMilliseconds(),
                pointCount = snapshot.points.size,
                languages = listOf(plan.language),
            ),
        )
        return SyncOutcome.Updated
    }

    /** The share of the bar each request fills, by how big its answer is. */
    private class Steps(plan: HoyolabUpdatePlan, private val progress: (Float?) -> Unit) {
        private val total = LABELS_WEIGHT +
            if (plan.full) plan.maps.sumOf { pointsWeight(it.id) + GROUPS_WEIGHT } else 0
        private var finished = 0

        fun pointsWeight(mapId: Int) = if (mapId == TEYVAT_MAP_ID) TEYVAT_POINTS_WEIGHT else OTHER_POINTS_WEIGHT

        fun within(weight: Int, fraction: Float) {
            progress((finished + weight * fraction) / total)
        }

        fun done(weight: Int) {
            finished += weight
            progress(finished.toFloat() / total)
        }
    }

    private companion object {
        const val LABELS_WEIGHT = 4
        const val GROUPS_WEIGHT = 1
        const val TEYVAT_POINTS_WEIGHT = 80
        const val OTHER_POINTS_WEIGHT = 2
    }
}

/** HoYoLAB calls Teyvat's map "the interactive map of Teyvat", too long for the map's own name. */
internal fun teyvatName(language: String): String = when (language) {
    "ru" -> "Тейват"
    "ja" -> "テイワット"
    "ko" -> "티바트"
    "zh" -> "提瓦特"
    else -> "Teyvat"
}

/**
 * What changes when anything on the maps changes: the newest day of the changelog with how many points it changed,
 * and the version of every map's tiles.
 */
internal fun sourceVersion(maps: List<MapInfoDto>, changelog: List<ChangelogEntryDto>): String {
    val newest = changelog.firstOrNull()
    val changes = newest?.let { "${it.timestamp}+${it.points.size}+${it.points.sumOf { count -> count.count }}" }
    return maps.joinToString(",") { "${it.id}:${it.detail.mapVersion}" } + "|" + changes.orEmpty()
}
