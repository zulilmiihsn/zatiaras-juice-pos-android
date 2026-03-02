package com.zatiaras.pos.feature.inventory.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.inventory.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.ProductType
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.components.ZatDialog
import kotlinx.coroutines.delay

@Composable
fun ProductTypeSelector(
    selectedType: ProductType,
    onTypeSelected: (ProductType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.product_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductType.entries.forEach { type ->
                val isSelected = type == selectedType
                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(AppShapes.M)
                        .background(containerColor)
                        .border(1.dp, borderColor, AppShapes.M)
                        .clickable { onTypeSelected(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
        
        // Show info message based on type
        if (selectedType == ProductType.MINUMAN) {
            Text(
                text = stringResource(R.string.product_drink_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOnSelector(
    availableAddOns: List<AddOn>,
    selectedAddOnIds: Set<String>,
    onAddOnToggle: (String) -> Unit,
    onAddNewAddOn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.product_addons_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextButton(
                onClick = onAddNewAddOn,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.product_create_new))
            }
        }
        
        if (availableAddOns.isEmpty()) {
            Text(
                text = stringResource(R.string.inventory_addons_empty_create),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableAddOns.forEach { addOn ->
                    val isSelected = selectedAddOnIds.contains(addOn.id)
                    FilterChip(
                        modifier = Modifier.height(38.dp),
                        selected = isSelected,
                        onClick = { onAddOnToggle(addOn.id) },
                        label = { 
                            Text("${addOn.name} (+${addOn.price})") 
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddNewAddOnDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceValue by remember { mutableStateOf(0L) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    val nameRequiredMessage = stringResource(R.string.validation_name_empty)
    val invalidPriceMessage = stringResource(R.string.validation_price_invalid)
    
    ZatDialog(
        onDismissRequest = onDismiss
    ) { dismiss ->
        fun validateAndSubmit() {
            val nameValid = name.isNotBlank()
            val priceValid = priceValue >= 0
            
            nameError = if (!nameValid) nameRequiredMessage else null
            priceError = if (!priceValid) invalidPriceMessage else null
            
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.inventory_addon_create_title),
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
                    placeholder = { Text(stringResource(R.string.inventory_addon_name_example)) },
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
