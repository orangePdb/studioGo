package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PreviewDeviceFrame
import com.example.engine.ComposePreviewParser
import com.example.engine.ParsedPreviewScreen
import com.example.engine.PreviewNode
import com.example.ui.theme.AndroidEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KotlinViolet
import com.example.ui.theme.StudioBlue

@Composable
fun LivePreviewPane(
    parsedPreview: ParsedPreviewScreen?,
    previewStateMap: Map<String, Any>,
    previewDarkTheme: Boolean,
    showLayoutBounds: Boolean,
    deviceFrame: PreviewDeviceFrame,
    onActionClick: (String) -> Unit,
    onStateChange: (String, Any) -> Unit,
    onResetState: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLayoutBounds: () -> Unit,
    onCycleDeviceFrame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Preview Header Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(AndroidEmerald),
            )
            Text(
                text = "@Preview ${parsedPreview?.composableName ?: "Main"}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.width(6.dp))

            PreviewChipButton(
                icon = if (deviceFrame == PreviewDeviceFrame.PIXEL_PHONE) {
                    Icons.Default.PhoneAndroid
                } else {
                    Icons.Default.TabletAndroid
                },
                label = deviceFrame.label,
                active = false,
                tag = "preview_device_toggle",
                onClick = onCycleDeviceFrame,
            )

            PreviewChipButton(
                icon = if (previewDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                label = if (previewDarkTheme) "Dark" else "Light",
                active = previewDarkTheme,
                tag = "preview_theme_toggle",
                onClick = onToggleTheme,
            )

            PreviewChipButton(
                icon = Icons.Default.GridOn,
                label = "Bounds",
                active = showLayoutBounds,
                tag = "preview_bounds_toggle",
                onClick = onToggleLayoutBounds,
            )

            PreviewChipButton(
                icon = Icons.Default.Refresh,
                label = "Reset",
                active = false,
                tag = "preview_reset_button",
                onClick = onResetState,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Interactive Device Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val frameWidth = if (deviceFrame == PreviewDeviceFrame.PIXEL_PHONE) 340.dp else 480.dp
            val bezelBg = if (previewDarkTheme) Color(0xFF0E1117) else Color(0xFFF8FAFC)
            val bezelText = if (previewDarkTheme) Color(0xFFE6EAF2) else Color(0xFF0F172A)
            val cardSurface = if (previewDarkTheme) Color(0xFF1A1F2B) else Color(0xFFFFFFFF)

            Surface(
                color = bezelBg,
                shape = RoundedCornerShape(26.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .widthIn(max = frameWidth)
                    .fillMaxWidth()
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(26.dp),
                    )
                    .testTag("live_emulator_frame"),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Simulated Android Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "9:41",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = bezelText.copy(alpha = 0.8f),
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF272B36)),
                        )
                        Text(
                            text = "5G  100%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = bezelText.copy(alpha = 0.7f),
                        )
                    }

                    // Rendered Composable Nodes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        if (parsedPreview == null || parsedPreview.rootNodes.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "No preview nodes",
                                    tint = KotlinViolet,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add @Composable UI (Column, Text, Button, Card) to preview live.",
                                    fontSize = 13.sp,
                                    color = bezelText.copy(alpha = 0.75f),
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                parsedPreview.rootNodes.forEach { node ->
                                    RenderPreviewNode(
                                        node = node,
                                        stateMap = previewStateMap,
                                        defaultTextColor = bezelText,
                                        cardSurfaceColor = cardSurface,
                                        showLayoutBounds = showLayoutBounds,
                                        onActionClick = onActionClick,
                                        onStateChange = onStateChange,
                                    )
                                }
                            }
                        }
                    }

                    // Simulated Gesture Navigation Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(84.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(bezelText.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewChipButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = if (active) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                tint = if (active) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun RenderPreviewNode(
    node: PreviewNode,
    stateMap: Map<String, Any>,
    defaultTextColor: Color,
    cardSurfaceColor: Color,
    showLayoutBounds: Boolean,
    onActionClick: (String) -> Unit,
    onStateChange: (String, Any) -> Unit,
) {
    val boundsModifier = if (showLayoutBounds) {
        Modifier.border(1.dp, StudioBlue.copy(alpha = 0.7f))
    } else {
        Modifier
    }

    when (node) {
        is PreviewNode.ColumnNode -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(boundsModifier)
                    .padding(node.paddingDp.dp),
                horizontalAlignment = if (node.centerHorizontally) {
                    Alignment.CenterHorizontally
                } else {
                    Alignment.Start
                },
            ) {
                node.children.forEach { child ->
                    RenderPreviewNode(
                        node = child,
                        stateMap = stateMap,
                        defaultTextColor = defaultTextColor,
                        cardSurfaceColor = cardSurfaceColor,
                        showLayoutBounds = showLayoutBounds,
                        onActionClick = onActionClick,
                        onStateChange = onStateChange,
                    )
                }
            }
        }
        is PreviewNode.RowNode -> {
            Row(
                modifier = Modifier
                    .then(boundsModifier)
                    .padding(node.paddingDp.dp),
                verticalAlignment = if (node.centerVertically) {
                    Alignment.CenterVertically
                } else {
                    Alignment.Top
                },
            ) {
                node.children.forEach { child ->
                    RenderPreviewNode(
                        node = child,
                        stateMap = stateMap,
                        defaultTextColor = defaultTextColor,
                        cardSurfaceColor = cardSurfaceColor,
                        showLayoutBounds = showLayoutBounds,
                        onActionClick = onActionClick,
                        onStateChange = onStateChange,
                    )
                }
            }
        }
        is PreviewNode.BoxNode -> {
            Box(
                modifier = Modifier
                    .then(boundsModifier)
                    .padding(node.paddingDp.dp),
            ) {
                node.children.forEach { child ->
                    RenderPreviewNode(
                        node = child,
                        stateMap = stateMap,
                        defaultTextColor = defaultTextColor,
                        cardSurfaceColor = cardSurfaceColor,
                        showLayoutBounds = showLayoutBounds,
                        onActionClick = onActionClick,
                        onStateChange = onStateChange,
                    )
                }
            }
        }
        is PreviewNode.CardNode -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardSurfaceColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(boundsModifier)
                    .padding(node.paddingDp.dp)
                    .border(
                        width = 1.dp,
                        color = defaultTextColor.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                node.children.forEach { child ->
                    RenderPreviewNode(
                        node = child,
                        stateMap = stateMap,
                        defaultTextColor = defaultTextColor,
                        cardSurfaceColor = cardSurfaceColor,
                        showLayoutBounds = showLayoutBounds,
                        onActionClick = onActionClick,
                        onStateChange = onStateChange,
                    )
                }
            }
        }
        is PreviewNode.TextNode -> {
            val resolved = ComposePreviewParser.resolveTemplateText(node.templateText, stateMap)
            Text(
                text = resolved,
                fontSize = node.fontSizeSp.sp,
                fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                color = node.colorHex?.let { Color(it) } ?: defaultTextColor,
                modifier = boundsModifier,
            )
        }
        is PreviewNode.ButtonNode -> {
            val resolvedLabel = ComposePreviewParser.resolveTemplateText(node.label, stateMap)
            Button(
                onClick = { onActionClick(node.actionCode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AndroidEmerald,
                    contentColor = Color(0xFF072112),
                ),
                modifier = boundsModifier.testTag("preview_button_$resolvedLabel"),
            ) {
                Text(text = resolvedLabel, fontWeight = FontWeight.SemiBold)
            }
        }
        is PreviewNode.SwitchNode -> {
            val checked = (stateMap[node.stateVarName] as? Boolean) ?: false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(boundsModifier)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = node.label,
                    fontSize = 13.sp,
                    color = defaultTextColor,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = checked,
                    onCheckedChange = { onStateChange(node.stateVarName, it) },
                    modifier = Modifier.testTag("preview_switch_${node.stateVarName}"),
                )
            }
        }
        is PreviewNode.SliderNode -> {
            val currentVal = (stateMap[node.stateVarName] as? Number)?.toFloat() ?: 0.5f
            Column(modifier = boundsModifier.fillMaxWidth()) {
                Text(text = node.label, fontSize = 12.sp, color = defaultTextColor)
                Slider(
                    value = currentVal.coerceIn(0f, 1f),
                    onValueChange = { onStateChange(node.stateVarName, it) },
                )
            }
        }
        is PreviewNode.IconNode -> {
            Icon(
                imageVector = mapPreviewIcon(node.iconName),
                contentDescription = node.iconName,
                tint = node.tintHex?.let { Color(it) } ?: AndroidEmerald,
                modifier = boundsModifier.size(28.dp),
            )
        }
        is PreviewNode.SpacerNode -> {
            Spacer(
                modifier = Modifier
                    .height(node.heightDp.dp)
                    .width(node.widthDp.dp),
            )
        }
        PreviewNode.DividerNode -> {
            HorizontalDivider(
                color = defaultTextColor.copy(alpha = 0.14f),
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

private fun mapPreviewIcon(name: String): ImageVector = when (name.lowercase()) {
    "add" -> Icons.Default.Add
    "favorite" -> Icons.Default.Favorite
    "star" -> Icons.Default.Star
    "check" -> Icons.Default.Check
    "wbsunny", "sunny" -> Icons.Default.WbSunny
    "build" -> Icons.Default.Build
    "code" -> Icons.Default.Code
    "playarrow" -> Icons.Default.PlayArrow
    "notifications" -> Icons.Default.Notifications
    "home" -> Icons.Default.Home
    "settings" -> Icons.Default.Settings
    "person" -> Icons.Default.Person
    else -> Icons.Default.Bolt
}
