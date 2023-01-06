package splineeditor

import androidx.compose.ui.window.WindowState
import com.natpryce.editing.EditState
import com.natpryce.editing.current
import com.natpryce.editing.newFile
import com.natpryce.editing.mapCurrent
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.selects.select
import splineeditor.io.format
import java.io.File


data class Workspace(
    val selection: Int? = null,
    val transforms: PathTransforms = noTransforms,
    val recentFiles: PersistentList<File> = persistentListOf(),
    val snapToGrid: Boolean = false,
    val hiddenIndices: PersistentSet<Int> = persistentSetOf()
)

val newWorkspace = Workspace()

fun Workspace.withNoSelection() =
    copy(selection = null)


const val maxRecentFileCount = 10

fun Workspace.withRecentFile(f: File) =
    copy(recentFiles = recentFiles.remove(f).add(0, f).take(maxRecentFileCount).toPersistentList())

fun Workspace.withoutMissingFiles(): Workspace =
    copy(recentFiles = recentFiles
        .filter { it.exists() && !it.isDirectory }
        .toPersistentList())


typealias SpritePaths = PersistentList<SpritePath>
typealias PathEditorState = EditState<SpritePaths, Workspace>
typealias Action = (PathEditorState) -> PathEditorState

val noSpritePaths = persistentListOf<SpritePath>()

fun newEditState(workspace: Workspace = newWorkspace) =
    EditState(noSpritePaths, workspace)

private fun PathEditorState.mapWorkspace(f: (Workspace) -> Workspace) =
    copy(workspace = f(workspace))

private fun PathEditorState.mapSelection(f: (SpritePath) -> SpritePath) =
    when (workspace.selection) {
        null -> this
        else -> mapCurrent { paths ->
            paths.set(workspace.selection, f(paths.get(workspace.selection)))
        }
    }

fun PathEditorState.newFile() =
    newFile(noSpritePaths)

fun <T> PersistentList<T>.moveElement(
    atIndex: Int,
    direction: Direction
) = mutate {
    it.add(atIndex + direction.delta, it.removeAt(atIndex))
}

fun PathEditorState.plusRandomPath(): PathEditorState {
    return mapCurrent { it + newSpritePath() }
        .copy(workspace = workspace.copy(selection = current.size))
}

fun PathEditorState.showingAllPaths(): PathEditorState =
    mapWorkspace { it.copy(hiddenIndices = persistentSetOf()) }

fun PathEditorState.hidingAllPaths(): PathEditorState =
    mapWorkspace { it.copy(hiddenIndices = current.indices.toPersistentSet()) }

fun PathEditorState.withPathVisibility(index: Int, visible: Boolean) =
    mapWorkspace {
        it.copy(
            hiddenIndices = when (visible) {
                true -> it.hiddenIndices - index
                false -> it.hiddenIndices + index
            }
        )
    }

fun PathEditorState.withSnapToGrid(snap: Boolean) =
    mapWorkspace { it.copy(snapToGrid = snap) }

fun PathEditorState.mapTransforms(change: (PathTransforms) -> PathTransforms) =
    withTransforms(change(workspace.transforms))

fun PathEditorState.withTransforms(transforms: PathTransforms) =
    mapWorkspace { it.copy(transforms = transforms) }

fun PathEditorState.withPathSelected(atIndex: Int?) =
    copy(workspace = workspace.copy(selection = atIndex))

fun PathEditorState.withPathMoved(atIndex: Int, direction: Direction) =
    this.mapCurrent { it.moveElement(atIndex, direction) }
        .mapWorkspace { it.copy(selection = atIndex.plus(direction.delta)) }

fun PathEditorState.withPathRemoved(atIndex: Int) =
    mapCurrent { paths -> paths.removeAt(atIndex) }
        .mapWorkspace {
            if (it.selection == null) {
                it
            } else if (it.selection == atIndex) {
                it.withNoSelection()
            } else if (it.selection > atIndex) {
                it.copy(selection = it.selection - 1)
            } else {
                it
            }
        }

fun PathEditorState.withSelectionReversed() =
    mapSelection { it.reversed() }

fun PathEditorState.removeControlPoint(pathIndex: Int, controlPointIndex: Int) =
    mapCurrent { paths ->
        paths.set(pathIndex, paths[pathIndex].withoutControlPoint(controlPointIndex))
    }

