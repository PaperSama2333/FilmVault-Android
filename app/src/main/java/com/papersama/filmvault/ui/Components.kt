package com.papersama.filmvault.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papersama.filmvault.data.Roll
import com.papersama.filmvault.data.RollStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FilmCard(roll: Roll, onClick: () -> Unit, archived: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (archived) 90.dp else 74.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brandColor(roll.brandName)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(roll.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${roll.iso} · ${roll.frames}张 · ${roll.type}", style = MaterialTheme.typography.labelMedium, color = Sub)
            if (archived && roll.archivedDate.isNotBlank()) {
                Text("完成于 ${roll.archivedDate}", style = MaterialTheme.typography.labelMedium, color = Sub)
            }
        }
        Text(
            if (archived) roll.status.label else "${roll.status.label} · ${roll.shot}/${roll.frames}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = statusColor(roll.status),
        )
    }
}

@Composable
fun ArchiveCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ArchiveBg)
            .border(1.dp, ArchiveLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(KodakYellow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("已拍摄完成的胶卷", style = MaterialTheme.typography.titleMedium)
            Text("收纳 $count 卷已标记完成的胶卷", style = MaterialTheme.typography.labelMedium, color = Sub)
        }
        Text("›", color = Weak, fontSize = 22.sp)
    }
}

@Composable
fun FilmProgress(shot: Int, frames: Int, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { if (frames > 0) (shot.toFloat() / frames).coerceIn(0f, 1f) else 0f },
        modifier = modifier.height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = KodakYellow,
        trackColor = Gray,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun TagChip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    AssistChip(
        onClick = onClick ?: {},
        enabled = onClick != null,
        label = { Text(text, fontSize = 13.sp) },
        shape = RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) KodakYellow else TagBg,
            labelColor = if (selected) Ink else TagInk,
            disabledContainerColor = TagBg,
            disabledLabelColor = TagInk,
        ),
        border = null,
        modifier = Modifier.height(32.dp),
    )
}

@Composable
fun FilmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        placeholder = { Text(placeholder, color = Weak) },
        singleLine = singleLine,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KodakYellow,
            unfocusedBorderColor = Line,
            cursorColor = KodakYellow,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun DateField(value: String, onValueChange: (String) -> Unit, label: String = "拍摄日期") {
    val context = LocalContext.current
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    val initial = runCatching { LocalDate.parse(value, formatter) }.getOrDefault(LocalDate.now())
    FilmTextField(
        value = value,
        onValueChange = {},
        label = label,
        readOnly = true,
        trailingIcon = {
            Text(
                "选择",
                color = KodakYellow,
                modifier = Modifier.padding(end = 12.dp).clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onValueChange(LocalDate.of(year, month + 1, day).format(formatter))
                        },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth,
                    ).show()
                },
            )
        },
    )
}

@Composable
fun LocalFileImage(path: String, modifier: Modifier, contentDescription: String? = null) {
    var bitmap by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            path.takeIf { it.isNotBlank() && File(it).exists() }
                ?.let(BitmapFactory::decodeFile)
                ?.asImageBitmap()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(Gray), contentAlignment = Alignment.Center) {
            Text("照片", color = Weak, style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun brandColor(brand: String): Color = when (brand.lowercase()) {
    "柯达", "kodak" -> KodakYellow
    "富士", "fujifilm", "fuji" -> Color(0xFF00A750)
    "伊尔福", "ilford" -> Color(0xFF2B2B2B)
    "cinestill" -> Color(0xFF1FA8A0)
    else -> Gray
}

fun statusColor(status: RollStatus): Color = when (status) {
    RollStatus.SHOOT -> Color(0xFFC98A00)
    RollStatus.DONE -> Ink
    RollStatus.SENT -> Color(0xFF3B82F6)
    RollStatus.WASH -> Color(0xFF2BA24C)
    RollStatus.UNSHOT -> Color(0xFF9A9AA1)
}
