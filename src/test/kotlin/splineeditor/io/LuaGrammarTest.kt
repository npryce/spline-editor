package splineeditor.io

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlinx.collections.immutable.persistentListOf
import splineeditor.SpritePath
import kotlin.test.Test
import kotlin.test.assertEquals
import splineeditor.Point2D as P

class LuaGrammarTest {
    @Test
    fun `parses empty`() {
        val s = "{}"
        
        val paths = LuaGrammar.parseToEnd(s)
        assert(paths.isEmpty()) { "expected empty list of paths, was $paths" }
    }
    
    @Test
    fun `skips whitespace`() {
        val s = """
            {
            }
        """.trimIndent()
        
        val paths = LuaGrammar.parseToEnd(s)
        
        assert(paths.isEmpty()) { "expected empty list of paths, was $paths" }
    }
    
    @Test
    fun `parses one path`() {
        val s = """{
            path {{x=43,y=-14},{x=-35,y=-49},{x=62,y=46}}
         }
        """.trimIndent()
        
        assertEquals(
            persistentListOf(
                SpritePath(P(43, -14), P(-35, -49), P(62, 46)),
            ),
            
            LuaGrammar.parseToEnd(s)
        )
    }
    
    @Test
    fun `allows optional final separator`() {
        val s = """{
            path {{x=43,y=-14},{x=-35,y=-49},{x=62,y=46}};
         }
        """.trimIndent()
        
        assertEquals(
            persistentListOf(
                SpritePath(P(43, -14), P(-35, -49), P(62, 46)),
            ),
            
            LuaGrammar.parseToEnd(s)
        )
    }
    
    @Test
    fun `parses two paths`() {
        val s = """{
            path {{x=43,y=-14},{x=-35,y=-49},{x=62,y=46}},
            path {{x=25,y=0},{x=-7,y=55},{x=25,y=34}}
         }
        """.trimIndent()
        
        assertEquals(
            persistentListOf(
                SpritePath(P(43, -14), P(-35, -49), P(62, 46)),
                SpritePath(P(25, 0), P(-7, 55), P(25, 34))
            ),
            
            LuaGrammar.parseToEnd(s)
        )
    }
}
