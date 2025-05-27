package com.example.temi_test.helpers

import android.util.Log
import com.example.temi_test.model.DecisionNode
import com.example.temi_test.model.MqttMessageData
import com.example.temi_test.model.StepLog
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScenarioHandler(private val scope: CoroutineScope, private val temi: Robot) {

    private var currentNode: DecisionNode? = null
    private var currentStepLog: StepLog? = null
    private var hasSpokenFail = false
    private val stepLogs = mutableListOf<StepLog>()
    private val decisionGraph: Map<String, DecisionNode> = buildDecisionGraph(temi)

    fun startScenario() {
        val firstNode = decisionGraph["Start"] ?: return
        moveToNode(firstNode)
    }

    private fun moveToNode(node: DecisionNode) {
        currentNode = node
        hasSpokenFail = false
        Log.d("node", "${node.name}")
        currentStepLog = StepLog(stepName = node.name, startTime = System.currentTimeMillis())
        stepLogs.add(currentStepLog!!)
        node.onEnter?.invoke()
        startTimeout(node)
    }
    private fun startTimeout(node: DecisionNode) {
        scope.launch {
            delay(node.timeoutMillis)
            if (currentNode == node) {
                node.onTimeout?.invoke()
                currentStepLog?.failAttempts = currentStepLog?.failAttempts?.plus(1) ?: 1
            }
        }
    }

    fun handleEvent(sensor: MqttMessageData?, modelEvent: String?) {
        currentNode?.let { node ->
            when (val outcome = node.conditionCheck(sensor, modelEvent)) {
                "success" -> {
                    currentStepLog?.endTime = System.currentTimeMillis()
                    currentStepLog?.outcome = "success"
                    val nextStepName = node.nextSteps["success"]
                    if (nextStepName != null) {
                        moveToNode(decisionGraph[nextStepName] ?: return)
                    }
                }
                "fail" -> {
                    if (!hasSpokenFail) {
                        node.onFailCheck?.invoke()
                        hasSpokenFail = true
                    }
                    currentStepLog?.failAttempts = currentStepLog?.failAttempts?.plus(1) ?: 1
                }
                else -> {
                }
            }
        }
    }
    fun calculateFinalScore(): Int {
        var score = 0
        stepLogs.forEach { log ->
            score += when (log.outcome) {
                "success" -> 10
                "timeout" -> 5
                "fail" -> 0
                else -> 0
            }
            score -= log.failAttempts * 2
        }
        return score
    }

    fun getLogs(): List<StepLog> = stepLogs

}

private fun buildDecisionGraph(temi: Robot): Map<String, DecisionNode> {
    return mapOf(
        "Start" to DecisionNode(
            name = "Start",
            timeoutMillis = 0,
            onTimeout = {},
            onEnter = {temi.speak(TtsRequest.create("אני רוצה לראות איך אתה מחמם אוכל במיקרוגל \n" +
                    "בבקשה הוצא אוכל שאותו אתה מחמם בצהריים ותחמם אותו במיקרוגל", false))},
            conditionCheck = { _, _ -> "success" },
            nextSteps = mapOf("success" to "OpenFridge")
        ),
        "OpenFridge" to DecisionNode(
            name = "OpenFridge",
            onTimeout = { temi.speak(TtsRequest.create("האם פתחת את המקרר?", false)) },
            onEnter = {},
            conditionCheck = { sensor, _ ->
                if (sensor?.device_name == "fridge_door" && sensor.status) "success"
                else "wait"
            },
            nextSteps = mapOf("success" to "SelectPot")
        ),
        "SelectPot" to DecisionNode(
            name = "SelectPot",
            onTimeout = { temi.speak(TtsRequest.create("האם הוצאת את הסיר של האורז?", false)) },
            onEnter = {},
            conditionCheck = { sensor, _ ->
                when {
                    sensor?.device_name == "pot_in_fridge" && !sensor.status -> "success"
                    sensor?.device_name == "pot_in_fridge" && sensor.status -> "fail"
                    sensor?.device_name != "pot_in_fridge" && sensor?.device_name == "fridge_door" && !sensor.status -> "fail"
                    else -> "wait"
                }
            },
            onFailCheck = { temi.speak(TtsRequest.create("האם בחרת את הסיר הנכון?.", false)) },
            nextSteps = mapOf("success" to "OpenDrawer", "fail" to "SelectPot")
        ),
        "OpenDrawer" to DecisionNode(
            name = "OpenDrawer",
            onTimeout = { temi.speak(TtsRequest.create("האם פתחת את המגירה כדי לבחור צלחת?", false)) },
            onEnter = {},
            conditionCheck = { sensor, _ ->
                if (sensor?.device_name == "drawer" && sensor.status) "success"
                else "wait"
            },
            nextSteps = mapOf("success" to "SelectPlate")
            ),
        "SelectPlate" to DecisionNode(
            name = "SelectPlate",
            onTimeout = { temi.speak(TtsRequest.create("האם בחרת צלחת מתאימה?", false)) },
            onEnter = { temi.goTo("pouringplace")},
            conditionCheck = { sensor, _ ->
                when {
                    sensor?.device_name == "plate-in-drawer" && !sensor.status -> "success"
                    sensor?.device_name == "plate-in-drawer" && sensor.status -> "fail"
                    sensor?.device_name == "drawer" && !sensor.status -> "success"
                    else -> "wait"
                }
            },
            onFailCheck = { temi.speak(TtsRequest.create("האם בחרת צלחת מתאימה?", false)) },
            nextSteps = mapOf("success" to "PouringFood", "fail" to "SelectPlate")
        ),
        "PouringFood" to DecisionNode(
            name = "PouringFood",
            onTimeout = { temi.speak(TtsRequest.create("האם מזגת אוכל לצלחת?", false)) },
            onEnter = {},
            conditionCheck = { _, modelEvent ->
                when {
                    modelEvent == "pouring_food" -> "success"
                    else -> "wait"
                }

            },
            onFailCheck = { temi.speak(TtsRequest.create("האם מזגת אוכל לצלחת?", false)) },
            nextSteps = mapOf("success" to "InsertIntoMicrowave")
        ),
        "InsertIntoMicrowave" to DecisionNode(
            name = "InsertIntoMicrowave",
            onTimeout = { temi.speak(TtsRequest.create("האם הכנסת את הצלחת למיקרו?", false)) },
            onEnter = {},
            conditionCheck = { sensor, modelEvent ->
                when {
                    modelEvent == "metal_pot_in_microwave" -> "fail"
                    sensor?.device_name == "micro_door" && !sensor.status && modelEvent == "plate_inserted_into_microwave" -> "success"
                    else -> "wait"
                }
            },
            onFailCheck = { temi.speak(TtsRequest.create("סיר מתכת הוכנס למיקרוגל — הוצא מיד!", false)) },
            nextSteps = mapOf("success" to "StartMicrowave", "fail" to "InsertIntoMicrowave")
        ),
        "StartMicrowave" to DecisionNode(
            name = "StartMicrowave",
            onTimeout = { temi.speak(TtsRequest.create("האם הפעלת את המיקרוגל?", false)) },
            onEnter = { temi.goTo("startposition")},
            conditionCheck = { sensor, _ ->
                if (sensor?.device_name == "Microwave usage" && sensor.current_status == "On") "success"
                else "wait"
            },
            nextSteps = mapOf("success" to "WaitForFinish")
        ),
        "WaitForFinish" to DecisionNode(
            name = "WaitForFinish",
            timeoutMillis = 120_000,
            onEnter = {},
            onTimeout = { temi.speak(TtsRequest.create("שימי לב לזמן החימום!", false)) },
            conditionCheck = { sensor, _ ->
                if (sensor?.device_name == "Microwave usage" && sensor.current_status == "Off") "success"
                else "wait"
            },
            nextSteps = mapOf("success" to "RemoveFood")
        ),
        "RemoveFood" to DecisionNode(
            name = "RemoveFood",
            onTimeout = { temi.speak(TtsRequest.create("האם הוצאת את האורז מהמיקרוגל?", false)) },
            onEnter = {},
            conditionCheck = { sensor, _ ->
                if (sensor?.device_name == "micro_door" && sensor.status) "success"
                else "wait"
            },
            nextSteps = mapOf("success" to "Finish")
        ),
        "Finish" to DecisionNode(
            name = "Finish",
            timeoutMillis = 0,
            onEnter = {},
            conditionCheck = { _, _ -> "success" },
            onTimeout = { temi.speak(TtsRequest.create("כל הכבוד! סיימת את המשימה.", false)) },
            nextSteps = emptyMap()
        )
    )
}


