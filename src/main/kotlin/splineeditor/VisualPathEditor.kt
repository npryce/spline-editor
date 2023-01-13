@file:OptIn(ExperimentalFoundationApi::class)

package splineeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect.Companion.dashPathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.natpryce.editing.havingAppliedEdit
import com.natpryce.editing.havingFinishedEditing
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList


private const val controlHandleSize = 3f

private data class GrabbablePath(
    val pathIndex: Int,
    val path: SpritePath
)

private data class Grab(
    val screenOffset: Offset,
    val path: SpritePath,
    val pathIndex: Int,
    val controlPointIndex: Int,
    val createNewControlPoint: Boolean = false
)

private fun GrabbablePath.maybeGrab(
    inputOnScreen: Offset,
    inputInModelSpace: Point2D,
    pointIndex: Int,
    controlPointInModelSpace: Point2D,
    createNewControlPoint: Boolean
): Pair<Double, Grab>? {
    val distance = (controlPointInModelSpace - inputInModelSpace).distance()
    return if (distance <= controlHandleSize) {
        distance to Grab(
            path = path,
            pathIndex = pathIndex,
            controlPointIndex = pointIndex,
            screenOffset = inputOnScreen,
            createNewControlPoint = createNewControlPoint
        )
    } else null
}


private fun PersistentList<GrabbablePath>.findGrab(
    screenOffset: Offset,
    modelPoint: Point2D
): Grab? =
    this
        .flatMap { gp ->
            val grabbedControlPoints = gp.path.indices.map { pointIndex ->
                gp.maybeGrab(
                    inputOnScreen = screenOffset,
                    inputInModelSpace = modelPoint,
                    pointIndex = pointIndex,
                    controlPointInModelSpace = gp.path.interp(pointIndex + 0.0),
                    createNewControlPoint = false
                )
            }
            
            val grabbedCreationPoints = (0 until gp.path.lastIndex).map { pointIndex ->
                gp.maybeGrab(
                    inputOnScreen = screenOffset,
                    inputInModelSpace = modelPoint,
                    pointIndex = pointIndex,
                    controlPointInModelSpace = gp.path.interp(pointIndex + 0.5),
                    createNewControlPoint = true
                )
            }
            
            val grabbedPrefixPoint = gp.maybeGrab(
                inputOnScreen = screenOffset,
                inputInModelSpace = modelPoint,
                pointIndex = -1,
                controlPointInModelSpace = gp.path.prefixControlPoint(),
                createNewControlPoint = true
            )
            
            val grabbedSuffixPoint = gp.maybeGrab(
                inputOnScreen = screenOffset,
                inputInModelSpace = modelPoint,
                pointIndex = gp.path.lastIndex,
                controlPointInModelSpace = gp.path.suffixControlPoint(),
                createNewControlPoint = true
            )
            
            grabbedControlPoints + grabbedCreationPoints + grabbedPrefixPoint + grabbedSuffixPoint
        }
        .filterNotNull()
        .minByOrNull { it.first }
        ?.second


private fun Grab.withNewControlPointApplied() =
    if (createNewControlPoint) {
        copy(
            path = when (controlPointIndex) {
                -1 ->
                    path.insertControlPoint(0, path.prefixControlPoint())
                path.lastIndex ->
                    path.insertControlPoint(path.size, path.suffixControlPoint())
                else ->
                    path.splitSegment(controlPointIndex)
            },
            controlPointIndex = controlPointIndex + 1,
            createNewControlPoint = false
        )
    } else {
        this
    }

private fun Grab.withUpdatedOffset(newScreenOffset: Offset, screenToModel: Matrix, snapToGrid: Boolean): Grab =
    copy(
        path = path.withControlPoint(
            controlPointIndex,
            screenToModel.map(newScreenOffset).toPoint2D().round(if (snapToGrid) 8 else 1)
        ),
        screenOffset = newScreenOffset
    )


@Composable
fun VisualPathEditor(
    paths: PersistentList<SpritePath>,
    selection: Int? = null,
    snapToGrid: Boolean = false,
    hiddenIndices: Set<Int>,
    transforms: PathTransforms = noTransforms,
    drawAxes: Boolean = true,
    strokeWidth: Float = 1f,
    editable: Boolean = false,
    performAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenToModel = remember { mutableStateOf(Matrix()) }
    val grabbablePaths = rememberUpdatedState(paths.grabSearchOrder(selection, hiddenIndices))
    val snapToGridState = rememberUpdatedState(snapToGrid)
    
    // Have to define them here because the canvas content is not a @Composable function!
    val gridColor = colors.onSurface.copy(alpha = 0.5f)
    val pathColor = colors.onSurface
    val selectedPathColor = colors.primaryVariant
    val transformedPathColor = colors.secondary
    
    Canvas(
        modifier
            .aspectRatio(1f)
            .fillMaxHeight()
            .background(colors.surface)
            .run {
                if (editable) {
                    pointerInput(Unit) {
                        detectEditingGestures(screenToModel, snapToGridState, grabbablePaths, performAction)
                    }
                } else {
                    this
                }
            }
    ) {
        val scale = size.minDimension / (Pico8ScreenSize.maxDimension * 2)
        
        val modelToScreen = Matrix().apply {
            translate(size.width / 2f, size.height / 2f)
            scale(scale, scale)
        }
        
        screenToModel.value = Matrix().apply {
            setFrom(modelToScreen)
            invert()
        }
        
        withTransform({
            transform(modelToScreen)
        }) {
            drawRect(
                topLeft = -Pico8ScreenSize.center * 2f,
                size = Pico8ScreenSize * 2f,
                color = gridColor,
                style = Stroke()
            )
            
            if (snapToGrid) {
                drawGrid(gridColor)
            }
            
            if (drawAxes) {
                drawAxes(gridColor)
            }
            
            drawRect(
                topLeft = -Pico8ScreenSize.center,
                size = Pico8ScreenSize,
                style = Stroke(
                    pathEffect = dashPathEffect(intervals = floatArrayOf(2f, 2f), phase = 1f)
                ),
                color = gridColor
            )
            
            grabbablePaths.value.asReversed()
                .forEach {
                    when (it.pathIndex) {
                        selection -> {
                            transforms.forEach { t ->
                                drawPath(t.matrix.transform(it.path), transformedPathColor, strokeWidth)
                            }
                            if (editable) {
                                drawControlPoints(it.path, selectedPathColor)
                            } else {
                                drawPath(it.path, pathColor, strokeWidth)
                            }
                        }
                        else -> {
                            drawPath(it.path, pathColor, strokeWidth)
                        }
                    }
                }
        }
    }
}

private fun DrawScope.drawAxes(gridColor: Color) {
    drawLine(
        start = Offset(0f, -Pico8ScreenSize.height),
        end = Offset(0f, Pico8ScreenSize.height),
        color = gridColor,
        pathEffect = dashPathEffect(intervals = floatArrayOf(1f, 1f))
    )
    drawLine(
        start = Offset(-Pico8ScreenSize.width, 0f),
        end = Offset(Pico8ScreenSize.width, 0f),
        color = gridColor,
        pathEffect = dashPathEffect(intervals = floatArrayOf(1f, 1f))
    )
}

private fun DrawScope.drawGrid(gridColor: Color) {
    drawPoints(
        points = (-128..128 step 8).flatMap { y ->
            (-128..128 step 8).map { x ->
                Offset(x.toFloat(), y.toFloat())
            }
        },
        pointMode = PointMode.Points,
        color = gridColor,
        strokeWidth = 0.5f
    )
}

/**
 * An uneditable view of just one path, used to display paths in the PathList
 */
@Composable
fun PathIcon(path: SpritePath, modifier: Modifier = Modifier) {
    VisualPathEditor(
        paths = persistentListOf(path),
        editable = false,
        selection = 0,
        hiddenIndices = persistentSetOf(),
        drawAxes = false,
        strokeWidth = 4f,
        modifier = modifier,
        performAction = {}
    )
}

private suspend fun PointerInputScope.detectEditingGestures(
    screenToModel: MutableState<Matrix>,
    snapToGridState: State<Boolean>,
    grabbablePaths: State<PersistentList<GrabbablePath>>,
    performAction: (Action) -> Unit
) {
    var grab: Grab? = null
    
    fun clearDragState() {
        grab = null
    }
    
    fun PointerKeyboardModifiers.snapToGrid() =
        snapToGridState.value != isShiftPressed
    
    detectPointerGestures(
        onPress = { screenOffset, _ ->
            val modelPoint = screenToModel.value.map(screenOffset).toPoint2D()
            grab = grabbablePaths.value.findGrab(screenOffset, modelPoint)
            performAction { it.withPathSelected(grab?.pathIndex) }
        },
        onDragStart = { screenOffset, keyboardModifiers ->
            grab = grab
                ?.withNewControlPointApplied()
                ?.withUpdatedOffset(screenOffset, screenToModel.value, keyboardModifiers.snapToGrid())
                ?.apply {
                    performAction { editState ->
                        editState.havingAppliedEdit { paths -> paths.set(pathIndex, path) }
                    }
                }
        },
        onDrag = { screenOffset, keyboardModifiers ->
            grab = grab
                ?.withUpdatedOffset(screenOffset, screenToModel.value, keyboardModifiers.snapToGrid())
                ?.apply {
                    performAction { editState ->
                        editState.havingAppliedEdit { paths -> paths.set(pathIndex, path) }
                    }
                }
        },
        onDragEnd = { screenOffset ->
            grab?.apply {
                performAction { editState ->
                    editState
                        .havingAppliedEdit { paths -> paths.set(pathIndex, path) }
                        .havingFinishedEditing()
                }
                clearDragState()
            }
        },
        onDoubleClick = { screenOffset, _ ->
            grab?.apply {
                if (path.size > 2 && !createNewControlPoint) {
                    performAction { editState ->
                        editState.removeControlPoint(pathIndex, controlPointIndex)
                    }
                }
            }
        }
    )
}

private fun Iterable<SpritePath>.grabSearchOrder(
    selection: Int?,
    hiddenIndices: Set<Int>
): PersistentList<GrabbablePath> {
    return mapIndexed(::GrabbablePath).toPersistentList()
        .let { grabbablePaths ->
            if (selection != null) {
                grabbablePaths.removeAt(selection).add(0, grabbablePaths[selection])
            } else {
                grabbablePaths
            }
        }
        .filter { grabbablePath ->
            grabbablePath.pathIndex == selection || grabbablePath.pathIndex !in hiddenIndices
        }
        .toPersistentList()
}

private fun Offset.toPoint2D(): Point2D =
    Point2D(x.toInt(), y.toInt())

private fun DrawScope.drawControlPoints(path: SpritePath, color: Color) {
    drawPath(path, color, 1f)
    
    drawPoints(
        points = path.points().map { it.toOffset() },
        pointMode = PointMode.Points,
        strokeWidth = controlHandleSize,
        cap = StrokeCap.Round,
        color = color
    )
    
    drawOval(
        topLeft = path[0].toOffset() - Offset(2f, 2f),
        size = Size(4f, 4f),
        style = Stroke(),
        color = color
    )
    
    (0..path.size - 2).forEach { p ->
        drawExtensionPoint(path.interp(p + 0.5).toOffset(), color)
    }
    drawExtensionPoint(path.prefixControlPoint().toOffset(), color)
    drawExtensionPoint(path.suffixControlPoint().toOffset(), color)
}

private fun DrawScope.drawExtensionPoint(
    offset: Offset,
    color: Color
) {
    drawRect(
        color = color,
        topLeft = offset - Offset(2f, 2f),
        size = Size(4f, 4f),
        style = Stroke()
    )
    drawLine(
        color = color,
        start = offset - Offset(0f, 2f),
        end = offset + Offset(0f, 2f)
    )
    drawLine(
        color = color,
        start = offset - Offset(2f, 0f),
        end = offset + Offset(2f, 0f)
    )
}

private fun SpritePath.prefixControlPoint() =
    this[0] - (interp(0.5) - this[0])

private fun SpritePath.suffixControlPoint() =
    last + (this.last - interp(size - 1.5))


private fun DrawScope.drawPath(
    p: SpritePath,
    color: Color,
    strokeWidth: Float
) {
    drawPoints(
        points = (0..(p.size - 1) * 16)
            .map { p.interp(it / 16.0) }
            .map { it.toOffset() },
        pointMode = PointMode.Points,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        color = color
    )
}

