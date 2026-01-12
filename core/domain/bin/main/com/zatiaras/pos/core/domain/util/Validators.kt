package com.zatiaras.pos.core.domain.util

/**
 * Input validation utilities for forms.
 * 
 * Provides reusable validation logic with localized error messages.
 */
object Validators {

    /**
     * Validate username input.
     * 
     * Rules:
     * - Minimum 3 characters
     * - Maximum 50 characters
     * - Only alphanumeric and underscores allowed
     */
    fun validateUsername(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Invalid("Username tidak boleh kosong")
            username.length < 3 -> ValidationResult.Invalid("Username minimal 3 karakter")
            username.length > 50 -> ValidationResult.Invalid("Username maksimal 50 karakter")
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> 
                ValidationResult.Invalid("Username hanya boleh huruf, angka, dan underscore")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validate password input.
     * 
     * Rules:
     * - Minimum 4 characters (simple POS system)
     * - Maximum 100 characters
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid("Password tidak boleh kosong")
            password.length < 4 -> ValidationResult.Invalid("Password minimal 4 karakter")
            password.length > 100 -> ValidationResult.Invalid("Password terlalu panjang")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validate price input.
     * 
     * Rules:
     * - Must be a positive number
     * - Maximum 1 billion (reasonable for POS)
     */
    fun validatePrice(price: String): ValidationResult {
        val priceValue = price.toLongOrNull()
        return when {
            price.isBlank() -> ValidationResult.Invalid("Harga tidak boleh kosong")
            priceValue == null -> ValidationResult.Invalid("Harga harus berupa angka")
            priceValue < 0 -> ValidationResult.Invalid("Harga tidak boleh negatif")
            priceValue > 1_000_000_000 -> ValidationResult.Invalid("Harga terlalu besar")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validate quantity input.
     * 
     * Rules:
     * - Must be a positive integer
     * - Maximum 999999
     */
    fun validateQuantity(quantity: String): ValidationResult {
        val quantityValue = quantity.toIntOrNull()
        return when {
            quantity.isBlank() -> ValidationResult.Invalid("Jumlah tidak boleh kosong")
            quantityValue == null -> ValidationResult.Invalid("Jumlah harus berupa angka")
            quantityValue <= 0 -> ValidationResult.Invalid("Jumlah harus lebih dari 0")
            quantityValue > 999999 -> ValidationResult.Invalid("Jumlah terlalu besar")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validate product name.
     * 
     * Rules:
     * - Minimum 2 characters
     * - Maximum 100 characters
     */
    fun validateProductName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Nama produk tidak boleh kosong")
            name.length < 2 -> ValidationResult.Invalid("Nama produk minimal 2 karakter")
            name.length > 100 -> ValidationResult.Invalid("Nama produk maksimal 100 karakter")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validate description (optional field).
     * 
     * Rules:
     * - Maximum 500 characters
     */
    fun validateDescription(description: String): ValidationResult {
        return when {
            description.length > 500 -> ValidationResult.Invalid("Deskripsi maksimal 500 karakter")
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Result of validation check.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
    
    val isValid: Boolean get() = this is Valid
    val errorMessage: String? get() = (this as? Invalid)?.message
}
