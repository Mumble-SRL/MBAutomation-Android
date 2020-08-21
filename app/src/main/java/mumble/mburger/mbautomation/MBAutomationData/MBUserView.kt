package mumble.mburger.mbautomation.MBAutomationData

import java.io.Serializable

class MBUserView(var id: Int, var view_name: String, var timestamp: Long, var sending: Boolean = false) : Serializable