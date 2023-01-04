package com.natpryce.cons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

open class ConsListZipperTest {
    @Test
    fun traversing() {
        val xs = consListOf(1,2,3,4,5,6)
        val x = xs.focusHead()?.next()?.next()?.current
        
        assertEquals(actual = x, expected = 3)
    }
    
    @Test
    fun modifying() {
        val xs = consListOf(1,2,3,4,5,6)
        val munged = xs.focusHead()?.next()?.next()?.remove()?.replaceWith(99)?.toConsList()
        
        assertEquals(actual = munged, expected = consListOf(1, 2, 99, 5, 6))
    }
    
    @Test
    fun `remove last shifts focus backwards`() {
        val xs = consListOf(1, 2, 3, 4)
        
        val afterRemoval = xs.focusHead()?.next()?.next()?.next()?.remove() ?: fail("no zipper")
        
        assertEquals(actual = afterRemoval.toConsList(), expected = consListOf(1, 2, 3))
        assertEquals(actual = 3, expected = afterRemoval.current)
    }
    
    @Test
    fun `remove first shifts focus forward`() {
        val xs = consListOf(1, 2, 3, 4)
        
        val afterRemoval = xs.focusHead()?.remove() ?: fail("no zipper")
        
        assertEquals(actual = afterRemoval.toConsList(), expected = consListOf(2, 3, 4))
        assertEquals(actual = 2, expected = afterRemoval.current)
    }
    
    @Test
    fun `remove middle shifts focus forwards`() {
        val xs = consListOf(1, 2, 3, 4)
        
        val afterRemoval = xs.focusHead()?.next()?.remove() ?: fail("no zipper")
        
        assertEquals(actual = afterRemoval.toConsList(), expected = consListOf(1, 3, 4))
        assertEquals(actual = 3, expected = afterRemoval.current)
    }
}