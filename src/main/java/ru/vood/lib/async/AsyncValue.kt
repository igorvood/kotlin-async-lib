package ru.vood.lib.async

import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_REPROCESS_ATTEMPTS
import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_TIMEOUT

data class AsyncValue<out T>(
    val value: T,
    val timeout: Long = DEFAULT_TIMEOUT,
    val reprocessAttempts: Int = DEFAULT_REPROCESS_ATTEMPTS,
)