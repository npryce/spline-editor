package com.natpryce.cons

/**
 * A singly-linked list
 */
sealed class ConsList<out T> : Iterable<T> {
    override fun iterator(): Iterator<T> =
        ConsListIterator(this)
    
    override fun toString() =
        joinToString(prefix = "[", separator = ", ", postfix = "]")
}

object Empty : ConsList<Nothing>()
data class Cons<out T>(val head: T, val tail: ConsList<T>) : ConsList<T>()

fun ConsList<*>.isEmpty() = this == Empty
fun ConsList<*>.isNotEmpty() = !isEmpty()

fun emptyConsList() = Empty
fun <T> consListOf(element: T) = Cons(element, Empty)
fun <T> consListOf(vararg elements: T) = elements.foldRight(Empty, ::Cons)
fun <T> Iterable<T>.toConsList() =
    reversed().fold<T, ConsList<T>>(Empty) { acc, elt -> Cons(elt, acc) }

fun <T> ConsList<T>.notEmpty(): Cons<T>? = this as? Cons<T>

// A short cut for this.notEmpty()?.let { cons -> ... }
fun <T, U> ConsList<T>.ifNotEmpty(f: (Cons<T>) -> U): U? {
    return when (this) {
        Empty -> null
        is Cons<T> -> f(this)
    }
}

private class ConsListIterator<out T>(private var current: ConsList<T>) : Iterator<T> {
    override fun hasNext() =
        current.isNotEmpty()
    
    override fun next() =
        current.let {
            if (it is Cons<T>) {
                current = it.tail
                it.head
            } else {
                throw NoSuchElementException()
            }
        }
}
