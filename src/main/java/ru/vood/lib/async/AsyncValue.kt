package ru.vood.lib.async

data class AsyncValue<out T>(
    val value: T,
    val timeout: Long = DEFAULT_TIMEOUT,
    val repressAttempts: Int = DEFAULT_REPRESS_ATTEMPTS,
) {
    companion object {
        internal const val DEFAULT_TIMEOUT = 1000L
        internal const val DEFAULT_REPRESS_ATTEMPTS = 0
    }
}
