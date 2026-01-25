package com.zatiaras.pos.feature.pos.presentation.cashrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zatiaras.pos.core.domain.model.DatePeriod
import com.zatiaras.pos.feature.pos.domain.model.CashRecordType
import com.zatiaras.pos.feature.pos.domain.repository.CashRecordRepository
import com.zatiaras.pos.feature.pos.domain.repository.CashSummary
import com.zatiaras.pos.feature.pos.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for Cash Record (Buku Kas) list screen.
 * 
 * Combines both POS transactions and manual cash records in one view with date filtering.
 */
@HiltViewModel
class CashRecordViewModel @Inject constructor(
    private val cashRecordRepository: CashRecordRepository,
    private val transactionRepository: TransactionRepository
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
                
                // Calculate summary including POS transactions
                val manualSummary = cashRecordRepository.getTodaySummary()
                val posRevenue = transactions.sumOf { it.grandTotal }
                
                // Total income = POS sales + manual income
                val totalIncome = posRevenue + manualSummary.totalIncome
                val totalExpense = manualSummary.totalExpense
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
                    error = e.message ?: "Gagal memuat data"
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
            amount.isBlank() -> "Jumlah wajib diisi"
            numericAmount <= 0 -> "Jumlah harus lebih dari 0"
            else -> null
        }
    }

    private fun validateDescription(description: String): String? {
        return when {
            description.isBlank() -> "Keterangan wajib diisi"
            description.length < 3 -> "Keterangan minimal 3 karakter"
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
                    error = e.message ?: "Gagal menyimpan data"
                )
            }
        }
    }

    private fun deleteRecord(id: String) {
        // Only allow deleting manual cash records (not POS transactions)
        if (!id.startsWith("cash_")) {
            _uiState.value = _uiState.value.copy(
                error = "Transaksi POS tidak bisa dihapus"
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
                        error = e.message ?: "Gagal menghapus data"
                    )
                }
        }
    }

    fun resetForm() {
        _formState.value = CashRecordFormState()
    }
    
    private fun getDateRange(period: DatePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return when (period) {
            DatePeriod.TODAY -> {
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                start to end
            }
            DatePeriod.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                start to end
            }
            DatePeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                start to end
            }
            DatePeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val end = calendar.timeInMillis
                start to end
            }
            DatePeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val start = calendar.timeInMillis
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                start to endCalendar.timeInMillis
            }
            DatePeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                val start = calendar.timeInMillis
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                start to endCalendar.timeInMillis
            }
            DatePeriod.CUSTOM -> {
                val start = _uiState.value.customStartDate ?: calendar.timeInMillis
                val end = _uiState.value.customEndDate ?: calendar.timeInMillis
                start to end
            }
        }
    }
}
