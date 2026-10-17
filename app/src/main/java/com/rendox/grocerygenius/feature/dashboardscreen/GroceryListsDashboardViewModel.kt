package com.rendox.grocerygenius.feature.dashboardscreen

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rendox.grocerygenius.feature.ReminderReceiver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rendox.grocerygenius.data.grocerylist.GroceryListRepository
import com.rendox.grocerygenius.database.grocery.CombinedGrocery
import com.rendox.grocerygenius.database.grocery.GroceryDao
import com.rendox.grocerygenius.model.GroceryList
import com.rendox.grocerygenius.ui.helpers.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import android.widget.Toast
import com.rendox.grocerygenius.data.grocery.GroceryRepository

@HiltViewModel
class GroceryListsDashboardViewModel @Inject constructor(
    private val groceryListRepository: GroceryListRepository,
    private val groceryDao: GroceryDao,
    val groceryRepository: com.rendox.grocerygenius.data.grocery.GroceryRepository
) : ViewModel() {

    val groceryListsFlow = groceryListRepository.getAllGroceryLists()
        .map { groceryLists ->
            groceryLists
                .sortedBy { it.sortingPriority }
                .map { it.copy(sortingPriority = 0) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _navigateToGroceryListEvent = MutableStateFlow<UiEvent<String>?>(null)
    val navigateToGroceryListEvent = _navigateToGroceryListEvent.asStateFlow()

    private val _groceryMap = MutableStateFlow<Map<String, List<CombinedGrocery>>>(emptyMap())
    val groceryMap = _groceryMap.asStateFlow()

    fun onIntent(intent: GroceryListsDashboardUiIntent) = when (intent) {
        is GroceryListsDashboardUiIntent.OnAdderItemClick -> createNewGroceryList()
        is GroceryListsDashboardUiIntent.OnUpdateGroceryLists -> updateGroceryLists(intent.groceryLists)
    }

    private fun createNewGroceryList() {
        viewModelScope.launch {
            val groceryListId = UUID.randomUUID().toString()
            groceryListRepository.insertGroceryList(
                GroceryList(
                    id = groceryListId,
                    name = ""
                )
            )
            _navigateToGroceryListEvent.update {
                object : UiEvent<String> {
                    override val data = groceryListId
                    override fun onConsumed() {
                        _navigateToGroceryListEvent.update { null }
                    }
                }
            }
        }
    }

    private fun updateGroceryLists(dashboardItems: List<GroceryList>) {
        viewModelScope.launch {
            val groceryLists = dashboardItems.mapIndexed { index, dashboardItem ->
                dashboardItem.copy(sortingPriority = index.toLong())
            }
            groceryListRepository.updateGroceryLists(groceryLists)
        }
    }

    fun scheduleReminder(context: Context, timeInMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }
}