package ru.vood.lib.async

sealed class Try<T> private constructor() {

    companion object {
        operator fun <T> invoke(func: () -> T): Try<T> =
            try {
                Success(func())
            } catch (exept: Exception) {
                Failure(exept)
            }

        object TrySequence {
            operator fun <T> Try<T>.component1(): T = when (this) {
                is Success -> this.value
                is Failure -> throw this.exception
            }
        }

        fun <T> sequential(func: TrySequence.() -> T): Try<T> = Try { func(TrySequence) }
    }

    abstract fun <R> map(transform: (T) -> R): Try<R>
    abstract fun <R> flatMap(func: (T) -> Try<R>): Try<R>
    abstract fun <R> recover(transform: (Exception) -> R): Try<R>
    abstract fun <R> recoverWith(transform: (Exception) -> Try<R>): Try<R>
}

class Success<T>(val value: T) : Try<T>() {
    override fun <R> map(transform: (T) -> R): Try<R> = Try { transform(value) }

    override fun <R> flatMap(func: (T) -> Try<R>): Try<R> =
        Try { func(value) }.let {
            when (it) {
                is Success -> it.value
                is Failure -> it as Try<R>
            }
        }

    override fun <R> recover(transform: (Exception) -> R): Try<R> = this as Try<R>

    override fun <R> recoverWith(transform: (Exception) -> Try<R>): Try<R> = this as Try<R>

    override fun toString(): String = "Success($value)"
}

class Failure<T>(val exception: Exception) : Try<T>() {
    override fun <R> map(transform: (T) -> R): Try<R> = this as Try<R>
    override fun <R> flatMap(func: (T) -> Try<R>): Try<R> = this as Try<R>

    override fun <R> recover(transform: (Exception) -> R): Try<R> = Try { transform(exception) }

    override fun <R> recoverWith(transform: (Exception) -> Try<R>): Try<R> =
        Try { transform(exception) }.let {
            when (it) {
                is Success -> it.value
                is Failure -> it as Try<R>
            }
        }

    override fun toString(): String = "Failure(${exception.message})"
}
