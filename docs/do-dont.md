# ✅❌ Do This, Not That

> **Purpose**: Concrete code examples showing correct vs incorrect patterns
> **For**: AI Agents and Human Developers

---

## 1. UI Layer

### Business Logic in Composables

```kotlin
// ❌ DON'T: Calculate in Composable
@Composable
fun ProductCard(product: Product) {
    val discountedPrice = product.price * 0.9  // ❌ Business logic!
    val formattedPrice = "Rp ${discountedPrice.toInt()}"  // ❌ Formatting logic!
    
    Text(text = formattedPrice)
}

// ✅ DO: Display pre-calculated values from ViewModel
@Composable
fun ProductCard(product: ProductUiModel) {
    Text(text = product.displayPrice)  // ✅ Already formatted
}

// ViewModel prepares the data:
data class ProductUiModel(
    val id: String,
    val name: String,
    val displayPrice: String  // "Rp 45.000"
)
```

---

### Hardcoded Strings

```kotlin
// ❌ DON'T: Hardcoded strings
@Composable
fun ErrorMessage() {
    Text("Terjadi kesalahan, silakan coba lagi")  // ❌ Not localizable
}

// ✅ DO: Use string resources
@Composable
fun ErrorMessage() {
    Text(stringResource(R.string.error_generic))  // ✅ Localizable
}
```

---

### State Handling

```kotlin
// ❌ DON'T: Ignore loading/error states
@Composable
fun ProductList(viewModel: ProductViewModel) {
    val products = viewModel.products.collectAsState()
    
    LazyColumn {
        items(products.value) { product ->  // ❌ What if loading? What if error?
            ProductCard(product)
        }
    }
}

// ✅ DO: Handle ALL states
@Composable
fun ProductList(viewModel: ProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is ProductUiState.Loading -> LoadingIndicator()
        is ProductUiState.Error -> ErrorMessage(uiState.message)
        is ProductUiState.Empty -> EmptyState()
        is ProductUiState.Success -> {
            LazyColumn {
                items(uiState.products) { product ->
                    ProductCard(product)
                }
            }
        }
    }
}
```

---

## 2. ViewModel Layer

### Exposing MutableState

```kotlin
// ❌ DON'T: Expose MutableStateFlow
@HiltViewModel
class ProductViewModel @Inject constructor() : ViewModel() {
    val uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)  // ❌ Mutable exposed!
}

// ✅ DO: Expose read-only StateFlow
@HiltViewModel
class ProductViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()  // ✅ Read-only
}
```

---

### Calling Repository Directly Without Error Handling

```kotlin
// ❌ DON'T: No error handling
fun loadProducts() {
    viewModelScope.launch {
        val products = repository.getProducts()  // ❌ What if this throws?
        _uiState.value = ProductUiState.Success(products)
    }
}

// ✅ DO: Proper error handling
fun loadProducts() {
    viewModelScope.launch {
        _uiState.value = ProductUiState.Loading
        try {
            val products = repository.getProducts()
            if (products.isEmpty()) {
                _uiState.value = ProductUiState.Empty
            } else {
                _uiState.value = ProductUiState.Success(products)
            }
        } catch (e: Exception) {
            _uiState.value = ProductUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

### Using GlobalScope

```kotlin
// ❌ DON'T: GlobalScope causes memory leaks
fun syncData() {
    GlobalScope.launch {  // ❌ Never cancelled, memory leak!
        repository.sync()
    }
}

// ✅ DO: Use viewModelScope
fun syncData() {
    viewModelScope.launch {  // ✅ Cancelled when ViewModel cleared
        repository.sync()
    }
}
```

---

## 3. Repository Layer

### Network Call in Repository Without Offline Support

```kotlin
// ❌ DON'T: Only network, no offline
class ProductRepositoryImpl(
    private val api: ProductApi
) : ProductRepository {
    override suspend fun getProducts(): List<Product> {
        return api.getProducts()  // ❌ Fails when offline!
    }
}

// ✅ DO: Offline-first with local DB
class ProductRepositoryImpl(
    private val localDataSource: ProductLocalDataSource,
    private val remoteDataSource: ProductRemoteDataSource
) : ProductRepository {
    override fun getProducts(): Flow<List<Product>> {
        return localDataSource.getProducts()  // ✅ Always works offline
    }
    
    override suspend fun sync() {
        try {
            val remoteProducts = remoteDataSource.getProducts()
            localDataSource.insertAll(remoteProducts)
        } catch (e: Exception) {
            // Network failed, but we still have local data
        }
    }
}
```

---

### Skipping Local DB for "Simple" Writes

```kotlin
// ❌ DON'T: Write directly to server
override suspend fun addProduct(product: Product) {
    api.createProduct(product)  // ❌ Fails offline, no retry!
}

// ✅ DO: Write to local first, sync later
override suspend fun addProduct(product: Product) {
    // 1. Save locally (always succeeds)
    localDataSource.insert(product.copy(isSynced = false))
    
    // 2. Try remote (may fail, that's okay)
    runCatching {
        remoteDataSource.create(product)
        localDataSource.markAsSynced(product.id)
    }
}
```

---

## 4. Data Layer

### Using `Any` Type

```kotlin
// ❌ DON'T: Any type loses type safety
data class ApiResponse(
    val data: Any?,  // ❌ What is this?
    val error: Any?  // ❌ No type info!
)

// ✅ DO: Use generics and sealed classes
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
}
```

---

### Empty Catch Blocks

```kotlin
// ❌ DON'T: Silent failures
suspend fun syncProducts() {
    try {
        api.sync()
    } catch (e: Exception) {
        // ❌ Error swallowed, no one knows what happened!
    }
}

// ✅ DO: Log and handle properly
suspend fun syncProducts(): Result<Unit> {
    return try {
        api.sync()
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to sync products")  // ✅ Logged
        Result.failure(e)  // ✅ Caller knows there was an error
    }
}
```

---

## 5. Dependency Injection

### Manual Instantiation

```kotlin
// ❌ DON'T: Manual instantiation
class ProductViewModel : ViewModel() {
    private val repository = ProductRepositoryImpl(  // ❌ Tight coupling!
        ProductLocalDataSource(),
        ProductRemoteDataSource()
    )
}

// ✅ DO: Inject via Hilt
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository  // ✅ Injected, testable
) : ViewModel()
```

---

### Hardcoded Dependencies

```kotlin
// ❌ DON'T: Hardcoded API URL
class SupabaseClient {
    private val url = "https://xyz.supabase.co"  // ❌ Not configurable!
}

// ✅ DO: Inject config
class SupabaseClient @Inject constructor(
    private val config: BranchConfig  // ✅ Different per branch
) {
    private val url = config.supabaseUrl
}
```

---

## 6. Navigation

### Hardcoded Navigation Strings

```kotlin
// ❌ DON'T: Magic strings
navController.navigate("product_detail/123")  // ❌ Easy to typo!

// ✅ DO: Type-safe navigation
// Define routes:
sealed class Screen(val route: String) {
    data object ProductList : Screen("product_list")
    data class ProductDetail(val id: String) : Screen("product_detail/$id")
}

// Navigate:
navController.navigate(Screen.ProductDetail(productId).route)
```

---

## 7. Async Operations

### Blocking Main Thread

```kotlin
// ❌ DON'T: Blocking call
fun loadData() {
    val data = runBlocking {  // ❌ Blocks UI thread!
        repository.getData()
    }
}

// ✅ DO: Use coroutines properly
fun loadData() {
    viewModelScope.launch {  // ✅ Non-blocking
        val data = repository.getData()
        _uiState.value = UiState.Success(data)
    }
}
```

---

### Not Using Dispatchers

```kotlin
// ❌ DON'T: CPU-heavy work on main thread
fun processImage(bitmap: Bitmap) {
    viewModelScope.launch {
        val compressed = compressImage(bitmap)  // ❌ Runs on Main!
    }
}

// ✅ DO: Use appropriate dispatcher
fun processImage(bitmap: Bitmap) {
    viewModelScope.launch {
        val compressed = withContext(Dispatchers.Default) {  // ✅ CPU work
            compressImage(bitmap)
        }
        val saved = withContext(Dispatchers.IO) {  // ✅ IO work
            saveToStorage(compressed)
        }
    }
}
```

---

## Quick Reference Table

| ❌ Don't | ✅ Do |
|----------|-------|
| Business logic in Composables | Logic in ViewModel |
| Hardcoded strings | `stringResource(R.string.x)` |
| Ignore Loading/Error states | Handle ALL states with sealed class |
| Expose `MutableStateFlow` | Expose read-only `StateFlow` |
| `GlobalScope.launch` | `viewModelScope.launch` |
| Network-only repository | Offline-first with Room |
| `Any` type | Generics or sealed classes |
| Empty `catch {}` | Log + return Result.failure() |
| Manual instantiation | Hilt injection |
| Magic navigation strings | Type-safe routes |
| `runBlocking` in ViewModel | `viewModelScope.launch` |

---
