package ru.vood.lib.async

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_REPROCESS_CONDITION
import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_REPROCESS_ATTEMPTS
import ru.vood.lib.async.AsyncValue.Companion.DEFAULT_TIMEOUT

internal class AsyncBatchOperationsTest {

    private val workList = IntRange(1, 100).map { it.toString() }.toList()

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL")
    fun testOnAsyncRun() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()

        val asyncBatchOperations = AsyncBatchOperations(
            batch = workList.map {
                AsyncValue(
                    value = it,
                    timeout = DEFAULT_TIMEOUT,
                    reprocessAttempts = DEFAULT_REPROCESS_ATTEMPTS
                )
            },
            resultCombiner = { it.size },
            work = { s: String -> s.toInt() },
        )
        val applyBatchOfValues = asyncBatchOperations(
            doOnFail = { _, _ -> threadsErr.add(Thread.currentThread().name) },
            doOnSuccess = { _, _ -> threads.add(Thread.currentThread().name) },

            reprocessCondition = DEFAULT_REPROCESS_CONDITION,

            )

        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size == 0)
        Assertions.assertEquals(applyBatchOfValues, workList.size)

    }


}


