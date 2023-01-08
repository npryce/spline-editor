package splineeditor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.FrameWindowScope
import com.natpryce.editing.canRedo
import com.natpryce.editing.canUndo
import com.natpryce.editing.isNotSaved
import com.natpryce.editing.redone
import com.natpryce.editing.undone
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.Clone
import compose.icons.lineawesomeicons.PenSolid
import compose.icons.lineawesomeicons.PencilRulerSolid
import compose.icons.lineawesomeicons.PlusSolid
import compose.icons.lineawesomeicons.RedoSolid
import compose.icons.lineawesomeicons.Save
import compose.icons.lineawesomeicons.UndoSolid
import splineeditor.io.save

@Composable
fun FrameWindowScope.PathEditorToolbar(
    editState: PathEditorState,
    performAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        IconButton(
            enabled = editState.isNotSaved(),
            onClick = { performAction(::save) },
        ) {
            Icon(LineAwesomeIcons.Save, "Save")
        }
        IconButton(
            onClick = { performAction(PathEditorState::plusRandomPath) }
        ) {
            Icon(LineAwesomeIcons.PlusSolid, "Add path")
        }
        IconButton(
            onClick = { performAction(PathEditorState::cloneSelection) }
        ) {
            Icon(LineAwesomeIcons.Clone, "Clone selected path")
        }
        IconButton(
            enabled = editState.canUndo(),
            onClick = { performAction(PathEditorState::undone) }
        ) {
            Icon(LineAwesomeIcons.UndoSolid, "Undo")
        }
        IconButton(
            enabled = editState.canRedo(),
            onClick = { performAction(PathEditorState::redone) }
        ) {
            Icon(LineAwesomeIcons.RedoSolid, "Redo")
        }
        IconToggleButton(
            checked = editState.workspace.snapToGrid,
            onCheckedChange = { checked ->
                performAction { editState -> editState.withSnapToGrid(checked) }
            }
        ) {
            Icon(
                when (editState.workspace.snapToGrid) {
                    true -> LineAwesomeIcons.PencilRulerSolid
                    false -> LineAwesomeIcons.PenSolid
                },
                "Snap to grid"
            )
        }
    }
}


