package com.example.tugas3kelompok

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tugas3kelompok.databinding.ActivityKategoriBinding

class KategoriActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKategoriBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKategoriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardRumah.setOnClickListener {
            startActivity(Intent(this, RumahActivity::class.java))
        }

        binding.cardSekolah.setOnClickListener {
            startActivity(Intent(this, SekolahActivity::class.java))
        }

        binding.cardKantor.setOnClickListener {
            startActivity(Intent(this, KantorActivity::class.java))
        }

        binding.cardLainnya.setOnClickListener {
            startActivity(Intent(this, LainnyaActivity::class.java))
        }

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
                    true
                }
                else -> false
            }
        }
    }
}
