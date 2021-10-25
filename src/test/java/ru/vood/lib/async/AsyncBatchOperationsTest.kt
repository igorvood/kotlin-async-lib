package ru.vood.lib.async

import io.mockk.coVerify
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
    @DisplayName("Тест на асинхронность запуска без DSL, кастомный комбайнер, с ошибками, без репроцессинга")
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

        val (asyncBatchOperations, applyBatchOfValues) = testCase(
            brokenWorkList,
            workCnt,
            threadsRun,
            workCntError,
            threadsErr,
            workCntSuccess,
            threads
        )

        Assertions.assertEquals(
            brokenWorkList.size,
            workCnt.get()
        ) { "Число запусков рабочей ф-ции не равно ожидаемому" }
        val cntErr = 67
        Assertions.assertEquals(cntErr, workCntError.get()) { "Число запусков обработчика ошибок не равно ожидаемому" }
        val cntOk = 33
        Assertions.assertEquals(
            cntOk,
            workCntSuccess.get()
        ) { "Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size > 1)
        Assertions.assertTrue(threadsRun.size > 1)
        val (list, list1) = applyBatchOfValues

        Assertions.assertEquals(
            cntOk,
            list.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertEquals(
            cntErr,
            list1.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика ошибок запусков не равно ожидаемому" }

        verify(exactly = 1) {
            asyncBatchOperations.invoke(any(), any(), DEFAULT_REPROCESS_CONDITION)
            asyncBatchOperations.run(any(), any(), DEFAULT_REPROCESS_CONDITION)
        }

        coVerify(exactly = 1) {
            asyncBatchOperations.doTask(any(), any(), DEFAULT_REPROCESS_CONDITION, any(), any())
        }
    }

    @Test
    @DisplayName("Тест на асинхронность запуска без DSL, кастомный комбайнер, с ошибками, c репроцессингом")
    fun testOnAsyncRunReprocess() {
        val workCnt = AtomicInteger(0)
        val workCntSuccess = AtomicInteger(0)
        val workCntError = AtomicInteger(0)
        val threads = mutableSetOf<String>()
        val threadsRun = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()
        val brokenWorkList = workList
            .map { it.toInt() }
            .map { if (it % 3 == 0) it.toString() else it.toString() + "q" }

        val (asyncBatchOperations, applyBatchOfValues) = testCase(
            brokenWorkList,
            workCnt,
            threadsRun,
            workCntError,
            threadsErr,
            workCntSuccess,
            threads,
            reprocessAttempts = 2,
            reprocessCondition = { true }
        )
        val cntErr = 67
        val cntOk = 33
        Assertions.assertEquals(
            brokenWorkList.size + cntErr * 2,
            workCnt.get()
        ) { "Число запусков рабочей ф-ции не равно ожидаемому" }
        Assertions.assertEquals(cntErr, workCntError.get()) { "Число запусков обработчика ошибок не равно ожидаемому" }

        Assertions.assertEquals(
            cntOk,
            workCntSuccess.get()
        ) { "Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertTrue(threads.size > 1)
        Assertions.assertTrue(threadsErr.size > 1)
        Assertions.assertTrue(threadsRun.size > 1)
        val (list, list1) = applyBatchOfValues

        Assertions.assertEquals(
            cntOk,
            list.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика успешных запусков не равно ожидаемому" }
        Assertions.assertEquals(
            cntErr,
            list1.size
        ) { "Комбайнер отработл не верно, Число запусков обработчика ошибок запусков не равно ожидаемому" }

        verify(exactly = 1) {
            asyncBatchOperations.invoke(any(), any(), any())
            asyncBatchOperations.run(any(), any(), any())
        }

        coVerify(exactly = 6) {
            asyncBatchOperations.doTask(any(), any(), any(), any(), any())
        }
    }

    @Test
    @DisplayName("Тест, параметры по умолчанияю. не бросает исключение")
    fun testOnAsyncRunWithNoException() {
        val batch = listOf("q1", "q2")
        val asyncBatch = asyncBatch(
            batch = batch,
            work = { it.toInt() },
            exceptionForAtLastOneFail = false
        )()
        Assertions.assertEquals(batch.size, asyncBatch)
    }

    @Test
    @DisplayName("Тест, параметры по умолчанияю. бросает исключение")
    fun testOnAsyncRunWithException() {

        val batch = listOf("q1", "q2")
        val asyncBatch1 = asyncBatch(
            batch = batch,
            work = { it.toInt() },
            exceptionForAtLastOneFail = true
        )
        val assertThrows = Assertions.assertThrows(IllegalStateException::class.java) { asyncBatch1() }

        Assertions.assertEquals("result async run contains ${batch.size} exception of ${batch.size} runs",assertThrows.message)
    }

    private fun testCase(
        workList: List<String>,
        workCnt: AtomicInteger,
        threadsRun: MutableSet<String>,
        workCntError: AtomicInteger,
        threadsErr: MutableSet<String>,
        workCntSuccess: AtomicInteger,
        threads: MutableSet<String>,
        reprocessAttempts: Int = 0,
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION
    ): Pair<AsyncBatchOperations<String, Int, Pair<List<Try<Int>>, List<Try<Int>>>>, Pair<List<Try<Int>>, List<Try<Int>>>> {
        val asyncBatchOperations = spyk(
            AsyncBatchOperations(
                batch = workList.map {
                    AsyncValue(
                        value = it,
                        timeout = 2000,
                        reprocessAttempts = reprocessAttempts
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
            reprocessCondition = reprocessCondition,
        )
        return Pair(asyncBatchOperations, applyBatchOfValues)
    }


}


