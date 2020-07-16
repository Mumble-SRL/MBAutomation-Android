package mumble.mburger.mbmessages.triggers

class MBTriggerLocation(type: String = MBTriggersConstants.location, var after: Int, var radius: Int, var address: String?,
                        var latitude: Double, var longitude: Double) : MBTrigger(type)