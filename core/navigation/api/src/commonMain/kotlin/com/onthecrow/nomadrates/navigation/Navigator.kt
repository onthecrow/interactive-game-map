package com.onthecrow.nomadrates.navigation

interface Navigator {
    fun navigate(destination: Destination)
    fun navigateBack()
}
