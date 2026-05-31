package com.zatiaras.pos.feature.pos.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.domain.model.Product
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.Brand500
import com.zatiaras.pos.core.ui.theme.Slate100
import com.zatiaras.pos.core.ui.theme.Slate200
import com.zatiaras.pos.core.ui.theme.Slate50
import com.zatiaras.pos.core.ui.theme.Slate500
import com.zatiaras.pos.core.ui.theme.Slate600
import com.zatiaras.pos.core.ui.theme.Slate700
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.pos.R

/**
 * Product customization sheet before adding an item to cart.
 *
 * Local draft state is safe here because it is committed only through
 * onAddToCart; dismissing the sheet drops the draft customizations.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductOptionsBottomSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (Product, Int, String) -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }

    var selectedSugarLevel by remember { mutableStateOf("Normal") }
    var selectedIceLevel by remember { mutableStateOf("Normal") }
    val sugarLevels = listOf("Tanpa", "Sedikit", "Normal")
    val iceLevels = listOf("Tanpa", "Sedikit", "Normal")

    val totalPrice = product.price * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        shape = AppShapes.TopPanel,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = Brand500,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.pos_close),
                        tint = Slate600,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Slate200.copy(alpha = 0.6f))

            Text(
                text = "Level Gula",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sugarLevels.forEach { level ->
                    OptionChip(
                        text = level,
                        isSelected = selectedSugarLevel == level,
                        onClick = { selectedSugarLevel = level },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Level Es",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                iceLevels.forEach { level ->
                    OptionChip(
                        text = level,
                        isSelected = selectedIceLevel == level,
                        onClick = { selectedIceLevel = level },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.pos_notes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.pos_custom_notes_hint), color = Slate500) },
                shape = AppShapes.M,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand500,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = AppShapes.L,
                    color = Slate100,
                    modifier = Modifier.height(50.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.pos_decrease),
                                tint = if (quantity > 1) Slate700 else Slate500,
                            )
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                        IconButton(onClick = { quantity++ }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.pos_increase),
                                tint = Brand500,
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Harga",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600,
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(totalPrice),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Brand500,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Encode non-default sugar/ice choices into notes until the cart
            // model grows typed customization fields.
            Button(
                onClick = {
                    val finalNotes = buildString {
                        if (selectedSugarLevel != "Normal") append("Gula: $selectedSugarLevel, ")
                        if (selectedIceLevel != "Normal") append("Es: $selectedIceLevel, ")
                        if (notes.isNotEmpty()) append(notes)
                    }.removeSuffix(", ")

                    onAddToCart(product, quantity, finalNotes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(bottom = 24.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = AppShapes.L,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Brand500,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = stringResource(R.string.pos_add_to_cart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.M,
        color = if (isSelected) Slate50 else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Brand500 else Slate200,
        ),
        modifier = Modifier.height(40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Brand500 else Slate600,
            )
        }
    }
}
