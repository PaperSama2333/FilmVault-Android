package com.papersama.filmvault

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.papersama.filmvault.data.AppUiState
import com.papersama.filmvault.data.LabRecord
import com.papersama.filmvault.data.NewRollInput
import com.papersama.filmvault.data.ShotInput
import com.papersama.filmvault.data.TagCategory
import com.papersama.filmvault.reminder.FilmReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilmVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as FilmVaultApplication).database
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { withContext(Dispatchers.IO) { database.loadState() } }
                .onSuccess { _state.value = it }
                .onFailure { showError(it) }
        }
    }

    fun createRoll(input: NewRollInput, onDone: (String) -> Unit = {}) = mutate(onDone) {
        database.createRoll(input)
    }

    fun saveShot(rollId: String, shotId: String?, input: ShotInput, onDone: () -> Unit = {}) =
        mutate(onDone) { database.saveShot(rollId, shotId, input) }

    fun deleteShot(rollId: String, shotId: String) = mutate {
        database.deleteShot(rollId, shotId)
    }

    fun updateLab(rollId: String, lab: LabRecord) = mutate(
        onDone = { _: Unit ->
            if (lab.status != com.papersama.filmvault.data.LabStatus.NONE) {
                FilmReminder.cancel(getApplication(), rollId)
            }
        },
    ) { database.updateLab(rollId, lab) }

    fun markDone(id: String, date: String) {
        val rollName = _state.value.rolls.firstOrNull { it.id == id }?.name.orEmpty()
        mutate(
            onDone = { _: Unit -> FilmReminder.schedule(getApplication(), id, rollName) },
        ) { database.markDone(id, date) }
    }

    fun unarchive(id: String) = mutate(
        onDone = { _: Unit -> FilmReminder.cancel(getApplication(), id) },
    ) { database.unarchive(id) }

    fun deleteRoll(id: String, onDone: () -> Unit = {}) = mutate(onDone = { _: Unit ->
        FilmReminder.cancel(getApplication(), id)
        onDone()
    }) {
        database.deleteRoll(id)
    }

    fun updateRollTags(rollId: String, projectTagIds: List<String>, customTags: List<String>) = mutate {
        database.updateRollTags(rollId, projectTagIds, customTags)
    }

    fun createTag(name: String, category: TagCategory) = mutate {
        database.createTag(name, category)
    }

    fun renameTag(id: String, name: String) = mutate { database.renameTag(id, name) }

    fun deleteTag(id: String) = mutate { database.deleteTag(id) }

    fun updateName(name: String) = mutate { database.updateProfile("name", name.trim()) }

    fun updateAvatar(uri: Uri?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val oldPath = _state.value.profile.avatar
                    if (oldPath.isNotBlank()) runCatching { File(oldPath).delete() }
                    val path = uri?.let { copyUriToPrivateFile(getApplication(), it, "avatar", "avatar-") }.orEmpty()
                    database.updateProfile("avatar", path)
                    database.loadState()
                }
            }.onSuccess { _state.value = it.copy(message = "头像已更新") }
                .onFailure { showError(it) }
        }
    }

    fun importSamples(uris: List<Uri>, onDone: (List<String>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    uris.map { copyUriToPrivateFile(getApplication(), it, "media", "sample-") }
                }
            }.onSuccess(onDone).onFailure(::showError)
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching { withContext(Dispatchers.IO) { database.exportBackup() } }
                .onSuccess {
                    _state.value = _state.value.copy(busy = false)
                    onReady(it)
                }
                .onFailure(::showError)
        }
    }

    fun restoreBackup(raw: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    database.importBackup(raw)
                    database.loadState()
                }
            }.onSuccess { _state.value = it.copy(message = "数据已从备份恢复") }
                .onFailure(::showError)
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    private fun mutate(block: () -> Unit) = mutate({ _: Unit -> }, block)

    private fun <T> mutate(onDone: (T) -> Unit, block: () -> T) {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val result = block()
                    result to database.loadState()
                }
            }.onSuccess { (result, nextState) ->
                _state.value = nextState
                onDone(result)
            }.onFailure(::showError)
        }
    }

    private fun mutate(onDone: () -> Unit, block: () -> Unit) = mutate({ _: Unit -> onDone() }, block)

    private fun showError(error: Throwable) {
        _state.value = _state.value.copy(
            loading = false,
            busy = false,
            message = error.message?.takeIf(String::isNotBlank) ?: "操作失败，请重试",
        )
    }
}

private fun copyUriToPrivateFile(context: Context, uri: Uri, directory: String, prefix: String): String {
    val dir = File(context.filesDir, directory).apply { mkdirs() }
    val extension = context.contentResolver.getType(uri)?.substringAfterLast('/')
        ?.takeIf { it.length in 2..5 } ?: "jpg"
    val destination = File(dir, "$prefix${System.currentTimeMillis()}-${(1000..9999).random()}.$extension")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "无法读取所选图片" }
        destination.outputStream().use(input::copyTo)
    }
    return destination.absolutePath
}
