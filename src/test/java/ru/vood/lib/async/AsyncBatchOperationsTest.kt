package ru.vood.lib.async

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_REPROCESS_CONDITION
import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_REPROCESS_ATTEMPTS
import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_TIMEOUT
import ru.vood.lib.async.dsl.AsyncBatchOperationsBuilder

internal class AsyncBatchOperationsTest {

    private val workList = IntRange(1, 100).map { it.toString() }.toList()

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL")
    fun testOnAsyncRun() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()

        AsyncBatchOperations<String, Int, Map<Any, Any>>(
            doOnFail = { _, _ -> threadsErr.add(Thread.currentThread().name) },
            doOnSuccess = { _, _ -> threads.add(Thread.currentThread().name) },
            resultCombiner = { mapOf() }
        ).applyBatchOfValues(
            batch = workList.map {
                AsyncValue(
                    value = it,
                    timeout = DEFAULT_TIMEOUT,
                    reprocessAttempts = DEFAULT_REPROCESS_ATTEMPTS
                )
            },
            reprocessCondition = DEFAULT_REPROCESS_CONDITION,
            work = { s: String -> s.toInt() }
        )

        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size == 0)
    }

    @Test
    fun testOnAsyncRun1() {

//        val prepareBatch =
//            prepareBatch<String, Int, Map<Any, Any>>(workList) withTimeout 2 /*reprocessAttempts 3 */ run {}
    }

}

inline fun <reified T, reified R, reified AGG> prepareBatch(batch: Iterable<T>): AsyncBatchOperationsBuilder<T, R, AGG> {
    return AsyncBatchOperationsBuilder(batch)
}

inline fun <reified T, reified R, reified AGG> withFunction(batch: Iterable<T>): AsyncBatchOperationsBuilder<T, R, AGG> {
    return AsyncBatchOperationsBuilder(batch)
}


inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.withTimeout(timeout: Long): AsyncBatchOperationsBuilder<T, R, AGG> {
    this.allThreadTimeout = timeout
    return this
}

inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.run(b: () -> Unit): AGG =
    this.build()

