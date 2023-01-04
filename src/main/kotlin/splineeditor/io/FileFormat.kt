package splineeditor.io

import splineeditor.SpritePaths
import java.io.File

interface FileFormat {
    val name: String
    val extension: String
    fun load(file: File): SpritePaths
    fun save(file: File, paths: SpritePaths)
}
