package com.wadop.touchmacro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wadop.touchmacro.ui.list.MacroListScreen
import com.wadop.touchmacro.ui.permission.PermissionOnboardingScreen
import com.wadop.touchmacro.ui.record.RecordScreen
import com.wadop.touchmacro.ui.settings.PlaybackSettingsScreen
import com.wadop.touchmacro.ui.unit.UnitEditorScreen

/** ルート定義。 */
object Routes {
    const val LIST = "list"
    const val RECORD = "record"
    const val UNIT_EDITOR = "unit_editor"
    const val PERMISSIONS = "permissions"
    const val SETTINGS = "settings/{macroId}/{kind}"

    fun settings(macroId: String, kind: String) = "settings/$macroId/$kind"
}

/**
 * アプリのナビゲーション。初回起動時に必須権限が不足していれば権限画面から開始する。
 */
@Composable
fun AppNavigation(startAtPermissions: Boolean) {
    val navController = rememberNavController()
    val start = if (startAtPermissions) Routes.PERMISSIONS else Routes.LIST

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LIST) {
            MacroListScreen(
                onOpenSettings = { item ->
                    navController.navigate(Routes.settings(item.id, item.kind.name))
                },
                onCreateUnit = { navController.navigate(Routes.UNIT_EDITOR) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onOpenRecord = { navController.navigate(Routes.RECORD) },
            )
        }
        composable(Routes.RECORD) {
            RecordScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.UNIT_EDITOR) {
            UnitEditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PERMISSIONS) {
            PermissionOnboardingScreen(onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(Routes.LIST) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            })
        }
        composable(
            route = Routes.SETTINGS,
            arguments = listOf(
                navArgument("macroId") { type = NavType.StringType },
                navArgument("kind") { type = NavType.StringType },
            ),
        ) {
            PlaybackSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
