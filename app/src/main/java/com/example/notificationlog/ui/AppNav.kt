package com.example.notificationlog.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.notificationlog.App
import com.example.notificationlog.ui.chat.ChatScreen
import com.example.notificationlog.ui.home.ConversationListScreen
import com.example.notificationlog.ui.llm.LlmSetupScreen
import com.example.notificationlog.ui.selfcheck.SelfCheckOnboardingScreen
import com.example.notificationlog.ui.selfcheck.SelfCheckScreen
import com.example.notificationlog.ui.settings.SettingsScreen
import com.example.notificationlog.ui.trend.TrendReportScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{key}/{title}"
    const val SELF_CHECK = "selfcheck?key={key}&title={title}"
    const val SELF_CHECK_ONBOARDING = "selfcheck_onboarding"
    const val TREND = "trend"
    const val LLM_SETUP = "llm_setup"

    fun chat(key: String, title: String): String =
        "chat/${Uri.encode(key)}/${Uri.encode(title)}"

    fun selfCheck(key: String = "", title: String = ""): String =
        "selfcheck?key=${Uri.encode(key)}&title=${Uri.encode(title)}"
}

@Composable
fun AppNav(app: App) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            ConversationListScreen(
                app = app,
                onOpenConversation = { convo ->
                    navController.navigate(Routes.chat(convo.conversationKey, convo.conversationTitle))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenSelfCheck = { navController.navigate(Routes.selfCheck()) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("key") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key")?.let(Uri::decode).orEmpty()
            val title = backStackEntry.arguments?.getString("title")?.let(Uri::decode).orEmpty()
            ChatScreen(
                app = app,
                conversationKey = key,
                title = title,
                onBack = { navController.popBackStack() },
                onOpenSelfCheck = { navController.navigate(Routes.selfCheck(key, title)) }
            )
        }

        composable(
            route = Routes.SELF_CHECK,
            arguments = listOf(
                navArgument("key") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val key = backStackEntry.arguments?.getString("key")?.let(Uri::decode).orEmpty()
            val title = backStackEntry.arguments?.getString("title")?.let(Uri::decode).orEmpty()
            SelfCheckScreen(
                app = app,
                conversationKey = key,
                title = title,
                onBack = { navController.popBackStack() },
                onOpenLlmSetup = { navController.navigate(Routes.LLM_SETUP) }
            )
        }

        composable(Routes.SELF_CHECK_ONBOARDING) {
            SelfCheckOnboardingScreen(app = app, onBack = { navController.popBackStack() })
        }

        composable(Routes.TREND) {
            TrendReportScreen(app = app, onBack = { navController.popBackStack() })
        }

        composable(Routes.LLM_SETUP) {
            LlmSetupScreen(app = app, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onOpenSelfCheckOnboarding = { navController.navigate(Routes.SELF_CHECK_ONBOARDING) },
                onOpenTrend = { navController.navigate(Routes.TREND) },
                onOpenLlmSetup = { navController.navigate(Routes.LLM_SETUP) }
            )
        }
    }
}
