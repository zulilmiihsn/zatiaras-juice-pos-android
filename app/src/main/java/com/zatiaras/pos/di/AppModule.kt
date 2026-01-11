package com.zatiaras.pos.di

import com.zatiaras.pos.feature.pos.presentation.receipt.ReceiptPrinterHelper
import com.zatiaras.pos.feature.printer.domain.PrinterServiceHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level bindings.
 * 
 * This module bridges feature modules by providing bindings
 * for interfaces defined in one module but implemented in another.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    /**
     * Bind PrinterServiceHelper to ReceiptPrinterHelper.
     * This allows the POS module to print receipts without
     * direct dependency on the printer module.
     */
    @Binds
    @Singleton
    abstract fun bindReceiptPrinterHelper(
        impl: PrinterServiceHelper
    ): ReceiptPrinterHelper
}
