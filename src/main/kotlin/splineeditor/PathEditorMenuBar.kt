@file:OptIn(ExperimentalComposeUiApi::class)

package splineeditor

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.natpryce.editing.isNotSaved
import com.natpryce.editing.redone
import com.natpryce.editing.undone
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import splineeditor.io.load
import splineeditor.io.loadFile
import splineeditor.io.save

@Composable
fun FrameWindowScope.PathEditorMenuBar(
    editState: PathEditorState,
    performAction: (Action) -> Unit
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {
            Item(
                "New",
                shortcut = KeyShortcut(Key.N, meta = true),
                onClick = {
                    performAction(PathEditorState::newFile)
                }
            )
            Item(
                "Open...",
                shortcut = KeyShortcut(Key.O, meta = true),
                onClick = { performAction { load(it) } }
            )
            Menu(
                "Open Recent",
                enabled = editState.workspace.recentFiles.isNotEmpty(),
            ) {
                editState.workspace.recentFiles.forEach { f ->
                    Item(
                        f.absolutePath,
                        onClick = {
                            performAction { it.loadFile(f) }
                        }
                    )
                }
            }
            Item(
                "Save", shortcut = KeyShortcut(Key.S, meta = true),
                enabled = editState.isNotSaved(),
                onClick = { performAction { save(it) } }
            )
            Item("Save as...", shortcut = KeyShortcut(Key.S, ctrl = true, shift = true),
                onClick = { performAction { save(it, chooseDifferentFile = true) } })
        }
        Menu("Edit", mnemonic = 'E') {
            Item(
                "Undo",
                shortcut = KeyShortcut(Key.Z, meta = true),
                onClick = { performAction(PathEditorState::undone) },
            )
            Item(
                "Redo",
                shortcut = KeyShortcut(Key.Z, meta = true, shift = true),
                onClick = { performAction(PathEditorState::redone) }
            )
            Separator()
            CheckboxItem(
                "Snap to grid",
                checked = editState.workspace.snapToGrid,
                onCheckedChange = { checked ->
                    performAction { it.withSnapToGrid(checked) }
                }
            )
            Separator()
            Item(
                "Add path",
                shortcut = KeyShortcut(Key.A, meta = true),
                onClick = { performAction { it.plusRandomPath() } }
            )
            Item(
                "Clone path",
                enabled = editState.workspace.selection != null,
                shortcut = KeyShortcut(Key.D, meta = true),
                onClick = { performAction { it.cloneSelection() } }
            )
            Item(
                "Reverse path",
                shortcut = KeyShortcut(Key.R, meta = true),
                enabled = editState.workspace.selection != null,
                onClick = {
                    performAction {
                        it.withSelectionReversed()
                    }
                }
            )
        }
        Menu("View", mnemonic = 'V') {
            enumValues<PathTransform>().forEach { t: PathTransform ->
                CheckboxItem(
                    text = t.description,
                    checked = (t in editState.workspace.transforms),
                    onCheckedChange = { checked: Boolean ->
                        performAction {
                            it.mapTransforms { ts -> if (checked) (ts + t) else (ts - t) }
                        }
                    }
                )
            }
            Separator()
            Item(
                text = "Clear all transforms",
                onClick = {
                    performAction { it.withTransforms(noTransforms) }
                }
            )
            Separator()
            Item(
                text = "Hide all paths",
                onClick = {
                    performAction { editState ->
                        editState.hidingAllPaths()
                    }
                }
            )
            Item(
                text = "Show all paths",
                onClick = {
                    performAction { it.showingAllPaths() }
                }
            )
        }
    }
}



