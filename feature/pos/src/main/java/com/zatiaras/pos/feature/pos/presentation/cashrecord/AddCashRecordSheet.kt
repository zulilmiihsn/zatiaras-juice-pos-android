package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.feature.pos.R
import com.zatiaras.pos.core.domain.util.LocaleUtils
import com.zatiaras.pos.core.ui.components.CurrencyTextField
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.ExpenseRed
import com.zatiaras.pos.core.ui.theme.IncomeGreen
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCashRecordSheet(
    formState: CashRecordFormState,
    onEvent: (CashRecordEvent) -> Unit,
    onCancel: () -> Unit
) {
    val dimensions = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensions.paddingXL)
    ) {
        // Centered Title
        Text(
            text = stringResource(R.string.cash_record_add_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingXXS))
        
        Text(
            text = stringResource(R.string.cash_record_add_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingL))
        
        // Type Selection - LARGE Segmented Button Style
        Text(
            text = stringResource(R.string.cash_record_type),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(dimensions.spacingS))
        
        // Large segmented buttons for easy touch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeightLarge), // Standard touch target
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS)
        ) {
            // PEMASUKAN Button
            Surface(
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.INCOME)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = AppShapes.L,
                color = if (formState.type == CashRecordType.INCOME) 
                    IncomeGreen.copy(alpha = 0.15f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                border = if (formState.type == CashRecordType.INCOME)
                    androidx.compose.foundation.BorderStroke(1.dp, IncomeGreen)
                else
                    null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (formState.type == CashRecordType.INCOME) 
                            IncomeGreen 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimensions.iconSizeL)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingXS))
                    Text(
                        text = stringResource(R.string.cash_record_type_income).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (formState.type == CashRecordType.INCOME) 
                            IncomeGreen 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // PENGELUARAN Button
            Surface(
                onClick = { onEvent(CashRecordEvent.SetType(CashRecordType.EXPENSE)) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = AppShapes.L,
                color = if (formState.type == CashRecordType.EXPENSE) 
                    ExpenseRed.copy(alpha = 0.15f)
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                border = if (formState.type == CashRecordType.EXPENSE)
                    androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed)
                else
                    null
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (formState.type == CashRecordType.EXPENSE) 
                            ExpenseRed 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(dimensions.iconSizeL)
                    )
                    Spacer(modifier = Modifier.width(dimensions.spacingXS))
                    Text(
                        text = stringResource(R.string.cash_record_type_expense).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (formState.type == CashRecordType.EXPENSE) 
                            ExpenseRed 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingM))
        
        // Date Selection
        val date = formState.date ?: System.currentTimeMillis()
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy", LocaleUtils.LOCALE_ID)
        var showDatePicker by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dateFormatter.format(Date(date)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.cash_record_date)) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = stringResource(R.string.cash_record_pick_date)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = AppShapes.M
            )
            
            // Invisible clickable layer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true }
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = date
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onEvent(CashRecordEvent.SetDate(it))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingM))
        
        // Amount with currency formatting
        CurrencyTextField(
            value = formState.amount,
            onValueChange = { onEvent(CashRecordEvent.SetAmount(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cash_record_amount)) },
            showPrefix = true,
            isError = formState.amountError != null,
            singleLine = true,
            shape = AppShapes.M
        )
        
        // Show error if any
        if (formState.amountError != null) {
            Text(
                text = formState.amountError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = dimensions.spacingM, top = dimensions.paddingXXS)
            )
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingXS))
        
        // Description
        OutlinedTextField(
            value = formState.description,
            onValueChange = { onEvent(CashRecordEvent.SetDescription(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cash_record_description)) },
            placeholder = { Text(stringResource(R.string.cash_record_description_hint)) },
            isError = formState.descriptionError != null,
            supportingText = formState.descriptionError?.let { { Text(it) } },
            singleLine = true,
            shape = AppShapes.M
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingXS))
        
        // Category (Dropdown)
        val categories = if (formState.type == CashRecordType.INCOME) {
            com.zatiaras.pos.core.domain.model.CashCategories.INCOME_CATEGORIES
        } else {
            com.zatiaras.pos.core.domain.model.CashCategories.EXPENSE_CATEGORIES
        }
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = formState.category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.cash_record_category)) },
                placeholder = { Text(stringResource(R.string.cash_record_category_placeholder)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = AppShapes.M
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onEvent(CashRecordEvent.SetCategory(category))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.spacingXS))
        
        // Notes (optional)
        OutlinedTextField(
            value = formState.notes,
            onValueChange = { onEvent(CashRecordEvent.SetNotes(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.cash_record_notes)) },
            minLines = 2,
            shape = AppShapes.M
        )
        
        Spacer(modifier = Modifier.height(dimensions.spacingL))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = AppShapes.M
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
            
            Button(
                onClick = { onEvent(CashRecordEvent.SaveRecord) },
                modifier = Modifier.weight(1f),
                enabled = formState.isValid && !formState.isSubmitting,
                shape = AppShapes.M,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (formState.type == CashRecordType.INCOME) {
                        IncomeGreen
                    } else {
                        ExpenseRed
                    }
                )
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensions.iconSizeM),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.cash_record_save))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(dimensions.paddingXXL))
    }
}
