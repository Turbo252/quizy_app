package com.example.quizyapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class QuizActivity : AppCompatActivity() {

    private val questionList = mutableListOf<Question>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        loadQuestionsFromAssets()
    }

    private fun loadQuestionsFromAssets() {
        val reader = BufferedReader(InputStreamReader(assets.open("questions.txt")))
        var line: String? = reader.readLine()

        while (line != null) {
            if (line.isNotBlank()) {
                val parts = line.split("|").map { it.trim() }

                if (parts.size >= 6) {
                    val questionText = parts[0]
                    // Варианты ответов с индекса 1 по 4 включительно
                    val options = listOf(parts[1], parts[2], parts[3], parts[4])
                    // Индекс правильного ответа (последний элемент)
                    val correctIndex = parts[5].toInt()

                    questionList.add(Question(questionText, options, correctIndex))
                }
            }
            line = reader.readLine()
        }

        reader.close()
    }
}