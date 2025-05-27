package com.example.temi_test.model

data class DecisionNode(
    val name: String,
    val timeoutMillis: Long = 20_000,
    val onEnter: (() -> Unit)? = null,
    val onTimeout: (() -> Unit)? = null,
    val conditionCheck: (sensorData: MqttMessageData?, modelEvent: String?) -> String, // success, fail, wait
    val onFailCheck: (() -> Unit)? = null,
    val nextSteps: Map<String, String>
)
