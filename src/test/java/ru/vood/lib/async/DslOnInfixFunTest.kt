package ru.vood.lib.async

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.vood.lib.async.dsl.AsyncBatchOperationsBuilder

class DslOnInfixFunTest {
    private val workList = IntRange(1, 100).map { it.toString() }.toList()

    @Test
    fun testOnAsyncRun1() {
        val threads = mutableSetOf<String>()
        val threadsErr = mutableSetOf<String>()

      /*  val prepareBatch =
            prepareBatch<String, Int, Int>(workList) timeout
                    2000 workFunction
                    { s: String -> s.toInt() } resultCombiner
                    { it.size } run {}*/

//        Assertions.assertTrue(threads.size > 1)
//        Assertions.assertTrue(threadsErr.size == 0)

    }

}

inline fun <reified T, reified R, reified AGG> prepareBatch(batch: Iterable<T>): AsyncBatchOperationsBuilder<T, R, AGG> {
    return AsyncBatchOperationsBuilder(batch)
}

inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.workFunction(noinline work: (T) -> R): AsyncBatchOperationsBuilder<T, R, AGG> {
    this.work = work
    return this
}

inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.resultCombiner(noinline resultCombiner: (Map<T, Try<R>>) -> AGG): AsyncBatchOperationsBuilder<T, R, AGG> {
    this.resultCombiner = resultCombiner
    return this
}

inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.timeout(timeout: Long): AsyncBatchOperationsBuilder<T, R, AGG> {
    this.allThreadTimeout = timeout
    return this
}

inline infix fun <reified T, reified R, reified AGG> AsyncBatchOperationsBuilder<T, R, AGG>.run(b: () -> Unit): AGG =
    this.build()