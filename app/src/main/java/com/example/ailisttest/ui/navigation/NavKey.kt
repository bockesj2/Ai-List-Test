package com.example.ailisttest.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppNavKey : NavKey {
    @Serializable
    data object Main : AppNavKey

    @Serializable
    data object Configure : AppNavKey

    @Serializable
    data object Display : AppNavKey

    @Serializable
    data object ListScreens : AppNavKey

    @Serializable
    data object GraphicsScreens : AppNavKey

    @Serializable
    data object GraphicsGroups : AppNavKey

    @Serializable
    data object PollingStatus : AppNavKey

    @Serializable
    data object DataTypesConfig : AppNavKey

    @Serializable
    data object ModbusByteOrderConfig : AppNavKey

    @Serializable
    data object Debugging : AppNavKey

    @Serializable
    data class DynamicListScreen(val screenId: Long) : AppNavKey

    @Serializable
    data class DynamicGraphicsScreen(val screenId: Long) : AppNavKey
}
