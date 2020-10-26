package mumble.mburger.mbautomation.MBAutomationHelpers_Tasks

import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import android.provider.Settings
import mumble.mburger.mbautomation.MBAutomationComponents.MBAutomationAPIConstants
import mumble.mburger.mbautomation.MBAutomationData.MBUserEvent
import mumble.mburger.sdk.kt.Common.MBApiManager.MBAPIManager4
import mumble.mburger.sdk.kt.Common.MBApiManager.MBApiManagerConfig
import mumble.mburger.sdk.kt.Common.MBApiManager.MBApiManagerUtils
import mumble.mburger.sdk.kt.Common.MBCommonMethods
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Task that sends events to the server
 *
 * @author Enrico Ori
 * @version {@value MBIAMConstants#version}
 */
internal class MBAsyncTask_sendEvents(context: Context, var events: ArrayList<MBUserEvent>) : AsyncTask<Void, Void, Void>() {

    private var weakContext: WeakReference<Context> = WeakReference(context)

    private var result = MBApiManagerConfig.COMMON_INTERNAL_ERROR
    private var error: String? = null
    private var map: MutableMap<String, Any?>? = null

    override fun doInBackground(vararg params: Void?): Void? {
        putValuesAndCall()
        if (MBApiManagerUtils.hasMapOkResults(map, false)) {
            result = MBApiManagerConfig.RESULT_OK
        } else {
            result = if (map!!.containsKey(MBApiManagerConfig.AM_RESULT)) {
                map!![MBApiManagerConfig.AM_RESULT] as Int
            } else {
                MBApiManagerConfig.COMMON_INTERNAL_ERROR
            }

            error = if (map!!.containsKey(MBApiManagerConfig.AM_ERROR)) {
                map!![MBApiManagerConfig.AM_ERROR] as String
            } else {
                MBCommonMethods.getErrorMessageFromResult(weakContext.get()!!, result)
            }
        }
        return null
    }

    override fun onPostExecute(postResult: Void?) {
        val helper = MBAutomationEventsDBHelper(weakContext.get())
        if (result == MBApiManagerConfig.RESULT_OK) {
            helper.deleteEvents(events)
        } else {
            helper.removeSendingEvents(events)
        }
    }

    fun putValuesAndCall() {
        map = MBAPIManager4.callApi(weakContext.get()!!, MBAutomationAPIConstants.API_SEND_EVENTS, ContentValues(),
                MBApiManagerConfig.MODE_POST, false, false, dataString = getJsonEvents())
    }

    fun getJsonEvents(): String {
        val jBuilder = StringBuilder("{")

        val jArr = JSONArray()
        for (ev in events) {
            val jEvent = JSONObject()
            jEvent.put("event", ev.event)
            jEvent.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(ev.timestamp))
            if (ev.event_name != null) {
                jEvent.put("name", ev.event_name)
            }
            if (ev.metadata != null) {
                jEvent.put("metadata", ev.metadata)
            }
            jArr.put(jEvent)
        }

        val device_id = Settings.Secure.getString(weakContext.get()!!.contentResolver, Settings.Secure.ANDROID_ID)
        jBuilder.append("\"events\": $jArr,")
        jBuilder.append("\"device_id\": \"$device_id\",")
        jBuilder.append("\"os\": \"android\"")
        jBuilder.append("}")
        return jBuilder.toString()
    }
}
