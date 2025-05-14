package com.example.temi_test

import android.util.Log
import com.example.temi_test.model.MqttMessageData
import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.*

class SimpleMqttClient {

    private val serverUri = "tcp://mqttbroker.hitheal.org.il:1883"
    private val clientId = "AndroidClient_" + System.currentTimeMillis()
    private val topics = listOf("sensors", "experiment", "made")
    var onMqttMessageReceived: ((MqttMessageData) -> Unit)? = null


    private lateinit var mqttClient: MqttClient

    fun connect() {
        try {
            mqttClient = MqttClient(serverUri, clientId, null)

            val options = MqttConnectOptions().apply {
                isCleanSession = true
                userName = BuildConfig.MQTT_USERNAME
                password = BuildConfig.MQTT_PASSWORD.toCharArray()
            }

            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("MQTT", "Connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    convertJsonMQTTtoObj(message)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            mqttClient.connect(options)
            subscribeToTopics()

            Log.d("MQTT", "Connected and subscribed.")

        } catch (e: MqttException) {
            Log.e("MQTT", "${e.message}")
        }
    }

    private fun subscribeToTopics() {
        for (topic in topics) {
            try {
                mqttClient.subscribe(topic, 1)
                Log.d("MQTT", "Subscribed to topic: $topic")
            } catch (e: MqttException) {
                Log.e("MQTT", "Failed to subscribe to $topic: ${e.message}")
            }
        }
    }

    fun disconnect() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
        }
    }
    fun convertJsonMQTTtoObj(message: MqttMessage?){
        val payload = message.toString()
        try {
            val gson = Gson()
            val data = gson.fromJson(payload, MqttMessageData::class.java)
            onMqttMessageReceived?.invoke(data)
            Log.d("MQTT", "Device: ${data.device_name}, Status: ${data.status}")

        } catch (e: Exception) {
            Log.e("MQTT", "Failed to parse JSON: ${e.message}")
        }
    }
}