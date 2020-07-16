package mumble.mburger.mbmessages.triggers

class MBTriggerView(type: String = MBTriggersConstants.view, var times: Int, var view_name: String,
                    var seconds_on_view: Int) : MBTrigger(type)