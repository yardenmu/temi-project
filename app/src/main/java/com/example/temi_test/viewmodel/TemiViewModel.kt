package com.example.temi_test.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.temi_test.model.MqttMessageData
import com.example.temi_test.model.Scenario
import kotlinx.coroutines.launch

class TemiViewModel : ViewModel() {

    private val _aiMessage = MutableLiveData<String?>()
    val aiMessage: LiveData<String?> = _aiMessage

    private val _sensorData = MutableLiveData<MqttMessageData?>()
    val sensorData: LiveData<MqttMessageData?> = _sensorData

    private val _isMonitoring = MutableLiveData(false)
    val isMonitoring: LiveData<Boolean> = _isMonitoring

    private var activeScenario: Scenario? = null

    private var lastRuleName: String? = null

    fun setScenario(scenario: Scenario) {
        activeScenario = scenario
        lastRuleName = null
    }

    fun updateAiMessage(message: String?) {
        _aiMessage.postValue(message)
        runScenario()
    }

    fun updateSensorData(data: MqttMessageData) {
        _sensorData.postValue(data)
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

        if (sensor != null || ai != null) {
            activeScenario?.let { scenario ->
                for (rule in scenario.rules) {
                    if (rule.condition(sensor, ai) && rule.name != lastRuleName) {
                        lastRuleName = rule.name
                        viewModelScope.launch {
                            rule.action()
                        }
                        break
                    }
                }
            }
        }
    }
}

