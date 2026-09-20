package com.example.content_feed

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.content_feed.ui.ReadingFragment
import com.example.content_feed.ui.SavedFragment

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var readingFragment: ReadingFragment? = null
    private var savedFragment: SavedFragment? = null
    private var activeFragment: Fragment? = null

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

        setupFragments(savedInstanceState)
        // 初始化導航欄切換邏輯
        setupNavigation()
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            readingFragment = ReadingFragment()
            savedFragment = SavedFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, readingFragment!!, "reading")
                .add(R.id.nav_host_fragment, savedFragment!!, "saved")
                .hide(savedFragment!!)
                .commit()
            activeFragment = readingFragment
        } else {
            readingFragment = supportFragmentManager.findFragmentByTag("reading") as? ReadingFragment
            savedFragment = supportFragmentManager.findFragmentByTag("saved") as? SavedFragment
            
            // 找出當前顯示的是哪一個
            activeFragment = if (readingFragment?.isHidden == false) readingFragment else savedFragment
        }
    }

    private fun setupNavigation() {
        val navReading = findViewById<ViewGroup>(R.id.navReading)
        val navSaved = findViewById<ViewGroup>(R.id.navSaved)
        
        // 獲取內部的 FrameLayout (它們是 LinearLayout 的第一個子 View)
        val containerReading = navReading.getChildAt(0)
        val containerSaved = navSaved.getChildAt(0)

        // 根據 activeFragment 設定初始選中狀態
        containerReading.isSelected = activeFragment == readingFragment
        containerSaved.isSelected = activeFragment == savedFragment

        navReading.setOnClickListener {
            if (!containerReading.isSelected) {
                containerReading.isSelected = true
                containerSaved.isSelected = false
                showFragment(readingFragment)
            }
        }

        navSaved.setOnClickListener {
            if (!containerSaved.isSelected) {
                containerReading.isSelected = false
                containerSaved.isSelected = true
                showFragment(savedFragment)
            }
        }
    }

    private fun showFragment(fragment: Fragment?) {
        if (fragment == null || fragment == activeFragment) return
        
        supportFragmentManager.beginTransaction()
            .hide(activeFragment!!)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }
}