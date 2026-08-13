package com.papersama.filmvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papersama.filmvault.FilmVaultViewModel
import com.papersama.filmvault.Route
import com.papersama.filmvault.data.AppUiState
import com.papersama.filmvault.data.LabRecord
import com.papersama.filmvault.data.LabStatus
import com.papersama.filmvault.data.Roll
import com.papersama.filmvault.data.RollStatus
import com.papersama.filmvault.data.TagCategory
import com.papersama.filmvault.data.splitTags
import com.papersama.filmvault.data.today

private enum class DetailSheet { ACTIONS, LAB, TAGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RollDetailScreen(
    rollId: String,
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    back: () -> Unit,
    open: (Route) -> Unit,
) {
    val roll = state.rolls.firstOrNull { it.id == rollId }
    var sheet by remember { mutableStateOf<DetailSheet?>(null) }
    if (roll == null) {
        ScreenScaffold("胶卷详情", back) {
            Text("胶卷不存在或已删除", color = Sub, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        return
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = back, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text(
                roll.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { sheet = DetailSheet.ACTIONS }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = "更多操作")
            }
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { RollSummary(roll, onTags = { sheet = DetailSheet.TAGS }) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("拍摄日期", style = MaterialTheme.typography.headlineSmall)
                    Button(
                        onClick = { open(Route.AddShot(roll.id)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KodakYellow, contentColor = Ink),
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("添加")
                    }
                }
            }
            if (roll.shots.isEmpty()) {
                item {
                    Text(
                        "还没有拍摄记录，点击「+ 添加」记录第一次拍摄吧。",
                        color = Sub,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(roll.shots.sortedBy { it.date }, key = { it.id }) { shot ->
                    Column(
                        Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { open(Route.AddShot(roll.id, shot.id)) }.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(shot.date, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${shot.count}张", color = Sub)
                                Text(
                                    "删除",
                                    color = Danger,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.clickable { viewModel.deleteShot(roll.id, shot.id) },
                                )
                            }
                        }
                        if (shot.note.isNotBlank()) Text(shot.note, color = TagInk, lineHeight = 20.sp)
                        if (shot.samples.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(40.dp).background(Gray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text("暂无样张", color = Weak, style = MaterialTheme.typography.labelMedium) }
                        } else {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                shot.samples.forEach { LocalFileImage(it, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))) }
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("冲洗 / 扫描", style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = { sheet = DetailSheet.LAB }) { Text("编辑", color = KodakYellow) }
                    }
                    Text(labSummary(roll.lab), color = TagInk)
                }
            }
        }
    }

    when (sheet) {
        DetailSheet.ACTIONS -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Surface) {
            Text(roll.name, color = Sub, modifier = Modifier.fillMaxWidth().padding(12.dp), textAlign = TextAlign.Center)
            if (roll.archived) {
                SheetAction("移出收纳") { viewModel.unarchive(roll.id); sheet = null }
            } else {
                SheetAction("标记为完成") { viewModel.markDone(roll.id, today()); sheet = null }
            }
            SheetAction("删除胶卷", Danger) {
                sheet = null
                viewModel.deleteRoll(roll.id, back)
            }
            Spacer(Modifier.height(24.dp))
        }
        DetailSheet.LAB -> LabSheet(roll.lab, dismiss = { sheet = null }) {
            viewModel.updateLab(roll.id, it)
            sheet = null
        }
        DetailSheet.TAGS -> TagSheet(
            roll = roll,
            state = state,
            dismiss = { sheet = null },
        ) { projectIds, custom ->
            viewModel.updateRollTags(roll.id, projectIds, custom)
            sheet = null
        }
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RollSummary(roll: Roll, onTags: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(56.dp).background(brandColor(roll.brandName), RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(roll.name, style = MaterialTheme.typography.titleLarge)
                Text("${roll.iso} · ${roll.frames}张 · ${roll.type}", color = Sub)
            }
            Text(roll.status.label, color = statusColor(roll.status), fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilmProgress(roll.shot, roll.frames, Modifier.weight(1f))
            Text("${roll.shot}/${roll.frames}", color = Sub, style = MaterialTheme.typography.labelMedium)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTags),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (roll.tags.isEmpty()) Text("+ 添加标签", color = KodakYellow)
            else roll.tags.forEach { TagChip(it) }
        }
    }
}

@Composable
private fun SheetAction(label: String, color: androidx.compose.ui.graphics.Color = Ink, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(label, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LabSheet(initial: LabRecord, dismiss: () -> Unit, save: (LabRecord) -> Unit) {
    var status by remember(initial) { mutableStateOf(initial.status) }
    var store by remember(initial) { mutableStateOf(initial.store) }
    var date by remember(initial) { mutableStateOf(initial.date.takeUnless { it == "—" }.orEmpty()) }
    var scanned by remember(initial) { mutableStateOf(initial.scanned) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Surface) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("冲洗 / 扫描", style = MaterialTheme.typography.titleLarge)
            Text("冲洗状态", color = Sub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(LabStatus.WASH, LabStatus.SENT).forEach { item ->
                    TagChip(item.label, status == item) { status = item }
                }
            }
            FilmTextField(store, { store = it }, label = "冲洗店", placeholder = "如：某某胶片冲洗")
            DateField(date.ifBlank(::today), { date = it }, label = "送洗日期")
            TagChip(if (scanned) "已扫描" else "未扫描", scanned) { scanned = !scanned }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { save(LabRecord()) }, modifier = Modifier.weight(1f)) { Text("清除记录", color = Danger) }
                Button(
                    onClick = { save(LabRecord(status, store.trim(), date, scanned)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KodakYellow, contentColor = Ink),
                ) { Text("保存") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagSheet(
    roll: Roll,
    state: AppUiState,
    dismiss: () -> Unit,
    save: (List<String>, List<String>) -> Unit,
) {
    val projects = state.tags.filter { it.category == TagCategory.PROJECT }
    var selected by remember(roll) { mutableStateOf(roll.tagIds.intersect(projects.map { it.id }.toSet()).toList()) }
    var custom by remember(roll) { mutableStateOf(roll.customTags.joinToString(", ")) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Surface) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("编辑标签", style = MaterialTheme.typography.titleLarge)
            Text("项目标签", color = Sub)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (projects.isEmpty()) Text("暂无项目标签，可在「标签 - 管理」中新建", color = Weak)
                projects.forEach { tag ->
                    TagChip(tag.name, tag.id in selected) {
                        selected = if (tag.id in selected) selected - tag.id else selected + tag.id
                    }
                }
            }
            FilmTextField(custom, { custom = it }, label = "自定义标签", placeholder = "如 测试卷, 第一次")
            PrimaryButton("保存") { save(selected, splitTags(custom)) }
        }
    }
}

private fun labSummary(lab: LabRecord): String = when (lab.status) {
    LabStatus.SENT -> "已送洗 · ${lab.store.ifBlank { "未填写" }}${if (lab.scanned) " · 已扫描" else ""}"
    else -> "待冲洗 · ${lab.store.ifBlank { "未填写" }}"
}

@Composable
fun DoneScreen(state: AppUiState, back: () -> Unit, open: (Route) -> Unit) {
    val archived = state.rolls.filter(Roll::archived)
    var filter by remember { mutableStateOf<RollStatus?>(null) }
    val visible = archived.filter { filter == null || it.status == filter }
    ScreenScaffold("已拍摄完成的胶卷", back) {
        if (archived.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("🎞", style = MaterialTheme.typography.displaySmall)
                Text("还没有已完成的胶卷", style = MaterialTheme.typography.titleLarge)
                Text("把拍完的胶卷标记为完成，方便统一归档与冲洗。", color = Sub, textAlign = TextAlign.Center)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(73.dp).background(ArchiveBg, RoundedCornerShape(12.dp))
                    .border(1.dp, ArchiveLine, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(archived.size.toString(), style = MaterialTheme.typography.displaySmall)
                Column {
                    Text("卷已标记完成", fontWeight = FontWeight.Medium)
                    Text(
                        "含 ${archived.count { it.status == RollStatus.DONE }} 卷已完成 · ${archived.count { it.status == RollStatus.WASH }} 卷待冲洗",
                        style = MaterialTheme.typography.labelMedium,
                        color = Sub,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "全部", RollStatus.DONE to "已完成", RollStatus.WASH to "待冲洗").forEach {
                    TagChip(it.second, filter == it.first) { filter = it.first }
                }
            }
            if (visible.isEmpty()) Text("该筛选下暂无胶卷", color = Sub, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            visible.forEach { roll ->
                FilmCard(roll, onClick = { open(Route.RollDetail(roll.id)) }, archived = true)
            }
        }
    }
}
