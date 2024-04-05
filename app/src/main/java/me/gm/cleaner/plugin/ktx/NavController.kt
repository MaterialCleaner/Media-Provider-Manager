/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.ktx

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination

private val destinationToListener: MutableMap<NavDestination, OneShotDestinationChangedListener> =
    mutableMapOf()

fun NavController.addOnExitListener(action: (controller: NavController, destination: NavDestination, arguments: Bundle?) -> Unit) {
    val newListener = OneShotDestinationChangedListener(this, action)
    val oldListener = destinationToListener.put(currentDestination!!, newListener)
    if (oldListener != null) {
        removeOnDestinationChangedListener(oldListener)
    }
    addOnDestinationChangedListener(newListener)
}

class OneShotDestinationChangedListener(
    navController: NavController,
    private val action: (controller: NavController, destination: NavDestination, arguments: Bundle?) -> Unit
) : OnDestinationChangedListener {
    private val callerDestination = navController.currentDestination

    override fun onDestinationChanged(
        controller: NavController, destination: NavDestination, arguments: Bundle?
    ) {
        if (destination != callerDestination) {
            destinationToListener.remove(callerDestination)
            controller.removeOnDestinationChangedListener(this)
            action(controller, destination, arguments)
        }
    }
}
