package com.zatiaras.pos.feature.pos.presentation.cashrecord

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import com.zatiaras.pos.feature.pos.domain.repository.CashRecordRepository
import com.zatiaras.pos.feature.pos.domain.repository.CashSummary
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import com.zatiaras.pos.feature.pos.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import com.zatiaras.pos.core.domain.util.DateUtils
import javax.inject.Inject

/**
 * ViewModel for Cash Record (Buku Kas) list screen.
 * 
 * Combines both POS transactions and manual cash records in one view with date filtering.
 */
@HiltViewModel
class CashRecordViewModel @Inject constructor(
    private val cashRecordRepository: CashRecordRepository,
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashRecordUiState())
    val uiState: StateFlow<CashRecordUiState> = _uiState.asStateFlow()
    
    private val _formState = MutableStateFlow(CashRecordFormState())
    val formState: StateFlow<CashRecordFormState> = _formState.asStateFlow()
    
    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()
    
    private val _selectedDatePeriod = MutableStateFlow(DatePeriod.TODAY)
    val selectedDatePeriod: StateFlow<DatePeriod> = _selectedDatePeriod.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            val (startDate, endDate) = getDateRange(_selectedDatePeriod.value)
            
            // Calculate dynamic summary directly from combined streams based on date range
            // Combine POS transactions and manual cash records
            combine(
                transactionRepository.getTransactionsByDateRange(startDate, endDate),
                cashRecordRepository.getRecordsByDateRange(startDate, endDate)
            ) { transactions, cashRecords ->
                // Convert to unified CashFlowItems
                val transactionItems = transactions.map { CashFlowItem.FromTransaction(it) }
                val cashRecordItems = cashRecords.map { CashFlowItem.FromCashRecord(it) }
                
                // Combine and sort by time (newest first)
                val allItems = (transactionItems + cashRecordItems)
                    .sortedByDescending { it.createdAt }
                
                val posRevenue = transactions.sumOf { it.grandTotal }
                
                // Calculate Dynamic Summary based on Date Range
                val manualIncome = cashRecords.filter { it.type == CashRecordType.INCOME }.sumOf { it.amount }
                val manualExpense = cashRecords.filter { it.type == CashRecordType.EXPENSE }.sumOf { it.amount }
                
                // Total income = POS sales + dynamic manual income
                val totalIncome = posRevenue + manualIncome
                val totalExpense = manualExpense
                val netCash = totalIncome - totalExpense
                
                Triple(
                    allItems,
                    CashSummary(totalIncome, totalExpense, netCash),
                    transactions.size
                )
            }
            .catch { e ->
                Timber.e(e, "Error loading cash records")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: context.getString(R.string.cash_record_error_load)
                )
            }
            .collect { (items, summary, posCount) ->
                _uiState.value = _uiState.value.copy(
                    items = items,
                    summary = summary,
                    posTransactionCount = posCount,
                    isLoading = false
                )
            }
        }
    }

    fun onEvent(event: CashRecordEvent) {
        when (event) {
            is CashRecordEvent.SetDateFilter -> {
                _selectedDatePeriod.value = event.period
                _uiState.value = _uiState.value.copy(
                    customStartDate = event.customStartDate,
                    customEndDate = event.customEndDate
                )
                loadRecords()
            }
            
            is CashRecordEvent.SetType -> {
                _formState.value = _formState.value.copy(type = event.type)
            }
            
            is CashRecordEvent.SetAmount -> {
                val cleanAmount = event.amount.filter { it.isDigit() }
                _formState.value = _formState.value.copy(
                    amount = cleanAmount,
                    amountError = validateAmount(cleanAmount)
                )
            }
            
            is CashRecordEvent.SetDescription -> {
                _formState.value = _formState.value.copy(
                    description = event.description,
                    descriptionError = validateDescription(event.description)
                )
            }
            
            is CashRecordEvent.SetCategory -> {
                _formState.value = _formState.value.copy(category = event.category)
            }
            
            is CashRecordEvent.SetNotes -> {
                _formState.value = _formState.value.copy(notes = event.notes)
            }
            
            is CashRecordEvent.SetDate -> {
                _formState.value = _formState.value.copy(date = event.date)
            }
            
            is CashRecordEvent.SaveRecord -> {
                saveRecord()
            }
            
            is CashRecordEvent.DeleteRecord -> {
                deleteRecord(event.id)
            }
            
            is CashRecordEvent.DismissError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    private fun validateAmount(amount: String): String? {
        val numericAmount = amount.toLongOrNull() ?: 0
        return when {
            amount.isBlank() -> context.getString(R.string.cash_record_error_amount_required)
            numericAmount <= 0 -> context.getString(R.string.cash_record_error_amount_positive)
            else -> null
        }
    }

    private fun validateDescription(description: String): String? {
        return when {
            description.isBlank() -> context.getString(R.string.cash_record_error_description_required)
            description.length < 3 -> context.getString(R.string.cash_record_error_description_min)
            else -> null
        }
    }

    private fun saveRecord() {
        val form = _formState.value
        
        // Validate all fields
        val amountError = validateAmount(form.amount)
        val descriptionError = validateDescription(form.description)
        
        if (amountError != null || descriptionError != null) {
            _formState.value = form.copy(
                amountError = amountError,
                descriptionError = descriptionError
            )
            return
        }
        
        viewModelScope.launch {
            _formState.value = form.copy(isSubmitting = true)
            
            cashRecordRepository.createRecord(
                type = form.type,
                amount = form.amount.toLongOrNull() ?: 0,
                description = form.description.trim(),
                category = form.category.trim().ifBlank { null },
                notes = form.notes.trim().ifBlank { null },
                date = form.date ?: System.currentTimeMillis()
            ).onSuccess {
                Timber.d("Cash record saved successfully")
                resetForm()
                _saveSuccess.emit(true)
            }.onFailure { e ->
                Timber.e(e, "Failed to save cash record")
                _formState.value = form.copy(isSubmitting = false)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: context.getString(R.string.cash_record_error_save)
                )
            }
        }
    }

    private fun deleteRecord(id: String) {
        // Only allow deleting manual cash records (not POS transactions)
        if (!id.startsWith("cash_")) {
            _uiState.value = _uiState.value.copy(
                error = context.getString(R.string.cash_record_error_pos_not_deletable)
            )
            return
        }
        
        val originalId = id.removePrefix("cash_")
        viewModelScope.launch {
            cashRecordRepository.deleteRecord(originalId)
                .onSuccess {
                    Timber.d("Cash record deleted: $originalId")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete cash record: $originalId")
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: context.getString(R.string.cash_record_error_delete)
                    )
                }
        }
    }

    fun resetForm() {
        _formState.value = CashRecordFormState()
    }
    
    private fun getDateRange(period: DatePeriod): Pair<Long, Long> {
        return when (period) {
            DatePeriod.TODAY -> DateUtils.getTodayRange()
            DatePeriod.YESTERDAY -> DateUtils.getYesterdayRange()
            DatePeriod.THIS_WEEK -> DateUtils.getThisWeekRange()
            DatePeriod.THIS_MONTH -> DateUtils.getThisMonthRange()
            DatePeriod.LAST_7_DAYS -> DateUtils.getLastNDaysRange(7)
            DatePeriod.LAST_30_DAYS -> DateUtils.getLastNDaysRange(30)
            DatePeriod.CUSTOM -> {
                val start = _uiState.value.customStartDate ?: DateUtils.getStartOfDay()
                val end = _uiState.value.customEndDate ?: DateUtils.getEndOfDay()
                start to end
            }
        }
    }
}
