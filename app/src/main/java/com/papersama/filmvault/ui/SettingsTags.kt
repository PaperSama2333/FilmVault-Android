package com.papersama.filmvault.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.papersama.filmvault.BuildConfig
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

    fun openNotificationSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
        )
    }

    ScreenScaffold("设置", back) {
        Text("权限与提醒", color = Sub, fontWeight = FontWeight.SemiBold)
        PermissionCard(
            title = "通知权限",
            description = "仅用于你主动开启的冲洗提醒，不推送广告。",
            status = if (notificationAllowed) "已允许" else "未允许",
            statusColor = if (notificationAllowed) Success else Danger,
            action = if (notificationAllowed) "系统设置" else "开启",
        ) {
            if (notificationAllowed) openNotificationSettings()
            else notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Column(
            Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("冲洗提醒", fontWeight = FontWeight.Medium)
                    Text("胶卷标记完成后，在指定天数后提醒冲洗。", color = Sub, style = MaterialTheme.typography.labelMedium)
                }
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
            }
            Text("提醒时间", color = Sub, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7).forEach { days ->
                    TagChip("${days}天后", reminderDays == days) {
                        reminderDays = days
                        ReminderPreferences.setDelayDays(context, days)
                    }
                }
            }
            if (notificationAllowed) {
                TextButton(onClick = { FilmReminder.sendTest(context) }) {
                    Text("发送测试提醒", color = Ink)
                }
            }
        }

        Text("隐私与系统访问", color = Sub, fontWeight = FontWeight.SemiBold)
        PrivacyAccessRow("照片", "系统照片选择器", "只读取你明确选择的图片，无需相册权限")
        PrivacyAccessRow("相机", "系统相机", "拍摄后直接保存到 App 私有目录，无需相机权限")
        PrivacyAccessRow("文件", "系统文件选择器", "仅在导入或导出备份时访问你选择的位置")
        PrivacyAccessRow("网络", "未使用", "App 不申请联网权限，数据默认只保存在本机")

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
        Text(
            "胶片匣 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = Weak,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth(),
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
private fun PermissionCard(
    title: String,
    description: String,
    status: String,
    statusColor: androidx.compose.ui.graphics.Color,
    action: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)),
        colors = ListItemDefaults.colors(containerColor = Surface),
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(status, color = statusColor, style = MaterialTheme.typography.labelMedium)
            }
        },
        supportingContent = { Text(description, color = Sub, style = MaterialTheme.typography.labelMedium) },
        trailingContent = { TextButton(onClick = onClick) { Text(action, color = Ink) } },
    )
}

@Composable
private fun PrivacyAccessRow(title: String, access: String, description: String) {
    ListItem(
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)),
        colors = ListItemDefaults.colors(containerColor = Surface),
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(description, color = Sub, style = MaterialTheme.typography.labelMedium) },
        trailingContent = { Text(access, color = Success, style = MaterialTheme.typography.labelMedium) },
    )
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    button: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)),
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
