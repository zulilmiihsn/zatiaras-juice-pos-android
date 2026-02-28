package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.zatiaras.pos.feature.pos.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.AddOn
import com.zatiaras.pos.core.domain.model.IceLevel
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.domain.model.SugarLevel
import com.zatiaras.pos.core.ui.theme.ZatiarasPink

/**
 * Bottom sheet for selecting product options:
 * - Add-ons (ekstra/topping)
 * - Sugar level (for beverages)
 * - Ice level (for beverages)
 * - Notes
 * - Quantity
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductOptionsBottomSheet(
    product: Product,
    availableAddOns: List<AddOn>,
    selectedAddOnIds: Set<String>,
    selectedSugarLevel: SugarLevel,
    selectedIceLevel: IceLevel,
    productNote: String,
    quantity: Int,
    sheetState: SheetState,
    onToggleAddOn: (String) -> Unit,
    onSugarLevelChange: (SugarLevel) -> Unit,
    onIceLevelChange: (IceLevel) -> Unit,
    onNoteChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Calculate total price
    val addOnTotal = availableAddOns.filter { selectedAddOnIds.contains(it.id) }.sumOf { it.price }
    val unitPrice = product.price + addOnTotal
    val totalPrice = unitPrice * quantity
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(horizontal = 20.dp)
        ) {
            // Header with product name and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = com.zatiaras.pos.core.ui.util.CurrencyFormatter.formatCurrency(product.price),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ZatiarasPink
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.pos_close))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Sugar Level Section (only for beverages)
                if (product.supportsSugarIce) {
                    SectionTitle(stringResource(R.string.pos_sugar_level))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SugarLevel.entries.forEach { level ->
                            OptionChip(
                                text = level.label,
                                isSelected = selectedSugarLevel == level,
                                onClick = { onSugarLevelChange(level) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Ice Level Section
                    SectionTitle(stringResource(R.string.pos_ice_level))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IceLevel.entries.forEach { level ->
                            OptionChip(
                                text = level.label,
                                isSelected = selectedIceLevel == level,
                                onClick = { onIceLevelChange(level) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Add-ons Section
                if (availableAddOns.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.pos_addons))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAddOns.forEach { addOn ->
                            AddOnChip(
                                addOn = addOn,
                                isSelected = selectedAddOnIds.contains(addOn.id),
                                onClick = { onToggleAddOn(addOn.id) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Notes Section
                SectionTitle(stringResource(R.string.pos_notes))
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = productNote,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.pos_notes_hint)) },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Quantity Section
                SectionTitle(stringResource(R.string.pos_quantity))
                Spacer(modifier = Modifier.height(8.dp))
                
                QuantitySelector(
                    quantity = quantity,
                    onIncrement = { onQuantityChange(quantity + 1) },
                    onDecrement = { if (quantity > 1) onQuantityChange(quantity - 1) }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Footer with total and confirm button
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total price display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pos_total),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = com.zatiaras.pos.core.ui.util.CurrencyFormatter.formatCurrency(totalPrice),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ZatiarasPink
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm button
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZatiarasPink)
            ) {
                Text(
                    text = stringResource(R.string.pos_add_to_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun OptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) ZatiarasPink else Color.White
    val borderColor = if (isSelected) ZatiarasPink else Color.LightGray
    val textColor = if (isSelected) Color.White else ZatiarasPink
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun AddOnChip(
    addOn: AddOn,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) ZatiarasPink else Color.White
    val borderColor = if (isSelected) ZatiarasPink else Color.LightGray
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val priceColor = if (isSelected) Color.White.copy(alpha = 0.9f) else ZatiarasPink
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = addOn.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "+${com.zatiaras.pos.core.ui.util.CurrencyFormatter.formatCurrency(addOn.price)}",
                style = MaterialTheme.typography.labelSmall,
                color = priceColor
            )
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalIconButton(
            onClick = onDecrement,
            enabled = quantity > 1
        ) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cart_item_decrease))
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        FilledTonalIconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.pos_add))
        }
    }
}
