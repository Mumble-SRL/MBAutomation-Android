package mumble.mburger.mbautomation.MBAutomationData

import java.io.Serializable

class MBUserEvent(var id: Int, var event: String, var event_name:String?,
                  var metadata: String?, var timestamp: Long, var sending: Boolean = false) : Serializable