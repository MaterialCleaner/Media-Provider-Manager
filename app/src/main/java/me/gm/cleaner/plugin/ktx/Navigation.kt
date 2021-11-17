package me.gm.cleaner.plugin.ktx

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination

fun NavController.addOnExitListener(action: (controller: NavController, destination: NavDestination, arguments: Bundle?) -> Unit) =
    addOnDestinationChangedListener(OneShotDestinationChangedListener(this, action))

class OneShotDestinationChangedListener(
    navController: NavController,
    private val action: (controller: NavController, destination: NavDestination, arguments: Bundle?) -> Unit
) : OnDestinationChangedListener {
    private val callerDestination = navController.currentDestination

    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        if (destination != callerDestination) {
            controller.removeOnDestinationChangedListener(this)
            action(controller, destination, arguments)
        }
    }
}
