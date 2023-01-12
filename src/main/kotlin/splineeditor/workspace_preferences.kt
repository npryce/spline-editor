package splineeditor

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import java.io.File
import java.util.prefs.Preferences


fun Preferences.putWorkspace(workspace: Workspace) {
    put("mirror", workspace.transforms.joinToString(transform = { it.name }, separator = ","))
    
    val recentFilesNode = node("recent-files")
    
    workspace.recentFiles.forEachIndexed { i, file ->
        recentFilesNode.put("$i", file.absolutePath)
    }
}

fun Preferences.getWorkspace(): Workspace {
    val transforms = try {
        get("mirror", "").split(",").map { enumValueOf<PathTransform>(it) }.toPersistentSet()
    } catch (e: Exception) {
        persistentSetOf()
    }
    
    val recentFilesNode = node("recent-files")
    
    val recentFiles =
        (0 until maxRecentFileCount)
            .map { recentFilesNode.get("$it", null) }
            .takeWhile { it != null }
            .map { File(it) }
            .toPersistentList()
    
    return newWorkspace
        .copy(
            transforms = transforms,
            recentFiles = recentFiles
        )
}
