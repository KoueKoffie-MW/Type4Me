package com.transcriptor.hid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transcriptor.hid.data.db.SnippetEntity
import com.transcriptor.hid.data.db.SyntaxType

@Composable
fun SnippetCard(
    snippet: SnippetEntity,
    onTrigger: (SnippetEntity) -> Unit,
    onToggleFavorite: (SnippetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTrigger(snippet) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Syntax Badge, Title, Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    SyntaxBadge(syntaxType = snippet.syntaxType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = snippet.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onToggleFavorite(snippet) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (snippet.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (snippet.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (snippet.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Code Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(8.dp)
            ) {
                Text(
                    text = snippet.content.trimEnd(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Tags & 1-Tap Dispatch Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (snippet.tags.isNotEmpty()) {
                    Text(
                        text = snippet.tags.joinToString(separator = " ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                FilledTonalButton(
                    onClick = { onTrigger(snippet) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Type", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SyntaxBadge(
    syntaxType: SyntaxType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (syntaxType) {
        SyntaxType.GIT -> Triple(Color(0xFFF4511E).copy(alpha = 0.15f), Color(0xFFF4511E), "GIT")
        SyntaxType.DOCKER -> Triple(Color(0xFF0288D1).copy(alpha = 0.15f), Color(0xFF0288D1), "DOCKER")
        SyntaxType.KUBERNETES -> Triple(Color(0xFF3949AB).copy(alpha = 0.15f), Color(0xFF3949AB), "K8S")
        SyntaxType.RUST -> Triple(Color(0xFFD84315).copy(alpha = 0.15f), Color(0xFFD84315), "RUST")
        SyntaxType.PYTHON -> Triple(Color(0xFF00897B).copy(alpha = 0.15f), Color(0xFF00897B), "PYTHON")
        SyntaxType.SHELL -> Triple(Color(0xFF43A047).copy(alpha = 0.15f), Color(0xFF43A047), "SH")
        SyntaxType.SQL -> Triple(Color(0xFFE65100).copy(alpha = 0.15f), Color(0xFFE65100), "SQL")
        SyntaxType.MARKDOWN -> Triple(Color(0xFF1E88E5).copy(alpha = 0.15f), Color(0xFF1E88E5), "MD")
        SyntaxType.PROMPT -> Triple(Color(0xFF8E24AA).copy(alpha = 0.15f), Color(0xFF8E24AA), "AI")
        SyntaxType.PLAIN_TEXT -> Triple(Color(0xFF757575).copy(alpha = 0.15f), Color(0xFF757575), "TXT")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
