package com.wadop.touchmacro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wadop.touchmacro.ui.navigation.AppNavigation
import com.wadop.touchmacro.ui.theme.TouchMacroTheme
import com.wadop.touchmacro.util.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint

/**
 * 単一 Activity。Compose でUIを構築する。
 * 初回起動時に必須権限が不足していれば、権限案内画面から開始する。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val needsPermission = !PermissionChecker.hasRequiredForPlayback(this)

        setContent {
            TouchMacroTheme {
                AppNavigation(startAtPermissions = needsPermission)
            }
        }
    }
}
