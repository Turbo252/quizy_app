package com.example.quizyapp

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {
    
    private lateinit var tvStats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatsPrefs.applySavedTheme(this)
        setContentView(R.layout.activity_stats)

        tvStats = findViewById(R.id.tv_stats)
        val btnBack = findViewById<Button>(R.id.btn_back)
        val btnResetStats = findViewById<Button>(R.id.btn_reset_stats)
        val btnTheme = findViewById<ImageButton>(R.id.btn_theme)

        loadAndDisplayStats()

        btnTheme.setOnClickListener {
            val isDark = GlobalStatsPrefs.isDarkTheme(this)
            GlobalStatsPrefs.setDarkTheme(this, !isDark)
        }

        btnResetStats.setOnClickListener {
            showResetConfirmationDialog()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadAndDisplayStats() {
        val prefs = getSharedPreferences("quizyapp_prefs", Context.MODE_PRIVATE)
        val difficulties = listOf("Легкая", "Умеренная", "Сложная")
        val statsText = StringBuilder()
        
        for (diff in difficulties) {
            val games = prefs.getInt("${diff}_total_games", 0)
            val questions = prefs.getInt("${diff}_total_questions", 0)
            val correct = prefs.getInt("${diff}_total_correct", 0)
            val timeMs = prefs.getLong("${diff}_total_time_ms", 0L)
            val fastest10TimeMs = prefs.getLong("${diff}_fastest_10_time_ms", Long.MAX_VALUE)

            val accuracy = if (questions > 0) (correct * 100) / questions else 0
            val avgTime = if (questions > 0) (timeMs / 1000.0) / questions else 0.0

            val fastestTimeText = if (fastest10TimeMs == Long.MAX_VALUE) "Нет рекорда" else "${"%.1f".format(fastest10TimeMs / 1000.0)} сек"

            statsText.append("--- Сложность: $diff ---\n")
            statsText.append("Всего сыграно игр: $games\n")
            statsText.append("Всего вопросов: $questions\n")
            statsText.append("Правильных ответов: $correct ($accuracy%)\n")
            statsText.append("Среднее время на ответ: ${"%.1f".format(avgTime)} сек\n")
            statsText.append("Рекорд на 10 вопросов: $fastestTimeText\n\n")
        }

        tvStats.text = statsText.toString().trimEnd()
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Сброс статистики")
            .setMessage("Вы уверены, что хотите полностью сбросить всю статистику? Это действие нельзя отменить.")
            .setPositiveButton("Сбросить") { _, _ ->
                GlobalStatsPrefs.clearStats(this)
                loadAndDisplayStats()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}