package com.example.temi_test.model

data class MqttMessageData(val deviceID: String,
                           val topic: String,
                           val device_name: String,
                           val status: Boolean,
                           val status_date: String)
