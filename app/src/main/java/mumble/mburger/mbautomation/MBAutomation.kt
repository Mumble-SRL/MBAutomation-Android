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
import mumble.mburger.mbmessages.iam.MBMessagesManager
import mumble.mburger.mbmessages.triggers.MBTriggerAppOpening
import mumble.mburger.mbmessages.triggers.MBTriggerEvent
import mumble.mburger.mbmessages.triggers.MBTriggerView
import mumble.mburger.mbmessages.triggers.TriggerMethod
import mumble.mburger.sdk.kt.MBPlugins.MBPlugin
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        /**TRIGGER - App Opening, Inactive User**/
        if (activity is FragmentActivity) {
            curActivity = activity
        }

        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
        if (MBAutomationCommon.isLauncherActivity(activity)) {
            var atLeastOneUpdate = false
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerAppOpening) {
                            atLeastOneUpdate = true
                            trigger.history.add(System.currentTimeMillis())
                            if (trigger.history.size == trigger.times) {
                                trigger.solved = true
                            }
                            break
                        }
                    }

                    helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                }
            }

            if (atLeastOneUpdate) {
                checkForTriggers(context)
            }
        }
    }

    var startTime: Long = -1L
    var localClassName: String? = null
    override fun onActivityStarted(activity: Activity) {
        /**TRIGGER - View Activity**/
        if (activity is FragmentActivity) {
            curActivity = activity
        }

        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
        for (message in automationMessages) {
            if (message.triggers != null) {
                val triggers = message.triggers
                for (trigger in triggers!!.triggers) {
                    if (trigger is MBTriggerView) {
                        if (trigger.view_name == activity.localClassName) {
                            localClassName = activity.localClassName
                            startTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
        var atLeastOneUpdate = false
        for (message in automationMessages) {
            if (message.triggers != null) {
                val triggers = message.triggers
                for (trigger in triggers!!.triggers) {
                    if (trigger is MBTriggerView) {
                        if (trigger.view_name == activity.localClassName) {
                            if ((activity.localClassName == localClassName) && (startTime != -1L)) {
                                val time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
                                trigger.history.add(time)
                                var totalTime = 0L
                                for (h in trigger.history) {
                                    totalTime += h
                                }

                                if ((totalTime >= trigger.seconds_on_view) && (trigger.history.size == trigger.times)) {
                                    trigger.solved = true
                                }

                                atLeastOneUpdate = true
                            }
                        }
                    }
                }
            }
        }

        localClassName = null
        startTime = -1L

        if (atLeastOneUpdate) {
            checkForTriggers(context)
        }
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
                                if (trigger.history.size == trigger.times) {
                                    trigger.solved = true
                                }
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
            for (mess in automationMessages) {
                val triggers = mess.triggers
                if (triggers != null) {
                    val all_tr = triggers.triggers
                    var nSolved = 0
                    for (t in all_tr) {
                        if (t.solved) {
                            nSolved += 1
                        }
                    }

                    if (triggers.method == TriggerMethod.ANY) {
                        if (nSolved > 0) {
                            if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {
                                MBMessagesManager.startFlow(mess.message, curActivity!!)
                            }
                        }
                    } else {
                        if (nSolved == all_tr.size) {
                            if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {
                                MBMessagesManager.startFlow(mess.message, curActivity!!)
                            }
                        }
                    }
                }
            }
        }
    }
}