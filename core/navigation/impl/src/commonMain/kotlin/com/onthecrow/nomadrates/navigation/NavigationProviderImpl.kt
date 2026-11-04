package com.onthecrow.nomadrates.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

// NavDisplay keeps the content of an entry, so the entry reads the key itself to follow its changes
private val LocalContentKey = compositionLocalOf<Any?> { null }

internal class NavigationProviderImpl(
    private val navigator: NavigatorImpl,
    private val startDestination: Destination,
): NavigationProvider, KoinComponent {

    private val scopeController = NavigationScopeController()

    @Composable
    override fun Navigation(modifier: Modifier, contentKey: Any?) {
        val koin = getKoin()
        val entries = remember(koin) { koin.getAll<FeatureEntry<*>>() }
        val entryMap = remember(entries) { entries.associateBy<FeatureEntry<*>, KClass<*>> { it.keyClass } }
        val config = remember(entries) {
            SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        entries.forEach { entry ->
                            @Suppress("UNCHECKED_CAST")
                            val entry = entry as FeatureEntry<NavKey>
                            subclass(entry.keyClass, entry.serializer)
                        }
                    }
                }
            }
        }
        val backStack = rememberNavBackStack(config, startDestination)

        scopeController.ObserveBackStack(backStack)

        LaunchedEffect(navigator) {
            navigator.commands.collect { command ->
                when (command) {
                    is NavigationCommand.To -> backStack.add(command.destination)
                    is NavigationCommand.Back -> if (backStack.size > 1) backStack.removeLastOrNull()
                }
            }
        }

        // NavDisplay composes only the screen on top, so the first one is kept under the others instead:
        // the map isn't created again, black, on the way back. The others slide in and out over it.
        val root = backStack.first()

        CompositionLocalProvider(LocalContentKey provides contentKey) {
            Box(modifier = modifier) {
                Covered(covered = backStack.size > 1) {
                    EntryContent(root, entryMap)
                }
                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    transitionSpec = {
                        // Slide in from right when navigating forward
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                    },
                    popTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                    predictivePopTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                ) { key ->
                    NavEntry(
                        key = key,
                        content = @Composable {
                            // The first screen is drawn under NavDisplay, its entry is a see-through place
                            if (key != root) {
                                EntryContent(key, entryMap)
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun EntryContent(key: NavKey, entryMap: Map<KClass<*>, FeatureEntry<*>>) {
        NavigationScopeProvider(
            controller = scopeController,
            key = key
        ) {
            val entry = entryMap[key::class]
            @Suppress("UNCHECKED_CAST")
            val typedEntry = entry as? FeatureEntry<NavKey>
            // The parameter of the lambda hides key()
            androidx.compose.runtime.key(LocalContentKey.current) {
                typedEntry?.content?.invoke(key, Modifier.fillMaxSize())
            }
        }
    }
}

/** A screen under the others while [covered]: it can't be touched, focused or read out then. */
@Composable
private fun Covered(covered: Boolean, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(covered) {
        // The keyboard of a field on it would stay over the next screen
        if (covered) focusManager.clearFocus(force = true)
    }
    Box(modifier = Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics {} else Modifier)) {
        content()
        if (covered) {
            // Where the screen on top doesn't take a touch, it would reach this one
            Box(
                modifier = Modifier.matchParentSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
            )
        }
    }
}
