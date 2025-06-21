package com.example.tugas3kelompok.task

data class Task(
    var id: Int = -1,
    var text: String,
    var deadline: String,
    var isDone: Boolean = false,
    var isOverdue: Boolean = false
)
