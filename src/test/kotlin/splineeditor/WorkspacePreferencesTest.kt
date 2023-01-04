package splineeditor

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.jupiter.api.Test
import java.io.File
import java.util.StringTokenizer
import java.util.prefs.AbstractPreferences
import kotlin.test.assertEquals

class InMemoryPreferences
private constructor(
    parent: InMemoryPreferences?,
    name: String
) : AbstractPreferences(parent, name) {
    private val properties = mutableMapOf<String, String>()
    private val children = mutableMapOf<String, AbstractPreferences>()
    
    // This is not a generic implementation of Preferences but good enough for the test
    private fun checkKey(key: String) {
        assert('/' !in key) { "key should not contain '/' character, was: $key" }
    }
    
    override fun putSpi(key: String, value: String) {
        checkKey(key)
        properties[key] = value
    }
    
    override fun getSpi(key: String): String? {
        checkKey(key)
        return properties[key]
    }
    
    override fun removeSpi(key: String?) {
        properties.remove(key)
    }
    
    override fun removeNodeSpi() {
        // nothing to do
    }
    
    override fun keysSpi(): Array<String> {
        return properties.keys.toTypedArray()
    }
    
    override fun childrenNamesSpi(): Array<String> {
        return children.keys.toTypedArray()
    }
    
    override fun childSpi(name: String): AbstractPreferences {
        return children.computeIfAbsent(name) { InMemoryPreferences(this, name) }
    }
    
    override fun syncSpi() {
        // nothing to do
    }
    
    override fun flushSpi() {
        // nothing to do
    }
    
    companion object {
        fun newRoot() = InMemoryPreferences(null, "")
    }
}

class WorkspacePreferencesTest {
    val preferences = InMemoryPreferences.newRoot()
    
    @Test
    fun `saves and loads workspace`() {
        val workspace = Workspace(
            selection = 1,
            transforms = persistentSetOf(PathTransform.rot90, PathTransform.mirrorBLTR),
            recentFiles = persistentListOf(File("/example/file/one"), File("/example/file/two"))
        )
        
        preferences.putWorkspace(workspace)
        
        val reloaded = preferences.getWorkspace()
        
        assertEquals(
            workspace.copy(selection = null),  // selection is not saved
            reloaded
        )
    }
}
