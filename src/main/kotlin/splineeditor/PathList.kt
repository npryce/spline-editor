@file:OptIn(ExperimentalFoundationApi::class)

package splineeditor

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.Eye
import compose.icons.lineawesomeicons.EyeSlash
import compose.icons.lineawesomeicons.SortDownSolid
import compose.icons.lineawesomeicons.SortUpSolid
import compose.icons.lineawesomeicons.TrashSolid
import splineeditor.Direction.DOWN
import splineeditor.Direction.UP


enum class Direction(val delta: Int) {
    UP(-1),
    DOWN(1)
}

@Composable
fun PathListItem(
    path: SpritePath,
    index: Int,
    maxIndex: Int,
    pathVisible: Boolean,
    performAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        Text(
            text = "${index + 1}",
            textAlign = TextAlign.End,
            modifier = Modifier
                .requiredWidth(64.dp)
                .align(CenterVertically)
                .padding(start = 16.dp, end = 16.dp)
        )
        PathIcon(
            path = path,
            modifier = Modifier
                .size(64.dp)
                .padding(4.dp)
        )
        Column(Modifier.align(CenterVertically)) {
            PathActionButton(
                icon = LineAwesomeIcons.SortUpSolid,
                text = "Move path up",
                enabled = index > 0,
                onClick = { performAction { it.withPathMoved(index, UP) } },
                modifier = Modifier.size(24.dp)
            )
            PathActionButton(
                icon = LineAwesomeIcons.SortDownSolid,
                text = "Move path down",
                enabled = index < maxIndex,
                onClick = { performAction { it.withPathMoved(index, DOWN) } },
                modifier = Modifier.size(24.dp)
            )
        }
        IconToggleButton(
            checked = pathVisible,
            onCheckedChange = { checked ->
                performAction { editState ->
                    editState.withPathVisibility(index = index, visible = checked)
                }
            },
            modifier = Modifier
                .size(48.dp)
                .align(CenterVertically)
        ) {
            if (pathVisible) {
                Icon(LineAwesomeIcons.Eye, "Path visible")
            } else {
                Icon(LineAwesomeIcons.EyeSlash, "Path invisible")
            }
        }
        Spacer(Modifier.weight(1f, fill = true))
        PathActionButton(
            icon = LineAwesomeIcons.TrashSolid,
            text = "Delete path",
            onClick = { performAction { it.withPathRemoved(index) } },
            modifier = Modifier
                .size(48.dp)
                .align(CenterVertically)
        )
    }
}

@Composable
private fun PathActionButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(icon, text)
    }
}

@Composable
@Preview
fun PathList(
    paths: List<SpritePath>,
    hiddenIndices: Set<Int>,
    selection: Int?,
    performAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        val lazyListState = rememberLazyListState()
        
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .selectableGroup()
        ) {
            itemsIndexed(paths) { i, path ->
                PathListItem(
                    path = path,
                    pathVisible = i !in hiddenIndices,
                    index = i,
                    maxIndex = paths.lastIndex,
                    performAction = performAction,
                    modifier = Modifier
                        .background(
                            color = if (i == selection) colors.secondary else Color.Transparent
                        )
                        .selectable(
                            selected = i == selection,
                            onClick = { performAction { it.withPathSelected(i) } }
                        )
                )
            }
        }
        
        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(lazyListState)
        )
    }
}