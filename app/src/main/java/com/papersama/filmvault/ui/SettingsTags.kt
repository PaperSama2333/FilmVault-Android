package com.papersama.filmvault.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.papersama.filmvault.FilmVaultViewModel
import com.papersama.filmvault.data.AppUiState
import com.papersama.filmvault.data.TagCategory
import com.papersama.filmvault.data.TagItem
import com.papersama.filmvault.reminder.FilmReminder
import com.papersama.filmvault.reminder.ReminderPreferences
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var confirmRestore by remember { mutableStateOf(false) }
    var notificationAllowed by remember { mutableStateOf(FilmReminder.canNotify(context)) }
    var remindersEnabled by remember { mutableStateOf(ReminderPreferences.isEnabled(context)) }
    var reminderDays by remember { mutableStateOf(ReminderPreferences.delayDays(context)) }
    var enableAfterPermission by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationAllowed = granted && FilmReminder.canNotify(context)
        if (enableAfterPermission) {
            remindersEnabled = notificationAllowed
            ReminderPreferences.setEnabled(context, remindersEnabled)
            enableAfterPermission = false
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.exportBackup { raw ->
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                    requireNotNull(writer) { "无法创建备份文件" }
                    writer.write(raw)
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                requireNotNull(reader) { "无法读取备份文件" }
                reader.readText()
            }
        }.onSuccess(viewModel::restoreBackup)
    }
    val timestamp = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAllowed = FilmReminder.canNotify(context)
                if (!notificationAllowed && remindersEnabled) {
                    remindersEnabled = false
                    ReminderPreferences.setEnabled(context, false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ScreenScaffold("设置", back) {
        Text("提醒", color = Sub, fontWeight = FontWeight.SemiBold)
        val reminderShape = RoundedCornerShape(12.dp)
        ListItem(
            modifier = Modifier.fillMaxWidth().clip(reminderShape).border(1.dp, Line, reminderShape),
            colors = ListItemDefaults.colors(containerColor = Surface),
            headlineContent = { Text("冲洗提醒", fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text(
                    "胶卷标记完成后，在指定天数后提醒冲洗。",
                    color = Sub,
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            trailingContent = {
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationAllowed) {
                            enableAfterPermission = true
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            remindersEnabled = enabled
                            ReminderPreferences.setEnabled(context, enabled)
                        }
                    },
                )
            },
        )
        ListItem(
            modifier = Modifier.fillMaxWidth().clip(reminderShape).border(1.dp, Line, reminderShape),
            colors = ListItemDefaults.colors(containerColor = Surface),
            headlineContent = { Text("提醒时间", fontWeight = FontWeight.Medium) },
            supportingContent = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7).forEach { days ->
                        TagChip("${days}天后", reminderDays == days) {
                            reminderDays = days
                            ReminderPreferences.setDelayDays(context, days)
                        }
                    }
                }
            },
            trailingContent = if (notificationAllowed) {
                {
                    TextButton(onClick = { FilmReminder.sendTest(context) }) {
                        Text("测试", color = Ink)
                    }
                }
            } else null,
        )
        Text("数据管理", color = Sub, fontWeight = FontWeight.SemiBold)
        SettingsCard(
            title = "导出数据",
            description = "把所有胶卷、拍摄记录、标签和资料导出为备份文件，可保存到云端或分享。",
            button = if (state.busy) "处理中…" else "导出",
            enabled = !state.busy,
        ) { exportLauncher.launch("filmvault-backup-$timestamp.filmvault.json") }
        SettingsCard(
            title = "还原数据",
            description = "从备份文件恢复数据，将覆盖当前所有数据。注意：头像不会随备份还原。",
            button = if (state.busy) "处理中…" else "还原",
            enabled = !state.busy,
        ) { confirmRestore = true }
        Text(
            "备份文件仅包含本地数据库内容，样张和头像图片不会被打包。",
            color = Weak,
            style = MaterialTheme.typography.labelMedium,
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("还原数据") },
            text = { Text("将从备份文件覆盖当前数据，现有数据会被替换。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restoreLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                }) { Text("确定还原", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    ListItem(
        modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, Line, shape),
        colors = ListItemDefaults.colors(containerColor = Surface),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(description, color = Sub, style = MaterialTheme.typography.labelMedium) },
        trailingContent = {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KodakYellow, contentColor = Ink),
            ) { Text(button) }
        },
    )
}

private enum class TagEditorMode { CREATE, RENAME }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagsManageScreen(
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    back: () -> Unit,
) {
    var editor by remember { mutableStateOf<TagEditorMode?>(null) }
    var editingTag by remember { mutableStateOf<TagItem?>(null) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TagCategory.PROJECT) }

    ScreenScaffold("标签管理", back) {
        if (state.tags.isEmpty()) {
            Text("还没有标签，点击下方新建。", color = Sub)
        } else {
            TagCategory.entries.forEach { group ->
                Text(group.title, color = Sub, fontWeight = FontWeight.SemiBold)
                val tags = state.tags.filter { it.category == group }
                if (tags.isEmpty()) Text("该分类下暂无标签", color = Weak)
                tags.forEach { tag ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)),
                        colors = ListItemDefaults.colors(containerColor = Surface),
                        headlineContent = { Text(tag.name, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                "${state.tagCounts[tag.id] ?: 0} 卷",
                                color = Sub,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        trailingContent = {
                            Row {
                                TextButton(onClick = {
                                    editingTag = tag
                                    name = tag.name
                                    category = tag.category
                                    editor = TagEditorMode.RENAME
                                }) { Text("重命名", color = Sub) }
                                TextButton(onClick = { viewModel.deleteTag(tag.id) }) {
                                    Text("删除", color = Danger)
                                }
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("+ 新建标签") {
            editingTag = null
            name = ""
            category = TagCategory.PROJECT
            editor = TagEditorMode.CREATE
        }
    }

    if (editor != null) {
        ModalBottomSheet(onDismissRequest = { editor = null }, containerColor = Surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (editor == TagEditorMode.CREATE) "新建标签" else "重命名标签", style = MaterialTheme.typography.titleLarge)
                FilmTextField(name, { name = it }, label = "标签名称", placeholder = "输入标签名")
                if (editor == TagEditorMode.CREATE) {
                    Text("分类", color = Sub)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TagCategory.PROJECT, TagCategory.BRAND, TagCategory.TYPE).forEach {
                            TagChip(it.title, category == it) { category = it }
                        }
                    }
                }
                PrimaryButton(if (editor == TagEditorMode.CREATE) "创建" else "确认") {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        if (editor == TagEditorMode.CREATE) viewModel.createTag(trimmed, category)
                        else editingTag?.let { viewModel.renameTag(it.id, trimmed) }
                    }
                    editor = null
                }
            }
        }
    }
}
