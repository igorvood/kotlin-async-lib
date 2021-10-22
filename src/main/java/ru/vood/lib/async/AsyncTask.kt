package ru.vood.lib.async

import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_REPRESS_ATTEMPTS
import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_TIMEOUT

data class AsyncTask<T, R>(
    val value: T,
    val timeOut: Long = DEFAULT_TIMEOUT,
    val attemptsLeft: Int = DEFAULT_REPRESS_ATTEMPTS,
    val fn: (T) -> R
)
