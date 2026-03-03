package com.zatiaras.pos.feature.inventory.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.inventory.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.Category
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import androidx.compose.foundation.border

@Composable
fun AddOnListContent(
    addOns: List<AddOn>,
    onEditAddOn: (AddOn) -> Unit,
    onDeleteAddOn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var addOnToDelete by remember { mutableStateOf<AddOn?>(null) }

    if (addOns.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.inventory_empty_addons),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.inventory_add_hint_general),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(addOns, key = { it.id }) { addOn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = AppShapes.L,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Add-on info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = addOn.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyFormatter.formatIdr(addOn.price),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onEditAddOn(addOn) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.inventory_action_edit),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { addOnToDelete = addOn },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.inventory_action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Delete confirmation dialog
        addOnToDelete?.let { addOn ->
            DeleteAddOnConfirmDialog(
                addOnName = addOn.name,
                onConfirm = { 
                    onDeleteAddOn(addOn.id)
                    addOnToDelete = null
                },
                onDismiss = { addOnToDelete = null }
            )
        }
    }
}

@Composable
fun AddAddOnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceValue by remember { mutableLongStateOf(0L) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        val nameEmptyStr = stringResource(R.string.validation_name_empty)
        val priceInvalidStr = stringResource(R.string.validation_price_invalid)
        
        fun validateAndSubmit() {
            val nameValid = name.isNotBlank()
            val priceValid = priceValue >= 0
            
            nameError = if (!nameValid) nameEmptyStr else null
            priceError = if (!priceValid) priceInvalidStr else null
            
            if (nameValid && priceValid) {
                onConfirm(name, priceValue)
                dismiss()
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(0.92f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    AppShapes.XXL
                ),
            shape = AppShapes.XXL,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.inventory_add_addon),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.inventory_addon_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    shape = AppShapes.M,
                    modifier = Modifier.fillMaxWidth()
                )
                
                CurrencyTextField(
                    value = priceValue,
                    onValueChange = { 
                        priceValue = it
                        priceError = null
                    },
                    label = { Text(stringResource(R.string.inventory_addon_price)) },
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    showPrefix = true,
                    singleLine = true,
                    shape = AppShapes.M,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = dismiss,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_cancel))
                    }
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_save))
                    }
                }
            }
        }
    }
}

@Composable
fun EditAddOnDialog(
    addOn: AddOn,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf(addOn.name) }
    var priceValue by remember { mutableLongStateOf(addOn.price) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        val nameEmptyStr = stringResource(R.string.validation_name_empty)
        val priceInvalidStr = stringResource(R.string.validation_price_invalid)

        fun validateAndSubmit() {
            val nameValid = name.isNotBlank()
            val priceValid = priceValue >= 0
            
            nameError = if (!nameValid) nameEmptyStr else null
            priceError = if (!priceValid) priceInvalidStr else null
            
            if (nameValid && priceValid) {
                onConfirm(addOn.id, name, priceValue)
                dismiss()
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(0.92f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    AppShapes.XXL
                ),
            shape = AppShapes.XXL,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.inventory_edit_addon),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.inventory_addon_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    shape = AppShapes.M,
                    modifier = Modifier.fillMaxWidth()
                )
                
                CurrencyTextField(
                    value = priceValue,
                    onValueChange = { 
                        priceValue = it
                        priceError = null
                    },
                    label = { Text(stringResource(R.string.inventory_addon_price)) },
                    isError = priceError != null,
                    supportingText = priceError?.let { { Text(it) } },
                    showPrefix = true,
                    singleLine = true,
                    shape = AppShapes.M,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = dismiss,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_cancel))
                    }
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_save))
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAddOnConfirmDialog(
    addOnName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        Card(
            modifier = Modifier.fillMaxWidth(0.9f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    AppShapes.XXL
                ),
            shape = AppShapes.XXL,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.addon_delete_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.addon_delete_confirm_desc, addOnName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = dismiss,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_cancel))
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            dismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = AppShapes.M
                    ) {
                        Text(stringResource(R.string.inventory_action_delete))
                    }
                }
            }
        }
    }
}
