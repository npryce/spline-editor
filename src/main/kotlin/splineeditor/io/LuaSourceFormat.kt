package splineeditor.io

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import kotlinx.collections.immutable.toPersistentList
import splineeditor.Point2D
import splineeditor.SpritePath
import splineeditor.SpritePaths
import splineeditor.io.FileFormat
import java.io.File
import kotlin.math.roundToInt


object LuaSourceFormat : FileFormat {
    override val name: String = "Lua Source Code"
    override val extension: String = ".p8.lua"
    
    override fun load(file: File): SpritePaths {
        return LuaGrammar.parseToEnd(file.readText())
    }
    
    override fun save(file: File, paths: SpritePaths) {
        file.printWriter().use { w ->
            w.println("{")
            paths.forEachIndexed { pathIndex, path: SpritePath ->
                if (pathIndex > 0) {
                    w.println(",")
                }
                
                w.print(" path {")
                path.points().forEachIndexed { pointIndex, p ->
                    if (pointIndex > 0) {
                        w.print(",")
                    }
                    w.print("{x=")
                    w.print(p.x.roundToInt())
                    w.print(",y=")
                    w.print(p.y.roundToInt())
                    w.print("}")
                }
                w.print("}")
            }
            w.println()
            w.println("}")
        }
    }
}


internal object LuaGrammar : Grammar<SpritePaths>() {
    @Suppress("unused")
    private val whitespace by regexToken("\\s+", ignore = true)
    
    private val pathKeyword by literalToken("path")
    private val xKeyword by literalToken("x")
    private val yKeyword by literalToken("y")
    private val equal by literalToken("=")
    private val begin by literalToken("{")
    private val end by literalToken("}")
    private val sep by regexToken("[,;]")
    private val digits by regexToken("-?[0-9]*")
    
    private val int by digits.map { it.text.toInt() }
    
    private val pointParser by (
        begin
            and xKeyword and equal and int and sep
            and yKeyword and equal and int
            and end
        )
        .map { (_, _, _, x, _, _, _, y, _) ->
            Point2D(x, y)
        }
    
    private val pathParser by (pathKeyword and luaListOf(pointParser))
        .map { (_, points) -> SpritePath(points) }
    
    private val pathListParser by luaListOf(pathParser, acceptZero = true)
        .map { it.toPersistentList() }
    
    private inline fun <reified T> luaListOf(parser: Parser<T>, acceptZero: Boolean = false): Parser<List<T>> =
        (begin and separatedTerms(parser, sep, acceptZero) and optional(sep) and end)
            .map { (_, terms, _, _) -> terms }
    
    override val rootParser: Parser<SpritePaths> = pathListParser
}
