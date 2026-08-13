package com.papersama.filmvault.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.papersama.filmvault.FilmVaultViewModel
import com.papersama.filmvault.Route
import com.papersama.filmvault.data.AppUiState
import com.papersama.filmvault.data.Roll
import com.papersama.filmvault.data.RollStatus
import com.papersama.filmvault.data.TagCategory

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("库存", Icons.Outlined.Movie),
    TabItem("统计", Icons.Outlined.BarChart),
    TabItem("标签", Icons.Outlined.Label),
    TabItem("我的", Icons.Outlined.Person),
)

@Composable
fun MainTabs(
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    open: (Route) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = Surface,
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink,
                            selectedTextColor = Ink,
                            indicatorColor = KodakYellow,
                            unselectedIconColor = Sub,
                            unselectedTextColor = Sub,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeTab(state, open)
                1 -> StatsTab(state)
                2 -> TagsTab(state, open)
                else -> MineTab(state, viewModel, open) { selectedTab = it }
            }
        }
    }
}

@Composable
private fun HomeTab(state: AppUiState, open: (Route) -> Unit) {
    var filter by remember { mutableStateOf<RollStatus?>(null) }
    val visible = state.rolls.filter { !it.archived && (filter == null || it.status == filter) }
    val filters = listOf(
        null to "全部",
        RollStatus.UNSHOT to "未拍",
        RollStatus.SHOOT to "拍摄中",
        RollStatus.WASH to "待冲洗",
        RollStatus.DONE to "已完成",
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("胶片匣", style = MaterialTheme.typography.displaySmall)
                    Text("我的胶卷库存", style = MaterialTheme.typography.bodyLarge, color = Sub)
                }
                IconButton(
                    onClick = { open(Route.NewRoll) },
                    modifier = Modifier.size(40.dp).background(KodakYellow, CircleShape),
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { (key, label) ->
                    TagChip(label, selected = filter == key) { filter = key }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    "该筛选下暂无胶卷",
                    color = Sub,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
            }
        } else {
            items(visible, key = Roll::id) { roll ->
                FilmCard(roll, onClick = { open(Route.RollDetail(roll.id)) })
            }
        }
        if (filter == null) {
            item {
                ArchiveCard(state.rolls.count(Roll::archived)) { open(Route.Done) }
            }
        }
    }
}

@Composable
private fun StatsTab(state: AppUiState) {
    val total = state.rolls.size
    val shooting = state.rolls.count { it.status == RollStatus.SHOOT }
    val done = state.rolls.count { it.status == RollStatus.DONE }
    val wash = state.rolls.count { it.status == RollStatus.WASH }
    val byMonth = state.rolls.flatMap(Roll::shots)
        .groupingBy { it.date.take(7) }
        .fold(0) { totalCount, shot -> totalCount + shot.count }
        .toSortedMap()
    val max = byMonth.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val brands = state.rolls.groupingBy { it.brandName }.eachCount()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("统计", style = MaterialTheme.typography.displaySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(total to "总卷数", shooting to "拍摄中", done to "已完成", wash to "待冲洗").forEach {
                StatCard(it.first, it.second, Modifier.weight(1f))
            }
        }
        Panel(title = "按月拍摄量") {
            if (byMonth.isEmpty()) {
                Text("暂无拍摄数据", color = Weak)
            } else {
                Row(
                    Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    byMonth.forEach { (month, value) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                Modifier.width(24.dp)
                                    .height((14 + value * 106 / max).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(KodakYellow),
                            )
                            Text(month.takeLast(5).replace('.', '/'), fontSize = 10.sp, color = Weak)
                        }
                    }
                }
            }
        }
        Panel(title = "品牌分布") {
            brands.forEach { (brand, count) ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).background(brandColor(brand), CircleShape))
                        Text(brand)
                    }
                    Text("${count}卷", color = Sub, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsTab(state: AppUiState, open: (Route) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("标签", style = MaterialTheme.typography.displaySmall)
            TextButton(onClick = { open(Route.TagsManage) }) { Text("管理", color = KodakYellow) }
        }
        TagCategory.entries.forEach { category ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category.title, color = Sub, fontWeight = FontWeight.SemiBold)
                val tags = state.tags.filter { it.category == category }
                if (tags.isEmpty()) Text("暂无标签", color = Weak)
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { TagChip("${it.name} ${state.tagCounts[it.id] ?: 0}") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MineTab(
    state: AppUiState,
    viewModel: FilmVaultViewModel,
    open: (Route) -> Unit,
    selectTab: (Int) -> Unit,
) {
    var nameEditor by remember { mutableStateOf(false) }
    var avatarMenu by remember { mutableStateOf(false) }
    var name by remember(state.profile.name) { mutableStateOf(state.profile.name) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::updateAvatar)
    }
    val shotCount = state.rolls.sumOf(Roll::shot)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.displaySmall)
        Row(
            Modifier.fillMaxWidth().height(96.dp).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).clickable { avatarMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                if (state.profile.avatar.isBlank()) {
                    Box(Modifier.fillMaxSize().background(KodakYellow), contentAlignment = Alignment.Center) {
                        Text("头像", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    LocalFileImage(state.profile.avatar, Modifier.fillMaxSize())
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.clickable { nameEditor = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(state.profile.name, style = MaterialTheme.typography.titleLarge)
                    Icon(Icons.Outlined.Edit, contentDescription = "修改昵称", tint = Sub, modifier = Modifier.size(16.dp))
                }
                Text("已记录 ${state.rolls.size} 卷 · $shotCount 张", color = Sub)
            }
        }
        listOf(
            Triple("我的胶卷", { selectTab(0) }, 0),
            Triple("冲洗记录", { selectTab(1) }, 0),
            Triple("标签管理", { selectTab(2) }, 0),
            Triple("设置", { open(Route.Settings) }, 0),
        ).forEach { item ->
            Row(
                Modifier.fillMaxWidth().height(52.dp).border(1.dp, Line, RoundedCornerShape(12.dp))
                    .clickable(onClick = item.second).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.first)
                Text("›", fontSize = 20.sp, color = Weak)
            }
        }
    }

    if (avatarMenu) {
        ModalBottomSheet(onDismissRequest = { avatarMenu = false }, containerColor = Surface) {
            Text("头像", modifier = Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = {
                    avatarMenu = false
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("切换头像", color = Ink) }
            TextButton(
                onClick = { avatarMenu = false; viewModel.updateAvatar(null) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("使用默认头像", color = Ink) }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (nameEditor) {
        ModalBottomSheet(onDismissRequest = { nameEditor = false }, containerColor = Surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("修改昵称", style = MaterialTheme.typography.titleLarge)
                FilmTextField(name, { name = it }, placeholder = "输入昵称")
                Button(
                    onClick = {
                        if (name.isNotBlank()) viewModel.updateName(name)
                        nameEditor = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KodakYellow, contentColor = Ink),
                ) { Text("保存", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.height(72.dp).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Sub)
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}
