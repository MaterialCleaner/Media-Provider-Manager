package me.gm.cleaner.plugin.util

inline fun <reified T> Any.getObjectField(sourceCls: Class<*> = javaClass) =
    sourceCls.declaredFields
        .first { it.type == T::class.java }
        .apply { isAccessible = true }[this] as T
