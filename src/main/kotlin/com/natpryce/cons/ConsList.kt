package com.natpryce.cons

/**
 * A singly-linked list
 */
sealed class ConsList<out T> : Iterable<T> {
    abstract val head: T?
    abstract val tail: ConsList<T>
    
    override fun iterator(): Iterator<T> =
        ConsListIterator(this)
    
    override fun toString() =
        joinToString(prefix = "[", separator = ", ", postfix = "]")
}

object Empty : ConsList<Nothing>() {
    override val head get() = null
    override val tail get() = this
}

data class Cons<out T>(override val head: T, override val tail: ConsList<T>) :
    ConsList<T>()

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
        (current.head ?: throw NoSuchElementException())
            .also { current = current.tail }
}
