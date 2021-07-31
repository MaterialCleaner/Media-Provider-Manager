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

import me.gm.cleaner.plugin.xposed.ManagerService
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.IllegalArgumentException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.HashMap

object ReflectUtils : ManagerService() {
    private val fieldCache: MutableMap<String, Field> = HashMap()
    fun findField(instance: Any, cls: Class<*>): Field? {
        val fullFieldName = instance.javaClass.name + '#' + cls.name
        if (fieldCache.containsKey(fullFieldName)) {
            return fieldCache[fullFieldName]
        }
        try {
            val declaredFields = instance.javaClass.declaredFields
            for (field in declaredFields) {
                field.isAccessible = true
                val objField = field[instance]
                if (objField != null && cls == objField.javaClass) {
                    fieldCache[field.name] = field
                    return field
                }
            }
            for (field in declaredFields) {
                val objField = field[instance]
                if (objField != null && cls.isAssignableFrom(objField.javaClass)) {
                    fieldCache[field.name] = field
                    return field
                }
            }
            throw NoSuchFieldException("$fullFieldName field not found")
        } catch (e: NoSuchFieldException) {
            throw NoSuchFieldError(e.message)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    fun getObjectField(instance: Any, cls: Class<*>): Any {
        return try {
            findField(instance, cls)!![instance]
        } catch (e: IllegalAccessException) {
            XposedBridge.log(e)
            throw IllegalAccessError(e.message)
        }
    }

    fun getObjectField(instance: Any, className: String?): Any {
        return try {
            findField(instance, XposedHelpers.findClass(className, classLoader))!![instance]
        } catch (e: IllegalAccessException) {
            XposedBridge.log(e)
            throw IllegalAccessError(e.message)
        }
    }

    fun setObjectField(instance: Any, cls: Class<*>, value: Any?) {
        try {
            findField(instance, cls)!![instance] = value
        } catch (e: IllegalAccessException) {
            XposedBridge.log(e)
            throw IllegalAccessError(e.message)
        }
    }

    fun setObjectField(instance: Any, className: String?, value: Any?) {
        try {
            findField(instance, XposedHelpers.findClass(className, classLoader))!![instance] = value
        } catch (e: IllegalAccessException) {
            XposedBridge.log(e)
            throw IllegalAccessError(e.message)
        }
    }

    fun findField(instance: Any, cls: Class<*>, handle: Callback) {
        try {
            val declaredFields = instance.javaClass.declaredFields
            for (field in declaredFields) {
                field.isAccessible = true
                val objField = field[instance]
                if (objField != null && cls == objField.javaClass && handle.onFieldFound(objField)) {
                    return
                }
            }
            for (field in declaredFields) {
                val objField = field[instance]
                if (objField != null && cls.isAssignableFrom(objField.javaClass)
                    && handle.onFieldFound(objField)
                ) {
                    return
                }
            }
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    fun handleObjectFields(instance: Any, cls: Class<*>, handle: Callback) {
        findField(instance, cls, handle)
    }

    fun handleObjectFields(instance: Any, className: String?, handle: Callback) {
        findField(instance, XposedHelpers.findClass(className, classLoader), handle)
    }

    fun callMethod(method: Method, instance: Any?, vararg args: Any?): Any {
        return try {
            method.isAccessible = true
            method.invoke(instance, *args)
        } catch (e: IllegalAccessException) {
            XposedBridge.log(e)
            throw IllegalArgumentException(e)
        } catch (e: InvocationTargetException) {
            XposedBridge.log(e)
            throw IllegalArgumentException(e)
        }
    }

    fun callStaticMethod(method: Method, vararg args: Any?): Any {
        return callMethod(method, null, *args)
    }

    interface Callback {
        /**
         * @param objField the value of the represented field in object
         * @return True if no further handling is desired
         */
        fun onFieldFound(objField: Any?): Boolean
    }
}
