package com.papersama.filmvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.papersama.filmvault.ui.AddShotScreen
import com.papersama.filmvault.ui.DoneScreen
import com.papersama.filmvault.ui.FilmVaultTheme
import com.papersama.filmvault.ui.Ink
import com.papersama.filmvault.ui.MainTabs
import com.papersama.filmvault.ui.NewRollScreen
import com.papersama.filmvault.ui.RollDetailScreen
import com.papersama.filmvault.ui.SettingsScreen
import com.papersama.filmvault.ui.TagsManageScreen

class MainActivity : ComponentActivity() {
    private external fun nativeMarker(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nativeMarker()
        enableEdgeToEdge()
        setContent {
            FilmVaultTheme {
                val viewModel: FilmVaultViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val backStack = remember { mutableStateListOf<Route>(Route.Main) }
                val snackbar = remember { SnackbarHostState() }

                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbar.showSnackbar(it)
                        viewModel.clearMessage()
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) backStack.removeLastOrNull() else finish()
                        },
                        entryProvider = entryProvider {
                            entry<Route.Main> {
                                MainTabs(
                                    state = state,
                                    viewModel = viewModel,
                                    open = backStack::add,
                                )
                            }
                            entry<Route.NewRoll> {
                                NewRollScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    back = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<Route.RollDetail> { route ->
                                RollDetailScreen(
                                    rollId = route.id,
                                    state = state,
                                    viewModel = viewModel,
                                    back = { backStack.removeLastOrNull() },
                                    open = backStack::add,
                                )
                            }
                            entry<Route.AddShot> { route ->
                                AddShotScreen(
                                    rollId = route.rollId,
                                    shotId = route.shotId,
                                    state = state,
                                    viewModel = viewModel,
                                    back = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<Route.Done> {
                                DoneScreen(
                                    state = state,
                                    back = { backStack.removeLastOrNull() },
                                    open = backStack::add,
                                )
                            }
                            entry<Route.TagsManage> {
                                TagsManageScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    back = { backStack.removeLastOrNull() },
                                )
                            }
                            entry<Route.Settings> {
                                SettingsScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    back = { backStack.removeLastOrNull() },
                                )
                            }
                        },
                    )
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                    if (state.loading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = Ink)
                    }
                }
            }
        }
    }

    companion object {
        init {
            System.loadLibrary("filmvault")
        }
    }
}

sealed interface Route {
    data object Main : Route
    data object NewRoll : Route
    data class RollDetail(val id: String) : Route
    data class AddShot(val rollId: String, val shotId: String? = null) : Route
    data object Done : Route
    data object TagsManage : Route
    data object Settings : Route
}
