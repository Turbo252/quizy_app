package com.example.quizyapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class AuthorsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalStatsPrefs.applySavedTheme(this)
        setContentView(R.layout.activity_authors)

        val btnBack = findViewById<Button>(R.id.btn_back)
        val btnTheme = findViewById<ImageButton>(R.id.btn_theme)

        btnTheme.setOnClickListener {
            val isDark = GlobalStatsPrefs.isDarkTheme(this)
            GlobalStatsPrefs.setDarkTheme(this, !isDark)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}