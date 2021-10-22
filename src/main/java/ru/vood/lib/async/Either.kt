package ru.vood.lib.async

sealed class Either<out L, out R> {
    private data class Left<out L>(val left: L) : Either<L, Nothing>()
    private data class Right<out R>(val right: R) : Either<Nothing, R>()

    fun <T> fold(lt: (L) -> T, rt: (R) -> T): T =
        when (this) {
            is Left -> lt(this.left)
            is Right -> rt(this.right)
        }

    companion object {
        fun <L> left(left: L): Either<L, Nothing> = Left(left)
        fun <R> right(right: R): Either<Nothing, R> = Right(right)
    }
}
