package mumble.mburger.mbmessages.triggers

class MBTriggerTagChange(type: String = MBTriggersConstants.tag_change, var tag: String, var value: String, var operator: TagChangeOperator) : MBTrigger(type)

enum class TagChangeOperator(val operator: String) {
    EQUALS("="),
    NOT_EQUAL("!=")
}