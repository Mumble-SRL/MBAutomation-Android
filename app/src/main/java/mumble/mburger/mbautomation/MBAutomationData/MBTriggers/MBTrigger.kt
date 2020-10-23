package mumble.mburger.mbmessages.triggers

import java.io.Serializable

open class MBTrigger(open var type: String, open var solved: Boolean = false, open var semi_solved: Boolean = false) : Serializable {

    var id: String?= null
    var history: ArrayList<Long> = ArrayList()

}