package com.onthecrow.nomadrates.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal class NavigatorImpl : Navigator {

    private val navigationCommands = Channel<NavigationCommand>(Channel.BUFFERED)
    val commands = navigationCommands.receiveAsFlow()

    override fun navigate(destination: Destination) {
        navigationCommands.trySend(NavigationCommand.To(destination = destination))
    }

    override fun navigateBack() {
        navigationCommands.trySend(NavigationCommand.Back)
    }
}
