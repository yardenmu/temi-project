package com.example.temi_test

import android.util.Log
import com.example.temi_test.model.MqttMessageData
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.Robot
import com.example.temi_test.model.Rule
import com.example.temi_test.model.Scenario


private var lastMetalWarningTime = 0L
private const val METAL_WARNING_DELAY=6000L
fun getRiceHeatingScenario(temi: Robot): Scenario {
    return Scenario(
        name = "scenario1",
        rules = listOf(
            Rule(
                name = "Fridge door opened",
                condition = { sensor, _ -> sensor?.device_name == "fridge_door" && sensor.status },
                action = { Log.d("scenario", "Fridge door open") }
            ),
            Rule(
                name = "Pot removed from fridge",
                condition = { sensor, _ -> sensor?.device_name == "pot_in_fridge" && !sensor.status },
                action = { Log.d("scenario", "Metal pot detected") }
            ),
            Rule(
                name = "Pot placed on counter",
                condition = { _, msg -> msg == "metal_pot_on_counter" },
                action = { Log.d("scenario", "A pot was placed on the counter.") }
            ),
            Rule(
                name = "Valid heating plate detected",
                condition = { _, msg -> msg == "plastic_plate" || msg == "ceramic_bowl" },
                action = { Log.d("scenario", "A proper heating element has been detected.") }
            ),
            Rule(
                name = "Cutlery detected from drawer",
                condition = { sensor, msg ->
                    sensor?.device_name == "drawer" && sensor.status &&
                            (msg == "fork" || msg == "spoon")
                },
                action = { Log.d("scenario", "cutlery was detected") }
            ),
            Rule(
                name = "Pot and plate on counter",
                condition = { _, msg -> msg == "metal_pot_and_plate_on_counter" },
                action = { Log.d("scenario", "A pot and plate are ready on the counter.") }
            ),
            Rule(
                name = "Microwave door opened",
                condition = { sensor, _ -> sensor?.device_name == "micro_door" && sensor.status },
                action = { Log.d("scenario", "Microwave door open") }
            ),
            Rule(
                name = "Danger! Metal pot in microwave",
                condition = { _, msg ->
                    msg?.contains("metal_pot_in_microwave") == true
                },
                action = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastMetalWarningTime >= METAL_WARNING_DELAY) {
                        lastMetalWarningTime = currentTime
                        temi.speak(TtsRequest.create("dangerous metal pot in microwave", false))
                        Log.d("scenario", "Metal hazard – pot detected inside microwave")
                    } else {
                        Log.d("scenario", "Ignoring metal message - still in delay")
                        }
                    }
            ),
            Rule(
                name = "Microwave door closed",
                condition = { sensor, _ -> sensor?.device_name == "micro_door" && !sensor.status },
                action = { Log.d("scenario", "Microwave door closed – heating begins") }
            ),
            Rule(
                name = "Plate removed from microwave",
                condition = { _, msg -> msg == "hand_and_plate_moving_away" },
                action = { Log.d("scenario", "A plate came out of the microwave.") }
            ),
            Rule(
                name = "Process completed",
                condition = { _, msg -> msg == "plate_on_counter" },
                action = { Log.d("scenario", "Successful completion of the experiment") }
            )
        )
    )
}

fun getScenarioByName(name: String, temi: Robot): Scenario? {
    return when (name.lowercase()) {
        "scenario1" -> getRiceHeatingScenario(temi)
        else -> null
    }
}