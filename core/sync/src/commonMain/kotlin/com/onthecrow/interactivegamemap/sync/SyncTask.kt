package com.onthecrow.interactivegamemap.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * What a game does to bring the data on the device up to date. The [Synchronizer] decides when; the task decides
 * where from, how to check what it got and how to keep it. Every app binds one, an offline one [NoopSyncTask].
 */
interface SyncTask {
    /** The version of the data on the device, null while there is none yet. */
    suspend fun localVersion(): String?

    /** A cheap look at the source: whether there is anything to download. */
    suspend fun check(): SyncCheck

    /**
     * Downloads, checks and saves the update that [check] found. Throws when it can't get it.
     *
     * @param progress gets how much is done, from 0 to 1, or null while that is unknown
     */
    suspend fun update(check: SyncCheck.UpdateAvailable, progress: (Float?) -> Unit): SyncOutcome

    /** The task's own reasons to sync besides the launch and the returns from the background, like a new language. */
    val triggers: Flow<Unit> get() = emptyFlow()
}

sealed interface SyncCheck {
    data object UpToDate : SyncCheck

    /** @param payload what the check learned for the update, the synchronizer only passes it back */
    data class UpdateAvailable(val remoteVersion: String, val payload: Any? = null) : SyncCheck
}

sealed interface SyncOutcome {
    data object Updated : SyncOutcome

    /** The source answered, but not with data the task would keep, like an empty list of markers. */
    data class Rejected(val reason: String) : SyncOutcome
}

/** The task of an app whose data is all in the build: there is always data and never anything to download. */
object NoopSyncTask : SyncTask {
    override suspend fun localVersion(): String = "bundle"

    override suspend fun check(): SyncCheck = SyncCheck.UpToDate

    override suspend fun update(check: SyncCheck.UpdateAvailable, progress: (Float?) -> Unit): SyncOutcome {
        return SyncOutcome.Updated
    }
}
