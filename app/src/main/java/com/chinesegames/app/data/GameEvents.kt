package com.chinesegames.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Краткий итог партии — для «живых» реакций интерфейса. */
data class GameOutcome(
    val accuracy: Float,
    val stars: Int,
    val score: Int
)

/**
 * Общие события приложения, на которые реагируют «живые» элементы интерфейса
 * (например, чиби-талисман хвалит за хорошую партию).
 */
object GameEvents {
    private val _finished = MutableSharedFlow<GameOutcome>(extraBufferCapacity = 8)

    /** Партия завершена и сохранена в статистику. */
    val finished: SharedFlow<GameOutcome> = _finished.asSharedFlow()

    fun gameFinished(outcome: GameOutcome) {
        _finished.tryEmit(outcome)
    }
}
