package mumble.mburger.mbmessages.triggers

class MBTriggerInactiveUser(type: String = MBTriggersConstants.inactive_user, var days: Int) : MBTrigger(type)