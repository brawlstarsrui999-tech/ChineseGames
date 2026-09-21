package com.chinesegames.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.screens.DeckDetailScreen
import com.chinesegames.app.ui.screens.DeckListScreen
import com.chinesegames.app.ui.screens.GamesHubScreen
import com.chinesegames.app.ui.screens.MainMenuScreen
import com.chinesegames.app.ui.screens.MatchGameScreen
import com.chinesegames.app.ui.screens.MatchSetupScreen

object Routes {
    const val MENU = "menu"
    const val DECKS = "decks"
    const val DECK_DETAIL = "deck/{deckId}"
    const val GAMES = "games"
    const val MATCH_SETUP = "match_setup"
    const val MATCH_GAME = "match_game/{deckIds}/{pairs}/{mode}"

    fun deck(deckId: Long) = "deck/$deckId"

    fun matchGame(deckIds: List<Long>, pairs: Int, mode: GameMode) =
        "match_game/${deckIds.joinToString("-")}/$pairs/${mode.name}"

    const val DECK_ID_ARG = "deckId"
    const val DECK_IDS_ARG = "deckIds"
    const val PAIRS_ARG = "pairs"
    const val MODE_ARG = "mode"
}

/** Корневой навигационный граф приложения. */
@Composable
fun ChineseGamesRoot(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Общая вью-модель словаря живёт на уровне всего приложения
    val deckViewModel: DeckViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.MENU,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(320)) { it / 5 } + fadeIn(tween(320))
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(280)) { it / 5 } + fadeOut(tween(280))
        }
    ) {

        composable(Routes.MENU) {
            val totalWords by deckViewModel.totalWords.collectAsState()
            val learnedWords by deckViewModel.learnedWords.collectAsState()
            val decks by deckViewModel.decks.collectAsState()
            val games by deckViewModel.gamesPlayed.collectAsState()
            val best by deckViewModel.bestScore.collectAsState()

            MainMenuScreen(
                totalWords = totalWords,
                learnedWords = learnedWords,
                decksCount = decks.size,
                gamesPlayed = games,
                bestScore = best,
                onOpenDecks = { navController.navigate(Routes.DECKS) },
                onOpenGames = { navController.navigate(Routes.GAMES) }
            )
        }

        composable(Routes.DECKS) {
            DeckListScreen(
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onOpenDeck = { deckId -> navController.navigate(Routes.deck(deckId)) }
            )
        }

        composable(
            route = Routes.DECK_DETAIL,
            arguments = listOf(navArgument(Routes.DECK_ID_ARG) { type = NavType.LongType })
        ) { entry ->
            val deckId = entry.arguments?.getLong(Routes.DECK_ID_ARG) ?: 0L
            DeckDetailScreen(
                deckId = deckId,
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onPlayWithDeck = {
                    navController.navigate(Routes.MATCH_SETUP)
                }
            )
        }

        composable(Routes.GAMES) {
            val games by deckViewModel.gamesPlayed.collectAsState()
            val pairs by deckViewModel.pairsFound.collectAsState()
            val accuracy by deckViewModel.averageAccuracy.collectAsState()
            val best by deckViewModel.bestScore.collectAsState()

            GamesHubScreen(
                gamesPlayed = games,
                pairsFound = pairs,
                averageAccuracy = accuracy,
                bestScore = best,
                onBack = { navController.popBackStack() },
                onOpenMatch = { navController.navigate(Routes.MATCH_SETUP) }
            )
        }

        composable(Routes.MATCH_SETUP) {
            MatchSetupScreen(
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onStart = { deckIds, pairs, mode ->
                    navController.navigate(Routes.matchGame(deckIds, pairs, mode))
                }
            )
        }

        composable(
            route = Routes.MATCH_GAME,
            arguments = listOf(
                navArgument(Routes.DECK_IDS_ARG) { type = NavType.StringType },
                navArgument(Routes.PAIRS_ARG) { type = NavType.IntType },
                navArgument(Routes.MODE_ARG) { type = NavType.StringType }
            )
        ) { entry ->
            val rawIds = entry.arguments?.getString(Routes.DECK_IDS_ARG).orEmpty()
            val deckIds = rawIds.split("-")
                .mapNotNull { it.trim().toLongOrNull() }
                .ifEmpty { listOf(-1L) }
            val pairs = entry.arguments?.getInt(Routes.PAIRS_ARG) ?: 10
            val mode = runCatching {
                GameMode.valueOf(entry.arguments?.getString(Routes.MODE_ARG) ?: GameMode.CLASSIC.name)
            }.getOrDefault(GameMode.CLASSIC)

            MatchGameScreen(
                deckIds = deckIds,
                pairs = pairs,
                mode = mode,
                onExit = { navController.popBackStack() },
                onOpenSettings = {
                    navController.popBackStack()
                }
            )
        }
    }
}
