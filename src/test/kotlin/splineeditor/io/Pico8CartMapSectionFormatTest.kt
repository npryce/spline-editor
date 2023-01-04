package splineeditor.io

import splineeditor.SpritePaths
import java.io.File

class Pico8CartMapSectionFormatTest : FileFormatContract() {
    override val format = Pico8CartMapSectionFormat
    
    companion object {
        val cartFileVariants = listOf("with-map", "without-map")
    }
    
    override fun examples(dataDir: File): List<Pair<SpritePaths, File>> {
        return examples
            .flatMap { example ->
                cartFileVariants.map { v -> example to v }
            }
            .map { (example, variant) ->
                val variantFile = File("src/test/resources/example-$variant.p8")
                val tmpFile = File(dataDir, "${this::class.simpleName}-$variant-${example.size}${format.extension}")
                
                variantFile.copyTo(tmpFile, overwrite = true)
                
                example to tmpFile
            }
    }
}
