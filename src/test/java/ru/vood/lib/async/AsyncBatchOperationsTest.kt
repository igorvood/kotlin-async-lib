package ru.vood.lib.async

import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.vood.lib.async.AsyncBatchOperations.Companion.DEFAULT_REPROCESS_CONDITION
import ru.vood.lib.async.AsyncBatchOperations.Companion.asyncBatch
import java.util.concurrent.atomic.AtomicInteger

internal class AsyncBatchOperationsTest {

    private val workList = IntRange(1, 100).map { it.toString() }.toList()

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, Кастомный комбайнер, с ошибками")
    fun testOnAsyncRun() {
        val workCnt = AtomicInteger(0)
        val workCntSuccess = AtomicInteger(0)
        val workCntError = AtomicInteger(0)
        val threads = mutableSetOf<String>()
        val threadsRun = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()
        val brokenWorkList = workList
            .map { it.toInt() }
            .map { if (it % 3 == 0) it.toString() else it.toString() + "q" }

        val asyncBatchOperations = spyk(
            AsyncBatchOperations(
                batch = brokenWorkList.map {
                    AsyncValue(
                        value = it,
                        timeout = 2000,
                        reprocessAttempts = 0
                    )
                },
                resultCombiner = { m ->
                    m.values
                        .partition {
                            when (it) {
                                is Success -> true
                                is Failure -> false
                            }
                        }
                },
                work = { s: String ->
                    workCnt.incrementAndGet()
                    threadsRun.add(Thread.currentThread().name)
                    s.toInt()
                },
            )
        )
        val applyBatchOfValues = asyncBatchOperations(
            doOnFail = { _, _ ->
                workCntError.incrementAndGet()
                threadsErr.add(Thread.currentThread().name)
            },
            doOnSuccess = { _, _ ->
                workCntSuccess.incrementAndGet()
                threads.add(Thread.currentThread().name)
            },
            reprocessCondition = DEFAULT_REPROCESS_CONDITION,
        )

        Assertions.assertEquals(
            brokenWorkList.size,
            workCnt.get()
        ) { "Число запусков рабочей ф-ции не равно ожидаемому" }
        Assertions.assertEquals(67, workCntError.get()) { "Число запусков обработчика ошибок не равно ожидаемому" }
        Assertions.assertEquals(
            33,
            workCntSuccess.get()
        ) { "Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size > 1)
        Assertions.assertTrue(threadsRun.size > 1)
        val (list, list1) = applyBatchOfValues

        Assertions.assertEquals(
            33,
            list.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertEquals(
            67,
            list1.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика ошибок запусков не равно ожидаемому" }

        verify(exactly = 1) {
            asyncBatchOperations.invoke(any(), any(), DEFAULT_REPROCESS_CONDITION)
            asyncBatchOperations.run(any(), any(), DEFAULT_REPROCESS_CONDITION)
        }

        coVerify(exactly = 1) {
            asyncBatchOperations.doTask(any(), any(), DEFAULT_REPROCESS_CONDITION, any(), any())
        }

        confirmVerified(asyncBatchOperations)
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
    @DisplayName("Тест на асинхронность запуска без DSL")
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
        Assertions.assertEquals(0, applyBatchOfValues)
    }

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, вторичный конструктор")
    fun testOnAsyncRunOther1Constructor1() {
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
        Assertions.assertEquals(0, applyBatchOfValues)
    }

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, вторичный конструктор")
    fun testOnAsyncRunMinimalParams() {
        val threads = mutableSetOf<String>()

        val batch: List<String> = workList
        val asyncBatchOperations = asyncBatch(
            batch = batch,
            work = { s: String ->
                threads.add(Thread.currentThread().name)
                s.toInt()
            },
        )()

        Assertions.assertTrue(threads.size > 1)
        Assertions.assertEquals(workList.size, workList.size)
        Assertions.assertEquals(0, asyncBatchOperations)

    }


}


