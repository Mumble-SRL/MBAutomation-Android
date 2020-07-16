package mumble.mburger.mbautomation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import mumble.mburger.mbautomation.MBAutomationComponents.MBAutomationCommon
import mumble.mburger.mbautomation.MBAutomationData.MBMessageWithTriggers
import mumble.mburger.mbmessages.MBMessages
import mumble.mburger.mbmessages.iam.MBIAMData.MBMessage
import mumble.mburger.mbmessages.triggers.MBTriggerAppOpening
import mumble.mburger.mbmessages.triggers.MBTriggerEvent
import mumble.mburger.sdk.kt.MBPlugins.MBPlugin
import org.json.JSONObject

class MBAutomation : Application.ActivityLifecycleCallbacks, MBPlugin() {

    override var id: String? = "MBAutomation"
    override var order: Int = -1
    override var delayInSeconds: Long = 0
    override var error: String? = null
    override var initialized: Boolean = false

    override fun init(context: Context) {
        super.init(context)
        curActivity = null
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        this.initialized = true
    }

    override fun doStart(activity: FragmentActivity) {
        super.doStart(activity)
        MBMessages.isAutomationConnected = true
    }

    override fun messagesReceived(messages: ArrayList<*>?, fromStart: Boolean) {
        super.messagesReceived(messages, fromStart)
        val tempMessages = ArrayList<MBMessageWithTriggers>()
        for (ms in (messages as ArrayList<MBMessage>)) {
            val jTriggers = JSONObject(ms.sTriggers!!)
            tempMessages.add(MBMessageWithTriggers(ms.id, ms, MBAutomationParserConverter.parseTriggers(jTriggers)))
        }


        val helper = MBAutomationDBHelper(context)
        for (ms in tempMessages) {
            helper.addAMessage(ms.id, MBAutomationParserConverter.convertMessageToJSON(ms))
        }

        if (initialized && fromStart) {
            checkForTriggers(context)
        }
    }

    override fun locationDataUpdated(latitude: Double, longitude: Double) {
        super.locationDataUpdated(latitude, longitude)
    }

    override fun onActivityStarted(activity: Activity) {
        /**TRIGGER - View Activity**/
        if (activity is FragmentActivity) {
            curActivity = activity
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        /**TRIGGER - App Opening, Inactive User**/
        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
        if (MBAutomationCommon.isLauncherActivity(activity)) {
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerAppOpening) {
                            trigger.history.add(System.currentTimeMillis())
                            break
                        }
                    }

                    helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                }
            }
        }

        checkForTriggers(context)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (activity is FragmentActivity) {
            if (curActivity != null) {
                if (activity == curActivity) {
                    curActivity = null
                }
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}

    companion object {
        var curActivity: FragmentActivity? = null

        /**TRIGGER - Add an event**/
        fun addEvent(context: Context, event_name: String) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerEvent) {
                            if (trigger.event_name == event_name) {
                                trigger.history.add(System.currentTimeMillis())
                                break
                            }
                        }
                    }

                    helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                }
            }

            checkForTriggers(context)
        }

        /**TRIGGER - Set location**/
        fun addLocation(context: Context, latitude: Double, longitude: Double) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            for (message in automationMessages) {

            }

            checkForTriggers(context)
        }

        private fun checkForTriggers(context: Context) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {

            }
        }
    }
}