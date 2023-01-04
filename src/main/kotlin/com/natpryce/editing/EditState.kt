package com.natpryce.editing

import java.io.File

data class EditState<Model : Any, Workspace : Any>(
    val history: UndoRedoStack<Model>,
    val editInProgress: Model? = null,
    val lastSavedState: Model,
    val file: File? = null,
    val workspace: Workspace
)

fun <Model : Any, Workspace : Any> EditState(
    initialModel: Model,
    initialWorkspace: Workspace
): EditState<Model, Workspace> =
    EditState(
        history = UndoRedoStack(initialModel),
        workspace = initialWorkspace,
        lastSavedState = initialModel
    )

fun <Model : Any, Workspace : Any> EditState<Model, Workspace>.newFile(initialModel: Model) =
    EditState(initialModel, workspace)

val <T : Any> EditState<T, *>.current
    get() =
        editInProgress ?: history.current

fun <T : Any> EditState<T, *>.isEditing(): Boolean =
    editInProgress != null

fun <T : Any, W : Any> EditState<T, W>.havingStartedEditing(): EditState<T, W> =
    copy(editInProgress = history.current)

fun <T : Any, W : Any> EditState<T, W>.havingAppliedEdit(f: (T) -> T): EditState<T, W> =
    copy(editInProgress = f(editInProgress ?: history.current))

fun <T : Any, W : Any> EditState<T, W>.havingFinishedEditing(): EditState<T, W> =
    if (editInProgress != null) {
        copy(history = history.havingDone(editInProgress), editInProgress = null)
    } else {
        this
    }

fun <T : Any, W : Any> EditState<T, W>.havingCancelledEditing(): EditState<T, W> =
    copy(editInProgress = null)

fun <T : Any, W : Any> EditState<T, W>.havingDone(nextState: T): EditState<T, W> =
    copy(history = history.havingDone(nextState))

inline fun <T : Any, W : Any> EditState<T, W>.mapCurrent(f: (T) -> T): EditState<T, W> =
    havingDone(f(current))

fun <T : Any> EditState<T, *>.canUndo(): Boolean =
    history.canUndo()

fun <T : Any, W : Any> EditState<T, W>.undone(): EditState<T, W> =
    copy(history = history.undone())

fun <T : Any> EditState<T, *>.canRedo(): Boolean =
    history.canRedo()

fun <T : Any, W : Any> EditState<T, W>.redone(): EditState<T, W> =
    copy(history = history.redone())

fun <T : Any> EditState<T, *>.isSaved(): Boolean =
    lastSavedState == history.current

fun <T : Any> EditState<T, *>.isNotSaved(): Boolean = !isSaved()

fun <T : Any, W : Any> EditState<T, W>.saved(): EditState<T, W> =
    copy(lastSavedState = history.current)

fun <T : Any, W : Any> EditState<T, W>.savedTo(file: File): EditState<T, W> =
    copy(lastSavedState = history.current, file = file)

