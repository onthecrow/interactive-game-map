package com.onthecrow.nomadrates.navigation.di

import com.onthecrow.nomadrates.navigation.NavigationProviderImpl
import com.onthecrow.nomadrates.navigation.NavigationProvider
import com.onthecrow.nomadrates.navigation.Navigator
import com.onthecrow.nomadrates.navigation.NavigatorImpl
import com.onthecrow.nomadrates.navigation.ScreenResultDispatcher
import com.onthecrow.nomadrates.navigation.ScreenResultDispatcherImpl
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val StartDestination = named("StartDestination")

val navigationModule = module {
    single { NavigatorImpl() } binds arrayOf(Navigator::class, NavigatorImpl::class)

    single {
        NavigationProviderImpl(
            navigator = get(),
            startDestination = get(StartDestination),
        )
    } bind NavigationProvider::class

    single { ScreenResultDispatcherImpl() } bind ScreenResultDispatcher::class
}
