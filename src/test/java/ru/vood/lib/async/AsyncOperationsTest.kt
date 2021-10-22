package ru.vood.lib.async

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.function.BiConsumer

internal class AsyncOperationsTest {

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL")
    fun testOnAsyncRun() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()
        val workList = IntRange(1, 100).map { it.toString() }.toList()
        AsyncOperations<String, Int, Map<Any, Any>>(
            doOnFail = BiConsumer { _, _ -> threadsErr.add(Thread.currentThread().name) },
            doOnSuccess = { _, _ -> threads.add(Thread.currentThread().name) },
            resultCombiner = { mapOf() }
        ).applyBatchOfValues(
            workList.map { AsyncValue(it) },
            work = { s: String -> s.toInt() }
        )

        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size == 0)
    }
}