package com.voltguard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltguard.app.ui.theme.Card
import com.voltguard.app.ui.theme.CardStroke
import com.voltguard.app.ui.theme.Cyan
import com.voltguard.app.ui.theme.StatLabel
import com.voltguard.app.ui.theme.StatNumber
import com.voltguard.app.ui.theme.TextMuted
import com.voltguard.app.ui.theme.TextPrimary
import com.voltguard.app.ui.theme.TextSecondary

@Composable
fun VgCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    color: Color = Card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = color,
        border = BorderStroke(1.dp, CardStroke),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(18.dp)) { content() }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Box(Modifier.weight(1f).padding(horizontal = 8.dp))
        } else Box(Modifier.weight(1f))
        Text(
            text,
            style = StatLabel,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    sub: String? = null,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(16.dp),
            )
            Text(label, style = StatLabel, modifier = Modifier.padding(start = 6.dp))
        }
        Text(
            value,
            style = StatNumber,
            color = valueColor,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        if (sub != null) {
            Text(
                sub,
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
