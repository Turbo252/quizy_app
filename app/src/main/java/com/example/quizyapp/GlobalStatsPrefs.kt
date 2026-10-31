package com.example.quizyapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object GlobalStatsPrefs {
    private const val PREFS_NAME = "quizyapp_prefs"

    fun isDarkTheme(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("dark_theme", false)

    fun setDarkTheme(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("dark_theme", isDark).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applySavedTheme(context: Context) {
        setDarkTheme(context, isDarkTheme(context))
    }

    fun addGameResult(context: Context, difficulty: String, stats: GameStats) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val games = prefs.getInt("${difficulty}_total_games", 0) + 1
        val questions = prefs.getInt("${difficulty}_total_questions", 0) + stats.totalQuestions
        val correct = prefs.getInt("${difficulty}_total_correct", 0) + stats.correctAnswers
        val timeMs = prefs.getLong("${difficulty}_total_time_ms", 0L) + stats.totalTimeMs
        val bestStreak = maxOf(prefs.getInt("${difficulty}_best_streak", 0), stats.maxStreak)
        
        var fastestTime = prefs.getLong("${difficulty}_fastest_10_time_ms", Long.MAX_VALUE)
        if (stats.totalQuestions == 10 && stats.totalTimeMs < fastestTime) {
            fastestTime = stats.totalTimeMs
        }

        prefs.edit()
            .putInt("${difficulty}_total_games", games)
            .putInt("${difficulty}_total_questions", questions)
            .putInt("${difficulty}_total_correct", correct)
            .putLong("${difficulty}_total_time_ms", timeMs)
            .putInt("${difficulty}_best_streak", bestStreak)
            .putLong("${difficulty}_fastest_10_time_ms", fastestTime)
            .apply()
    }

    fun clearStats(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)
        prefs.edit().clear().putBoolean("dark_theme", isDark).apply()
    }
}