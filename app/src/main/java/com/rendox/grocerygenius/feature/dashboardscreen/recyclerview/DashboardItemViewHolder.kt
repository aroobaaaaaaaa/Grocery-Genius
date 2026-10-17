package com.rendox.grocerygenius.feature.dashboardscreen.recyclerview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import com.rendox.grocerygenius.R
import com.rendox.grocerygenius.model.GroceryList
import com.rendox.grocerygenius.ui.theme.GroceryGeniusTheme
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.IconButton
import com.rendox.grocerygenius.data.grocery.GroceryRepository
import com.rendox.grocerygenius.database.grocery.CombinedGrocery

class DashboardItemViewHolder(
        private val composeView: ComposeView,
        private val onDrag: (RecyclerView.ViewHolder) -> Unit,
        private val groceryRepository: GroceryRepository,
        private val onViewClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(composeView) {

        fun bind(groceryList: GroceryList) {
            composeView.setContent {
                val context = composeView.context
                val scope = CoroutineScope(Dispatchers.Main)
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    list = groceryList,
                    groceries = emptyList(),
                    onDrag = { onDrag(this) },
                    onClick = { onViewClicked(groceryList.id) },
                    onShareClick = {
                        scope.launch {
                            val groceries = groceryRepository.getCombinedGroceriesFromList(groceryList.id).first()
                            val listText = if (groceries.isNotEmpty()) {
                                groceries.joinToString("\n") { "- ${it.name}" }
                            } else {
                                "No items in this list."
                            }
                            val shareText = """
                            🛒 Grocery List: ${groceryList.name}
                            $listText
                        """.trimIndent()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                )
            }
        }
    }


@Composable
private fun ListItem(
    modifier: Modifier = Modifier,
    list: GroceryList,
    groceries: List<CombinedGrocery>,
    onDrag: () -> Unit = {},
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) = ElevatedCard(modifier = modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(end = 50.dp)) {
            Text(
                modifier = Modifier.padding(bottom = 2.dp),
                text = list.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.dashboard_item_num_of_groceries_title,
                    count = list.numOfGroceries,
                    list.numOfGroceries
                )
            )
        }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share List"
                )
            }

            Icon(
                modifier = Modifier
                    .padding(8.dp)
                    .draggable(
                        state = rememberDraggableState { onDrag() },
                        startDragImmediately = true,
                        orientation = Orientation.Vertical
                    ),
                painter = painterResource(id = R.drawable.baseline_drag_handle_24),
                contentDescription = stringResource(id = R.string.drag_handle)
            )
        }
    }
}

@Composable
@Preview
private fun PreviewListItem() {
    GroceryGeniusTheme {
        Surface {
            ListItem(
                modifier = Modifier.width(400.dp),
                list = GroceryList(
                    id = "1",
                    name = "Sample List",
                    numOfGroceries = 2
                ),
                groceries = listOf(
                    CombinedGrocery(
                        productId = "a",
                        name = "Milk",
                        purchased = false,
                        description = null,
                        iconId = null,
                        iconFilePath = null,
                        categoryId = null,
                        categoryName = null,
                        categorySortingPriority = null,
                        purchasedLastModified = 0L,
                        productIsDefault = false,
                        groceryListId = "1",
                    ),
                    CombinedGrocery(
                        productId = "b",
                        name = "Eggs",
                        purchased = true,
                        description = null,
                        iconId = null,
                        iconFilePath = null,
                        categoryId = null,
                        categoryName = null,
                        categorySortingPriority = null,
                        purchasedLastModified = 0L,
                        productIsDefault = false,
                        groceryListId = "1",
                    )
                )
            )
        }
    }
}