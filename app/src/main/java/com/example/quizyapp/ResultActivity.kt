package com.example.quizyapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatsPrefs.applySavedTheme(this)
        setContentView(R.layout.activity_result)

        val tvCurrentStats = findViewById<TextView>(R.id.tv_current_stats)
        val tvSeriesStats = findViewById<TextView>(R.id.tv_series_stats)
        val btnContinue = findViewById<Button>(R.id.btn_continue)
        val btnExit = findViewById<Button>(R.id.btn_exit)
        val btnTheme = findViewById<ImageButton>(R.id.btn_theme)

        btnTheme.setOnClickListener {
            val isDark = GlobalStatsPrefs.isDarkTheme(this)
            GlobalStatsPrefs.setDarkTheme(this, !isDark)
        }

        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 10)
        val avgTimeSec = intent.getDoubleExtra("AVG_TIME_SEC", 0.0)

        val accuracy = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0

        tvCurrentStats.text = "Текущая игра:\nПравильных ответов: $correctAnswers/$totalQuestions ($accuracy%)\nСр. время на ответ: ${"%.1f".format(avgTimeSec)} сек"

        val seriesStats = QuizSessionManager.getSeriesStats()
        tvSeriesStats.text = "Серия игр (всего игр: ${QuizSessionManager.gamesInSeries}):\nВсего вопросов: ${seriesStats.totalQuestions}\nПравильных: ${seriesStats.correctAnswers} (${seriesStats.accuracyPercent}%)\nСр. время: ${"%.1f".format(seriesStats.avgTimeSec)} сек"

        btnContinue.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnExit.setOnClickListener {
            QuizSessionManager.resetSeries()
            finish()
        }
    }
}