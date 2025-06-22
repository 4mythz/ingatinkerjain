package com.example.tugas3kelompok.task

data class Task(
    var id: Int = -1,
    var text: String,
    var deadline: String,
    var category: String = "Pekerjaan Lainnya",
    var isDone: Boolean = false,
    var isOverdue: Boolean = false,
    var lastModified: Long = System.currentTimeMillis(),
    var syncStatus: SyncStatus = SyncStatus.PENDING,
    var firebaseId: String? = null
)

enum class SyncStatus {
    PENDING,    // Belum di-sync ke Firebase
    SYNCED,     // Sudah di-sync ke Firebase
    CONFLICT    // Ada konflik antara local dan cloud
}
