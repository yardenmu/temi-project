package com.example.temi_test.model

data class StepLog(
    val stepName: String,
    val startTime: Long,
    var endTime: Long? = null,
    var outcome: String = "incomplete", // success, fail, timeout
    var failAttempts: Int = 0
)
