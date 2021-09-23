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

package me.gm.cleaner.plugin.util

import androidx.annotation.VisibleForTesting
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.json.JSONObject
import java.lang.reflect.Method

object DevUtils {
    private lateinit var classLoader: ClassLoader

    @VisibleForTesting
    fun init(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    fun log(o: Any) {
        XposedBridge.log(o.toString())
    }

    fun logJSONObject() {
        XposedBridge.hookAllConstructors(JSONObject::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedBridge.log(param.args[0].toString())
            }
        })
    }

    private fun logMethod(method: Method) {
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                XposedBridge.log(method.name)
            }
        })
    }

    fun logMethods(cls: Class<*>) {
        for (method in cls.declaredMethods) {
            logMethod(method)
        }
    }

    fun logMethods(className: String) {
        for (method in XposedHelpers.findClass(className, classLoader).declaredMethods) {
            logMethod(method)
        }
    }

    fun logMethods(className: String, methodName: String) {
        for (method in XposedHelpers.findClass(className, classLoader).declaredMethods) {
            if (method.name == methodName) {
                logMethod(method)
            }
        }
    }

    fun disableMethods(className: String) {
        for (method in XposedHelpers.findClass(className, classLoader).declaredMethods) {
            disableMethod(method)
        }
    }

    fun disableMethods(className: String, methodName: String) {
        for (method in XposedHelpers.findClass(className, classLoader).declaredMethods) {
            if (method.name == methodName) {
                disableMethod(method)
            }
        }
    }

    private fun disableMethod(method: Method) {
        XposedBridge.hookMethod(
            method, XC_MethodReplacement.returnConstant(
                if (method.returnType == Boolean::class.javaPrimitiveType) true else null
            )
        )
    }
}
