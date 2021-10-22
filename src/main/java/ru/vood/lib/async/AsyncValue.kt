package ru.vood.lib.async

data class AsyncValue<out T>(
    val value: T,
    val timeout: Long = DEFAULT_TIMEOUT,
    val reprocessAttempts: Int = DEFAULT_REPROCESS_ATTEMPTS,
) {
    companion object {
        internal const val DEFAULT_TIMEOUT = 1000L
        internal const val DEFAULT_REPROCESS_ATTEMPTS = 0
    }
}
