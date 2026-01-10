# ⚙️ POS Feature Specifications

**Feature**: Point of Sales (POS)
**Plan Reference**: [docs/plans/04-pos.md](../plans/04-pos.md)

---

## 1. Database Schema (Room)

### TransactionEntity
```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isSynced"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,                    // UUID
    val transactionNumber: String,                 // TRX-20260109-0001
    val subtotal: Long,                            // Before tax/discount
    val discountAmount: Long = 0,                  // Discount in rupiah
    val discountPercent: Double = 0.0,             // Discount percentage
    val taxAmount: Long = 0,                       // Tax in rupiah
    val taxPercent: Double = 0.0,                  // Tax percentage (e.g., 11.0)
    val grandTotal: Long,                          // Final amount customer pays
    val paymentMethod: String,                     // CASH, QRIS, TRANSFER
    val amountPaid: Long,                          // Amount given by customer
    val changeAmount: Long,                        // Change returned
    val notes: String? = null,                     // Optional notes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
```

### TransactionItemEntity
```kotlin
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey val id: String,                    // UUID
    val transactionId: String,                     // FK to transactions
    val productId: String,                         // Reference (not FK, product may be deleted)
    val productName: String,                       // Snapshot of name at purchase time
    val productPrice: Long,                        // Snapshot of price at purchase time
    val quantity: Int,                             // Quantity ordered
    val subtotal: Long,                            // productPrice * quantity
    val notes: String? = null                      // Item-level notes (e.g., "less ice")
)
```

---

## 2. Domain Models

### Cart (In-Memory Only)
```kotlin
data class Cart(
    val items: List<CartItem> = emptyList()
) {
    val itemCount: Int get() = items.sumOf { it.quantity }
    val subtotal: Long get() = items.sumOf { it.subtotal }
    
    fun isEmpty(): Boolean = items.isEmpty()
    
    fun addItem(product: Product, quantity: Int = 1): Cart
    fun updateQuantity(productId: String, quantity: Int): Cart
    fun removeItem(productId: String): Cart
    fun clear(): Cart
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String? = null
) {
    val subtotal: Long get() = product.price * quantity
}
```

### Transaction
```kotlin
data class Transaction(
    val id: String,
    val transactionNumber: String,
    val items: List<TransactionItem>,
    val subtotal: Long,
    val discountAmount: Long,
    val discountPercent: Double,
    val taxAmount: Long,
    val taxPercent: Double,
    val grandTotal: Long,
    val paymentMethod: PaymentMethod,
    val amountPaid: Long,
    val changeAmount: Long,
    val notes: String?,
    val createdAt: Long,
    val isSynced: Boolean
)

data class TransactionItem(
    val id: String,
    val productId: String,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
    val subtotal: Long,
    val notes: String?
)

enum class PaymentMethod {
    CASH, QRIS, TRANSFER
}
```

---

## 3. Repository Interface

```kotlin
interface TransactionRepository {
    // Save new transaction with its items
    suspend fun createTransaction(
        cart: Cart,
        paymentMethod: PaymentMethod,
        amountPaid: Long,
        discountPercent: Double = 0.0,
        taxPercent: Double = 0.0,
        notes: String? = null
    ): Result<Transaction>
    
    // Get transaction by ID
    suspend fun getTransactionById(id: String): Transaction?
    
    // Get today's transactions
    fun getTodayTransactions(): Flow<List<Transaction>>
    
    // Get transactions by date range
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    // Sync transactions to remote
    suspend fun syncToRemote(): Result<Unit>
    
    // Generate next transaction number
    suspend fun generateTransactionNumber(): String
}
```

---

## 4. UI States (Compose)

### PosUiState (Catalog + Cart Combined)
```kotlin
data class PosUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cart: Cart = Cart(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

### CheckoutUiState
```kotlin
sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState
    
    data class Ready(
        val cart: Cart,
        val subtotal: Long,
        val discountPercent: Double = 0.0,
        val discountAmount: Long = 0,
        val taxPercent: Double = 11.0,    // Default PPN
        val taxAmount: Long = 0,
        val grandTotal: Long,
        val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
        val amountPaid: String = "",
        val changeAmount: Long = 0,
        val isProcessing: Boolean = false,
        val paymentError: String? = null
    ) : CheckoutUiState
    
    data class Success(
        val transaction: Transaction
    ) : CheckoutUiState
    
    data class Error(val message: String) : CheckoutUiState
}
```

---

## 5. UI Components

### POSCatalogScreen
- Top: Search bar + Category filter chips
- Center: LazyVerticalGrid of ProductCard (tap to add to cart)
- Right/Bottom: Cart sidebar/bottom sheet

### CartComponent
- List of CartItems with quantity controls (+/-)
- Subtotal display
- "Checkout" button

### CheckoutScreen (or BottomSheet)
- Order summary (read-only list)
- Discount input (optional)
- Tax toggle (PPN 11%)
- Payment method selection (Cash/QRIS)
- Amount paid input (for Cash)
- Change calculation
- "Complete Transaction" button

### ReceiptPreview
- Transaction number
- Date/time
- Item list
- Totals breakdown
- Payment info
- "Print" button (Phase 7)
- "New Transaction" button

---

## 6. Acceptance Criteria (Checklist)

### Catalog
- [ ] Products display in responsive grid
- [ ] Category filter works correctly
- [ ] Search filters products by name
- [ ] Tap product adds 1 to cart
- [ ] Products with images load correctly (Coil)

### Cart
- [ ] Cart displays all added items
- [ ] +/- buttons update quantity
- [ ] Quantity 0 removes item from cart
- [ ] Subtotal updates in real-time
- [ ] Item count badge shows on cart icon
- [ ] Cart persists through screen rotation

### Checkout
- [ ] Displays correct subtotal
- [ ] Discount calculation works (percentage)
- [ ] Tax calculation works (11% PPN)
- [ ] Grand total = subtotal - discount + tax
- [ ] Cash payment shows change calculation
- [ ] Validation: amountPaid >= grandTotal
- [ ] Cannot checkout with empty cart

### Transaction
- [ ] Transaction saved to Room on completion
- [ ] Transaction number generated correctly
- [ ] Receipt preview shows all details
- [ ] "New Transaction" clears cart and returns to catalog
- [ ] Transaction marked isSynced=false initially

### Offline
- [ ] Full POS flow works without internet
- [ ] Products loaded from local Room
- [ ] Transactions saved locally
- [ ] No crashes or errors when offline

---

*Created: 2026-01-09*
*Last Updated: 2026-01-09*
