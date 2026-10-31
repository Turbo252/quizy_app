package com.example.quizyapp

import android.content.Context

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Int // 0..3
)

data class GameStats(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val totalTimeMs: Long,
    val fastestAnswerMs: Long,
    val maxStreak: Int
) {
    val accuracyPercent: Int
        get() = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0

    val avgTimeSec: Double
        get() = if (totalQuestions > 0) (totalTimeMs / 1000.0) / totalQuestions else 0.0
}

object QuizSessionManager {
    var selectedDifficulty: String = "Легкая"
    private var cachedQuestions = mutableMapOf<String, List<Question>>()
    private var unusedQuestions = mutableListOf<Question>()

    // Статистика текущей серии игр (живет только в RAM)
    var accumulatedTimeMs: Long = 0L // Время таймера
    var gamesInSeries = 0
        private set
    var seriesQuestions = 0
        private set
    private var seriesCorrect = 0
    private var seriesTimeMs = 0L
    private var seriesFastestMs = Long.MAX_VALUE
    private var seriesMaxStreak = 0

    fun setQuestionsForDifficulty(difficulty: String, questions: List<Question>) {
        cachedQuestions[difficulty] = questions
        unusedQuestions = questions.shuffled().toMutableList()
    }

    fun hasQuestionsForDifficulty(difficulty: String): Boolean {
        return cachedQuestions.containsKey(difficulty) && cachedQuestions[difficulty]!!.isNotEmpty()
    }

    fun getNext10Questions(difficulty: String): List<Question> {
        val questions = cachedQuestions[difficulty] ?: emptyList()
        if (unusedQuestions.size < 10) {
            unusedQuestions = questions.shuffled().toMutableList()
        }
        if (unusedQuestions.isEmpty()) return emptyList()

        val takeCount = minOf(10, unusedQuestions.size)
        val batch = unusedQuestions.take(takeCount)
        unusedQuestions.removeAll(batch)
        return batch
    }

    fun recordGameResult(stats: GameStats, difficulty: String, context: Context) {
        gamesInSeries++
        seriesQuestions += stats.totalQuestions
        seriesCorrect += stats.correctAnswers
        seriesTimeMs += stats.totalTimeMs
        if (stats.fastestAnswerMs < seriesFastestMs) seriesFastestMs = stats.fastestAnswerMs
        if (stats.maxStreak > seriesMaxStreak) seriesMaxStreak = stats.maxStreak

        // Сохраняем в глобальную статистику (для экрана Статистики из меню)
        GlobalStatsPrefs.addGameResult(context, difficulty, stats)
    }

    fun getSeriesStats(): GameStats = GameStats(
        totalQuestions = seriesQuestions,
        correctAnswers = seriesCorrect,
        totalTimeMs = seriesTimeMs,
        fastestAnswerMs = if (seriesFastestMs == Long.MAX_VALUE) 0L else seriesFastestMs,
        maxStreak = seriesMaxStreak
    )

    fun resetSeries() {
        accumulatedTimeMs = 0L
        gamesInSeries = 0
        seriesQuestions = 0
        seriesCorrect = 0
        seriesTimeMs = 0L
        seriesFastestMs = Long.MAX_VALUE
        seriesMaxStreak = 0
        unusedQuestions.clear()
        cachedQuestions.clear() // Clear cache so we fetch new questions if they were updated
    }
}