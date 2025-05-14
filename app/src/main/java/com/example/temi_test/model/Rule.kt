package com.example.temi_test.model

data class Rule( val name: String,
                 val condition: (sensorData: MqttMessageData?, message: String?) -> Boolean,
                 val action: suspend () -> Unit)
