package com.example.temi_test.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.temi_test.helpers.ScenarioHandler
import com.example.temi_test.model.MqttMessageData
import com.robotemi.sdk.Robot

class TemiViewModel : ViewModel() {

    private val _sensorData = MutableLiveData<MqttMessageData?>()
    val sensorData: LiveData<MqttMessageData?> = _sensorData

    private val _aiMessage = MutableLiveData<String?>()
    val aiMessage: LiveData<String?> = _aiMessage

    private val _isMonitoring = MutableLiveData(false)
    val isMonitoring: LiveData<Boolean> = _isMonitoring

    private lateinit var scenarioHandler: ScenarioHandler

    fun initializeScenarioHandler(temi: Robot) {
        scenarioHandler = ScenarioHandler(scope = viewModelScope, temi = temi)
        scenarioHandler.startScenario()
    }

    fun updateSensorData(data: MqttMessageData) {
        _sensorData.postValue(data)
        runScenario()
    }

    fun updateAiMessage(message: String?) {
        _aiMessage.postValue(message)
        runScenario()
    }

    fun startMonitoring() {
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        _isMonitoring.value = false
    }

    fun runScenario() {
        if (_isMonitoring.value != true) return

        val sensor = _sensorData.value
        val ai = _aiMessage.value

        scenarioHandler.handleEvent(sensor, ai)
    }

    fun getScenarioLogs() = scenarioHandler.getLogs()
    fun getScenarioScore() = scenarioHandler.calculateFinalScore()
}


