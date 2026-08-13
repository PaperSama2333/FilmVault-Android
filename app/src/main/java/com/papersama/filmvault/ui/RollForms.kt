package com.papersama.filmvault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.papersama.filmvault.FilmVaultViewModel
import com.papersama.filmvault.data.AppUiState
import com.papersama.filmvault.data.NewRollInput
import com.papersama.filmvault.data.ShotInput
import com.papersama.filmvault.data.TagCategory
import com.papersama.filmvault.data.splitTags
import com.papersama.filmvault.data.today

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewRollScreen(
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    back: () -> Unit,
) {
    val brands = state.tags.filter { it.category == TagCategory.BRAND }
    val types = state.tags.filter { it.category == TagCategory.TYPE }
    val projects = state.tags.filter { it.category == TagCategory.PROJECT }
    var brand by remember(brands) { mutableStateOf(brands.firstOrNull()?.name.orEmpty()) }
    var name by remember { mutableStateOf("") }
    var type by remember(types) { mutableStateOf(types.firstOrNull()?.name.orEmpty()) }
    var iso by remember { mutableStateOf("ISO 400") }
    var frames by remember { mutableStateOf("36") }
    var selectedProjectIds by remember { mutableStateOf(emptyList<String>()) }
    var customTags by remember { mutableStateOf("") }

    fun save() {
        if (name.isBlank() || brand.isBlank() || type.isBlank()) return
        viewModel.createRoll(
            NewRollInput(
                name = name.trim(),
                brandName = brand,
                type = type,
                iso = iso,
                frames = frames.toIntOrNull()?.coerceAtLeast(1) ?: 36,
                projectTagIds = selectedProjectIds,
                customTags = splitTags(customTags),
            ),
        ) { back() }
    }

    ScreenScaffold(title = "新增胶卷", back = back, action = "保存", onAction = ::save) {
        FormSection("品牌") {
            ChipFlow(brands.map { it.name }, brand) { brand = it }
        }
        FormSection("胶卷名称") {
            FilmTextField(name, { name = it }, placeholder = "如 Portra 400")
        }
        FormSection("类型") {
            ChipFlow(types.map { it.name }, type) { type = it }
        }
        FormSection("ISO") {
            ChipFlow(listOf("ISO 100", "ISO 200", "ISO 400", "ISO 800", "ISO 1600"), iso) { iso = it }
        }
        FormSection("张数") {
            FilmTextField(frames, { frames = it.filter(Char::isDigit) }, placeholder = "36")
        }
        FormSection("标签（项目）") {
            if (projects.isEmpty()) Text("暂无项目标签，可在「标签 - 管理」中新建", color = Weak)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                projects.forEach { tag ->
                    TagChip(tag.name, tag.id in selectedProjectIds) {
                        selectedProjectIds = if (tag.id in selectedProjectIds) {
                            selectedProjectIds - tag.id
                        } else selectedProjectIds + tag.id
                    }
                }
            }
        }
        FormSection("自定义标签（逗号分隔，仅本卷）") {
            FilmTextField(customTags, { customTags = it }, placeholder = "如 测试卷, 第一次")
        }
        PrimaryButton("保存", enabled = !state.busy, onClick = ::save)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShotScreen(
    rollId: String,
    shotId: String?,
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    back: () -> Unit,
) {
    val roll = state.rolls.firstOrNull { it.id == rollId }
    val existing = roll?.shots?.firstOrNull { it.id == shotId }
    var date by remember(existing) { mutableStateOf(existing?.date ?: today()) }
    var count by remember(existing) { mutableStateOf((existing?.count ?: 6).toString()) }
    var note by remember(existing) { mutableStateOf(existing?.note.orEmpty()) }
    var samples by remember(existing) { mutableStateOf(existing?.samples.orEmpty()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(12)) { uris ->
        if (uris.isNotEmpty()) viewModel.importSamples(uris) { samples = samples + it }
    }
    val editing = existing != null

    fun save() {
        viewModel.saveShot(
            rollId,
            shotId,
            ShotInput(
                date = date,
                count = count.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                note = note.trim(),
                samples = samples,
            ),
            back,
        )
    }

    ScreenScaffold(
        title = if (editing) "编辑拍摄日期" else "添加拍摄日期",
        back = back,
        action = "保存",
        onAction = ::save,
    ) {
        Text(
            if (editing) "编辑 ${roll?.name.orEmpty()} 的拍摄记录" else "为 ${roll?.name.orEmpty()} 添加拍摄记录",
            color = Sub,
        )
        FormSection("拍摄日期") { DateField(date, { date = it }) }
        FormSection("本次拍摄张数") {
            FilmTextField(count, { count = it.filter(Char::isDigit) }, placeholder = "6")
        }
        FormSection("拍摄日记") {
            FilmTextField(
                note,
                { note = it },
                placeholder = "写下今天拍了什么、用的参数或心情…",
                modifier = Modifier.height(120.dp),
                singleLine = false,
            )
        }
        FormSection("样张") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                samples.forEach { path ->
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))) {
                        LocalFileImage(path, Modifier.fillMaxSize())
                        IconButton(
                            onClick = { samples = samples - path },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "移除样张", tint = Danger)
                        }
                    }
                }
                IconButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "添加样张", tint = Weak)
                }
            }
        }
        PrimaryButton("保存", enabled = !state.busy, onClick = ::save)
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    back: () -> Unit,
    action: String? = null,
    onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (action == null) Box(Modifier.size(40.dp))
            else TextButton(onClick = onAction, modifier = Modifier.height(40.dp)) {
                Text(action, color = KodakYellow, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
    }
}

@Composable
private fun FormSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Sub)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(values: List<String>, selected: String, onSelect: (String) -> Unit) {
    if (values.isEmpty()) Text("暂无选项，可在「标签 - 管理」中新建", color = Weak)
    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { TagChip(it, it == selected) { onSelect(it) } }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KodakYellow,
            contentColor = Ink,
            disabledContainerColor = KodakYellow.copy(alpha = .5f),
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
