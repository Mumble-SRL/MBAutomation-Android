package mumble.mburger.mbautomation.MBAutomationData

import mumble.mburger.mbmessages.iam.MBIAMData.MBMessage
import mumble.mburger.mbmessages.triggers.MBMessageTriggers

class MBMessageWithTriggers(var id: Long, var message: MBMessage, var triggers: MBMessageTriggers?)