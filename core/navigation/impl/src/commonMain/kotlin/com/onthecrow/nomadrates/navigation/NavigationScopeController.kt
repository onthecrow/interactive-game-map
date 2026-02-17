package com.onthecrow.nomadrates.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * A class that holds ViewModelStores and manages their lifecycle.
 */
class NavigationScopeController {
    private val viewModelStores = mutableMapOf<String, ViewModelStore>()

    /**
     * Observes backStack changes and automatically clears unused ViewModelStores.
     */
    @Composable
    fun ObserveBackStack(backStack: List<Any>) {
        LaunchedEffect(backStack) {
            snapshotFlow { backStack.toList() }
                .collect { currentList ->
                    val currentKeys = currentList.map { it.toString() }.toSet()
                    clearStores(keysToKeep = currentKeys)
                }
        }
    }

    private fun clearStores(keysToKeep: Set<String>) {
        val storedKeys = viewModelStores.keys.toSet()
        val keysToRemove = storedKeys - keysToKeep

        keysToRemove.forEach { key ->
            viewModelStores.remove(key)?.clear()
            println("ViewModelStore cleared for: $key")
        }
    }

    fun getStore(key: String): ViewModelStore {
        return viewModelStores.getOrPut(key) { ViewModelStore() }
    }
}

/**
 * Content wrapper with [ViewModelStoreOwner] provided
 */
@Composable
fun NavigationScopeProvider(
    controller: NavigationScopeController,
    key: Any,
    content: @Composable () -> Unit
) {
    val keyString = key.toString()

    val store = remember(controller, keyString) {
        controller.getStore(keyString)
    }

    val owner = remember(store) {
        object : ViewModelStoreOwner { override val viewModelStore = store }
    }

    CompositionLocalProvider(
        LocalViewModelStoreOwner provides owner,
        content = content
    )
}
