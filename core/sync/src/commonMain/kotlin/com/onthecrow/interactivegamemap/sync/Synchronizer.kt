package com.onthecrow.interactivegamemap.sync

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import com.onthecrow.interactivegamemap.network.isNetworkError
import com.onthecrow.interactivegamemap.network.networkErrorType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Where a sync is. */
sealed interface SyncPhase {
    data object Idle : SyncPhase

    /** The cheap look at the source, nothing to show for it. */
    data object Checking : SyncPhase

    /** @param progress from 0 to 1, null while unknown */
    data class Updating(val progress: Float?) : SyncPhase

    /** @param reason in a few words, for the analytics: no_network, timeout, rejected, the name of the error */
    data class Failed(val reason: String) : SyncPhase
}

/**
 * @param hasLocalData whether the device has data to show, null until the task has said
 * @param updates how many updates were applied since the launch, the screens read the data again when it grows
 */
data class SyncState(
    val hasLocalData: Boolean? = null,
    val phase: SyncPhase = SyncPhase.Idle,
    val updates: Int = 0,
)

/** @param minInterval how long after a successful sync the next automatic one is skipped */
data class SyncPolicy(
    val minInterval: Duration = Duration.ZERO,
)

/** Keeps the data of the app up to date: at the launch, on every return from the background and on the task's triggers. */
interface Synchronizer {
    val state: StateFlow<SyncState>

    /** Subscribes to the app's lifecycle, once; called as early as possible, in the Application. */
    fun start()

    /** Syncs now; [force] skips [SyncPolicy.minInterval], like the retry of a failed first load. */
    fun syncNow(force: Boolean = false)
}

/**
 * One sync at a time: the requests during one run once more after it, so a trigger is never lost.
 * The task runs in [scope], which outlives the screens.
 */
class DefaultSynchronizer(
    private val task: SyncTask,
    private val foregroundEvents: AppForegroundEvents,
    private val crashReporter: CrashReporter,
    private val policy: SyncPolicy = SyncPolicy(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : Synchronizer {

    private val mutableState = MutableStateFlow(SyncState())
    override val state: StateFlow<SyncState> = mutableState.asStateFlow()

    private val started = MutableStateFlow(false)
    private val running = MutableStateFlow(false)

    // Asked for during a running sync, with force if any of the requests had it
    private val pending = MutableStateFlow<Boolean?>(null)
    private var lastSuccess: TimeMark? = null

    override fun start() {
        if (!started.compareAndSet(expect = false, update = true)) return
        scope.launch { readLocalData() }
        foregroundEvents.register { syncNow() }
        scope.launch { task.triggers.collect { syncNow() } }
    }

    override fun syncNow(force: Boolean) {
        scope.launch { sync(force) }
    }

    private suspend fun readLocalData(): Boolean {
        val hasLocalData = try {
            task.localVersion() != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            crashReporter.log("Sync: can't read the local version, ${e.message}")
            crashReporter.recordNonFatal(e)
            false
        }
        mutableState.update { it.copy(hasLocalData = hasLocalData) }
        return hasLocalData
    }

    private suspend fun sync(force: Boolean) {
        // The request waits in pending; whoever becomes the runner takes the requests until none is left
        pending.update { (it ?: false) || force }
        while (running.compareAndSet(expect = false, update = true)) {
            val request = pending.getAndUpdate { null }
            try {
                if (request != null) runSync(request)
            } finally {
                running.value = false
            }
            if (pending.value == null) break
        }
    }

    private suspend fun runSync(force: Boolean) {
        val last = lastSuccess
        if (!force && last != null && last.elapsedNow() < policy.minInterval) return
        if (mutableState.value.hasLocalData == null) readLocalData()
        try {
            setPhase(SyncPhase.Checking)
            when (val check = task.check()) {
                SyncCheck.UpToDate -> {
                    lastSuccess = timeSource.markNow()
                    setPhase(SyncPhase.Idle)
                }
                is SyncCheck.UpdateAvailable -> {
                    setPhase(SyncPhase.Updating(progress = null))
                    when (val outcome = task.update(check) { setPhase(SyncPhase.Updating(it?.coerceIn(0f, 1f))) }) {
                        SyncOutcome.Updated -> {
                            lastSuccess = timeSource.markNow()
                            crashReporter.log("Sync: updated to ${check.remoteVersion}")
                            mutableState.update {
                                it.copy(hasLocalData = true, phase = SyncPhase.Idle, updates = it.updates + 1)
                            }
                        }
                        is SyncOutcome.Rejected -> {
                            crashReporter.log("Sync: rejected ${check.remoteVersion}, ${outcome.reason}")
                            crashReporter.recordNonFatal(SyncRejectedException(outcome.reason))
                            setPhase(SyncPhase.Failed(REASON_REJECTED))
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val reason = e.networkErrorType()
            crashReporter.log("Sync: failed, $reason: ${e.message}")
            // A broken connection is the user's, anything else may be a changed source
            if (!e.isNetworkError()) crashReporter.recordNonFatal(e)
            setPhase(SyncPhase.Failed(reason))
        }
    }

    private fun setPhase(phase: SyncPhase) {
        mutableState.update { it.copy(phase = phase) }
    }

    private companion object {
        const val REASON_REJECTED = "rejected"
    }
}

/** The source answered with data the task wouldn't keep. */
class SyncRejectedException(reason: String) : Exception(reason)
