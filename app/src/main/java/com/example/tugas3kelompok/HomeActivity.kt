package com.example.tugas3kelompok

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.tugas3kelompok.database.DatabaseHelper
import com.example.tugas3kelompok.task.Task
import com.example.tugas3kelompok.task.TaskAdapter
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var carouselAdapter: CarouselAdapter
    private var currentPage = 1
    private val scrollHandler = Handler()

    private val scrollRunnable = object : Runnable {
        override fun run() {
            viewPager.setCurrentItem(currentPage + 1, true)
            scrollHandler.postDelayed(this, 4000)
        }
    }

    private lateinit var taskPreviewContainer: LinearLayout
    private lateinit var searchInput: EditText

    private val taskList = mutableListOf<Task>()
    private val displayList = mutableListOf<Task>()
    private lateinit var adapter: TaskAdapter
    private lateinit var dbHelper: DatabaseHelper

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        try {
            // Initialize carousel
            viewPager = findViewById(R.id.viewPager)

            val btnLeft = findViewById<ImageButton>(R.id.btnPrev)
            val btnRight = findViewById<ImageButton>(R.id.btnNext)

            btnLeft.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem - 1
            }
            btnRight.setOnClickListener {
                viewPager.currentItem = viewPager.currentItem + 1
            }

            val realImages = listOf(
                R.drawable.img1,
                R.drawable.img2,
                R.drawable.img3
            )

            val realTitles = listOf(
                "Ingetin Kerjain!",
                "Pantau Tugas Harian",
                "Selesaikan Tepat Waktu"
            )

            val realSubtitles = listOf(
                "Tulis to-do listmu sekarang — setiap tugas adalah langkah menuju tujuan..",
                "Semua daftar tugas dalam satu tempat.",
                "Reminder yang bikin kamu nggak lupa."
            )

            val images = listOf(realImages.last()) + realImages + listOf(realImages.first())
            val titles = listOf(realTitles.last()) + realTitles + listOf(realTitles.first())
            val subtitles = listOf(realSubtitles.last()) + realSubtitles + listOf(realSubtitles.first())

            carouselAdapter = CarouselAdapter(images, titles, subtitles)
            viewPager.adapter = carouselAdapter
            viewPager.setCurrentItem(currentPage, false)

            viewPager.setPageTransformer { page, position ->
                page.alpha = 0.25f + (1 - kotlin.math.abs(position))
                page.translationX = -50 * position
            }

            (viewPager.getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                }

                override fun onPageScrollStateChanged(state: Int) {
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        if (currentPage == 0) {
                            viewPager.setCurrentItem(images.size - 2, false)
                        } else if (currentPage == images.size - 1) {
                            viewPager.setCurrentItem(1, false)
                        }
                    }
                }
            })

            scrollHandler.postDelayed(scrollRunnable, 4000)

            // Inisialisasi komponen lain
            taskPreviewContainer = findViewById(R.id.taskPreviewContainer)
            searchInput = findViewById(R.id.searchInput)

            dbHelper = DatabaseHelper(this)

            adapter = TaskAdapter(
                this,
                displayList,
                onTaskChecked = {},
                onTaskDelete = {},
                onTaskEdit = {},
                isPreviewMode = true
            )

            loadTasks()
            sortTasksByDeadline()
            displayList.addAll(taskList.take(5))
            renderTasks()

            // Pasang TextWatcher ke searchInput utama
            searchInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    filterTasks(s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            bottomNav.selectedItemId = R.id.nav_home
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
                        // Sudah di halaman ini
                        true
                    }
                    R.id.nav_kategori -> {
                        if (this !is KategoriActivity) {
                            startActivity(Intent(this, KategoriActivity::class.java))
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()
                        }
                        true
                    }
                    else -> false
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat halaman login: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun renderTasks() {
        taskPreviewContainer.removeAllViews()
        for (i in displayList.indices) {
            val view = adapter.getView(i, null, taskPreviewContainer)
            taskPreviewContainer.addView(view)
        }
    }

    private fun filterTasks(query: String) {
        try {
            displayList.clear()
            val filtered = if (query.isEmpty()) {
                taskList
            } else {
                val lowerCaseQuery = query.lowercase()
                taskList.filter { it.text.lowercase().contains(lowerCaseQuery) }
            }

            val sorted = filtered.sortedBy {
                try {
                    dateFormat.parse(it.deadline)
                } catch (_: Exception) {
                    Date(Long.MAX_VALUE)
                }
            }

            displayList.addAll(sorted.take(5))
            renderTasks()

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memfilter tugas: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTasks() {
        try {
            taskList.clear()
            taskList.addAll(dbHelper.getAllTasks())
            renderTasks()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat daftar tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun sortTasksByDeadline() {
        try {
            taskList.sortBy {
                try {
                    dateFormat.parse(it.deadline)
                } catch (_: Exception) {
                    Date(Long.MAX_VALUE)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengurutkan tugas: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            scrollHandler.removeCallbacks(scrollRunnable)
            dbHelper.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
