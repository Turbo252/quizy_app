package com.example.quizyapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class QuizActivity : AppCompatActivity() {

    private lateinit var tvQuestionCount: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var btnAnswer1: Button
    private lateinit var btnAnswer2: Button
    private lateinit var btnAnswer3: Button
    private lateinit var btnAnswer4: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var groupQuiz: Group
    private lateinit var btnTheme: ImageButton
    private lateinit var btnExitGame: ImageButton
    private lateinit var chronometer: Chronometer
    private lateinit var buttons: List<Button>

    private var currentQuestions = listOf<Question>()
    private var currentIndex = 0
    private var correctAnswersCount = 0
    private var gameStartTimeMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatsPrefs.applySavedTheme(this)
        setContentView(R.layout.activity_quiz)

        tvQuestionCount = findViewById(R.id.textView_questionCount)
        tvQuestion = findViewById(R.id.textView_question)
        btnAnswer1 = findViewById(R.id.btn_answer1)
        btnAnswer2 = findViewById(R.id.btn_answer2)
        btnAnswer3 = findViewById(R.id.btn_answer3)
        btnAnswer4 = findViewById(R.id.btn_answer4)
        progressBar = findViewById(R.id.progressBar)
        groupQuiz = findViewById(R.id.group_quiz)
        btnTheme = findViewById(R.id.btn_theme)
        btnExitGame = findViewById(R.id.btn_exit_game)
        chronometer = findViewById(R.id.chronometer_timer)

        buttons = listOf(btnAnswer1, btnAnswer2, btnAnswer3, btnAnswer4)

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener { onAnswerSelected(index) }
            // Фиксируем текущий цвет текста, чтобы он не менялся при отключении кнопки (isEnabled = false)
            button.setTextColor(button.currentTextColor)
        }

        btnTheme.setOnClickListener {
            val isDark = GlobalStatsPrefs.isDarkTheme(this)
            GlobalStatsPrefs.setDarkTheme(this, !isDark)
        }

        btnExitGame.setOnClickListener {
            showExitConfirmationDialog()
        }

        if (savedInstanceState != null) {
            val savedQuestions = savedInstanceState.getSerializable("currentQuestions") as? java.util.ArrayList<Question>
            if (savedQuestions != null && savedQuestions.isNotEmpty()) {
                currentQuestions = savedQuestions
                currentIndex = savedInstanceState.getInt("currentIndex", 0)
                correctAnswersCount = savedInstanceState.getInt("correctAnswersCount", 0)
                gameStartTimeMs = savedInstanceState.getLong("gameStartTimeMs", 0L)
                
                groupQuiz.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                chronometer.base = savedInstanceState.getLong("chronometerBase", SystemClock.elapsedRealtime())
                chronometer.start()
                
                showQuestion()
                return
            }
        }

        val difficulty = QuizSessionManager.selectedDifficulty
        if (!QuizSessionManager.hasQuestionsForDifficulty(difficulty)) {
            loadQuestionsFromGoogleSheets(difficulty)
        } else {
            startGame(difficulty)
        }
    }

    private fun loadQuestionsFromGoogleSheets(difficulty: String) {
        groupQuiz.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        thread {
            try {
                // Fetch JSON from Google Sheets
                val encodedSheet = URLEncoder.encode(difficulty, StandardCharsets.UTF_8.toString())
                val urlStr = "https://docs.google.com/spreadsheets/d/1RXXbvIr6a_OcRv1KBUpWB1_RdSHDD97NbevgiusP5wc/gviz/tq?tqx=out:json&sheet=$encodedSheet"
                val url = URL(urlStr)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                // Parse the google visualization response
                val startIdx = response.indexOf("{")
                val endIdx = response.lastIndexOf("}")
                if (startIdx >= 0 && endIdx >= 0) {
                    val jsonStr = response.substring(startIdx, endIdx + 1)
                    val jsonObject = JSONObject(jsonStr)
                    val rows = jsonObject.getJSONObject("table").getJSONArray("rows")

                    val questions = mutableListOf<Question>()
                    for (i in 0 until rows.length()) {
                        val cArray = rows.getJSONObject(i).getJSONArray("c")
                        if (cArray.length() >= 6) {
                            val qText = cArray.optJSONObject(0)?.optString("v") ?: continue
                            val o1 = cArray.optJSONObject(1)?.optString("v") ?: ""
                            val o2 = cArray.optJSONObject(2)?.optString("v") ?: ""
                            val o3 = cArray.optJSONObject(3)?.optString("v") ?: ""
                            val o4 = cArray.optJSONObject(4)?.optString("v") ?: ""
                            val ansIdxObj = cArray.optJSONObject(5)
                            var correctIndex = 0
                            if (ansIdxObj != null) {
                                val v = ansIdxObj.opt("v")
                                if (v is Int) correctIndex = v
                                else if (v is Double) correctIndex = v.toInt()
                                else if (v is String) correctIndex = v.toIntOrNull() ?: 0
                            }
                            questions.add(Question(qText, listOf(o1, o2, o3, o4), correctIndex))
                        }
                    }

                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            QuizSessionManager.setQuestionsForDifficulty(difficulty, questions)
                            startGame(difficulty)
                        }
                    }
                } else {
                    throw Exception("Invalid response format")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(this@QuizActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun startGame(difficulty: String) {
        currentQuestions = QuizSessionManager.getNext10Questions(difficulty)
        if (currentQuestions.isEmpty()) {
            Toast.makeText(this, "Нет доступных вопросов", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        groupQuiz.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        currentIndex = 0
        correctAnswersCount = 0
        gameStartTimeMs = System.currentTimeMillis()

        chronometer.base = SystemClock.elapsedRealtime() - QuizSessionManager.accumulatedTimeMs
        chronometer.start()

        showQuestion()
    }

    private fun showQuestion() {
        if (currentIndex >= currentQuestions.size || currentIndex >= 10) {
            finishGame()
            return
        }

        btnTheme.isEnabled = true
        btnExitGame.isEnabled = true

        val question = currentQuestions[currentIndex]
        val globalIndex = QuizSessionManager.seriesQuestions + currentIndex + 1
        val globalTotal = QuizSessionManager.seriesQuestions + currentQuestions.size
        tvQuestionCount.text = "Вопрос $globalIndex из $globalTotal"
        tvQuestion.text = question.text

        buttons.forEachIndexed { index, button ->
            if (index < question.options.size) {
                button.text = question.options[index]
                button.visibility = View.VISIBLE
            } else {
                button.visibility = View.INVISIBLE
            }
            // Reset button appearance
            button.backgroundTintList = null
            button.isEnabled = true
        }
    }

    private fun onAnswerSelected(selectedIndex: Int) {
        val question = currentQuestions[currentIndex]
        val isCorrect = selectedIndex == question.correctIndex

        if (isCorrect) {
            correctAnswersCount++
        }

        btnTheme.isEnabled = false
        btnExitGame.isEnabled = false

        buttons.forEachIndexed { index, button ->
            button.isEnabled = false
            if (index == question.correctIndex) {
                button.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            } else if (index == selectedIndex) {
                button.backgroundTintList = ColorStateList.valueOf(Color.RED)
            }
        }

        // Ставим таймер на паузу
        val pauseOffset = SystemClock.elapsedRealtime() - chronometer.base
        val realTimeAtPause = SystemClock.elapsedRealtime()
        chronometer.stop()

        Handler(Looper.getMainLooper()).postDelayed({
            if (isDestroyed) return@postDelayed
            // Возобновляем таймер
            val timeSpentPaused = SystemClock.elapsedRealtime() - realTimeAtPause
            gameStartTimeMs += timeSpentPaused // Исключаем эту 1 секунду из общей статистики
            chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
            chronometer.start()

            currentIndex++
            showQuestion()
        }, 1000)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (currentQuestions.isNotEmpty()) {
            outState.putSerializable("currentQuestions", java.util.ArrayList(currentQuestions))
            outState.putInt("currentIndex", currentIndex)
            outState.putInt("correctAnswersCount", correctAnswersCount)
            outState.putLong("gameStartTimeMs", gameStartTimeMs)
            outState.putLong("chronometerBase", chronometer.base)
        }
    }

    private fun finishGame() {
        chronometer.stop()
        QuizSessionManager.accumulatedTimeMs = SystemClock.elapsedRealtime() - chronometer.base

        val totalTimeMs = System.currentTimeMillis() - gameStartTimeMs
        val avgTimeSec = (totalTimeMs / 1000.0) / currentQuestions.size

        val stats = GameStats(
            totalQuestions = currentQuestions.size,
            correctAnswers = correctAnswersCount,
            totalTimeMs = totalTimeMs,
            fastestAnswerMs = 0L, // Not tracking individual times yet
            maxStreak = 0 // Not tracking streak in this simple implementation
        )

        QuizSessionManager.recordGameResult(stats, QuizSessionManager.selectedDifficulty, this)

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswersCount)
            putExtra("TOTAL_QUESTIONS", currentQuestions.size)
            putExtra("AVG_TIME_SEC", avgTimeSec)
        }
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmationDialog() {
        val pauseOffset = SystemClock.elapsedRealtime() - chronometer.base
        val realTimeAtPause = SystemClock.elapsedRealtime()
        chronometer.stop()

        AlertDialog.Builder(this)
            .setTitle("Выйти из игры?")
            .setMessage("Вы уверены, что хотите выйти досрочно? Результат текущей игры не будет сохранен в статистике.")
            .setPositiveButton("Да, выйти") { _, _ ->
                QuizSessionManager.resetSeries()
                finish()
            }
            .setNegativeButton("Отмена", null)
            .setOnDismissListener {
                if (!isFinishing) {
                    val timeSpentInDialog = SystemClock.elapsedRealtime() - realTimeAtPause
                    gameStartTimeMs += timeSpentInDialog // Корректируем общее время игры (не учитываем время в меню)

                    chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
                    chronometer.start()
                }
            }
            .show()
    }
}