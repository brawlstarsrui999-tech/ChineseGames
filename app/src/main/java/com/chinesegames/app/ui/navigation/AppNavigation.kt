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
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.QuizPresets
import com.chinesegames.app.ui.game.SettingsCodec
import com.chinesegames.app.ui.screens.DeckDetailScreen
import com.chinesegames.app.ui.screens.DeckListScreen
import com.chinesegames.app.ui.screens.FavoritesScreen
import com.chinesegames.app.ui.screens.GameSetupScreen
import com.chinesegames.app.ui.screens.GamesHubScreen
import com.chinesegames.app.ui.screens.MainMenuScreen
import com.chinesegames.app.ui.screens.MatchGameScreen
import com.chinesegames.app.ui.screens.PinyinGameScreen
import com.chinesegames.app.ui.screens.QuizGameScreen
import com.chinesegames.app.ui.screens.SettingsScreen
import com.chinesegames.app.ui.screens.StatsScreen

/**
 * Маршруты приложения. Идентификаторы игр ([GameKind.id]) — часть маршрута,
 * поэтому у идущих в навигации игр их нельзя менять.
 */
object Routes {
    const val MENU = "menu"
    const val DECKS = "decks"
    const val FAVORITES = "favorites"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val GAMES = "games"

    const val DECK_DETAIL = "deck/{deckId}"

    /** Настройка партии: `setup/match`, `setup/bubble?preselect=-1` и т. д. */
    const val GAME_SETUP = "setup/{kind}?preselect={preselect}"

    const val MATCH_GAME = "match_game/{deckIds}/{pairs}/{mode}/{preview}"
    const val QUIZ_GAME = "quiz/{kind}/{deckIds}/{config}"
    const val PINYIN_GAME = "pinyin/{deckIds}/{config}"

    const val DECK_ID_ARG = "deckId"
    const val KIND_ARG = "kind"
    const val PRESELECT_ARG = "preselect"
    const val DECK_IDS_ARG = "deckIds"
    const val PAIRS_ARG = "pairs"
    const val MODE_ARG = "mode"
    const val PREVIEW_ARG = "preview"
    const val CONFIG_ARG = "config"

    fun deck(deckId: Long) = "deck/$deckId"

    fun setup(kind: GameKind, preselect: List<Long> = emptyList()) =
        "setup/${kind.id}?preselect=${preselect.joinToString(",")}"

    fun matchGame(deckIds: List<Long>, pairs: Int, mode: GameMode, previewSeconds: Int) =
        "match_game/${deckIds.joinToString(",")}/$pairs/${mode.name}/$previewSeconds"

    fun quizGame(kind: GameKind, deckIds: List<Long>, token: String) =
        "quiz/${kind.id}/${deckIds.joinToString(",")}/${SettingsCodec.toRoute(token)}"

    fun pinyinGame(deckIds: List<Long>, token: String) =
        "pinyin/${deckIds.joinToString(",")}/${SettingsCodec.toRoute(token)}"
}

/** Разбор списка идентификаторов папок из маршрута. */
private fun parseDeckIds(raw: String?): List<Long> =
    raw.orEmpty()
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }

/** «Избранное» — папка-ярлык со словами из всех папок. */
private const val FAVORITES_ID = -1L

/**
 * Корневой навигационный граф приложения.
 *
 * Контроллер навигации приходит снаружи: при смене темы интерфейс
 * пересобирается заново (`key`), а стек экранов при этом сохраняется.
 */
@Composable
fun ChineseGamesRoot(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

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
                onOpenGames = { navController.navigate(Routes.GAMES) },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.DECKS) {
            val favoritesCount by deckViewModel.favoritesCount.collectAsState()
            val hardWordsCount by deckViewModel.hardWordCount.collectAsState()

            DeckListScreen(
                viewModel = deckViewModel,
                favoritesCount = favoritesCount,
                hardWordsCount = hardWordsCount,
                onBack = { navController.popBackStack() },
                onOpenDeck = { deckId -> navController.navigate(Routes.deck(deckId)) },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                onOpenHardWords = { navController.navigate(Routes.STATS) }
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
                onPlayWithDeck = { id ->
                    navController.navigate(Routes.setup(GameKind.MATCH, listOf(id)))
                }
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onPlay = { kind ->
                    navController.navigate(Routes.setup(kind, listOf(FAVORITES_ID)))
                }
            )
        }

        composable(Routes.STATS) {
            StatsScreen(
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onTrainHardWords = {
                    navController.navigate(Routes.setup(GameKind.SPRINT, listOf(-2L)))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GAMES) {
            val games by deckViewModel.gamesPlayed.collectAsState()
            val pairs by deckViewModel.pairsFound.collectAsState()
            val accuracy by deckViewModel.averageAccuracy.collectAsState()
            val best by deckViewModel.bestScore.collectAsState()
            val favoritesCount by deckViewModel.favoritesCount.collectAsState()

            GamesHubScreen(
                gamesPlayed = games,
                pairsFound = pairs,
                averageAccuracy = accuracy,
                bestScore = best,
                favoritesCount = favoritesCount,
                onBack = { navController.popBackStack() },
                onOpenGame = { kind, preselect ->
                    navController.navigate(Routes.setup(kind, preselect))
                },
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }

        composable(
            route = Routes.GAME_SETUP,
            arguments = listOf(
                navArgument(Routes.KIND_ARG) { type = NavType.StringType },
                navArgument(Routes.PRESELECT_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { entry ->
            val kind = GameKind.fromId(entry.arguments?.getString(Routes.KIND_ARG))
            val preselect = parseDeckIds(entry.arguments?.getString(Routes.PRESELECT_ARG))

            GameSetupScreen(
                kind = kind,
                viewModel = deckViewModel,
                onBack = { navController.popBackStack() },
                onStartMatch = { deckIds, pairs, mode, previewSeconds ->
                    navController.navigate(
                        Routes.matchGame(deckIds, pairs, mode, previewSeconds)
                    )
                },
                onStartQuiz = { quizKind, deckIds, token ->
                    if (quizKind == GameKind.PINYIN) {
                        navController.navigate(Routes.pinyinGame(deckIds, token))
                    } else {
                        navController.navigate(Routes.quizGame(quizKind, deckIds, token))
                    }
                }
            )
        }

        composable(
            route = Routes.MATCH_GAME,
            arguments = listOf(
                navArgument(Routes.DECK_IDS_ARG) { type = NavType.StringType },
                navArgument(Routes.PAIRS_ARG) { type = NavType.IntType },
                navArgument(Routes.MODE_ARG) { type = NavType.StringType },
                navArgument(Routes.PREVIEW_ARG) {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { entry ->
            val deckIds = parseDeckIds(entry.arguments?.getString(Routes.DECK_IDS_ARG))
                .ifEmpty { listOf(FAVORITES_ID) }
            val pairs = entry.arguments?.getInt(Routes.PAIRS_ARG) ?: 10
            val mode = GameMode.fromName(entry.arguments?.getString(Routes.MODE_ARG))
            val preview = entry.arguments?.getInt(Routes.PREVIEW_ARG) ?: 0

            MatchGameScreen(
                deckIds = deckIds,
                pairs = pairs,
                mode = mode,
                previewSeconds = preview,
                onExit = { navController.popBackStack() },
                onOpenSettings = {
                    val kind = if (mode == GameMode.MEMORY) GameKind.MEMORY_GRID else GameKind.MATCH
                    navController.navigate(Routes.setup(kind, deckIds))
                }
            )
        }

        composable(
            route = Routes.QUIZ_GAME,
            arguments = listOf(
                navArgument(Routes.KIND_ARG) { type = NavType.StringType },
                navArgument(Routes.DECK_IDS_ARG) { type = NavType.StringType },
                navArgument(Routes.CONFIG_ARG) { type = NavType.StringType }
            )
        ) { entry ->
            val kind = GameKind.fromId(entry.arguments?.getString(Routes.KIND_ARG))
            val deckIds = parseDeckIds(entry.arguments?.getString(Routes.DECK_IDS_ARG))
                .ifEmpty { listOf(FAVORITES_ID) }
            val token = SettingsCodec.fromRoute(entry.arguments?.getString(Routes.CONFIG_ARG))
            val config = QuizPresets.buildConfig(kind, deckIds, token)

            QuizGameScreen(
                config = config,
                onExit = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.setup(kind, deckIds))
                }
            )
        }

        composable(
            route = Routes.PINYIN_GAME,
            arguments = listOf(
                navArgument(Routes.DECK_IDS_ARG) { type = NavType.StringType },
                navArgument(Routes.CONFIG_ARG) { type = NavType.StringType }
            )
        ) { entry ->
            val deckIds = parseDeckIds(entry.arguments?.getString(Routes.DECK_IDS_ARG))
                .ifEmpty { listOf(FAVORITES_ID) }
            val values = SettingsCodec.decode(
                SettingsCodec.fromRoute(entry.arguments?.getString(Routes.CONFIG_ARG))
            )
            val preset = QuizPresets.of(GameKind.PINYIN)

            PinyinGameScreen(
                deckIds = deckIds,
                questions = SettingsCodec.int(values, SettingsCodec.KEY_QUESTIONS, preset.defaultQuestions),
                secondsPerQuestion = SettingsCodec.int(
                    values, SettingsCodec.KEY_SECONDS, preset.defaultSecondsPerQuestion
                ),
                srsFirst = SettingsCodec.bool(values, SettingsCodec.KEY_SRS, true),
                onExit = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.setup(GameKind.PINYIN, deckIds))
                }
            )
        }
    }
}
