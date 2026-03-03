package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zatiaras.pos.core.ui.theme.AppShapes
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.LocalDimensions
import com.zatiaras.pos.core.ui.theme.ProfitGreenDark
import com.zatiaras.pos.feature.reports.R
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Image

@Composable
fun ReportChatRoute(
    onNavigateBack: () -> Unit,
    viewModel: ReportChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    ReportChatScreen(
        uiState = uiState,
        onSendMessage = { viewModel.onEvent(ChatEvent.SendMessage(it)) },
        onSelectImage = { viewModel.onEvent(ChatEvent.SelectImage(it)) },
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportChatScreen(
    uiState: ReportChatUiState,
    onSendMessage: (String) -> Unit,
    onSelectImage: (String?) -> Unit,
    onEvent: (ChatEvent) -> Unit,
    onNavigateBack: () -> Unit
) {

    val listState = rememberLazyListState()
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        onSelectImage(uri?.toString())
    }
    
    val smartSuggestions = listOf(
        "Analisis Penjualan",
        "Produk Terlaris",
        "Analisis Laba Rugi",
        "Tips Operasional",
        "Pola Transaksi"
    )

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.chat_bot_name),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.reports_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(ChatEvent.ClearChat) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.chat_clear),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .imePadding()
        ) {
            val dimensions = LocalDimensions.current
            
            // Decorative background elements
            Box(modifier = Modifier.weight(1f)) {
                // Subtle mesh-like decorative circles
                Box(
                    modifier = Modifier
                        .offset(x = (-50).dp, y = 100.dp)
                        .size(300.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = dimensions.paddingM,
                        end = dimensions.paddingM,
                        top = dimensions.paddingM,
                        bottom = 120.dp // Space for suggestions + input
                    ),
                    verticalArrangement = Arrangement.spacedBy(dimensions.spacingM)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(
                                initialOffsetX = { if (message.isUser) it else -it }
                            ) + fadeIn()
                        ) {
                            ChatMessageItem(message)
                        }
                    }
                    
                    if (uiState.isLoading) {
                        item {
                            TypingIndicator()
                        }
                    }
                }
            }
            
            // Input Area
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = AppShapes.XXL,
                modifier = Modifier.border(
                    1.dp, 
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    AppShapes.XXL
                )
            ) {
                Column(
                    modifier = Modifier.padding(bottom = if (android.os.Build.VERSION.SDK_INT >= 30) 0.dp else 8.dp)
                ) {
                    // Smart Suggestions
                    this.AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.inputText.isEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(smartSuggestions) { suggestion ->
                                AssistChip(
                                    onClick = { onSendMessage(suggestion) },
                                    label = { Text(suggestion) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = AppShapes.L,
                                    border = null
                                )
                            }
                        }
                    }

                    // Image Preview
                    this.AnimatedVisibility(
                        visible = uiState.selectedImageUri != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                                .size(100.dp)
                                .clip(AppShapes.L)
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), AppShapes.L)
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onSelectImage(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.chat_remove_image),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            IconButton(onClick = { imagePicker.launch("image/*") }) {
                                Icon(
                                    imageVector = EvaIcons.Outline.Image,
                                    contentDescription = stringResource(R.string.chat_attach_image),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = { onEvent(ChatEvent.InputTextChanged(it)) },
                            modifier = Modifier.weight(1f),
                            placeholder = { 
                                Text(
                                    stringResource(R.string.chat_input_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                ) 
                            },
                            maxLines = 4,
                            shape = AppShapes.XXL,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (uiState.inputText.isNotBlank() || uiState.selectedImageUri != null) {
                                        onSendMessage(uiState.inputText)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val sendEnabled = (uiState.inputText.isNotBlank() || uiState.selectedImageUri != null) && !uiState.isLoading
                        
                        IconButton(
                            onClick = {
                                if (sendEnabled) {
                                    onSendMessage(uiState.inputText)
                                }
                            },
                            enabled = sendEnabled,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = if (sendEnabled) Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    ) else Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.chat_send),
                                tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        if (message.isThinking) { // We can repurpose this or use a state
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = stringResource(R.string.chat_ai_avatar),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        
        Surface(
            shape = AppShapes.XL,
            color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .background(
                    brush = if (isUser) Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ) else Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ),
                    shape = AppShapes.XL
                )
                .then(
                    if (!isUser) Modifier.border(
                        1.dp, 
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        AppShapes.XL
                    ) else Modifier
                )
        ) {
            Box(
                modifier = Modifier.background(
                    brush = if (isUser) Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    ) else Brush.linearGradient(
                        listOf(Color.Transparent, Color.Transparent)
                    )
                )
            ) {
                Column {
                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = stringResource(R.string.chat_message_image),
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(AppShapes.M)
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 260.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    if (message.content.isNotEmpty()) {
                        FormattedMessage(
                            content = message.content,
                            isUser = isUser,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        
        if (isUser) {
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun FormattedMessage(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val headerColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    
    Column(modifier = modifier) {
        val lines = content.split("\n")
        lines.forEach { line ->
            when {
                line.startsWith("---") -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = (if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outlineVariant).copy(alpha = 0.3f)
                    )
                }
                line.trim().startsWith("#") -> {
                    val hashCount = line.takeWhile { it == '#' }.length
                    val headingContent = line.dropWhile { it == '#' || it == ' ' }.trim()
                    if (hashCount > 0 && headingContent.isNotEmpty()) {
                        val style = when (hashCount) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleMedium
                            3 -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.bodyMedium
                        }
                        Text(
                            text = parseMarkdown(headingContent),
                            style = style,
                            fontWeight = FontWeight.Bold,
                            color = headerColor,
                            modifier = Modifier.padding(top = (8 - hashCount).coerceAtLeast(4).dp, bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = parseMarkdown(line),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    val cleanLine = line.trim().substring(2)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "•",
                            color = textColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseMarkdown(cleanLine),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.trim().let { it.isNotEmpty() && it[0].isDigit() && it.contains(". ") } -> {
                    val dotIndex = line.indexOf(". ")
                    val number = line.take(dotIndex + 2)
                    val text = line.drop(dotIndex + 2)
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = number,
                            color = textColor,
                            modifier = Modifier.padding(end = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseMarkdown(text),
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseMarkdown(line),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Enhanced markdown parser that highlights currency and percentages
 */
fun parseMarkdown(text: String): AnnotatedString {
    val highlightColor = ProfitGreenDark
    
    return buildAnnotatedString {
        val boldRegex = """\*\*(.*?)\*\*|__(.*?)__""".toRegex()
        val italicRegex = """\*(.*?)\*|_(.*?)_""".toRegex()
        val currencyRegex = """Rp\s?[0-9.]+""".toRegex()
        val percentRegex = """[0-9.]+%""".toRegex()

        var currentIdx = 0
        
        // Combine all special tokens for a single pass
        val tokens = (boldRegex.findAll(text).map { it to "bold" } +
                     italicRegex.findAll(text).map { it to "italic" } +
                     currencyRegex.findAll(text).map { it to "currency" } +
                     percentRegex.findAll(text).map { it to "percent" })
            .sortedBy { it.first.range.first }
            .toList()

        tokens.forEach { (match, type) ->
            // Add plain text before match
            if (match.range.first > currentIdx) {
                append(text.substring(currentIdx, match.range.first))
            }
            
            when (type) {
                "bold" -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1].ifEmpty { match.groupValues[2] })
                    }
                }
                "italic" -> {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(match.groupValues[1].ifEmpty { match.groupValues[2] })
                    }
                }
                "currency" -> {
                    withStyle(SpanStyle(
                        color = highlightColor, 
                        fontWeight = FontWeight.ExtraBold,
                    )) {
                        append(match.value)
                    }
                }
                "percent" -> {
                    withStyle(SpanStyle(
                        color = InfoBlue,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(match.value)
                    }
                }
            }
            currentIdx = match.range.last + 1
        }
        
        // Add remaining text
        if (currentIdx < text.length) {
            append(text.substring(currentIdx))
        }
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 40.dp)
            .graphicsLayer(alpha = alpha)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                AppShapes.L
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val orbScale by transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb"
        )
        
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer(scaleX = orbScale, scaleY = orbScale)
                .background(
                    brush = Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.chat_thinking),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
