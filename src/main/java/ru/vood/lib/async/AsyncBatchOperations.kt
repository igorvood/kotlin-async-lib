package ru.vood.lib.async

import kotlinx.coroutines.*
import ru.vood.lib.async.Either.Companion.left
import ru.vood.lib.async.Either.Companion.right
import java.util.function.BiConsumer
import java.util.function.Function

typealias ReprocessCondition = (Exception) -> Boolean

class AsyncBatchOperations<T, R, out AGG>(
    private val doOnFail: BiConsumer<in T, Throwable>,
    private val doOnSuccess: BiConsumer<in T, in R>,
    private val resultCombiner: Function<Map<T, Try<R>>, out AGG>
) {
    private val job = SupervisorJob()
    private val crScope = CoroutineScope(Dispatchers.IO + job)

    fun applyBatchOfValues(
        batch: Iterable<AsyncValue<T>>,
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION,
        work: Function<T, R>
    ): AGG {
        return runBlocking {
            val result = doTask(
                crScope,
                batch.map { t -> AsyncTask(t.value, t.timeout, t.repressAttempts) { work.apply(t.value) } },
                reprocessCondition
            )

            val res = result.associate { either ->
                either.fold(
                    { throw IllegalStateException("Async batch not finished.") },
                    { it }
                )
            }
            return@runBlocking resultCombiner.apply(res)
        }

    }

    private suspend fun doTask(
        scope: CoroutineScope,
        asyncTaskList: List<AsyncTask<T, R>>,
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION
    ): List<Either<AsyncTask<T, R>, Pair<T, Try<R>>>> {
        if (asyncTaskList.isEmpty()) {
            return listOf()
        }
        val calc = asyncTaskList
            .map { task ->
                scope.async {
                    try {
                        val r = withTimeout(task.timeOut) {
                            task.fn(task.value)
                        }
                        doOnSuccess.accept(task.value, r)
                        val success: Try<R> = Success(r)
                        return@async task.value to success
                    } catch (e: Exception) {
                        doOnFail.accept(task.value, e)
                        return@async task.value to Failure(e)
                    }
                }
            }
            .awaitAll()
            .zip(asyncTaskList)
            .map { resWithTask ->
                val (valueRes, asyncTask) = resWithTask
                val (value, result) = valueRes
                when (val tryR = result) {
                    is Success -> right(valueRes)
                    is Failure ->
                        if (asyncTask.attemptsLeft <= 0 || !reprocessCondition(tryR.exept))
                            right(value to Failure(tryR.exept))
                        else with(asyncTask) {
                            left(AsyncTask(this.value, this.timeOut, this.attemptsLeft - 1, this.fn))
                        }
                }
            }
        val (intermediate, fin) = calc.partition { it.fold({ false }, { true }) }
        val finishing = fin
            .map { either ->
                either.fold(
                    { it },
                    { throw java.lang.IllegalStateException("async either not partitioned") }
                )
            }
        return intermediate + doTask(scope, finishing, reprocessCondition)
    }

    companion object {
        val DEFAULT_REPROCESS_CONDITION: ReprocessCondition = { it is TimeoutCancellationException }
    }
}