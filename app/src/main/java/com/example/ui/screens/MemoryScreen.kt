package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.memory.MemoryCategory
import com.example.memory.MemoryEntry
import com.example.memory.UiState
import com.example.ui.components.HoloGlassCard
import com.example.ui.theme.HoloBorder
import com.example.ui.theme.HoloBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloGlass
import com.example.ui.theme.HoloMint
import com.example.ui.theme.HoloObsidian
import com.example.ui.theme.HoloRose
import com.example.ui.theme.HoloViolet
import com.example.ui.theme.HoloVoid
import com.example.viewmodel.JaniViewModel

@Composable
fun MemoryScreen(
    viewModel: JaniViewModel,
    modifier: Modifier = Modifier
) {
    val memoriesState by viewModel.memoriesState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<MemoryCategory?>(null) }
    var editingMemory by remember { mutableStateOf<MemoryEntry?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HoloGlassCard(
                borderColor = HoloBorderGlow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.memory_vault_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = HoloCyan
                            )
                            val count = (memoriesState as? UiState.Success)?.data?.size ?: 0
                            Text(
                                text = "$count permanent memories synced to cloud",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = HoloCyan
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.memory_search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HoloCyan,
                            unfocusedBorderColor = HoloBorder
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("memory_search_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = HoloCyan
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        MemoryCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = if (selectedCategory == cat) null else cat
                                },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = HoloCyan
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = memoriesState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = HoloCyan)
                        }
                    }
                    is UiState.Error -> {
                        HoloGlassCard(
                            borderColor = HoloRose,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.message,
                                color = HoloRose,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        val filtered = remember(state.data, searchQuery, selectedCategory) {
                            val q = searchQuery.trim().lowercase()
                            state.data.filter { item ->
                                val matchesCat = selectedCategory == null ||
                                    item.category == selectedCategory?.name
                                val matchesQuery = q.isEmpty() ||
                                    item.title.lowercase().contains(q) ||
                                    item.content.lowercase().contains(q)
                                matchesCat && matchesQuery
                            }
                        }

                        if (filtered.isEmpty()) {
                            HoloGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_memory_vault),
                                        contentDescription = "Empty memory vault",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = stringResource(R.string.memory_empty_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = HoloCyan
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.memory_empty_body),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 84.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filtered, key = { it.id }) { entry ->
                                    MemoryCardItem(
                                        entry = entry,
                                        onTogglePin = { viewModel.togglePinMemory(entry) },
                                        onEdit = { editingMemory = entry },
                                        onDelete = { viewModel.deleteMemory(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = HoloCyan,
            contentColor = HoloVoid,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_memory_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.memory_add_button)
            )
        }
    }

    if (showCreateDialog) {
        MemoryEditorDialog(
            initialEntry = null,
            onDismiss = { showCreateDialog = false },
            onSave = { cat, title, content, pinned ->
                viewModel.saveManualMemory(cat, title, content, pinned)
                showCreateDialog = false
            }
        )
    }

    editingMemory?.let { entry ->
        MemoryEditorDialog(
            initialEntry = entry,
            onDismiss = { editingMemory = null },
            onSave = { cat, title, content, pinned ->
                viewModel.updateMemory(entry.id, cat, title, content, pinned)
                editingMemory = null
            }
        )
    }
}

@Composable
private fun MemoryCardItem(
    entry: MemoryEntry,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    HoloGlassCard(
        borderColor = if (entry.pinned) HoloCyan else HoloBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = HoloCyan.copy(alpha = 0.14f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = HoloCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = if (entry.source == "AUTO") {
                            HoloViolet.copy(alpha = 0.18f)
                        } else {
                            HoloMint.copy(alpha = 0.14f)
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (entry.source == "AUTO") "AUTO-LEARNED" else "MANUAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.source == "AUTO") HoloViolet else HoloMint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (entry.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (entry.pinned) "Unpin memory" else "Pin memory",
                            tint = if (entry.pinned) HoloCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit memory",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete memory",
                            tint = HoloRose
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemoryEditorDialog(
    initialEntry: MemoryEntry?,
    onDismiss: () -> Unit,
    onSave: (MemoryCategory, String, String, Boolean) -> Unit
) {
    var category by remember {
        mutableStateOf(MemoryCategory.fromCode(initialEntry?.category))
    }
    var title by remember { mutableStateOf(initialEntry?.title.orEmpty()) }
    var content by remember { mutableStateOf(initialEntry?.content.orEmpty()) }
    var pinned by remember { mutableStateOf(initialEntry?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HoloObsidian,
        title = {
            Text(
                text = if (initialEntry == null) "Store Permanent Memory" else "Edit Memory",
                style = MaterialTheme.typography.titleLarge,
                color = HoloCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemoryCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it.take(2000) },
                    label = { Text("Memory Details") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pin to top of Jani context",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(category, title, content, pinned) },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HoloCyan,
                    contentColor = HoloVoid
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
