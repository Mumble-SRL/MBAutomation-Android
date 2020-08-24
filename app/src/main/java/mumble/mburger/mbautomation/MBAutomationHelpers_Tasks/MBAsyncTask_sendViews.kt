package mumble.mburger.mbautomation.MBAutomationHelpers_Tasks

import android.content.ContentValues
import android.content.Context
import android.os.AsyncTask
import mumble.mburger.mbautomation.MBAutomationComponents.MBAutomationAPIConstants
import mumble.mburger.mbautomation.MBAutomationData.MBUserView
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
internal class MBAsyncTask_sendViews(context: Context, var views: ArrayList<MBUserView>) : AsyncTask<Void, Void, Void>() {

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
            helper.deleteViews(views)
        } else {
            helper.removeSendingViews(views)
        }
    }

    fun putValuesAndCall() {
        val values = ContentValues()
        values.put("views", getJsonViews())
        map = MBAPIManager4.callApi(weakContext.get()!!, MBAutomationAPIConstants.API_SEND_VIEWS, values,
                MBApiManagerConfig.MODE_POST, false, false)
    }

    fun getJsonViews(): String {
        val jArr = JSONArray()
        for (view in views) {
            val jEvent = JSONObject()
            jEvent.put("view", view.view_name)
            jEvent.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(view.timestamp))
            if (view.metadata != null) {
                jEvent.put("metadata", view.metadata)
            }
            jArr.put(jEvent)
        }

        return jArr.toString()
    }
}
