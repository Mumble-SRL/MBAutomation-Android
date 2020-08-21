package mumble.mburger.mbmessages.triggers

class MBTriggerEvent(type: String = MBTriggersConstants.event, var times: Int, var event_val: String?, var metadata: MBEventMetadata?) : MBTrigger(type)

class MBEventMetadata(var key: String, var value: String)