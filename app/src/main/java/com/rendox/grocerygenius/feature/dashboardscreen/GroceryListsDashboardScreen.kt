package com.rendox.grocerygenius.feature.dashboardscreen

import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rendox.grocerygenius.R
import com.rendox.grocerygenius.databinding.ListRecyclerviewBinding
import com.rendox.grocerygenius.database.grocery.CombinedGrocery
import com.rendox.grocerygenius.feature.ReminderReceiver
import com.rendox.grocerygenius.feature.dashboardscreen.recyclerview.DashboardRecyclerViewAdapter
import com.rendox.grocerygenius.model.GroceryList
import com.rendox.grocerygenius.ui.helpers.ObserveUiEvent
import com.rendox.grocerygenius.ui.theme.GroceryGeniusTheme
import com.rendox.grocerygenius.ui.theme.TopAppBarActionsHorizontalPadding
import com.rendox.grocerygenius.ui.theme.TopAppBarSmallHeight
import java.util.Calendar
import android.widget.Toast
import android.app.DatePickerDialog as AndroidDatePickerDialog
import android.app.TimePickerDialog as AndroidTimePickerDialog
@Composable
fun GroceryListsDashboardRoute(
    viewModel: GroceryListsDashboardViewModel = hiltViewModel(),
    navigateToGroceryListScreen: (String) -> Unit,
    navigateToSettingsScreen: () -> Unit
) {
    val screenState by viewModel.groceryListsFlow.collectAsStateWithLifecycle()
    val navigateToGroceryListEvent by viewModel.navigateToGroceryListEvent.collectAsStateWithLifecycle()
    val groceryMap by viewModel.groceryMap.collectAsStateWithLifecycle()

    ObserveUiEvent(navigateToGroceryListEvent) { groceryListId ->
        navigateToGroceryListScreen(groceryListId)
    }

    GroceryListsDashboardScreen(
        groceryLists = screenState,
        groceryMap = groceryMap,
        onIntent = viewModel::onIntent,
        navigateToSettingsScreen = navigateToSettingsScreen,
        navigateToGroceryListScreen = navigateToGroceryListScreen,
        viewModel = viewModel
    )
}

@Composable
fun GroceryListsDashboardScreen(
    groceryLists: List<GroceryList>,
    groceryMap: Map<String, List<CombinedGrocery>>,
    onIntent: (GroceryListsDashboardUiIntent) -> Unit = {},
    navigateToSettingsScreen: () -> Unit = {},
    navigateToGroceryListScreen: (String) -> Unit = {},
    viewModel: GroceryListsDashboardViewModel
) {
    val context = LocalContext.current
    var scrollState by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarSmallHeight)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(
                modifier = Modifier
                    .padding(end = TopAppBarActionsHorizontalPadding)
                    .align(Alignment.CenterEnd),
                onClick = navigateToSettingsScreen
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        ReminderSetter(viewModel)

        AndroidViewBinding(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            factory = { inflater, parent, attachToParent ->
                val binding = ListRecyclerviewBinding.inflate(inflater, parent, attachToParent)
                binding.listRecyclerview.addOnScrollListener(
                    object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)
                            scrollState += dy
                        }
                    }
                )
                binding.listRecyclerview.adapter = DashboardRecyclerViewAdapter(
                    recyclerView = binding.listRecyclerview,
                    groceryRepository = viewModel.groceryRepository, // ✅ this line
                    groceryLists = groceryLists,
                    groceryMap = groceryMap,
                    updateLists = { newValue ->
                        onIntent(GroceryListsDashboardUiIntent.OnUpdateGroceryLists(newValue))
                    },
                    onItemClicked = navigateToGroceryListScreen,
                    onAdderItemClicked = {
                        onIntent(GroceryListsDashboardUiIntent.OnAdderItemClick)
                    }
                )
                binding
            },
            update = {
                val adapter = listRecyclerview.adapter as DashboardRecyclerViewAdapter
                val isInitialUpdate = adapter.groceryLists.isEmpty()
                adapter.updateGroceryLists(groceryLists)

                if (isInitialUpdate) {
                    val layoutManager = listRecyclerview.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(0, -scrollState)
                }
            }
        )
    }
}

@Composable
fun ReminderSetter(viewModel: GroceryListsDashboardViewModel) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var triggerReminderFlow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            triggerReminderFlow = true
            showDatePicker = true
        }) {
            Text("Set Reminder")
        }

        if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Reminder set for: $selectedDate at $selectedTime")
        }
    }

    if (showDatePicker) {
        ShowDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateChange = { year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                selectedDate = "$day/${month + 1}/$year"
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        ShowTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onTimeChange = { hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                selectedTime = "%02d:%02d".format(hour, minute)
                showTimePicker = false

                val millis = calendar.timeInMillis
                if (millis > System.currentTimeMillis()) {
                    viewModel.scheduleReminder(context, millis)
                    Toast.makeText(context, "✅ Reminder set!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "❌ Time must be in the future", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}@Preview
@Composable
private fun GroceryListsDashboardPreview() {
    GroceryGeniusTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GroceryListsDashboardScreen(
                groceryLists = List(20) {
                    GroceryList(id = it.toString(), name = "List $it", numOfGroceries = it)
                },
                groceryMap = emptyMap(),
                viewModel = hiltViewModel()
            )
        }
    }
}

@Composable
fun ShowDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateChange: (year: Int, month: Int, day: Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(Unit) {
        AndroidDatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateChange(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
        onDismissRequest()
    }
}

@Composable
fun ShowTimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(Unit) {
        AndroidTimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeChange(hourOfDay, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
        onDismissRequest()
    }
}