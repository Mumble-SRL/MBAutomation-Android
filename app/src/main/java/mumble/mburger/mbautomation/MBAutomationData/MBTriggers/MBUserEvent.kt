package mumble.mburger.mbautomation.MBAutomationData.MBTriggers

import java.io.Serializable

class MBUserEvent(var id: Int, var event: String, var metadata: String, var timestamp: Long, var sending: Boolean = false) : Serializable