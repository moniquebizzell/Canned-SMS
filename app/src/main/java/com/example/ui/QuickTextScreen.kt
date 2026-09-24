package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CannedTemplate
import com.example.model.TemplateCategory
import com.example.ui.components.AddEditTemplateDialog
import com.example.ui.components.TemplateCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.SuccessGreen
import com.example.util.SmsDispatcher
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTextScreen(
    viewModel: QuickTextViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAddDialogOpen by viewModel.isAddDialogOpen.collectAsStateWithLifecycle()
    val editingTemplate by viewModel.editingTemplate.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var missingSmsAppText by remember { mutableStateOf<String?>(null) }

    // State for the inline Quick Add Input field
    var quickInputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.action?.invoke()
                        }
                    }
                }
                is UiEvent.TemplateDispatched -> {
                    // Feedback handled by system launching SMS app
                }
                is UiEvent.NoSmsAppFound -> {
                    missingSmsAppText = event.text
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("quick_text_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("snackbar_host")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "QuickText",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (uiState.isDrivingMode) {
                                Surface(
                                    color = AmberAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "DRIVE MODE",
                                        color = AmberAccent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "One-tap canned SMS responses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Driving Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDrivingMode() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_driving_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = if (uiState.isDrivingMode) "Disable Drive Mode" else "Enable Drive Mode",
                            tint = if (uiState.isDrivingMode) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Privacy Shield Info
                    IconButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("privacy_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Information",
                            tint = SuccessGreen
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset to Defaults") },
                                onClick = {
                                    showMenu = false
                                    viewModel.resetToDefaults()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About QuickText") },
                                onClick = {
                                    showMenu = false
                                    showPrivacyDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("fab_add_template"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom template"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Adaptive Container for wide / tablet screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // Inline Add Template Input Card (Core Requirement 2: "Include an input field with an 'Add Template' button to let users save new custom strings")
                InlineAddTemplateCard(
                    inputText = quickInputText,
                    onInputTextChange = { quickInputText = it },
                    onAddClick = {
                        if (quickInputText.isNotBlank()) {
                            viewModel.saveTemplate(quickInputText, TemplateCategory.CUSTOM)
                            quickInputText = ""
                        }
                    }
                )

                // Search Bar & Filter Chips
                SearchBarAndFilters(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = viewModel::onCategorySelect
                )

                // Commuter Privacy & Safety Badge
                PrivacySafetyBadge(
                    isDrivingMode = uiState.isDrivingMode,
                    onClickPrivacy = { showPrivacyDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Templates List or Empty State
                if (uiState.templates.isEmpty()) {
                    EmptyTemplatesView(
                        searchQuery = uiState.searchQuery,
                        onClearSearch = { viewModel.onSearchQueryChange("") },
                        onResetDefaults = { viewModel.resetToDefaults() },
                        onAddNew = { viewModel.openAddDialog() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("templates_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.templates,
                            key = { it.id }
                        ) { template ->
                            TemplateCard(
                                template = template,
                                isDrivingMode = uiState.isDrivingMode,
                                onTapSend = { viewModel.dispatchSms(context, template) },
                                onDelete = { viewModel.deleteTemplate(template) },
                                onEdit = { viewModel.openEditDialog(template) },
                                onCopy = { viewModel.copyTemplate(context, template.text) },
                                onTogglePin = { viewModel.togglePin(template) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Template Dialog
    if (isAddDialogOpen) {
        AddEditTemplateDialog(
            initialTemplate = editingTemplate,
            onDismiss = viewModel::closeDialog,
            onSave = { text, category ->
                viewModel.saveTemplate(text, category)
            }
        )
    }

    // Missing SMS App Handler Alert Dialog
    missingSmsAppText?.let { textToCopy ->
        AlertDialog(
            onDismissRequest = { missingSmsAppText = null },
            title = { Text("No Default SMS App Found") },
            text = {
                Text(
                    "Your device does not have a registered default SMS handler for \"smsto:\".\n\nWould you like to copy this response to your clipboard instead?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SmsDispatcher.copyToClipboard(context, textToCopy)
                        missingSmsAppText = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Copied to clipboard!")
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { missingSmsAppText = null },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Privacy & Safe Handoff Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SuccessGreen
                    )
                    Text("100% Privacy Guarantee")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• Zero SMS Permissions Required",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "QuickText does not request dangerous SEND_SMS or READ_SMS permissions. Your messages and contacts remain completely private.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Safe System Handoff",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "When you tap a template, it uses Android's native Intent system (ACTION_SENDTO) to hand off the text to your default messaging app. You maintain full control to review before hitting send.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Driver & Commuter Safety",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Toggle Driving Mode at any time for enlarged typography and high-contrast touch surfaces.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun InlineAddTemplateCard(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("inline_add_container"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("Type a quick canned response...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inline_template_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = { onInputTextChange("") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear input")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddClick,
                    enabled = inputText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_template_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Template", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SearchBarAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: TemplateCategory,
    onCategorySelect: (TemplateCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Filter Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Filter responses...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_filter_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("category_filters_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateCategory.entries.forEach { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category) },
                    label = {
                        Text(
                            text = category.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("filter_chip_${category.name}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun PrivacySafetyBadge(
    isDrivingMode: Boolean,
    onClickPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("privacy_badge"),
        shape = RoundedCornerShape(12.dp),
        color = if (isDrivingMode) AmberAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (isDrivingMode) AmberAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isDrivingMode) Icons.Default.Speed else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (isDrivingMode) AmberAccent else SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isDrivingMode) "Driver Safe Mode • Extra Large Tap Targets" else "Zero Permissions • Safe System Handoff",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                onClick = onClickPrivacy,
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyTemplatesView(
    searchQuery: String,
    onClearSearch: () -> Unit,
    onResetDefaults: () -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("empty_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotBlank()) "No matching responses found" else "No templates yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isNotBlank()) {
                "Try searching for something else or clear your search term."
            } else {
                "Add your frequently sent canned messages or restore the commuter defaults."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (searchQuery.isNotBlank()) {
                Button(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Clear Filter")
                }
            } else {
                Button(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Restore Defaults")
                }

                Button(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Add Template")
                }
            }
        }
    }
}
