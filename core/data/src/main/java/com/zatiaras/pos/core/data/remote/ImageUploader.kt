package com.zatiaras.pos.core.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles image upload to Supabase Storage.
 * 
 * Features:
 * - Compress images before upload (reduce bandwidth)
 * - Generate unique filenames
 * - Return public URL after upload
 * 
 * Bucket: "produk-images" (must be created in Supabase Dashboard)
 */
@Singleton
class ImageUploader @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val BUCKET_NAME = "produk-images"
        private const val MAX_IMAGE_SIZE = 1024 // Max dimension in pixels
        private const val COMPRESSION_QUALITY = 80 // JPEG quality (0-100)
    }

    /**
     * Upload image from Uri to Supabase Storage.
     * 
     * @param context Android context for content resolver
     * @param imageUri Local image Uri (from gallery/camera)
     * @param productId Product ID for subfolder organization
     * @return Public URL of uploaded image, or null if failed
     */
    suspend fun uploadProductImage(
        context: Context,
        imageUri: Uri,
        productId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Read and compress image
            val compressedBytes = compressImage(context, imageUri)
            if (compressedBytes == null) {
                return@withContext Result.failure(Exception("Gagal memproses gambar"))
            }

            // 2. Generate unique filename
            val fileName = "${productId}/${UUID.randomUUID()}.jpg"

            // 3. Upload to Supabase Storage
            val storage = supabaseClient.storage
            val bucket = storage.from(BUCKET_NAME)
            
            bucket.upload(fileName, compressedBytes, upsert = true)

            // 4. Get public URL
            val publicUrl = bucket.publicUrl(fileName)
            
            Timber.d("Image uploaded successfully: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload image")
            Result.failure(e)
        }
    }

    /**
     * Delete image from Supabase Storage.
     */
    suspend fun deleteProductImage(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Extract path from URL
            val path = imageUrl.substringAfter("$BUCKET_NAME/")
            
            val storage = supabaseClient.storage
            val bucket = storage.from(BUCKET_NAME)
            bucket.delete(path)
            
            Timber.d("Image deleted: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete image")
            Result.failure(e)
        }
    }

    /**
     * Compress image to reduce file size.
     * - Resize if larger than MAX_IMAGE_SIZE
     * - Compress as JPEG with COMPRESSION_QUALITY
     */
    private fun compressImage(context: Context, imageUri: Uri): ByteArray? {
        return try {
            // Read bitmap from Uri
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Timber.e("Failed to decode bitmap from Uri")
                return null
            }

            // Calculate new dimensions (maintain aspect ratio)
            val (newWidth, newHeight) = calculateScaledDimensions(
                originalBitmap.width,
                originalBitmap.height,
                MAX_IMAGE_SIZE
            )

            // Resize if needed
            val scaledBitmap = if (newWidth != originalBitmap.width || newHeight != originalBitmap.height) {
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            
            // Cleanup
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            Timber.e(e, "Error compressing image")
            null
        }
    }

    private fun calculateScaledDimensions(width: Int, height: Int, maxSize: Int): Pair<Int, Int> {
        if (width <= maxSize && height <= maxSize) {
            return Pair(width, height)
        }

        val ratio = width.toFloat() / height.toFloat()
        return if (width > height) {
            Pair(maxSize, (maxSize / ratio).toInt())
        } else {
            Pair((maxSize * ratio).toInt(), maxSize)
        }
    }
}
