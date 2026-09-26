package com.example.ailisttest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTypesConfigScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    val dataTypePlcPreset by viewModel.dataTypePlcPreset.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var plcPresetDropdownExpanded by remember { mutableStateOf(false) }
    val isCustomPreset = dataTypePlcPreset.equals("Custom", ignoreCase = true)
    val plcPresets = listOf("Custom", "Click Plus PLC")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure / Define Data Types") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val canScrollMore by remember { derivedStateOf { scrollState.canScrollForward } }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Define Data Types",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "View and customize the data types definition table. Select 'Click Plus PLC' to fill default values and lock editing, or select 'Custom' to edit fields.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // PLC Preset Selection Card
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PLC Type Preset",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = plcPresetDropdownExpanded,
                                onExpandedChange = { plcPresetDropdownExpanded = !plcPresetDropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = dataTypePlcPreset,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("PLC Preset") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plcPresetDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = plcPresetDropdownExpanded,
                                    onDismissRequest = { plcPresetDropdownExpanded = false }
                                ) {
                                    plcPresets.forEach { presetOption ->
                                        DropdownMenuItem(
                                            text = { Text(presetOption) },
                                            onClick = {
                                                viewModel.setDataTypePlcPreset(presetOption)
                                                plcPresetDropdownExpanded = false
                                                scope.launch {
                                                    if (presetOption.equals("Click Plus PLC", ignoreCase = true)) {
                                                        snackbarHostState.showSnackbar("Filled default values for Click Plus PLC.")
                                                    } else {
                                                        snackbarHostState.showSnackbar("Set preset to Custom. Table is now editable.")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            if (!isCustomPreset) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.resetDataTypesToClickPlusDefaults()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Reset DataTypes to Click Plus PLC defaults.")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset Defaults")
                                }
                            }
                        }
                    }
                }

                // DataTypes Table Card
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DataTypes Table (${dataTypes.size} rows)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCustomPreset) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Editable Mode (Custom)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Locked (Click Plus PLC)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        if (dataTypes.isEmpty()) {
                            Text("No data types found in database.")
                        } else {
                            dataTypes.forEach { dt ->
                                DataTypeRowCard(
                                    dataType = dt,
                                    isEditable = isCustomPreset,
                                    onSaveDataType = { updatedDt ->
                                        viewModel.updateDataType(updatedDt)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Saved changes to DataType '${updatedDt.description}'")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            ScrollMoreDownIndicator(
                canScrollMore = canScrollMore,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTypeRowCard(
    dataType: DataTypes,
    isEditable: Boolean,
    onSaveDataType: (DataTypes) -> Unit
) {
    var description by remember(dataType.id, dataType.description) { mutableStateOf(dataType.description) }
    var shortName by remember(dataType.id, dataType.shortName) { mutableStateOf(dataType.shortName) }
    var dataTypeKind by remember(dataType.id, dataType.dataType) { mutableStateOf(dataType.dataType) }
    var bytes by remember(dataType.id, dataType.bytes) { mutableStateOf(dataType.bytes.toString()) }
    var defaultModbusAddress by remember(dataType.id, dataType.defaultModbusAddress) { mutableStateOf(dataType.defaultModbusAddress.toString()) }
    var isZeroBasedAddressing by remember(dataType.id, dataType.isZeroBasedAddressing) { mutableStateOf(dataType.isZeroBasedAddressing) }
    var hasBits by remember(dataType.id, dataType.hasBits) { mutableStateOf(dataType.hasBits) }

    var kindDropdownExpanded by remember { mutableStateOf(false) }
    val dataTypeKinds = listOf("INT", "UINT", "FLOAT")

    val isChanged = description.trim() != dataType.description ||
            shortName.trim() != dataType.shortName ||
            dataTypeKind.trim() != dataType.dataType ||
            bytes.toIntOrNull() != dataType.bytes ||
            defaultModbusAddress.toLongOrNull() != dataType.defaultModbusAddress ||
            isZeroBasedAddressing != dataType.isZeroBasedAddressing ||
            hasBits != dataType.hasBits

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ID: ${dataType.id} • ${dataType.shortName} (${dataType.description})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (isEditable && isChanged) {
                    Button(
                        onClick = {
                            val updated = dataType.copy(
                                description = description.trim().ifEmpty { dataType.description },
                                shortName = shortName.trim().ifEmpty { dataType.shortName },
                                dataType = dataTypeKind.trim().ifEmpty { dataType.dataType },
                                bytes = bytes.toIntOrNull() ?: dataType.bytes,
                                defaultModbusAddress = defaultModbusAddress.toLongOrNull() ?: dataType.defaultModbusAddress,
                                isZeroBasedAddressing = isZeroBasedAddressing,
                                hasBits = hasBits
                            )
                            onSaveDataType(updated)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Save", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    readOnly = !isEditable,
                    enabled = isEditable,
                    label = { Text("Short Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    readOnly = !isEditable,
                    enabled = isEditable,
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // DataType Kind Dropdown ("INT", "UINT", "FLOAT")
                ExposedDropdownMenuBox(
                    expanded = kindDropdownExpanded && isEditable,
                    onExpandedChange = {
                        if (isEditable) kindDropdownExpanded = !kindDropdownExpanded
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = dataTypeKind,
                        onValueChange = {},
                        readOnly = true,
                        enabled = isEditable,
                        label = { Text("Type") },
                        trailingIcon = {
                            if (isEditable) ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    if (isEditable) {
                        ExposedDropdownMenu(
                            expanded = kindDropdownExpanded,
                            onDismissRequest = { kindDropdownExpanded = false }
                        ) {
                            dataTypeKinds.forEach { kind ->
                                DropdownMenuItem(
                                    text = { Text(kind) },
                                    onClick = {
                                        dataTypeKind = kind
                                        kindDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bytes,
                    onValueChange = { bytes = it.filter { c -> c.isDigit() } },
                    readOnly = !isEditable,
                    enabled = isEditable,
                    label = { Text("Bytes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = defaultModbusAddress,
                    onValueChange = { defaultModbusAddress = it.filter { c -> c.isDigit() } },
                    readOnly = !isEditable,
                    enabled = isEditable,
                    label = { Text("Base Addr") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isZeroBasedAddressing,
                        onCheckedChange = { if (isEditable) isZeroBasedAddressing = it },
                        enabled = isEditable
                    )
                    Text("0-Based Addressing", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasBits,
                        onCheckedChange = { if (isEditable) hasBits = it },
                        enabled = isEditable
                    )
                    Text("Has Bits", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
