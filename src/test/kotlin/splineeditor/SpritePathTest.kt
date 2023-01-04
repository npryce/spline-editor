package splineeditor

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private typealias P = Point2D

class SpritePathTest {
    val p = SpritePath(
        P(3.0, 4.0),
        P(5.0, 7.0),
        P(8.0, 11.0),
    )
    
    @Test
    fun `can remove first control point`() {
        assertEquals(
            SpritePath(
                P(5.0, 7.0),
                P(8.0, 11.0)
            ),
            p.withoutControlPoint(0)
        )
    }
    
    @Test
    fun `can remove last control point`() {
        assertEquals(
            SpritePath(
                P(3.0, 4.0),
                P(5.0, 7.0)
            ),
            p.withoutControlPoint(2)
        )
    }
    
    @Test
    fun `can remove middle control point`() {
        assertEquals(
            SpritePath(
                P(3.0, 4.0),
                P(8.0, 11.0)
            ),
            p.withoutControlPoint(1)
        )
    }
}