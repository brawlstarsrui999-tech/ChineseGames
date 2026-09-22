package com.chinesegames.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.chinesegames.app.ui.DeckViewModel
import com.chinesegames.app.ui.components.MainBottomBar
import com.chinesegames.app.ui.components.MainTab
import com.chinesegames.app.ui.game.GameKind
import com.chinesegames.app.ui.game.GameMode
import com.chinesegames.app.ui.game.QuizPresets
import com.chinesegames.app.ui.game.SettingsCodec
import com.chinesegames.app.ui.screens.DeckDetailScreen
import com.chinesegames.app.ui.screens.DeckListScreen
import com.chinesegames.app.ui.screens.FavoritesScreen
import com.chinesegames.app.ui.screens.GameSetupScreen
import com.chinesegames.app.ui.screens.GamesHubScreen
import com.chinesegames.app.ui.screens.HskGameScreen
import com.chinesegames.app.ui.screens.HskGroupScreen
import com.chinesegames.app.ui.screens.HskLevelScreen
import com.chinesegames.app.ui.screens.HskTopicScreen
import com.chinesegames.app.ui.screens.MainMenuScreen
import com.chinesegames.app.ui.screens.MatchGameScreen
import com.chinesegames.app.ui.screens.PinyinGameScreen
import com.chinesegames.app.ui.screens.QuizGameScreen
import com.chinesegames.app.ui.screens.SentenceGameScreen
import com.chinesegames.app.ui.screens.SentenceLevelScreen
import com.chinesegames.app.ui.screens.SentenceTopicScreen
import com.chinesegames.app.ui.screens.SettingsScreen
import com.chinesegames.app.ui.screens.StatsScreen
import com.chinesegames.app.ui.screens.StudyHomeScreen
import com.chinesegames.app.ui.study.StudyViewModel

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
    const val STUDY = "study"

    const val DECK_DETAIL = "deck/{deckId}"

    /** Настройка партии: `setup/match`, `setup/bubble?preselect=-1` и т. д. */
    const val GAME_SETUP = "setup/{kind}?preselect={preselect}"

    const val MATCH_GAME = "match_game/{deckIds}/{pairs}/{mode}/{preview}"
    const val QUIZ_GAME = "quiz/{kind}/{deckIds}/{config}"
    const val PINYIN_GAME = "pinyin/{deckIds}/{config}"

    /* ---------------------- Поэтапное изучение (HSK) ---------------------- */

    const val HSK_LEVEL = "hsk_level/{level}"
    const val HSK_TOPIC = "hsk_topic/{level}/{topicId}"
    const val HSK_GROUP = "hsk_group/{level}/{topicId}/{groupIndex}"
    const val HSK_GAME = "hsk_game/{level}/{topicId}/{groupIndex}/{gameId}"
    const val HSK_EXAM = "hsk_exam/{level}"
    const val HSK_SENTENCES = "hsk_sentences/{level}"
    const val HSK_SENTENCE_TOPIC = "hsk_sentence_topic/{level}/{topicId}"
    const val HSK_SENTENCE_GAME = "hsk_sentence_game/{level}/{topicId}/{gameId}"

    const val DECK_ID_ARG = "deckId"
    const val KIND_ARG = "kind"
    const val PRESELECT_ARG = "preselect"
    const val DECK_IDS_ARG = "deckIds"
    const val PAIRS_ARG = "pairs"
    const val MODE_ARG = "mode"
    const val PREVIEW_ARG = "preview"
    const val CONFIG_ARG = "config"
    const val LEVEL_ARG = "level"
    const val TOPIC_ID_ARG = "topicId"
    const val GROUP_INDEX_ARG = "groupIndex"
    const val GAME_ID_ARG = "gameId"

    fun deck(deckId: Long) = "deck/$deckId"

    fun setup(kind: GameKind, preselect: List<Long> = emptyList()) =
        // без предвыбранных папок параметр не пишем вовсе: пустое значение
        // в маршруте не нужно и может смущать Navigation
        if (preselect.isEmpty()) "setup/${kind.id}"
        else "setup/${kind.id}?preselect=${preselect.joinToString(",")}"

    fun matchGame(deckIds: List<Long>, pairs: Int, mode: GameMode, previewSeconds: Int) =
        "match_game/${deckIds.joinToString(",")}/$pairs/${mode.name}/$previewSeconds"

    fun quizGame(kind: GameKind, deckIds: List<Long>, token: String) =
        "quiz/${kind.id}/${deckIds.joinToString(",")}/${SettingsCodec.toRoute(token)}"

    fun pinyinGame(deckIds: List<Long>, token: String) =
        "pinyin/${deckIds.joinToString(",")}/${SettingsCodec.toRoute(token)}"

    fun hskLevel(level: Int) = "hsk_level/$level"

    fun hskTopic(level: Int, topicId: String) = "hsk_topic/$level/$topicId"

    fun hskGroup(level: Int, topicId: String, groupIndex: Int) =
        "hsk_group/$level/$topicId/$groupIndex"

    fun hskGame(level: Int, topicId: String, groupIndex: Int, gameId: String) =
        "hsk_game/$level/$topicId/$groupIndex/$gameId"

    fun hskExam(level: Int) = "hsk_exam/$level"

    fun hskSentences(level: Int) = "hsk_sentences/$level"

    fun hskSentenceTopic(level: Int, topicId: String) = "hsk_sentence_topic/$level/$topicId"

    fun hskSentenceGame(level: Int, topicId: String, gameId: String) =
        "hsk_sentence_game/$level/$topicId/$gameId"
}

/** Разбор списка идентификаторов папок из маршрута. */
private fun parseDeckIds(raw: String?): List<Long> =
    raw.orEmpty()
        .split(",")
        .mapNotNull { it.trim().toLongOrNull() }

/** «Избранное» — папка-ярлык со словами из всех папок. */
private const val FAVORITES_ID = -1L

/** Переход на вкладку нижней панели: один экземпляр в стеке, состояние сохраняется. */
private fun NavHostController.openTab(tab: MainTab) {
    if (currentDestination?.route == tab.route) return
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Корневой навигационный граф приложения.
 *
 * Контроллер навигации приходит снаружи: при смене темы интерфейс
 * пересобирается заново (`key`), а стек экранов при этом сохраняется.
 *
 * Все основные разделы (главная, словарь, игры, курс, прогресс, настройки)
 * живут в нижней навигационной панели; она скрыта в партиях и настройке игр.
 */
@Composable
fun ChineseGamesRoot(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    // Общие вью-модели живут на уровне всего приложения: они нужны на разных
    // экранах сразу (словарь — в играх и в курсе, курс — на всех экранах HSK).
    val deckViewModel: DeckViewModel = viewModel()
    val studyViewModel: StudyViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = MainTab.fromRoute(currentRoute)

    Scaffold(
        modifier = modifier,
        // фон окна тёмно-фиолетовый, панели сами рисуют свой фон
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                MainBottomBar(
                    current = currentTab,
                    onSelect = { tab -> navController.openTab(tab) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MENU,
            modifier = Modifier
                .padding(innerPadding)
                // экраны сами добавляют navigationBarsPadding — гасим их здесь,
                // чтобы не получалось двойного отступа под нижней панелью
                .consumeWindowInsets(innerPadding),
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
                val games by deckViewModel.gamesPlayed.collectAsState()
                val best by deckViewModel.bestScore.collectAsState()

                MainMenuScreen(
                    totalWords = totalWords,
                    learnedWords = learnedWords,
                    gamesPlayed = games,
                    bestScore = best
                )
            }

            composable(Routes.DECKS) {
                val favoritesCount by deckViewModel.favoritesCount.collectAsState()
                val hardWordsCount by deckViewModel.hardWordCount.collectAsState()

                DeckListScreen(
                    viewModel = deckViewModel,
                    favoritesCount = favoritesCount,
                    hardWordsCount = hardWordsCount,
                    onOpenDeck = { deckId -> navController.navigate(Routes.deck(deckId)) },
                    onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                    onOpenHardWords = { navController.openTab(MainTab.STATS) }
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
                    onTrainHardWords = {
                        navController.navigate(Routes.setup(GameKind.SPRINT, listOf(-2L)))
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = deckViewModel)
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
                    onOpenGame = { kind, preselect ->
                        navController.navigate(Routes.setup(kind, preselect))
                    },
                    onOpenStats = { navController.openTab(MainTab.STATS) }
                )
            }

            /* ---------------------- Поэтапное изучение --------------------- */

            composable(Routes.STUDY) {
                StudyHomeScreen(
                    viewModel = studyViewModel,
                    onOpenLevel = { level ->
                        navController.navigate(Routes.hskLevel(level))
                    },
                    onOpenSentenceLevel = { level ->
                        navController.navigate(Routes.hskSentences(level))
                    }
                )
            }

            composable(
                route = Routes.HSK_LEVEL,
                arguments = listOf(navArgument(Routes.LEVEL_ARG) { type = NavType.IntType })
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                HskLevelScreen(
                    level = level,
                    viewModel = studyViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenTopic = { topicId ->
                        navController.navigate(Routes.hskTopic(level, topicId))
                    },
                    onOpenExam = { navController.navigate(Routes.hskExam(level)) }
                )
            }

            composable(
                route = Routes.HSK_TOPIC,
                arguments = listOf(
                    navArgument(Routes.LEVEL_ARG) { type = NavType.IntType },
                    navArgument(Routes.TOPIC_ID_ARG) { type = NavType.StringType }
                )
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                val topicId = entry.arguments?.getString(Routes.TOPIC_ID_ARG).orEmpty()
                HskTopicScreen(
                    level = level,
                    topicId = topicId,
                    viewModel = studyViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenGroup = { groupIndex ->
                        navController.navigate(Routes.hskGroup(level, topicId, groupIndex))
                    }
                )
            }

            composable(
                route = Routes.HSK_GROUP,
                arguments = listOf(
                    navArgument(Routes.LEVEL_ARG) { type = NavType.IntType },
                    navArgument(Routes.TOPIC_ID_ARG) { type = NavType.StringType },
                    navArgument(Routes.GROUP_INDEX_ARG) { type = NavType.IntType }
                )
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                val topicId = entry.arguments?.getString(Routes.TOPIC_ID_ARG).orEmpty()
                val groupIndex = entry.arguments?.getInt(Routes.GROUP_INDEX_ARG) ?: 0
                HskGroupScreen(
                    level = level,
                    topicId = topicId,
                    groupIndex = groupIndex,
                    viewModel = studyViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenGame = { gameId ->
                        navController.navigate(
                            Routes.hskGame(level, topicId, groupIndex, gameId)
                        )
                    }
                )
            }

            composable(
                route = Routes.HSK_GAME,
                arguments = listOf(
                    navArgument(Routes.LEVEL_ARG) { type = NavType.IntType },
                    navArgument(Routes.TOPIC_ID_ARG) { type = NavType.StringType },
                    navArgument(Routes.GROUP_INDEX_ARG) { type = NavType.IntType },
                    navArgument(Routes.GAME_ID_ARG) { type = NavType.StringType }
                )
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                val topicId = entry.arguments?.getString(Routes.TOPIC_ID_ARG).orEmpty()
                val groupIndex = entry.arguments?.getInt(Routes.GROUP_INDEX_ARG) ?: 0
                val gameId = entry.arguments?.getString(Routes.GAME_ID_ARG).orEmpty()
                HskGameScreen(
                    level = level,
                    topicId = topicId,
                    groupIndex = groupIndex,
                    gameId = gameId,
                    isExam = false,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.HSK_EXAM,
                arguments = listOf(navArgument(Routes.LEVEL_ARG) { type = NavType.IntType })
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                HskGameScreen(
                    level = level,
                    topicId = "",
                    groupIndex = 0,
                    gameId = "",
                    isExam = true,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.HSK_SENTENCES,
                arguments = listOf(navArgument(Routes.LEVEL_ARG) { type = NavType.IntType })
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                SentenceLevelScreen(
                    level = level,
                    viewModel = studyViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenTopic = { topicId ->
                        navController.navigate(Routes.hskSentenceTopic(level, topicId))
                    }
                )
            }

            composable(
                route = Routes.HSK_SENTENCE_TOPIC,
                arguments = listOf(
                    navArgument(Routes.LEVEL_ARG) { type = NavType.IntType },
                    navArgument(Routes.TOPIC_ID_ARG) { type = NavType.StringType }
                )
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                val topicId = entry.arguments?.getString(Routes.TOPIC_ID_ARG).orEmpty()
                SentenceTopicScreen(
                    level = level,
                    topicId = topicId,
                    viewModel = studyViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenGame = { gameId ->
                        navController.navigate(Routes.hskSentenceGame(level, topicId, gameId))
                    }
                )
            }

            composable(
                route = Routes.HSK_SENTENCE_GAME,
                arguments = listOf(
                    navArgument(Routes.LEVEL_ARG) { type = NavType.IntType },
                    navArgument(Routes.TOPIC_ID_ARG) { type = NavType.StringType },
                    navArgument(Routes.GAME_ID_ARG) { type = NavType.StringType }
                )
            ) { entry ->
                val level = entry.arguments?.getInt(Routes.LEVEL_ARG) ?: 1
                val topicId = entry.arguments?.getString(Routes.TOPIC_ID_ARG).orEmpty()
                val gameId = entry.arguments?.getString(Routes.GAME_ID_ARG).orEmpty()
                SentenceGameScreen(
                    level = level,
                    topicId = topicId,
                    gameId = gameId,
                    onExit = { navController.popBackStack() }
                )
            }

            /* ------------------------------ Игры --------------------------- */

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
}
