package mumble.mburger.mbautomation

import android.app.Activity
import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.FragmentActivity
import mumble.mburger.mbaudience.MBAudience
import mumble.mburger.mbautomation.MBAutomationComponents.MBAutomationCommon
import mumble.mburger.mbautomation.MBAutomationData.MBMessageWithTriggers
import mumble.mburger.mbautomation.MBAutomationData.MBUserEvent
import mumble.mburger.mbautomation.MBAutomationData.MBUserView
import mumble.mburger.mbautomation.MBAutomationHelpers_Tasks.MBAsyncTask_sendEvents
import mumble.mburger.mbautomation.MBAutomationHelpers_Tasks.MBAsyncTask_sendViews
import mumble.mburger.mbautomation.MBAutomationHelpers_Tasks.MBAutomationDBHelper
import mumble.mburger.mbautomation.MBAutomationHelpers_Tasks.MBAutomationEventsDBHelper
import mumble.mburger.mbautomation.MBAutomationListeners.MBAutomationPluginInitialized
import mumble.mburger.mbmessages.MBMessages
import mumble.mburger.mbmessages.MBMessagesManager
import mumble.mburger.mbmessages.iam.MBIAMConstants.MBIAMConstants
import mumble.mburger.mbmessages.iam.MBIAMData.MBMessage
import mumble.mburger.mbmessages.triggers.*
import mumble.mburger.sdk.kt.Common.MBCommonMethods
import mumble.mburger.sdk.kt.MBPlugins.MBPlugin
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MBAutomation : Application.ActivityLifecycleCallbacks, MBPlugin {

    override var id: String? = "MBAutomation"
    override var order: Int = -1
    override var delayInSeconds: Long = 0
    override var error: String? = null
    override var initialized: Boolean = false

    constructor(channel_id: String, notificationIcon: Int) : super() {
        Companion.channel_id = channel_id
        Companion.notificationIcon = notificationIcon
    }

    private val PROPERTY_LATEST_IN = "latest_in"

    var initListener: MBAutomationPluginInitialized? = null

    override fun init(context: Context) {
        super.init(context)
        curActivity = null
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        this.initialized = true
    }

    override fun doStart(activity: FragmentActivity) {
        super.doStart(activity)
        MBMessages.isAutomationConnected = true
        MBAudience.isAutomationConnected = true
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
            helper.addAMessage(ms.id, ms.triggers, MBAutomationParserConverter.convertMessageToJSON(ms))
        }

        if (initialized && fromStart) {
            checkForTriggers(context)
        }

        initListener?.onInitialized()
        startedCheckAndAdd()
        startEventsAndViewsAutomation(context)
    }

    override fun locationDataUpdated(latitude: Double, longitude: Double) {
        super.locationDataUpdated(latitude, longitude)
        addLocation(context, latitude, longitude)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        /**TRIGGER - App Opening, Inactive User, Location**/
        if (activity is FragmentActivity) {
            curActivity = activity
        }

        if (MBAutomationCommon.isLauncherActivity(activity)) {
            startedCheckAndAdd()
        }
    }

    private fun startedCheckAndAdd() {
        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
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
                    }

                    if (trigger is MBTriggerInactiveUser) {
                        /**Check for inactive user**/
                        val latestEnter = MBCommonMethods.getSharedPreferences(context)?.getLong(PROPERTY_LATEST_IN, -1L)
                        if (latestEnter != null) {
                            if (latestEnter != -1L) {
                                val diff = System.currentTimeMillis() - latestEnter
                                val days = TimeUnit.MILLISECONDS.toDays(diff)
                                if (days >= trigger.days) {
                                    atLeastOneUpdate = true
                                    trigger.history.add(System.currentTimeMillis())
                                    trigger.solved = true
                                }
                            }
                        }
                    }

                    if (trigger is MBTriggerLocation) {
                        if (trigger.semi_solved) {
                            val enter_time = trigger.history[0]
                            val diff = System.currentTimeMillis() - enter_time
                            val diffDays = TimeUnit.MILLISECONDS.toDays(diff)
                            if (trigger.after >= diffDays) {
                                atLeastOneUpdate = true
                                trigger.solved = true
                            }
                        }
                    }
                }
            }

            helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
        }

        if (atLeastOneUpdate) {
            checkForTriggers(context)
        }

        MBCommonMethods.getSharedPreferencesEditor(context)?.putLong(PROPERTY_LATEST_IN, System.currentTimeMillis())?.apply()
    }

    var startTime: Long = -1L
    var localTitle: String? = null
    override fun onActivityStarted(activity: Activity) {
        /**TRIGGER - View Activity**/
        if (trackViewsAutomatically) {
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
                            if (trigger.view_name == activity.title) {
                                localTitle = activity.title.toString()
                                startTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
            }

            checkForTriggers(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (trackViewsAutomatically) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            var atLeastOneUpdate = false
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerView) {
                            if (trigger.view_name == activity.title.toString()) {
                                if ((activity.title.toString() == localTitle) && (startTime != -1L)) {
                                    val time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
                                    trigger.history.add(time)
                                    var totalTime = 0L
                                    for (h in trigger.history) {
                                        totalTime += h
                                    }

                                    if ((totalTime >= trigger.seconds_on_view) && (trigger.history.size == trigger.times)) {
                                        trigger.solved = true
                                    }

                                    val eventHelper = MBAutomationEventsDBHelper(context)
                                    eventHelper.addView(localTitle!!, null, false)

                                    atLeastOneUpdate = true
                                }
                            }
                        }
                    }
                }

                helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
            }

            localTitle = null
            startTime = -1L

            if (atLeastOneUpdate) {
                checkForTriggers(context)
            }
        }
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (trackViewsAutomatically) {
            if (activity is FragmentActivity) {
                if (curActivity != null) {
                    if (activity == curActivity) {
                        curActivity = null
                    }
                }
            }
        }

        if (curRunnable != null) {
            mHandlerTrackViews?.removeCallbacks(curRunnable)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityResumed(activity: Activity) {}

    override fun tagChanged(key: String, value: String) {
        val helper = MBAutomationDBHelper(context)
        val automationMessages = helper.getMessages()
        var atLeastOneUpdate = false
        for (message in automationMessages) {
            if (message.triggers != null) {
                val triggers = message.triggers
                for (trigger in triggers!!.triggers) {
                    if (trigger is MBTriggerTagChange) {
                        if (!trigger.semi_solved && !trigger.solved) {
                            if (trigger.tag == key) {
                                when (trigger.operator) {
                                    TagChangeOperator.EQUALS -> {
                                        if (trigger.value == value) {
                                            trigger.solved = true
                                        }
                                    }

                                    TagChangeOperator.NOT_EQUAL -> {
                                        if (trigger.value != value) {
                                            trigger.solved = true
                                        }
                                    }
                                }

                                trigger.history.add(System.currentTimeMillis())
                                atLeastOneUpdate = true
                            }
                        }
                    }
                }

                helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
            }
        }

        if (atLeastOneUpdate) {
            checkForTriggers(context)
        }
    }

    override fun tagRemoved(key: String) {
        super.tagRemoved(key)
    }

    companion object {
        var mHandler: Handler? = null
        var mHandlerTrackViews: Handler? = null

        var sendDataRunnable: Runnable? = null

        var curActivity: FragmentActivity? = null

        //SECONDS VALUE
        var eventsTimerTime = 10

        //TRACK VIEWS AUTOMATICALLY
        var trackViewsAutomatically = true

        lateinit var channel_id: String
        var notificationIcon = -1

        var curRunnable: Runnable? = null

        fun startEventsAndViewsAutomation(context: Context) {
            mHandler = Handler()
            mHandlerTrackViews = Handler()
            sendDataRunnable = Runnable {
                getEventsViewsAndSend(context)
                mHandler?.postDelayed(sendDataRunnable, TimeUnit.SECONDS.toMillis(eventsTimerTime.toLong()))
            }

            sendDataRunnable?.run()
        }

        fun stopAutomation(context: Context) {
            if (sendDataRunnable != null) {
                mHandler?.removeCallbacks(sendDataRunnable!!)
            }

            val helper = MBAutomationEventsDBHelper(context)
            val events: ArrayList<MBUserEvent> = helper.allEvents
            if (events.isNotEmpty()) {
                helper.setSendingEvents(events)
                MBAsyncTask_sendEvents(context, events).execute()
            }

            val views: ArrayList<MBUserView> = helper.allViews
            if (views.isNotEmpty()) {
                helper.setSendingViews(views)
                MBAsyncTask_sendViews(context, views).execute()
            }
        }

        /**TRIGGER - Add an event**/
        fun sendEvent(context: Context, event_val: String, event_name: String? = null, metadata: String? = null) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            var atLeastOneUpdate = false
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerEvent) {
                            if (!trigger.semi_solved && !trigger.solved) {
                                if (trigger.event_val == event_val) {
                                    atLeastOneUpdate = true
                                    trigger.history.add(System.currentTimeMillis())
                                    if (trigger.history.size == trigger.times) {
                                        trigger.solved = true
                                    }
                                    break
                                }
                            }
                        }
                    }

                    helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                }
            }

            val eventHelper = MBAutomationEventsDBHelper(context)
            eventHelper.addEvent(event_val, event_name, metadata, false)

            if (atLeastOneUpdate) {
                checkForTriggers(context)
            }
        }

        /**TRIGGER - Set location**/
        fun addLocation(context: Context, latitude: Double, longitude: Double) {
            val helper = MBAutomationDBHelper(context)
            val automationMessages = helper.getMessages()
            var atLeastOneUpdate = false
            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerLocation) {
                            if (!trigger.semi_solved && !trigger.solved) {
                                val results = FloatArray(1)
                                Location.distanceBetween(trigger.latitude, trigger.longitude,
                                        latitude, longitude, results)

                                if (results[0] < trigger.radius) {
                                    trigger.history.add(System.currentTimeMillis())
                                    trigger.semi_solved = true
                                }
                            }
                        }
                    }

                    helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                }
            }

            if (atLeastOneUpdate) {
                checkForTriggers(context)
            }
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
                            if (mess.message.type == MBIAMConstants.CAMPAIGN_PUSH) {
                                if (!hasOneOnlineTrigger(triggers.triggers)) {
                                    MBMessagesManager.showNotification(context, channel_id, notificationIcon, mess.message)
                                }
                            } else {
                                if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {
                                    MBMessagesManager.startFlow(mess.message, curActivity!!)
                                }
                            }
                        }
                    } else {
                        if (nSolved == all_tr.size) {
                            if (mess.message.type == MBIAMConstants.CAMPAIGN_PUSH) {
                                if (!hasOneOnlineTrigger(triggers.triggers)) {
                                    MBMessagesManager.showNotification(context, channel_id, notificationIcon, mess.message)
                                }
                            } else {
                                if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {
                                    MBMessagesManager.startFlow(mess.message, curActivity!!)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun getEventsViewsAndSend(context: Context) {
            val helper = MBAutomationEventsDBHelper(context)
            val events: ArrayList<MBUserEvent> = helper.allEvents
            if (events.isNotEmpty()) {
                helper.setSendingEvents(events)
                MBAsyncTask_sendEvents(context, events).execute()
            }

            val views: ArrayList<MBUserView> = helper.allViews
            if (views.isNotEmpty()) {
                helper.setSendingViews(views)
                MBAsyncTask_sendViews(context, views).execute()
            }
        }

        fun trackScreenView(user_activity: FragmentActivity, custom_name: String? = null, metadata: String?) {
            val nameForTheScreen = custom_name ?: user_activity.title.toString()

            val helper = MBAutomationDBHelper(user_activity.applicationContext)
            val automationMessages = helper.getMessages()
            var atLeastOneUpdate = false

            for (message in automationMessages) {
                if (message.triggers != null) {
                    val triggers = message.triggers
                    for (trigger in triggers!!.triggers) {
                        if (trigger is MBTriggerView) {
                            if (trigger.view_name == nameForTheScreen) {

                                if (curRunnable != null) {
                                    mHandlerTrackViews?.removeCallbacks(curRunnable)
                                }

                                curRunnable = Runnable {
                                    if (MBAutomationCommon.isActivityAliveAndWell(curActivity)) {
                                        trigger.history.add(trigger.seconds_on_view.toLong())
                                        if (trigger.history.size == trigger.times) {
                                            trigger.solved = true
                                        }

                                        val eventHelper = MBAutomationEventsDBHelper(user_activity.applicationContext)
                                        eventHelper.addView(nameForTheScreen, metadata, false)

                                        helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
                                        checkForTriggers(user_activity.applicationContext)
                                    }
                                }

                                mHandlerTrackViews?.postDelayed(curRunnable, TimeUnit.SECONDS.toMillis(trigger.seconds_on_view.toLong()))
                            }
                        }
                    }
                }

                helper.updateMessage(message.id, MBAutomationParserConverter.convertMessageToJSON(message))
            }

            if (atLeastOneUpdate) {
                checkForTriggers(user_activity.applicationContext)
            }
        }

        fun hasOneOnlineTrigger(triggers: ArrayList<MBTrigger>): Boolean {
            for (trigger in triggers) {
                if (trigger is MBTriggerLocation) {
                    return true
                }

                if (trigger is MBTriggerInactiveUser) {
                    return true
                }
            }

            return false
        }
    }
}