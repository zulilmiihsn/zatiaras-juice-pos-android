package com.zatiaras.pos.feature.printer.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.IconColors
import com.zatiaras.pos.core.ui.theme.ReceiptColors
import com.zatiaras.pos.core.ui.util.CurrencyFormatter
import com.zatiaras.pos.feature.printer.domain.model.PaperWidth
import com.zatiaras.pos.feature.printer.R
import androidx.compose.foundation.border

private val PreviewIconColor = IconColors.Preview

/**
 * Premium Receipt Preview that simulates a high-end minimarket receipt
 */
@Composable
internal fun ReceiptPreviewCard(
    storeName: String,
    storeAddress: String,
    storeLogoUri: String?,
    paperWidth: PaperWidth
) {
    val context = LocalContext.current
    val receiptWidth = if (paperWidth == PaperWidth.MM_58) 240.dp else 300.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.L),
        shape = AppShapes.L,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.M)
                        .background(PreviewIconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = PreviewIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.printer_preview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.printer_preview_subtitle,
                            if (paperWidth == PaperWidth.MM_58) stringResource(R.string.printer_preview_size_58) else stringResource(R.string.printer_preview_size_80)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Premium Receipt
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PremiumReceipt(
                    storeName = storeName,
                    storeAddress = storeAddress,
                    storeLogoUri = storeLogoUri,
                    receiptWidth = receiptWidth,
                    context = context
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info text
            Text(
                text = stringResource(R.string.printer_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun PremiumReceipt(
    storeName: String,
    storeAddress: String,
    storeLogoUri: String?,
    receiptWidth: androidx.compose.ui.unit.Dp,
    context: android.content.Context
) {
    // Thermal print colors - use centralized ReceiptColors tokens
    val textBlack = ReceiptColors.TextBlack
    val textGray = ReceiptColors.TextGray
    val textLightGray = ReceiptColors.TextLightGray
    val dividerColor = ReceiptColors.Divider
    
    // Receipt paper with shadow
    Card(
        modifier = Modifier
            .width(receiptWidth)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), AppShapes.XS),
        shape = AppShapes.XS,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    // Thermal paper yellowish tint
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            ReceiptColors.PaperWhite,
                            ReceiptColors.PaperMid,
                            ReceiptColors.PaperEdge
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ===== HEADER SECTION =====
            
            // Store Logo (grayscale effect)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ReceiptColors.TornEdge),
                contentAlignment = Alignment.Center
            ) {
                if (storeLogoUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(storeLogoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.printer_store_logo_title),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.zatiaras.pos.core.ui.R.drawable.zatiaras_logo),
                        contentDescription = stringResource(R.string.printer_store_logo_default_cd),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = storeName.ifEmpty { stringResource(R.string.printer_store_name_default) },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                color = textBlack
            )
            
            // Store address
            if (storeAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = storeAddress,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = textGray,
                    lineHeight = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Double line divider
            Column {
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Transaction info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.printer_receipt_transaction_no),
                        style = MaterialTheme.typography.labelSmall,
                        color = textLightGray
                    )
                    Text(
                        text = stringResource(R.string.printer_receipt_transaction_sample),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textBlack
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.printer_receipt_date),
                        style = MaterialTheme.typography.labelSmall,
                        color = textLightGray
                    )
                    Text(
                        text = stringResource(R.string.printer_receipt_date_sample),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textBlack
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dashed divider
            Text(
                text = stringResource(R.string.printer_receipt_separator),
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor,
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== ITEMS SECTION =====
            
            // Item rows
            ReceiptItemMono(
                name = stringResource(R.string.printer_item_sample_1),
                qty = 1,
                price = 5000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            ReceiptItemMono(
                name = stringResource(R.string.printer_item_sample_2),
                qty = 2,
                price = 30000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            ReceiptItemMono(
                name = stringResource(R.string.printer_item_sample_3),
                qty = 1,
                price = 25000,
                textColor = textBlack,
                subTextColor = textLightGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dashed divider
            Text(
                text = stringResource(R.string.printer_receipt_separator),
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor,
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== TOTALS SECTION =====
            
            // Subtotal
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.printer_receipt_subtotal), style = MaterialTheme.typography.bodySmall, color = textGray)
                Text(stringResource(R.string.printer_receipt_subtotal_sample), style = MaterialTheme.typography.bodySmall, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Tax
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.printer_receipt_tax_sample), style = MaterialTheme.typography.bodySmall, color = textLightGray)
                Text(stringResource(R.string.printer_receipt_tax_amount_sample), style = MaterialTheme.typography.bodySmall, color = textLightGray)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Solid divider
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grand Total - Bold and prominent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.printer_receipt_total_upper),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = textBlack
                )
                Text(
                    text = stringResource(R.string.printer_receipt_total_sample),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textBlack
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Payment info - simple text format (like real receipt)
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.printer_receipt_paid_cash), style = MaterialTheme.typography.bodySmall, color = textBlack)
                Text(stringResource(R.string.printer_receipt_paid_sample), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.printer_receipt_change), style = MaterialTheme.typography.bodySmall, color = textBlack)
                Text(stringResource(R.string.printer_receipt_change_sample), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = textBlack)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Double line divider
            Column {
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ===== FOOTER SECTION =====
            
            // Thank you message
            Text(
                text = stringResource(R.string.printer_receipt_thanks_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textBlack
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.printer_receipt_enjoy),
                style = MaterialTheme.typography.bodySmall,
                color = textGray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stringResource(R.string.printer_receipt_policy_line_1),
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
            Text(
                text = stringResource(R.string.printer_receipt_policy_line_2),
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Powered by
            Text(
                text = stringResource(R.string.printer_receipt_separator),
                style = MaterialTheme.typography.labelSmall,
                color = dividerColor.copy(alpha = 0.5f),
                letterSpacing = 0.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.printer_receipt_powered_by),
                style = MaterialTheme.typography.labelSmall,
                color = textLightGray
            )
        }
    }
}

/**
 * Receipt item row for thermal printer style output
 */
@Composable
internal fun ReceiptItemMono(
    name: String,
    qty: Int,
    price: Int,
    textColor: Color,
    subTextColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
            text = CurrencyFormatter.formatCurrency(price, includeSymbol = true, addSpace = true),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
        Text(
            text = stringResource(
                R.string.printer_receipt_item_qty_price,
                qty,
                CurrencyFormatter.formatCurrency(price / qty, includeSymbol = true, addSpace = true)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = subTextColor
        )
    }
}
