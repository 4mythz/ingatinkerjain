package com.example.tugas3kelompok

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RumahActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rumah)

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_kategori
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_task_form -> {
                    if (this !is TaskFormActivity) {
                        startActivity(Intent(this, TaskFormActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                R.id.nav_home -> {
                    if (this !is HomeActivity) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                R.id.nav_kategori -> {
                    if (this !is KategoriActivity) {
                        startActivity(Intent(this, KategoriActivity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
