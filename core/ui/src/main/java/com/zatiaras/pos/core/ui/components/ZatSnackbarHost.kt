package com.zatiaras.pos.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.AlertTriangle
import compose.icons.evaicons.outline.CheckmarkCircle2
import compose.icons.evaicons.outline.Info
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.SuccessGreen

/**
 * A reusable premium SnackbarHost wrapper that styles messages according to their type
 * (Success, Error, Info) based on the message content and application branding.
 */
@Composable
fun ZatSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val messageText = data.visuals.message
        val messageLower = messageText.lowercase()
        
        // Very basic heuristics to determine snackbar type
        val isError = messageLower.contains("gagal") || 
            messageLower.contains("error") || 
            messageLower.contains("sudah ada") ||
            messageLower.contains("kurang") ||
            messageLower.contains("tidak valid") ||
            messageLower.contains("salah") ||
            messageLower.contains("minimum")
            
        val isSuccess = messageLower.contains("berhasil") || 
            messageLower.contains("disimpan") || 
            messageLower.contains("tersimpan") ||
            messageLower.contains("dibuka") || 
            messageLower.contains("ditutup") ||
            messageLower.contains("sukses")
            
        val containerColor = when {
            isError -> MaterialTheme.colorScheme.errorContainer
            isSuccess -> SuccessGreen
            else -> MaterialTheme.colorScheme.inverseSurface
        }
        
        val contentColor = when {
            isError -> MaterialTheme.colorScheme.onErrorContainer
            isSuccess -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.inverseOnSurface
        }
        
        val icon = when {
            isError -> EvaIcons.Outline.AlertTriangle
            isSuccess -> EvaIcons.Outline.CheckmarkCircle2
            else -> EvaIcons.Outline.Info
        }

        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = containerColor,
            contentColor = contentColor,
            shape = AppShapes.L,
            action = null // Custom minimal look, no default action button
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}
