package splineeditor

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PointMatrixTransformTest {
    val p = Point2D(2, 3)
    
    @Test
    fun `flip x`() {
        assertEquals(Point2D(-2, 3), Matrix2x2.flipX * p)
    }
    
    @Test
    fun `flip y`() {
        assertEquals(Point2D(2, -3), Matrix2x2.flipY * p)
    }
    
    @Test
    fun `rotate 90`() {
        assertEquals(Point2D(-3,2), Matrix2x2.rot90 * p)
    }
    
    @Test
    fun `rotate 180`() {
        assertEquals(Point2D(-2,-3), Matrix2x2.rot180 * p)
    }
    
    @Test
    fun `rotate 270`() {
        assertEquals(Point2D(3,-2), Matrix2x2.rot270 * p)
    }
}