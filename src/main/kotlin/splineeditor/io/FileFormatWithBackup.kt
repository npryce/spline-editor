package splineeditor.io

import splineeditor.SpritePaths
import splineeditor.io.FileFormat
import java.io.File

class FileFormatWithBackup(val storage: FileFormat, val backupSuffix: String) : FileFormat by storage {
    override fun save(file: File, paths: SpritePaths) {
        file.copyTo(file.resolveSibling(file.name + backupSuffix), overwrite = true)
        storage.save(file, paths)
    }
}

fun FileFormat.withBackup(backupSuffix: String = ".bak") =
    FileFormatWithBackup(this, backupSuffix)
