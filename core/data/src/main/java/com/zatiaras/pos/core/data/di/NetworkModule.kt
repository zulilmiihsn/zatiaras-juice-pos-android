package com.zatiaras.pos.core.data.di

import android.content.Context
import com.zatiaras.pos.core.data.BuildConfig
import com.zatiaras.pos.core.data.session.EncryptedSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideEncryptedSessionManager(
        @ApplicationContext context: Context,
        json: Json,
    ): EncryptedSessionManager = EncryptedSessionManager(context, json)

    @Provides
    @Singleton
    fun provideSupabaseClient(
        sessionManager: EncryptedSessionManager,
    ): SupabaseClient {
        require(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is not configured"
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY is not configured"
        }
        Timber.d("Creating Supabase client")
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth) {
                // Use our encrypted session manager for secure token storage
                this.sessionManager = sessionManager
            }
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest = client.postgrest

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage = client.storage

    @Provides
    @Singleton
    fun provideSupabaseFunctions(client: SupabaseClient): Functions = client.functions
}
