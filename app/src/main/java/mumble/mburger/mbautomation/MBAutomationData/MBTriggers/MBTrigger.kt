package mumble.mburger.mbmessages.triggers

import java.io.Serializable

open class MBTrigger(open var type: String, open var solved: Boolean = false) : Serializable {

    var history: ArrayList<Long> = ArrayList()

}