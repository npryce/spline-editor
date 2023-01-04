package splineeditor.io

import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import splineeditor.Point2D
import splineeditor.SpritePath
import splineeditor.SpritePaths
import splineeditor.io.FileFormat
import splineeditor.noSpritePaths
import java.io.File
import java.io.IOException
import java.io.Reader
import kotlin.math.roundToInt

private data class SplitCartFile(
    val prefix: List<String>,
    val mapData: List<String>,
    val suffix: List<String>
)

object Pico8MapFormat : FileFormat {
    override val name: String = "Pico8 Map Data"
    override val extension: String = ".p8.map"
    
    override fun load(file: File): SpritePaths {
        return file.bufferedReader().use {
            it.readPaths()
        }
    }
    
    override fun save(file: File, paths: SpritePaths) {
        file.printWriter().use {
            paths.toByteValues().encodedAsMapDataLines().forEach { line ->
                it.println(line)
            }
        }
    }
}

private val mapSectionHeader = "__map__"

object Pico8CartMapSectionFormat : FileFormat {
    override val name: String = "Map Section of Pico8 Cartridge"
    override val extension: String = ".p8"
    
    override fun load(file: File): SpritePaths {
        return file.readCartFile()
            .run {
                if (mapData.isEmpty()) {
                    noSpritePaths
                } else {
                    mapData.joinToString("").reader().readPaths()
                }
            }
    }
    
    override fun save(file: File, paths: SpritePaths) {
        val fileParts = file.readCartFile()
        val newMapData = paths.toByteValues().encodedAsMapDataLines()
        val newFileLines = fileParts.prefix + newMapData + fileParts.suffix
        
        file.bufferedWriter().use { w ->
            newFileLines.forEach(w::appendLine)
        }
    }
    
    private fun File.readCartFile(): SplitCartFile {
        val allLines = readLines()
        
        val prefix = allLines.takeWhile { it != mapSectionHeader } + mapSectionHeader
        val mapData = allLines.drop(prefix.size).takeWhile { !it.isSectionHeader() }
        val suffix = allLines.drop(prefix.size + mapData.size)
        
        val sections = SplitCartFile(
            prefix = prefix,
            mapData = mapData,
            suffix = suffix
        )
        return sections
    }
    
    private fun String.isSectionHeader(): Boolean =
        startsWith("__") && endsWith("__")
    
    private fun String.isMapHeader(): Boolean = this != mapSectionHeader
}

private const val chunkSize = 128
private const val minPathSize = 2
private const val maxPathSize = 255 + minPathSize - 1

private fun Sequence<Int>.encodedAsMapDataLines() = chunked(chunkSize)
    .toMutableList()
    .apply { padWithZeroes() }
    .toList()
    .map { it.joinToString(transform = { b -> String.format("%02x", b) }, separator = "") }

private fun SpritePaths.toByteValues(): Sequence<Int> =
    asSequence().flatMap { it.toByteValues() } + sequenceOf(0)

fun MutableList<List<Int>>.padWithZeroes() {
    set(lastIndex, last() + zeroes(chunkSize - last().size))
    repeat(32 - size) {
        add(zeroes(chunkSize))
    }
}

private fun zeroes(count: Int) =
    generateSequence { 0 }.take(count).toList()

private fun SpritePath.toByteValues(): Sequence<Int> {
    if (minPathSize > size || size > maxPathSize) {
        error("path size $size out of valid range: min = $minPathSize, max = $maxPathSize")
    }
    
    return sequenceOf(size - minPathSize + 1) + points().asSequence().flatMap { it.toByteValues() }
}

private fun Point2D.toByteValues(): Sequence<Int> =
    sequenceOf(encodeCoordinate(x), encodeCoordinate(y))

private fun encodeCoordinate(c: Double): Int {
    val byteValue = c.roundToInt() + 128
    if (byteValue !in 0x00..0xFF) {
        error("byte value out of range: $byteValue")
    }
    return byteValue
}

private fun decodeCoordinate(byteValue: Int): Double {
    return (byteValue + Byte.MIN_VALUE).toDouble()
}


private fun Reader.readPaths(): SpritePaths {
    return persistentListOf<SpritePath>().mutate { list ->
        while (true) {
            val pathSize = readByteValue() + minPathSize - 1
            if (pathSize < minPathSize) {
                break
            } else {
                list.add(readPath(pathSize))
            }
        }
    }
}

fun Reader.readPath(pathSize: Int): SpritePath {
    return (1..pathSize)
        .map {
            val x = readCoordinate()
            val y = readCoordinate()
            Point2D(x, y)
        }
        .toPersistentList()
        .let(::SpritePath)
}

private fun Reader.readCoordinate(): Double =
    decodeCoordinate(readByteValue())

fun Reader.readByteValue(): Int {
    val higher = readNybbleValue().checkNibble("higher")
    val lower = readNybbleValue().checkNibble("lower")
    return (higher shl 4) or lower
}

private fun Int.checkNibble(name: String): Int {
    if (this !in 0..15) {
        error("$name out of range: $this")
    }
    return this
}

fun Reader.readNybbleValue(): Int {
    while (true) {
        val ci = read()
        when {
            ci == -1 -> {
                throw IOException("expected a hex digit, found end of file")
            }
            '0'.code <= ci && ci <= '9'.code -> {
                return ci - '0'.code
            }
            'a'.code <= ci && ci <= 'f'.code -> {
                return 10 + (ci - 'a'.code)
            }
            !Character.isWhitespace(ci) -> {
                throw IOException("expected a hex digit, found character with code $ci ('${ci.toChar()}')")
            }
        }
    }
}


