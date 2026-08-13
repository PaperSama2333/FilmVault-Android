package com.papersama.filmvault.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class RollStatus(val label: String) {
    UNSHOT("未拍"),
    SHOOT("拍摄中"),
    WASH("待冲洗"),
    SENT("已送洗"),
    DONE("已完成"),
}

enum class TagCategory(val value: String, val title: String) {
    BRAND("brand", "品牌"),
    TYPE("type", "类型"),
    PROJECT("project", "项目"),
}

enum class LabStatus(val value: String, val label: String) {
    NONE("none", "未送洗"),
    WASH("wash", "待冲洗"),
    SENT("sent", "已送洗"),
}

data class TagItem(
    val id: String,
    val name: String,
    val category: TagCategory,
)

data class Shot(
    val id: String,
    val date: String,
    val count: Int,
    val note: String,
    val samples: List<String>,
)

data class LabRecord(
    val status: LabStatus = LabStatus.NONE,
    val store: String = "",
    val date: String = "",
    val scanned: Boolean = false,
)

data class Roll(
    val id: String,
    val name: String,
    val brand: String,
    val brandName: String,
    val type: String,
    val iso: String,
    val frames: Int,
    val shot: Int,
    val status: RollStatus,
    val archived: Boolean,
    val archivedDate: String,
    val tags: List<String>,
    val tagIds: List<String>,
    val customTags: List<String>,
    val shots: List<Shot>,
    val lab: LabRecord,
)

data class UserProfile(
    val name: String = "胶片爱好者",
    val avatar: String = "",
)

data class AppUiState(
    val rolls: List<Roll> = emptyList(),
    val tags: List<TagItem> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val profile: UserProfile = UserProfile(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
)

data class NewRollInput(
    val name: String,
    val brandName: String,
    val type: String,
    val iso: String,
    val frames: Int,
    val projectTagIds: List<String>,
    val customTags: List<String>,
)

data class ShotInput(
    val date: String,
    val count: Int,
    val note: String,
    val samples: List<String>,
)

fun computeStatus(shot: Int, frames: Int, lab: LabRecord): RollStatus = when {
    lab.status == LabStatus.SENT && lab.scanned -> RollStatus.DONE
    lab.status == LabStatus.SENT -> RollStatus.SENT
    shot >= frames -> RollStatus.WASH
    shot > 0 -> RollStatus.SHOOT
    else -> RollStatus.UNSHOT
}

fun today(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))

fun uid(prefix: String = ""): String =
    "$prefix${System.currentTimeMillis().toString(36)}${(100000..999999).random().toString(36)}"

fun splitTags(value: String): List<String> = value
    .split(',', '，', ' ', '\n', '\t')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()
