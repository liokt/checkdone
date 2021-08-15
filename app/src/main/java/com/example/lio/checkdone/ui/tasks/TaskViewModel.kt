package com.example.lio.checkdone.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.lio.checkdone.data.PreferencesManager
import com.example.lio.checkdone.data.SortOrder
import com.example.lio.checkdone.data.Task
import com.example.lio.checkdone.data.TaskDao
import com.example.lio.checkdone.ui.ADD_TASK_RESULT_OK
import com.example.lio.checkdone.ui.EDIT_TASK_RESULT_OK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TaskViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {

    val searchQuery = state.getLiveData("searchQuery", "")

    val preferencesFlow = preferencesManager.preferencesFlow

    private val taskEventChannel = Channel<TaskEvent>()
    val taskEvent = taskEventChannel.receiveAsFlow()

    private val tasksFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ) { query, filterPreferences ->
        Pair(query, filterPreferences)
    }.flatMapLatest { (query, filterPreferences) ->
        taskDao.getTasks(query, filterPreferences.sortOrder, filterPreferences.hideCompleted)
    }

    fun onSortOrderSelected(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted: Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

    fun onTaskSelected(task: Task) {
        viewModelScope.launch {
            taskEventChannel.send(TaskEvent.NavigateToEditTaskScreen(task))
        }
    }

    fun onTaskCheckedChanged(task: Task, checked: Boolean) {
        viewModelScope.launch {
            taskDao.update(task.copy(completed = checked))
        }
    }

    fun onTaskSwiped(task: Task) {
        viewModelScope.launch {
            taskDao.delete(task)
            taskEventChannel.send(TaskEvent.ShowUndoDeleteTaskMessage(task))
        }
    }

    fun onUndoDeleteClick(task: Task) {
        viewModelScope.launch {
            taskDao.insert(task)
        }
    }

    fun onAddNewTaskClick() {
        viewModelScope.launch { 
            taskEventChannel.send(TaskEvent.NavigateToAddTaskScreen)
        }
    }

    fun onAddEditResult(result: Int) {
            when (result) {
                ADD_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task added")
                EDIT_TASK_RESULT_OK -> showTaskSavedConfirmationMessage("Task updated")
            }
    }

    private fun showTaskSavedConfirmationMessage(msg: String) = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.ShowTaskSavedConfirmationMessage(msg))
    }

    fun onDeleteAllCompletedClick() {
        viewModelScope.launch{
            taskEventChannel.send(TaskEvent.NavigateToDeleteAllCompletedScreen)
        }
    }

    sealed class TaskEvent {
        object NavigateToAddTaskScreen : TaskEvent()
        data class NavigateToEditTaskScreen(val task: Task) : TaskEvent()
        data class ShowUndoDeleteTaskMessage(val task: Task) : TaskEvent()
        data class ShowTaskSavedConfirmationMessage(val msg: String) : TaskEvent()
        object NavigateToDeleteAllCompletedScreen : TaskEvent()
    }

    val tasks = tasksFlow.asLiveData()
}
