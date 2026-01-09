# 📝 Code Templates for AI Agents

> **Purpose**: Copy-paste boilerplates for common file types
> **Usage**: AI should use these as starting points when generating new files

---

## 1. ViewModel Template

```kotlin
package com.zatiaras.pos.feature.[feature].presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class [Feature]ViewModel @Inject constructor(
    private val [feature]Repository: [Feature]Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<[Feature]UiState>([Feature]UiState.Idle)
    val uiState: StateFlow<[Feature]UiState> = _uiState.asStateFlow()

    fun on[Action]() {
        viewModelScope.launch {
            _uiState.value = [Feature]UiState.Loading
            try {
                val result = [feature]Repository.[action]()
                _uiState.value = [Feature]UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = [Feature]UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

## 2. UI State Sealed Class Template

```kotlin
package com.zatiaras.pos.feature.[feature].presentation

sealed interface [Feature]UiState {
    data object Idle : [Feature]UiState
    data object Loading : [Feature]UiState
    data class Success(val data: [DataType]) : [Feature]UiState
    data class Error(val message: String) : [Feature]UiState
    data object Empty : [Feature]UiState
}
```

---

## 3. Screen Composable Template

```kotlin
package com.zatiaras.pos.feature.[feature].presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun [Feature]Screen(
    viewModel: [Feature]ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateTo[Destination]: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    [Feature]Content(
        uiState = uiState,
        onAction = viewModel::on[Action],
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun [Feature]Content(
    uiState: [Feature]UiState,
    onAction: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.[feature]_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is [Feature]UiState.Idle -> { /* Initial state */ }
                is [Feature]UiState.Loading -> {
                    CircularProgressIndicator()
                }
                is [Feature]UiState.Success -> {
                    [Feature]SuccessContent(data = uiState.data)
                }
                is [Feature]UiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = onAction
                    )
                }
                is [Feature]UiState.Empty -> {
                    EmptyContent()
                }
            }
        }
    }
}

@Composable
private fun [Feature]SuccessContent(data: [DataType]) {
    // TODO: Implement success UI
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyContent() {
    Text(stringResource(R.string.no_data))
}
```

---

## 4. Repository Interface Template

```kotlin
package com.zatiaras.pos.feature.[feature].domain.repository

import com.zatiaras.pos.feature.[feature].domain.model.[Model]
import kotlinx.coroutines.flow.Flow

interface [Feature]Repository {
    
    /**
     * Get all [items] from local database.
     * Observes changes reactively.
     */
    fun get[Items](): Flow<List<[Model]>>
    
    /**
     * Get single [item] by ID.
     */
    suspend fun get[Item]ById(id: String): [Model]?
    
    /**
     * Create new [item].
     * Saves to local DB first, then syncs to remote.
     */
    suspend fun create[Item](item: [Model]): Result<[Model]>
    
    /**
     * Update existing [item].
     */
    suspend fun update[Item](item: [Model]): Result<[Model]>
    
    /**
     * Delete [item] by ID.
     * Soft delete preferred (set is_active = false).
     */
    suspend fun delete[Item](id: String): Result<Unit>
    
    /**
     * Sync local changes to remote.
     */
    suspend fun sync(): Result<Unit>
}
```

---

## 5. Repository Implementation Template

```kotlin
package com.zatiaras.pos.feature.[feature].data.repository

import com.zatiaras.pos.feature.[feature].data.local.[Feature]LocalDataSource
import com.zatiaras.pos.feature.[feature].data.remote.[Feature]RemoteDataSource
import com.zatiaras.pos.feature.[feature].domain.model.[Model]
import com.zatiaras.pos.feature.[feature].domain.repository.[Feature]Repository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class [Feature]RepositoryImpl @Inject constructor(
    private val localDataSource: [Feature]LocalDataSource,
    private val remoteDataSource: [Feature]RemoteDataSource
) : [Feature]Repository {

    override fun get[Items](): Flow<List<[Model]>> {
        return localDataSource.get[Items]()
    }

    override suspend fun get[Item]ById(id: String): [Model]? {
        return localDataSource.get[Item]ById(id)
    }

    override suspend fun create[Item](item: [Model]): Result<[Model]> {
        return try {
            // 1. Save to local DB first (Offline-First)
            val savedItem = localDataSource.insert[Item](item.copy(isSynced = false))
            
            // 2. Try to sync to remote (fire-and-forget)
            runCatching { remoteDataSource.create[Item](savedItem) }
                .onSuccess { localDataSource.markAsSynced(savedItem.id) }
            
            Result.success(savedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update[Item](item: [Model]): Result<[Model]> {
        return try {
            localDataSource.update[Item](item.copy(isSynced = false))
            runCatching { remoteDataSource.update[Item](item) }
                .onSuccess { localDataSource.markAsSynced(item.id) }
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete[Item](id: String): Result<Unit> {
        return try {
            localDataSource.softDelete[Item](id)
            runCatching { remoteDataSource.delete[Item](id) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sync(): Result<Unit> {
        return try {
            val unsyncedItems = localDataSource.getUnsyncedItems()
            unsyncedItems.forEach { item ->
                remoteDataSource.upsert[Item](item)
                localDataSource.markAsSynced(item.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 6. Room Entity Template

```kotlin
package com.zatiaras.pos.feature.[feature].data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "[table_name]")
data class [Feature]Entity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isActive: Boolean = true
)
```

---

## 7. Room DAO Template

```kotlin
package com.zatiaras.pos.feature.[feature].data.local.dao

import androidx.room.*
import com.zatiaras.pos.feature.[feature].data.local.entity.[Feature]Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface [Feature]Dao {

    @Query("SELECT * FROM [table_name] WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAll(): Flow<List<[Feature]Entity>>

    @Query("SELECT * FROM [table_name] WHERE id = :id")
    suspend fun getById(id: String): [Feature]Entity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: [Feature]Entity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<[Feature]Entity>)

    @Update
    suspend fun update(entity: [Feature]Entity)

    @Query("UPDATE [table_name] SET isActive = 0 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE [table_name] SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM [table_name] WHERE isSynced = 0")
    suspend fun getUnsynced(): List<[Feature]Entity>

    @Query("DELETE FROM [table_name]")
    suspend fun deleteAll()
}
```

---

## 8. Hilt Module Template

```kotlin
package com.zatiaras.pos.feature.[feature].di

import com.zatiaras.pos.feature.[feature].data.repository.[Feature]RepositoryImpl
import com.zatiaras.pos.feature.[feature].domain.repository.[Feature]Repository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class [Feature]Module {

    @Binds
    @Singleton
    abstract fun bind[Feature]Repository(
        impl: [Feature]RepositoryImpl
    ): [Feature]Repository
}
```

---

## 9. UseCase Template

```kotlin
package com.zatiaras.pos.feature.[feature].domain.usecase

import com.zatiaras.pos.feature.[feature].domain.model.[Model]
import com.zatiaras.pos.feature.[feature].domain.repository.[Feature]Repository
import javax.inject.Inject

class [Action][Feature]UseCase @Inject constructor(
    private val repository: [Feature]Repository
) {
    suspend operator fun invoke(/* params */): Result<[ReturnType]> {
        // Validation
        // if (invalid) return Result.failure(IllegalArgumentException("..."))
        
        // Business logic
        return repository.[action](/* params */)
    }
}
```

---

## 10. Domain Model Template

```kotlin
package com.zatiaras.pos.feature.[feature].domain.model

data class [Model](
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

## Quick Reference: File Naming

| Type | Pattern | Example |
|------|---------|---------|
| Screen | `[Feature]Screen.kt` | `POSScreen.kt` |
| ViewModel | `[Feature]ViewModel.kt` | `POSViewModel.kt` |
| UiState | `[Feature]UiState.kt` | `POSUiState.kt` |
| Repository Interface | `[Feature]Repository.kt` | `ProductRepository.kt` |
| Repository Impl | `[Feature]RepositoryImpl.kt` | `ProductRepositoryImpl.kt` |
| Entity | `[Feature]Entity.kt` | `ProductEntity.kt` |
| DAO | `[Feature]Dao.kt` | `ProductDao.kt` |
| UseCase | `[Verb][Feature]UseCase.kt` | `AddToCartUseCase.kt` |
| Module | `[Feature]Module.kt` | `ProductModule.kt` |

---
