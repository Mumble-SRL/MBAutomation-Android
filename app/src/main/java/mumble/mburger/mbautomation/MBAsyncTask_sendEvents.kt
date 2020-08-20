package mumble.mburger.mbautomation

import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import mumble.mburger.mbautomation.MBAutomationData.MBTriggers.MBUserEvent
import mumble.mburger.mbmessages.MBMessagesParser
import mumble.mburger.mbmessages.iam.MBIAMConstants.MBIAMAPIConstants
import mumble.mburger.sdk.kt.Common.MBApiManager.MBAPIManager4
import mumble.mburger.sdk.kt.Common.MBApiManager.MBApiManagerConfig
import mumble.mburger.sdk.kt.Common.MBApiManager.MBApiManagerUtils
import mumble.mburger.sdk.kt.Common.MBCommonMethods
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

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
            helper.removeSending(events)
        }
    }

    fun putValuesAndCall() {
        val values = ContentValues()
        //TODO
    }
}
