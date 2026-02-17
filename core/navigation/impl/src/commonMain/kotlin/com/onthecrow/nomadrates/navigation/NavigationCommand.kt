package com.onthecrow.nomadrates.navigation

internal sealed interface NavigationCommand {
    data class To(val destination: Destination) : NavigationCommand
    object Back : NavigationCommand
}
