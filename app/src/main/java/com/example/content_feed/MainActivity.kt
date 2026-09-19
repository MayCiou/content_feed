package com.example.content_feed

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化導航欄切換邏輯
        setupNavigation()
    }

    private fun setupNavigation() {
        val navReading = findViewById<ViewGroup>(R.id.navReading)
        val navSaved = findViewById<ViewGroup>(R.id.navSaved)
        
        // 獲取內部的 FrameLayout (它們是 LinearLayout 的第一個子 View)
        val containerReading = navReading.getChildAt(0)
        val containerSaved = navSaved.getChildAt(0)

        containerReading.isSelected = true // 預設選中第一個

        navReading.setOnClickListener {
            containerReading.isSelected = true
            containerSaved.isSelected = false
        }

        navSaved.setOnClickListener {
            containerReading.isSelected = false
            containerSaved.isSelected = true
        }
    }
}