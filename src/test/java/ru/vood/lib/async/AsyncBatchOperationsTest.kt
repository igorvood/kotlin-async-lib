package ru.vood.lib.async

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_REPROCESS_CONDITION
import ru.vood.lib.async.AsyncBatchOperations.Companion.asyncBatch

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
                    timeout = 2000,
                    reprocessAttempts = 0
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

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, вторичный конструктор")
    fun testOnAsyncRunOtherConstructor() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()
        val asyncBatchOperations = asyncBatch(
            batch = workList,
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

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, вторичный конструктор")
    fun testOnAsyncRunOther1Constructor() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()
        val asyncBatchOperations = asyncBatch(
            batch = workList,
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

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, вторичный конструктор")
    fun testOnAsyncRunMinimalParams() {
        val threads = mutableSetOf<String>()

        val batch: List<String> = workList
        val asyncBatchOperations = asyncBatch(
            batch = batch,
            work = { s: String -> s.toInt() },
        )()

        Assertions.assertTrue(threads.size > 1)
        Assertions.assertEquals(asyncBatchOperations, workList.size)

    }


}


