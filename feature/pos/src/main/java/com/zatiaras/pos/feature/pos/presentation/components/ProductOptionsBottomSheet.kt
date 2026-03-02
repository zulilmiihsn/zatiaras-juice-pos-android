package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Slate100
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.core.ui.theme.Slate600
import com.zatiaras.pos.core.ui.theme.Slate700
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.core.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductOptionsBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (Product, Int, String) -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    
    // Hardcoded options for UI demo - in real app, these would come from Product domain
    var selectedSugarLevel by remember { mutableStateOf("Normal") }
    var selectedIceLevel by remember { mutableStateOf("Normal") }
    val sugarLevels = listOf("0%", "50%", "Normal", "Extra")
    val iceLevels = listOf("No Ice", "Less", "Normal", "Extra")

    val totalPrice = product.price * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        shape = AppShapes.TopPanel,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = Brand500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate600
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Slate200.copy(alpha = 0.6f))
            
            // Options: Sugar Level
            Text(
                text = "Sugar Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sugarLevels.forEach { level ->
                    OptionChip(
                        text = level,
                        isSelected = selectedSugarLevel == level,
                        onClick = { selectedSugarLevel = level }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Options: Ice Level
            Text(
                text = "Ice Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                iceLevels.forEach { level ->
                    OptionChip(
                        text = level,
                        isSelected = selectedIceLevel == level,
                        onClick = { selectedIceLevel = level }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notes
            Text(
                text = stringResource(R.string.pos_notes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add special instructions...", color = Slate500) },
                shape = AppShapes.M,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand500,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quantity & Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Selector
                Surface(
                    shape = AppShapes.L,
                    color = Slate100,
                    modifier = Modifier.height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove, 
                                contentDescription = "Decrease",
                                tint = if (quantity > 1) Slate700 else Slate500
                            )
                        }
                        
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        
                        IconButton(onClick = { quantity++ }) {
                            Icon(
                                imageVector = Icons.Default.Add, 
                                contentDescription = "Increase",
                                tint = Brand500
                            )
                        }
                    }
                }
                
                // Total Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Price",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(totalPrice),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Brand500
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Add to Cart Button
            Button(
                onClick = { 
                    val finalNotes = buildString {
                        if (selectedSugarLevel != "Normal") append("Sugar: $selectedSugarLevel, ")
                        if (selectedIceLevel != "Normal") append("Ice: $selectedIceLevel, ")
                        if (notes.isNotEmpty()) append(notes)
                    }.removeSuffix(", ")
                    
                    onAddToCart(product, quantity, finalNotes) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 24.dp),
                shape = AppShapes.L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand500,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.pos_add_to_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.M,
        color = if (isSelected) Slate50 else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Brand500 else Slate200
        ),
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Brand500 else Slate600
            )
        }
    }
}

