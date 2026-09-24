package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TemplateRepository
import com.example.model.CannedTemplate
import com.example.model.TemplateCategory
import com.example.util.SmsDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val action: (() -> Unit)? = null) : UiEvent
    data class TemplateDispatched(val text: String) : UiEvent
    data class NoSmsAppFound(val text: String) : UiEvent
}

data class QuickTextUiState(
    val templates: List<CannedTemplate> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: TemplateCategory = TemplateCategory.ALL,
    val isDrivingMode: Boolean = false,
    val totalCount: Int = 0
)

class QuickTextViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TemplateRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(TemplateCategory.ALL)
    val selectedCategory: StateFlow<TemplateCategory> = _selectedCategory.asStateFlow()

    private val _isAddDialogOpen = MutableStateFlow(false)
    val isAddDialogOpen: StateFlow<Boolean> = _isAddDialogOpen.asStateFlow()

    private val _editingTemplate = MutableStateFlow<CannedTemplate?>(null)
    val editingTemplate: StateFlow<CannedTemplate?> = _editingTemplate.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    private var recentlyDeleted: CannedTemplate? = null

    val uiState: StateFlow<QuickTextUiState> = combine(
        repository.templatesFlow,
        repository.drivingModeFlow,
        _searchQuery,
        _selectedCategory
    ) { rawTemplates, drivingMode, query, category ->
        val filtered = rawTemplates
            .filter { template ->
                val matchesCategory = category == TemplateCategory.ALL || template.category == category
                val matchesQuery = query.isBlank() || template.text.contains(query, ignoreCase = true)
                matchesCategory && matchesQuery
            }
            .sortedWith(
                compareByDescending<CannedTemplate> { it.isPinned }
                    .thenByDescending { it.usageCount }
                    .thenByDescending { it.createdAt }
            )

        QuickTextUiState(
            templates = filtered,
            searchQuery = query,
            selectedCategory = category,
            isDrivingMode = drivingMode,
            totalCount = rawTemplates.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickTextUiState(templates = CannedTemplate.DEFAULT_TEMPLATES)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: TemplateCategory) {
        _selectedCategory.value = category
    }

    fun openAddDialog() {
        _editingTemplate.value = null
        _isAddDialogOpen.value = true
    }

    fun openEditDialog(template: CannedTemplate) {
        _editingTemplate.value = template
        _isAddDialogOpen.value = true
    }

    fun closeDialog() {
        _isAddDialogOpen.value = false
        _editingTemplate.value = null
    }

    fun toggleDrivingMode() {
        viewModelScope.launch {
            val current = uiState.value.isDrivingMode
            repository.setDrivingMode(!current)
        }
    }

    fun saveTemplate(text: String, category: TemplateCategory) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val currentEditing = _editingTemplate.value
            if (currentEditing != null) {
                repository.updateTemplate(
                    currentEditing.copy(
                        text = text.trim(),
                        category = category
                    )
                )
                _uiEvents.emit(UiEvent.ShowSnackbar("Template updated"))
            } else {
                repository.addTemplate(text, category)
                _uiEvents.emit(UiEvent.ShowSnackbar("Template added"))
            }
            closeDialog()
        }
    }

    fun deleteTemplate(template: CannedTemplate) {
        viewModelScope.launch {
            val removed = repository.deleteTemplate(template.id)
            if (removed != null) {
                recentlyDeleted = removed
                _uiEvents.emit(
                    UiEvent.ShowSnackbar(
                        message = "Template deleted",
                        actionLabel = "UNDO",
                        action = { undoDelete() }
                    )
                )
            }
        }
    }

    fun undoDelete() {
        val toRestore = recentlyDeleted ?: return
        viewModelScope.launch {
            repository.restoreTemplate(toRestore)
            recentlyDeleted = null
            _uiEvents.emit(UiEvent.ShowSnackbar("Template restored"))
        }
    }

    fun togglePin(template: CannedTemplate) {
        viewModelScope.launch {
            repository.togglePin(template.id)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            _uiEvents.emit(UiEvent.ShowSnackbar("Reset to default templates"))
        }
    }

    fun dispatchSms(context: Context, template: CannedTemplate) {
        viewModelScope.launch {
            repository.incrementUsage(template.id)
            val success = SmsDispatcher.launchSms(
                context = context,
                selectedText = template.text,
                onError = {
                    viewModelScope.launch {
                        _uiEvents.emit(UiEvent.NoSmsAppFound(template.text))
                    }
                }
            )
            if (success) {
                _uiEvents.emit(UiEvent.TemplateDispatched(template.text))
            }
        }
    }

    fun copyTemplate(context: Context, text: String) {
        SmsDispatcher.copyToClipboard(context, text)
        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowSnackbar("Copied to clipboard"))
        }
    }
}
