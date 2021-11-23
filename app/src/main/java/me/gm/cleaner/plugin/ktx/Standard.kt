package me.gm.cleaner.plugin.ktx

inline fun retry(times: Int, action: (Int) -> Unit) {
    for (index in 0 until times) {
        try {
            action(index)
            break
        } catch (e: Throwable) {
        }
    }
}
