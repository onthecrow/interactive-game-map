package com.onthecrow.interactivegamemap.sync

import com.onthecrow.interactivegamemap.analytics.CrashReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSynchronizerTest {

    private class FakeTask(var localVersion: String? = null) : SyncTask {
        var checks = 0
        var updates = 0
        var check: suspend () -> SyncCheck = { SyncCheck.UpToDate }
        var update: suspend ((Float?) -> Unit) -> SyncOutcome = { SyncOutcome.Updated }
        override val triggers = MutableSharedFlow<Unit>()

        override suspend fun localVersion(): String? = localVersion

        override suspend fun check(): SyncCheck {
            checks++
            return check.invoke()
        }

        override suspend fun update(check: SyncCheck.UpdateAvailable, progress: (Float?) -> Unit): SyncOutcome {
            updates++
            return update.invoke(progress).also { if (it == SyncOutcome.Updated) localVersion = check.remoteVersion }
        }
    }

    private class FakeForeground : AppForegroundEvents {
        private var listener: (() -> Unit)? = null
        override fun register(onForeground: () -> Unit) {
            listener = onForeground
        }

        fun emit() = listener?.invoke()
    }

    private class FakeCrashReporter : CrashReporter {
        val nonFatals = mutableListOf<Throwable>()
        override fun log(message: String) = Unit
        override fun setKeys(keys: Map<String, Any?>) = Unit
        override fun recordNonFatal(throwable: Throwable) {
            nonFatals += throwable
        }
    }

    private val task = FakeTask()
    private val foreground = FakeForeground()
    private val crashReporter = FakeCrashReporter()
    private val time = TestTimeSource()

    private fun TestScope.synchronizer(policy: SyncPolicy = SyncPolicy()) = DefaultSynchronizer(
        task = task,
        foregroundEvents = foreground,
        crashReporter = crashReporter,
        policy = policy,
        scope = backgroundScope,
        timeSource = time,
    )

    @Test
    fun `start reads the local data and syncs on every foreground`() = runTest {
        task.localVersion = "1"
        val synchronizer = synchronizer()

        synchronizer.start()
        runCurrent()
        assertEquals(true, synchronizer.state.value.hasLocalData)
        assertEquals(0, task.checks)

        foreground.emit()
        runCurrent()
        foreground.emit()
        runCurrent()
        assertEquals(2, task.checks)
        assertEquals(SyncPhase.Idle, synchronizer.state.value.phase)
        assertEquals(0, synchronizer.state.value.updates)
    }

    @Test
    fun `an update reports its progress and counts`() = runTest {
        val gate = CompletableDeferred<Unit>()
        task.check = { SyncCheck.UpdateAvailable("2") }
        task.update = { progress ->
            progress(0.5f)
            gate.await()
            SyncOutcome.Updated
        }
        val synchronizer = synchronizer()
        synchronizer.start()
        runCurrent()
        assertEquals(false, synchronizer.state.value.hasLocalData)

        foreground.emit()
        runCurrent()
        assertEquals(SyncPhase.Updating(0.5f), synchronizer.state.value.phase)

        gate.complete(Unit)
        runCurrent()
        val state = synchronizer.state.value
        assertEquals(SyncState(hasLocalData = true, phase = SyncPhase.Idle, updates = 1), state)
    }

    @Test
    fun `a network failure fails the sync without a crash report`() = runTest {
        task.check = { throw UnknownHostException("offline") }
        val synchronizer = synchronizer()
        synchronizer.start()
        foreground.emit()
        runCurrent()

        assertEquals(SyncPhase.Failed("no_network"), synchronizer.state.value.phase)
        assertEquals(false, synchronizer.state.value.hasLocalData)
        assertTrue(crashReporter.nonFatals.isEmpty())
    }

    @Test
    fun `other failures and rejected updates are reported`() = runTest {
        task.check = { throw IllegalStateException("changed format") }
        val synchronizer = synchronizer()
        synchronizer.start()
        foreground.emit()
        runCurrent()
        assertIs<SyncPhase.Failed>(synchronizer.state.value.phase)

        task.check = { SyncCheck.UpdateAvailable("2") }
        task.update = { SyncOutcome.Rejected("empty") }
        synchronizer.syncNow(force = true)
        runCurrent()
        assertEquals(SyncPhase.Failed("rejected"), synchronizer.state.value.phase)
        assertEquals(2, crashReporter.nonFatals.size)
        assertIs<SyncRejectedException>(crashReporter.nonFatals.last())
    }

    @Test
    fun `requests during a sync run once more after it`() = runTest {
        val gate = CompletableDeferred<Unit>()
        task.check = {
            if (task.checks == 1) gate.await()
            SyncCheck.UpToDate
        }
        val synchronizer = synchronizer()
        synchronizer.start()
        foreground.emit()
        runCurrent()
        assertEquals(1, task.checks)

        foreground.emit()
        foreground.emit()
        task.triggers.emit(Unit)
        runCurrent()
        assertEquals(1, task.checks)

        gate.complete(Unit)
        runCurrent()
        assertEquals(2, task.checks)
    }

    @Test
    fun `the minimal interval skips automatic syncs but not forced ones`() = runTest {
        val synchronizer = synchronizer(SyncPolicy(minInterval = 10.minutes))
        synchronizer.start()
        foreground.emit()
        runCurrent()
        time += 5.minutes
        foreground.emit()
        runCurrent()
        assertEquals(1, task.checks)

        synchronizer.syncNow(force = true)
        runCurrent()
        assertEquals(2, task.checks)

        time += 11.minutes
        foreground.emit()
        runCurrent()
        assertEquals(3, task.checks)
    }

    @Test
    fun `the task's triggers sync`() = runTest {
        val synchronizer = synchronizer()
        synchronizer.start()
        runCurrent()

        task.triggers.emit(Unit)
        runCurrent()
        assertEquals(1, task.checks)
    }

    @Test
    fun `a failed read of the local data counts as none`() = runTest {
        val failing = object : SyncTask by task {
            override suspend fun localVersion(): String? = throw IOException("broken")
            override val triggers: Flow<Unit> get() = task.triggers
        }
        val synchronizer = DefaultSynchronizer(failing, foreground, crashReporter, scope = backgroundScope)
        synchronizer.start()
        runCurrent()
        assertEquals(false, synchronizer.state.value.hasLocalData)
    }
}
