package com.example.quizyapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatsPrefs.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnStats = findViewById<Button>(R.id.btn_stats)
        val btnTheme = findViewById<ImageButton>(R.id.btn_theme)
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        val spinnerDifficulty = findViewById<Spinner>(R.id.spinner_difficulty)

        val difficulties = arrayOf("Легкая", "Умеренная", "Сложная")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, difficulties)
        spinnerDifficulty.adapter = adapter

        btnStart.setOnClickListener {
            QuizSessionManager.selectedDifficulty = spinnerDifficulty.selectedItem as String
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
        }

        btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        btnTheme.setOnClickListener {
            val isDark = GlobalStatsPrefs.isDarkTheme(this)
            GlobalStatsPrefs.setDarkTheme(this, !isDark)
        }

        btnSettings.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Правила")
            popup.menu.add(0, 2, 0, "Авторы")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        startActivity(Intent(this, RulesActivity::class.java))
                        true
                    }
                    2 -> {
                        startActivity(Intent(this, AuthorsActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}