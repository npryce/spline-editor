package splineeditor.io

import androidx.compose.ui.window.FrameWindowScope
import com.natpryce.editing.UndoRedoStack
import splineeditor.PathEditorState
import splineeditor.afterSavingFile
import splineeditor.io.FileOperation.LOAD
import splineeditor.io.FileOperation.SAVE
import splineeditor.withNoSelection
import splineeditor.withRecentFile
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

fun PathEditorState.loadFile(file: File): PathEditorState {
    val loaded = file.format().load(file)
    
    return copy(
        history = UndoRedoStack(loaded),
        lastSavedState = loaded,
        workspace = workspace.withNoSelection().withRecentFile(file),
        file = file
    )
}

val fileFormats = listOf(
    Pico8CartMapSectionFormat.withBackup(".bak"),
    Pico8MapFormat,
    LuaSourceFormat
)

fun FrameWindowScope.load(currentState: PathEditorState): PathEditorState {
    val file = chooseFile(LOAD, currentState.file)
    
    if (file != null) {
        return currentState.loadFile(file)
    } else {
        return currentState
    }
}

fun FrameWindowScope.save(
    currentState: PathEditorState,
    chooseDifferentFile: Boolean = false
): PathEditorState {
    val file = currentState.file
        ?.takeUnless { chooseDifferentFile }
        ?: chooseFile(SAVE, currentState.file)
    
    return when (file) {
        null -> currentState
        else -> currentState.afterSavingFile(file)
    }
}

fun File.format() =
    fileFormats.find { format -> name.endsWith(format.extension) }
        ?: error("do not recognise extension of file $this")


private enum class FileOperation { LOAD, SAVE }

private fun FrameWindowScope.chooseFile(op: FileOperation, currentFile: File? = null): File? =
    JFileChooser().run {
        currentDirectory = currentFile?.parentFile ?: File(".")
        isAcceptAllFileFilterUsed = false
        fileFormats.forEach { format ->
            addChoosableFileFilter(object : FileFilter() {
                override fun accept(f: File): Boolean = f.name.endsWith(format.extension)
                override fun getDescription(): String = format.name
            })
        }
        
        val result = when (op) {
            LOAD -> {
                dialogTitle = "Load Paths"
                showOpenDialog(window)
            }
            SAVE -> {
                dialogTitle = "Save Paths"
                showSaveDialog(window)
            }
        }
        
        if (result == JFileChooser.APPROVE_OPTION) selectedFile else null
    }

