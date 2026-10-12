package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.Note
import com.example.ui.screens.ExportSheet
import com.example.ui.screens.FoldersTagsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NoteHistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.AmoledNotesTheme
import com.example.viewmodel.NotesViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            AmoledNotesTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmoledNotesApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AmoledNotesApp(viewModel: NotesViewModel) {
    val navController = rememberNavController()
    var exportTargetNote by remember { mutableStateOf<Note?>(null) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { noteId ->
                    navController.navigate("editor/$noteId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToFoldersTags = {
                    navController.navigate("folders_tags")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onRequireUnlock = { noteId ->
                    navController.navigate("lock/$noteId")
                }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId ->
                    navController.navigate("editor/$noteId")
                },
                onRequireUnlock = { noteId ->
                    navController.navigate("lock/$noteId")
                }
            )
        }

        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteEditorScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { id ->
                    navController.navigate("history/$id")
                },
                onOpenExport = { note ->
                    exportTargetNote = note
                }
            )
        }

        composable(
            route = "history/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            NoteHistoryScreen(
                noteId = noteId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRestored = { navController.popBackStack() }
            )
        }

        composable("folders_tags") {
            FoldersTagsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onFilterByFolder = { folderName ->
                    viewModel.setSelectedFolder(folderName)
                    navController.popBackStack()
                },
                onFilterByTag = { tagName ->
                    viewModel.setSelectedTag(tagName)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "lock/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            LockScreen(
                noteId = noteId,
                viewModel = viewModel,
                onSuccess = {
                    navController.popBackStack()
                    navController.navigate("editor/$noteId")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    // Export bottom sheet
    exportTargetNote?.let { note ->
        ExportSheet(
            note = note,
            onDismiss = { exportTargetNote = null }
        )
    }
}
