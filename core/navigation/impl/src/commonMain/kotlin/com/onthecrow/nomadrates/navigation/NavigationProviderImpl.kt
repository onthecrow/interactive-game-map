package com.onthecrow.nomadrates.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.component.KoinComponent

internal class NavigationProviderImpl(
    private val navigator: NavigatorImpl,
    private val startDestination: Destination,
): NavigationProvider, KoinComponent {

    private val scopeController = NavigationScopeController()

    @Composable
    override fun Navigation(modifier: Modifier) {
        val koin = getKoin()
        val entries = remember(koin) { koin.getAll<FeatureEntry<*>>() }
        val entryMap = remember(entries) { entries.associateBy { it.keyClass } }
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

        NavDisplay(
            modifier = modifier,
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
                    NavigationScopeProvider(
                        controller = scopeController,
                        key = key
                    ) {
                        val entry = entryMap[key::class]
                        @Suppress("UNCHECKED_CAST")
                        val typedEntry = entry as? FeatureEntry<NavKey>
                        typedEntry?.content?.invoke(key, Modifier.fillMaxSize())
                    }
                },
            )
        }
    }
}
