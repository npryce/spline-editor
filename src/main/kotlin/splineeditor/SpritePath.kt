package splineeditor

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class Point2D(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
}

operator fun Point2D.minus(that: Point2D): Point2D =
    Point2D(this.x - that.x, this.y - that.y)

operator fun Point2D.plus(that: Point2D): Point2D =
    Point2D(this.x + that.x, this.y + that.y)

operator fun Point2D.div(d: Double): Point2D =
    Point2D(this.x / d, this.y / d)

fun Point2D.round(nearest: Int): Point2D {
    return Point2D(
        ((x.roundToInt() / nearest) * nearest).toDouble(),
        ((y.roundToInt() / nearest) * nearest).toDouble()
    )
}

fun Point2D.distance(): Double =
    sqrt((x * x) + (y * y))


class Matrix2x2(
    val r1c1: Double,
    val r1c2: Double,
    val r2c1: Double,
    val r2c2: Double
) {
    fun transform(p: Point2D): Point2D {
        return Point2D(
            x = r1c1 * p.x + r1c2 * p.y,
            y = r2c1 * p.x + r2c2 * p.y
        )
    }
    
    operator fun times(that: Matrix2x2): Matrix2x2 {
        return Matrix2x2(
            r1c1 = this.r1c1 * that.r1c1 + this.r1c2 * that.r2c1,
            r1c2 = this.r1c1 * that.r1c2 + this.r1c2 * that.r2c2,
            r2c1 = this.r2c1 * that.r1c1 + this.r2c2 * that.r2c1,
            r2c2 = this.r2c1 * that.r1c2 + this.r2c2 * that.r2c2
        )
    }
    
    companion object {
        val identity = Matrix2x2(1.0, 0.0, 0.0, 1.0)
        val rot90 = Matrix2x2(0.0, -1.0, 1.0, 0.0)
        val rot180 = Matrix2x2(-1.0, 0.0, 0.0, -1.0)
        val rot270 = Matrix2x2(0.0, 1.0, -1.0, 0.0)
        val flipX = Matrix2x2(-1.0, 0.0, 0.0, 1.0)
        val flipY = Matrix2x2(1.0, 0.0, 0.0, -1.0)
    }
}

operator fun Matrix2x2.times(p: Point2D) =
    transform(p)

class SpritePath
private constructor(private val controlPoints: PersistentList<Point2D>) {
    constructor(vararg pathPoints: Point2D) : this(pathPoints.asList())
    
    constructor(pathPoints: List<Point2D>) : this(
        buildList {
            val n = pathPoints.size
            
            add(pathPoints[0] - (pathPoints[1] - pathPoints[0]))
            addAll(pathPoints)
            add(pathPoints[n - 1] - (pathPoints[n - 2] - pathPoints[n - 1]))
        }.toPersistentList()
    )
    
    // Size of path.  You can interpolate 0 until size.
    val size get() = this.controlPoints.size - 2
    
    val lastIndex get() = size - 1
    
    val indices get() = 0..lastIndex
    
    val last get() = this[lastIndex]
    
    // Points that define the path and are saved to the file
    operator fun get(i: Int) = controlPoints[i + 1]
    
    fun points() =
        controlPoints.subList(1, controlPoints.size - 1)
    
    fun interp(offset: Double): Point2D = when {
        offset <= 0 ->
            controlPoints[1]
        offset >= size - 1 ->
            controlPoints[controlPoints.size - 2]
        else -> {
            val i = offset.toInt()
            val t = offset - i
            val cpi = i + 1
            
            Point2D(
                x = interpCoord(cpi, t, Point2D::x),
                y = interpCoord(cpi, t, Point2D::y)
            )
        }
    }
    
    private fun interpCoord(cpi: Int, t: Double, coord: Point2D.() -> Double): Double {
        val p1 = controlPoints[cpi - 1].coord()
        val p2 = controlPoints[cpi].coord()
        val p3 = controlPoints[cpi + 1].coord()
        val p4 = controlPoints[cpi + 2].coord()
        
        return (
            (p2 * 2) +
                (-p1 + p3) * t +
                (p1 * 2 - p2 * 5 + p3 * 4 - p4) * t * t +
                (-p1 + p2 * 3 - p3 * 3 + p4) * t * t * t
            ) / 2.0
    }
    
    fun withControlPoint(i: Int, cp: Point2D): SpritePath =
        SpritePath(controlPoints.set(i + 1, cp).smoothEnds())
    
    fun withoutControlPoint(i: Int): SpritePath =
        SpritePath(controlPoints.removeAt(i + 1).smoothEnds())
    
    fun insertControlPoint(i: Int, cp: Point2D): SpritePath =
        SpritePath(controlPoints.add(i + 1, cp).smoothEnds())
    
    fun splitSegment(i: Int): SpritePath =
        insertControlPoint(i + 1, interp(i + 0.5))
    
    fun reversed(): SpritePath =
        SpritePath(points().reversed())
    
    private fun PersistentList<Point2D>.smoothEnds(): PersistentList<Point2D> =
        mutate {
            val n = it.lastIndex
            it[0] = it[1] - (it[2] - it[1])
            it[n] = it[n - 1] - (it[n - 2] - it[n - 1])
        }
    
    override fun hashCode(): Int =
        controlPoints.hashCode()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as SpritePath
        
        if (controlPoints != other.controlPoints) return false
        
        return true
    }
    
    override fun toString(): String =
        controlPoints.joinToString(
            prefix = "[",
            separator = ", ",
            postfix = "]",
            transform = { "(${it.x}, ${it.y})" }
        )
}


fun newSpritePath() =
    SpritePath(
        pathPoints = listOf(
            Point2D(Random.nextDouble(-64.0, 64.0), Random.nextDouble(-64.0, 64.0)),
            Point2D(Random.nextDouble(-64.0, 64.0), Random.nextDouble(-64.0, 64.0)),
            Point2D(Random.nextDouble(-64.0, 64.0), Random.nextDouble(-64.0, 64.0))
        )
    )


fun Matrix2x2.transform(path: SpritePath) =
    SpritePath(path.points().map { p -> this * p })
