package ru.vood.lib.async.dsl

import ru.vood.lib.async.AsyncBatchOperations
import ru.vood.lib.async.AsyncValue
import ru.vood.lib.async.ReprocessCondition
import ru.vood.lib.async.Try

class AsyncBatchOperationsBuilder<T, R, AGG>(
    var b: Iterable<T> = Iterable { TODO("Ф-ция  b должна быть указана") },
    var batch: Iterable<AsyncValue<T>> = Iterable { TODO("Ф-ция  batch должна быть указана") },
    var work: (T) -> R = { TODO("Ф-ция  work должна быть указана") },
    var resultCombiner: (Map<T, Try<R>>) -> AGG = { TODO("Ф-ция resultCombiner должна быть указана") },
    var reprocessCondition: ReprocessCondition = AsyncBatchOperations.DEFAULT_REPROCESS_CONDITION,
    var doOnFail: (T, Throwable) -> Unit = { _, _ -> },
    var doOnSuccess: (T, R) -> Unit = { _, _ -> },
    var allThreadTimeout: Long = AsyncValue.DEFAULT_TIMEOUT
) {


    fun build(): AGG {
        return AsyncBatchOperations(
            resultCombiner = this.resultCombiner,
            batch = this.batch,
            work = this.work,
        ).applyBatchOfValues(
            doOnFail = this.doOnFail,
            doOnSuccess = this.doOnSuccess,
            reprocessCondition = this.reprocessCondition,

        )
    }
}

/*
inline fun <reified T, R, AGG> prepareBatch(
    crossinline body: AsyncBatchOperationsBuilder<T, R, AGG>.() -> Unit
): ReadOnlyProperty<Nothing?, AGG> =
    ReadOnlyProperty { _, _ ->
        val asyncBatchOperationsBuilder = AsyncBatchOperationsBuilder<T, R, AGG>()
        asyncBatchOperationsBuilder.body()
        return@ReadOnlyProperty asyncBatchOperationsBuilder.build()
    }*/
