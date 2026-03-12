package com.zatiaras.pos.core.domain

/**
 * Custom Result sealed interface for domain and presentation layers.
 * 
 * Usage Guidelines:
 * - Use this [Result] for domain layer operations (UseCases, ViewModels)
 * - Use [kotlin.Result] for low-level data operations (DataSource, networking)
 * - Avoid mixing both Result types in one file to prevent ambiguous API usage
 * - When both are needed, alias one import explicitly
 * 
 * This separation allows:
 * - Domain layer to have explicit Loading state
 * - Data layer to use native Kotlin patterns
 * 
 * Example:
 * ```kotlin
 * // In UseCase/ViewModel:
 * suspend fun login(): Result<Unit>
 * 
 * // In DataSource:
 * suspend fun fetchFromApi(): kotlin.Result<List<Entity>>
 * ```
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable? = null) : Result<Nothing>
    data object Loading : Result<Nothing>
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onFailure(action: (Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}
