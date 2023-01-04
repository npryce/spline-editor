package splineeditor.io

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import splineeditor.Point2D
import splineeditor.SpritePath
import splineeditor.SpritePaths
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

abstract class FileFormatContract {
    abstract val format: FileFormat
    
    @Test
    fun `extension starts with a period`() {
        assertTrue("extension should start with \".\" but was \"${format.extension}\"") {
            format.extension.startsWith(".")
        }
    }
    
    @TestFactory
    fun `round trips paths`(): List<DynamicTest> {
        val dataDir = File("build/test-data")
        dataDir.mkdirs()
        
        return examples(dataDir)
            .map { (example, tmpFile) ->
                dynamicTest(tmpFile.name) {
                    roundTripViaFile(example, tmpFile)
                }
            }
    }
    
    protected open fun examples(dataDir: File) =
        examples.map { example ->
            example to File(dataDir, "${this::class.simpleName}-${example.size}${format.extension}")
        }
    
    private fun roundTripViaFile(example: SpritePaths, tmpFile: File) {
        format.save(tmpFile, example)
        val loaded = format.load(tmpFile)
        
        assertEquals(example, loaded)
    }
    
    companion object {
        val examples: List<SpritePaths> = listOf(
            persistentListOf(),
            persistentListOf(
                SpritePath(Point2D(43, -14), Point2D(-35, -49), Point2D(62, 46)),
            ),
            persistentListOf(
                SpritePath(Point2D(43, -14), Point2D(-35, -49), Point2D(62, 46)),
                SpritePath(Point2D(25, 0), Point2D(-7, 55), Point2D(25, 34))
            ),
            generateSequence {
                SpritePath(
                    Point2D(43, -14), Point2D(-35, -49), Point2D(62, 46),
                    Point2D(25, 0), Point2D(-7, 55), Point2D(25, 34)
                )
            }.take(64).toPersistentList()
        )
    }
}