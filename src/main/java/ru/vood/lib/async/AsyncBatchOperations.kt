package ru.vood.lib.async

import kotlinx.coroutines.*
import ru.vood.lib.async.Either.Companion.left
import ru.vood.lib.async.Either.Companion.right

internal typealias ReprocessCondition = (Exception) -> Boolean

class AsyncBatchOperations<T, R, out AGG> constructor(
    private val batch: Iterable<AsyncValue<T>>,
    private val work: (T) -> R,
    private val resultCombiner: (Map<T, Try<R>>) -> AGG,
    private val job: CompletableJob = SupervisorJob(),
    private val crScope: CoroutineScope = CoroutineScope(Dispatchers.IO + job),
) {

    operator fun invoke(
        doOnFail: (T, Throwable) -> Unit = { _, _ -> },
        doOnSuccess: (T, R) -> Unit = { _, _ -> },
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION,
    ): AGG = run(doOnFail, doOnSuccess, reprocessCondition)

    fun run(
        doOnFail: (T, Throwable) -> Unit = { _, _ -> },
        doOnSuccess: (T, R) -> Unit = { _, _ -> },
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION,
    ): AGG {
        return runBlocking {
            val result = doTask(
                crScope,
                batch.map { t -> AsyncTask(t.value, t.timeout, t.reprocessAttempts) { work(t.value) } },
                reprocessCondition,
                doOnFail,
                doOnSuccess
            )

            val res = result.associate { either ->
                either.fold(
                    { throw IllegalStateException("Async batch not finished.") },
                    { it }
                )
            }
            return@runBlocking resultCombiner(res)
        }
    }

    private suspend fun doTask(
        scope: CoroutineScope,
        asyncTaskList: List<AsyncTask<T, R>>,
        reprocessCondition: ReprocessCondition = DEFAULT_REPROCESS_CONDITION,
        doOnFail: (T, Throwable) -> Unit,
        doOnSuccess: (T, R) -> Unit
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
                        doOnSuccess(task.value, r)
                        val success: Try<R> = Success(r)
                        return@async task.value to success
                    } catch (e: Exception) {
                        doOnFail(task.value, e)
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
        return intermediate + doTask(scope, finishing, reprocessCondition, doOnFail, doOnSuccess)
    }

    companion object {
        val DEFAULT_REPROCESS_CONDITION: ReprocessCondition = { it is TimeoutCancellationException }

        const val DEFAULT_TIMEOUT = 1000L
        const val DEFAULT_REPROCESS_ATTEMPTS = 0

        inline fun <reified T, reified R, reified AGG> asyncBatch(
            batch: Iterable<T>,
            noinline work: (T) -> R,
            noinline resultCombiner: (Map<T, Try<R>>) -> AGG,
            timeout: Long = DEFAULT_TIMEOUT,
            reprocessAttempts: Int = DEFAULT_REPROCESS_ATTEMPTS,
            job: CompletableJob = SupervisorJob(),
            crScope: CoroutineScope = CoroutineScope(Dispatchers.IO + job),
        ): AsyncBatchOperations<T, R, AGG> = AsyncBatchOperations(
            batch = batch.map { AsyncValue(it, timeout, reprocessAttempts) },
            work = work,
            resultCombiner = resultCombiner,
            job = job,
            crScope = crScope,
        )

        inline fun <reified T, reified R> asyncBatch(
            batch: Iterable<T>,
            noinline work: (T) -> R,
            timeout: Long = DEFAULT_TIMEOUT,
            reprocessAttempts: Int = DEFAULT_REPROCESS_ATTEMPTS,
            job: CompletableJob = SupervisorJob(),
            crScope: CoroutineScope = CoroutineScope(Dispatchers.IO + job),
        ): AsyncBatchOperations<T, R, Int> = AsyncBatchOperations(
            batch = batch.map { AsyncValue(it, timeout, reprocessAttempts) },
            work = work,
            resultCombiner = { it.size },
            job = job,
            crScope = crScope,
        )


    }
}