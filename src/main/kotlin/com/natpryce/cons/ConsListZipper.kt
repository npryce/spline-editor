package com.natpryce.cons

data class ConsListZipper<T>(internal val back: ConsList<T>, val current: T, internal val forth: ConsList<T>) {
    fun hasNext(): Boolean =
        forth.isNotEmpty()
    
    fun next(): ConsListZipper<T>? =
        forth.ifNotEmpty { (head, tail) -> ConsListZipper(Cons(current, back), head, tail) }
    
    fun next(n: Int): ConsListZipper<T>? =
        if (n > 0) {
            next()?.next(n - 1)
        }
        else {
            this
        }
    
    fun hasPrev(): Boolean =
        back.isNotEmpty()
    
    fun prev(): ConsListZipper<T>? =
        back.ifNotEmpty { (head, tail) -> ConsListZipper(tail, head, Cons(current, forth)) }
    
    fun remove(): ConsListZipper<T>? =
        forth.ifNotEmpty { (head, tail) -> ConsListZipper(back, head, tail) }
            ?: back.ifNotEmpty { (head, tail) -> ConsListZipper(tail, head, forth) }
    
    fun insertBefore(t: T): ConsListZipper<T> = ConsListZipper(back, t, Cons(current, forth))
    
    fun insertAfter(t: T): ConsListZipper<T> = ConsListZipper(back, current, Cons(t, forth))
    
    fun replaceWith(t: T): ConsListZipper<T> = copy(current = t)
    
    fun toConsList(): ConsList<T> = back.fold(Cons(current, forth), { xs, x -> Cons(x, xs) })
}

fun <T> ConsList<T>.focusHead(): ConsListZipper<T>? =
    this.ifNotEmpty { (head, tail) -> ConsListZipper(Empty, head, tail) }

fun <T> ConsList<T>.focusNth(n: Int): ConsListZipper<T>? =
    focusHead()?.next(n)

fun <T> ConsList<T>.focusElements(): List<ConsListZipper<T>> =
    generateSequence(focusHead(), { it.next() }).toList()

fun <T> ConsListZipper<T>?.toConsList(): ConsList<T> =
    when(this) {
        null -> emptyConsList()
        else -> this.toConsList()
    }
