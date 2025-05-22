package com.example.temi_test

import android.util.Log
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.Robot
import com.example.temi_test.model.Rule
import com.example.temi_test.model.Scenario
import kotlinx.coroutines.*

private var lastMetalWarningTime = 0L
private const val METAL_WARNING_DELAY = 6000L

private var lastDrawerOpenTime = 0L
private var microwaveStartTime: Long = 0
private var microwaveJob:Job?= null
private var microwaveDoorOpened = false
private var fridgeOpenTime: Long? = null
private var fridgeJob: Job? = null
private var isFridgeOpen = false

fun getRiceHeatingScenario(temi: Robot): Scenario {
    return Scenario(
        name = "scenario1",
        rules = listOf(
            Rule(
                name = "Fridge door opened",
                condition = { sensor, _ ->
                    sensor?.device_name == "fridge_door" && sensor.status
                },
                action = {
                    Log.d("scenario1", "Fridge door open")
                    temi.speak(TtsRequest.create("דלת מקרר נפתחת", false))
                    isFridgeOpen = true
                    fridgeOpenTime = System.currentTimeMillis()
                    fridgeJob?.cancel()
                    fridgeJob = CoroutineScope(Dispatchers.Default).launch {
                        delay(10_000)
                        val now = System.currentTimeMillis()
                        if (isFridgeOpen && fridgeOpenTime != null && now - fridgeOpenTime!! >= 10_000) {
                            Log.d("scenario1", "הדלת עדיין פתוחה אחרי 10 שניות")
                            temi.speak(TtsRequest.create("השארתם את דלת המקרר פתוחה", false))
                        }
                    }
                }
            ),
            Rule("Pot removed from fridge", { sensor, _ -> sensor?.device_name == "pot_in_fridge" && !sensor.status }, {
                temi.speak(TtsRequest.create("סיר יצא מהמקרר", false))
            }),
            Rule("Pot and plate placed on counter", { _, msg -> msg == "pot_and_plate_on_counter" }, {
                Log.d("scenario1", "A pot was placed on the counter.")
                temi.speak(TtsRequest.create("סיר וצלחת הונחו על הדלפק", false))
            }),
            Rule("Cutlery detected from drawer", { sensor, msg ->
                sensor?.device_name == "drawer" && sensor.status &&
                        (msg == "cutlery_detected")
            }, {
                Log.d("scenario1", "Cutlery was detected")
                temi.speak(TtsRequest.create("סכום נלקח מהמגירה", false))
            }),
            Rule(
                name = "Fridge door closed",
                condition = { sensor, _ ->
                    sensor?.device_name == "fridge_door" && !sensor.status
                },
                action = {
                    Log.d("scenario1", "Fridge door closed")
                    isFridgeOpen = false
                    fridgeOpenTime = null
                    fridgeJob?.cancel()
                }
            ),
            Rule("Filling plate with rice", { _, msg ->
                msg == "filling_plate_with_rice" || msg == "transfer_rice"
            }, {
                Log.d("scenario1", "The plate is being filled with rice.")
                temi.speak(TtsRequest.create("מילוי צלחת באורז", false))
            }),
            Rule("Microwave door opened", { sensor, msg ->
                (sensor?.device_name == "micro_door" && sensor.status) || msg == "micro_door_open"
            }, {
                Log.d("scenario1", "Microwave door open")
                microwaveDoorOpened = true
               // temi.speak(TtsRequest.create("דלת מיקרוגל נפתחה", false))
            }),
            Rule("Microwave door closed", { sensor, _ ->
                sensor?.device_name == "micro_door" && !sensor.status
            }, {
                Log.d("scenario1", "Microwave door closed")
                microwaveDoorOpened = false
                //temi.speak(TtsRequest.create("דלת מיקרוגל נסגרה", false))
            }),
            Rule(
                name = "Microwave start",
                condition = { sensor, _ ->
                    sensor?.device_name == "Microwave usage" && sensor.current_status == "On"
                },
                action = {
                    Log.d("scenario1", "Microwave start – heating begins")
                    temi.goTo("startposition")
                    temi.speak(TtsRequest.create("מיקרו התחיל פעולה", false))
                    microwaveStartTime = System.currentTimeMillis()
                    microwaveJob = CoroutineScope(Dispatchers.Default).launch {
                        delay(1 * 10 * 1000) // 2 דקות
                        Log.d("scenario1", "עברו 2 דקות מאז תחילת ההפעלה")
                        temi.speak(TtsRequest.create("עברו שתי דקות, נא להוציא את האוכל", false))
                        }
                    }
            ),
            Rule(
                name = "Microwave end",
                condition = { sensor, _ ->
                    sensor?.device_name == "Microwave usage" && sensor.current_status == "Off"
                },
                action = {
                    Log.d("scenario1", "Microwave end – heating ended")
                    temi.speak(TtsRequest.create("מיקרו הפסיק פעולה", false))
                    microwaveJob?.cancel()
                    }
            ),
            Rule("Plate inserted properly", { _, msg -> msg == "plate_inserted_into_microwave" }, {
                Log.d("scenario1", "Plate inserted correctly in microwave")
                temi.speak(TtsRequest.create("צלחת נכנסה למיקרו", false))
            }),
            Rule("Danger! Metal pot in microwave", { _, msg ->
                msg?.contains("metal_pot_in_microwave") == true
            }, {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMetalWarningTime >= METAL_WARNING_DELAY) {
                    lastMetalWarningTime = currentTime
                    temi.speak(TtsRequest.create("סכנה!!! סיר מתכת נכנס למיקרו הוצא במיידית", false))
                    Log.d("scenario1", "Metal hazard – pot detected inside microwave")
                } else {
                    Log.d("scenario1", "Ignoring metal message - still in delay")
                }
            }),
            Rule("pouring_food", { _, msg ->
                msg == "pouring_food"
            }, {
                Log.d("scenario1", "person is pouring food")
                temi.speak(TtsRequest.create("מזיגת אוכל לצלחת", false))
            }),
            Rule("Safe removal of plate from microwave", { _, msg ->
                msg == "safe_plate_removal" || msg == "plate_removed_from_microwave"
            }, {
                Log.d("scenario1", "Plate safely removed from microwave")
                temi.speak(TtsRequest.create("הצלחת הוצאה מהמיקרו", false))
            }),
            Rule("Drawer opened", { sensor, _ ->
                sensor?.device_name == "drawer" && sensor.status
            }, {
                lastDrawerOpenTime = System.currentTimeMillis()
                temi.goTo("pouringplace")
                Log.d("scenario1", "Drawer opened")
                temi.speak(TtsRequest.create("מגירה נפתחה", false))
            }),
            Rule("Metal Plate Removed From Drawer", { sensor, _ -> sensor?.device_name == "plate-in-drawer" && !sensor.status }, {
                Log.d("scenario1", "Metal Plate Removed From Drawer")
                temi.speak(TtsRequest.create("הצלחת אינה מתאימה לחימום",
                    false))
            })

        )
    )
}

fun getScenarioByName(name: String, temi: Robot): Scenario? {
    return when (name.lowercase()) {
        "scenario1" -> getRiceHeatingScenario(temi)
        else -> null
    }
}