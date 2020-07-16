package mumble.mburger.mbmessages.triggers

import java.io.Serializable

class MBMessageTriggers(var method: TriggerMethod, var triggers: ArrayList<MBTrigger>) : Serializable

enum class TriggerMethod(val operator: String) {
    ALL("all"),
    ANY("any")
}