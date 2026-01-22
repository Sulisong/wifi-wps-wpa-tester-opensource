package sangiorgi.wps.opensource.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the app.
 * Using Kotlin Serialization with Navigation 2.8+ for compile-time safety.
 */
sealed interface NavRoute {

    @Serializable
    data object Scanner : NavRoute

    @Serializable
    data object NetworkDetail : NavRoute

    @Serializable
    data object ConnectionProgress : NavRoute
}
