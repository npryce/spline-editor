@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@file:JvmName("Main")

package splineeditor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.natpryce.editing.current
import com.natpryce.editing.isSaved
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import java.util.prefs.Preferences

val Pico8ScreenSize = Size(128f, 128f)

fun Point2D.toOffset() = Offset(x.toFloat(), y.toFloat())


enum class PathTransform(val description: String, val matrix: Matrix2x2) {
    mirrorX("Mirror ◫", Matrix2x2.flipX),
    mirrorY("Mirror ⊟", Matrix2x2.flipY),
    mirrorTLBR("Mirror ⧅", Matrix2x2.flipX * Matrix2x2.rot90),
    mirrorBLTR("Mirror ⧄", Matrix2x2.flipX * Matrix2x2.rot270),
    rot90("Rotate 90°", Matrix2x2.rot90),
    rot180("Rotate 180°", Matrix2x2.rot180),
    rot270("Rotate 270°", Matrix2x2.rot270),
}

typealias PathTransforms = PersistentSet<PathTransform>

val noTransforms = persistentSetOf<PathTransform>()


@Composable
fun App(onExit: () -> Unit) {
    var editState by remember {
        mutableStateOf(
            newEditState(preferences.getWorkspace().withoutMissingFiles())
        )
    }
    var closeRequested by remember { mutableStateOf(false) }
    
    fun performAction(actionFunction: Action) {
        val oldWorkspace = editState.workspace
        
        editState = editState
            .let(actionFunction)
            .run {
                if (workspace.selection != null && workspace.selection > current.lastIndex) {
                    copy(workspace = workspace.withNoSelection())
                } else {
                    this
                }
            }
        
        if (oldWorkspace != editState.workspace) {
            preferences.putWorkspace(editState.workspace)
        }
    }
    
    Window(
        title = editState.let {
            "Path Editor" +
                (if (it.file != null) " — " + it.file.name else "") +
                (if (it.isSaved()) "" else " (Unsaved)")
        },
        onCloseRequest = {
            if (editState.isSaved()) {
                onExit()
            } else {
                closeRequested = true
            }
        }
    ) {
        MaterialTheme {
            PathEditorMenuBar(
                editState = editState,
                performAction = ::performAction
            )
            
            Column {
                PathEditorToolbar(
                    editState = editState,
                    performAction = ::performAction
                )
                
                Row {
                    VisualPathEditor(
                        paths = editState.current,
                        selection = editState.workspace.selection,
                        transforms = editState.workspace.transforms,
                        snapToGrid = editState.workspace.snapToGrid,
                        hiddenIndices = editState.workspace.hiddenIndices,
                        editable = true,
                        performAction = ::performAction
                    )
                    
                    PathList(
                        paths = editState.current,
                        hiddenIndices = editState.workspace.hiddenIndices,
                        selection = editState.workspace.selection,
                        performAction = ::performAction,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f, fill = true)
                    )
                }
            }
            
            if (closeRequested) {
                AlertDialog(
                    title = { Text("Unsaved changes") },
                    text = { Text("You have not saved your latest changes.") },
                    onDismissRequest = {
                        closeRequested = false
                    },
                    confirmButton = {
                        Button(onClick = onExit) {
                            Text("Discard changes")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { closeRequested = false }) {
                            Text("Continue editing")
                        }
                    }
                )
            }
        }
    }
}



val preferences: Preferences =
    Preferences.userNodeForPackage(::main::class.java)

fun main() {
    application {
        App(
            onExit = {
                preferences.flush()
                exitApplication()
            }
        )
    }
}
