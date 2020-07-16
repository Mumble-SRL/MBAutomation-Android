package mumble.mburger.mbautomation.MBAutomationComponents

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

class MBAutomationCommon {

    companion object {
        fun isLauncherActivity(activity: Activity): Boolean {
            val isMain = activity.intent.action == Intent.ACTION_MAIN
            val isLauncher = activity.intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            return isMain && isLauncher
        }

        fun isActivityAliveAndWell(activity: FragmentActivity?): Boolean {
            if (activity != null) {
                if (!activity.isFinishing) {
                    val isActivityInForeground = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (isActivityInForeground) {
                        return true
                    }
                }
            }

            return false
        }
    }
}