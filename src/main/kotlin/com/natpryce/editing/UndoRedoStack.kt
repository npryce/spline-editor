package com.natpryce.editing

import com.natpryce.cons.Cons
import com.natpryce.cons.ConsListZipper
import com.natpryce.cons.Empty

/**
 * An UndoRedoStack lets you navigate through the history of a value.
 */
typealias UndoRedoStack<T> = ConsListZipper<T>

fun <T> UndoRedoStack(initialState: T) =
    UndoRedoStack(Empty, initialState, Empty)

fun <T> UndoRedoStack<T>.havingDone(nextState: T) =
    copy(Cons(current, back), nextState, Empty)

inline fun <T> UndoRedoStack<T>.mapCurrent(f: (T)->T) =
    this.havingDone(f(current))

fun <T> UndoRedoStack<T>.canUndo() = hasPrev()
fun <T> UndoRedoStack<T>.undone() = prev() ?: this

fun <T> UndoRedoStack<T>.canRedo() = hasNext()
fun <T> UndoRedoStack<T>.redone() = next() ?: this
