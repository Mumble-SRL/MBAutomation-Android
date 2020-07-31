package mumble.mburger.mbautomation

import mumble.mburger.mbautomation.MBAutomationData.MBMessageWithTriggers
import mumble.mburger.mbmessages.iam.MBIAMConstants.MBIAMConstants
import mumble.mburger.mbmessages.iam.MBIAMData.CTA
import mumble.mburger.mbmessages.iam.MBIAMData.MBMessage
import mumble.mburger.mbmessages.iam.MBIAMData.MBMessageIAM
import mumble.mburger.mbmessages.iam.MBIAMData.MBMessagePush
import mumble.mburger.mbmessages.triggers.*
import mumble.mburger.sdk.kt.Common.MBCommonMethods
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MBAutomationParserConverter {

    companion object {

        fun convertMessageToJSON(message: MBMessageWithTriggers): String {
            val jString = JSONObject()
            val msg = message.message
            jString.put("id", message.id)
            jString.put("message_id", msg.id)
            jString.put("message_title", msg.title)
            jString.put("message_description", msg.description)
            jString.put("message_type", msg.type)
            jString.put("message_send_after_days", msg.send_after_days)
            jString.put("message_repeat", msg.repeat)
            jString.put("message_starts_at", msg.starts_at)
            jString.put("message_ends_at", msg.ends_at)
            jString.put("message_automation", msg.automation)
            jString.put("message_sTriggers", msg.sTriggers)
            jString.put("message_created_at", msg.created_at)
            jString.put("message_updated_at", msg.updated_at)

            val jContent = when (msg.type) {
                MBIAMConstants.CAMPAIGN_PUSH -> jsonizeContentPush(msg.content as MBMessagePush)
                else -> jsonizeContentIAM(msg.content as MBMessageIAM)
            }

            jString.put("message_content", jContent)
            jString.put("message_triggers", jsonizeTriggers(message.triggers))
            return jString.toString()
        }

        fun jsonizeContentPush(mbPush: MBMessagePush): JSONObject {
            val jString = JSONObject()
            jString.put("id", mbPush.id)
            jString.put("title", mbPush.title)
            jString.put("body", mbPush.body)
            jString.put("date", mbPush.date)
            jString.put("sent", mbPush.sent)
            jString.put("topics", mbPush.topics)
            jString.put("total", mbPush.total)
            jString.put("created_at", mbPush.created_at)
            jString.put("updated_at", mbPush.updated_at)
            return jString
        }

        fun jsonizeContentIAM(mbIAM: MBMessageIAM): JSONObject {
            val jString = JSONObject()
            jString.put("id", mbIAM.id)
            jString.put("type", mbIAM.type)
            jString.put("title", mbIAM.title)
            jString.put("content", mbIAM.content)
            jString.put("title_color", mbIAM.title_color)
            jString.put("content_color", mbIAM.content_color)
            jString.put("backgroundColor", mbIAM.backgroundColor)
            jString.put("closeButtonColor", mbIAM.closeButtonColor)
            jString.put("closeButtonBGColor", mbIAM.closeButtonBGColor)
            jString.put("cta1", jsonizeCTA(mbIAM.cta1))
            jString.put("cta2", jsonizeCTA(mbIAM.cta2))
            jString.put("durationInSeconds", mbIAM.durationInSeconds)
            jString.put("expiresAt", mbIAM.expiresAt)
            jString.put("image", mbIAM.image)
            return jString
        }

        fun jsonizeCTA(cta: CTA?): JSONObject? {
            return if (cta == null) {
                null
            } else {
                val jString = JSONObject()
                jString.put("text", cta.text)
                jString.put("text_color", cta.text_color)
                jString.put("background_color", cta.background_color)
                jString.put("action_type", cta.action_type)
                jString.put("action", cta.action)
                jString
            }
        }

        fun jsonizeTriggers(mbMTriggers: MBMessageTriggers?): JSONObject? {
            if (mbMTriggers != null) {
                val jString = JSONObject()
                val jTriggers = JSONArray()
                jString.put("method", mbMTriggers.method.operator)

                for (trigger in mbMTriggers.triggers) {
                    var jTrigger: JSONObject? = when (trigger.type) {
                        MBTriggersConstants.location -> {
                            val realTrigger = trigger as MBTriggerLocation
                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("after", realTrigger.after)
                                    .put("radius", realTrigger.radius)
                                    .put("address", realTrigger.address)
                                    .put("latitude", realTrigger.latitude)
                                    .put("longitude", realTrigger.longitude)
                        }

                        MBTriggersConstants.app_opening -> {
                            val realTrigger = trigger as MBTriggerAppOpening
                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("times", realTrigger.times)
                        }

                        MBTriggersConstants.view -> {
                            val realTrigger = trigger as MBTriggerView
                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("times", realTrigger.times)
                                    .put("view_name", realTrigger.view_name)
                                    .put("seconds_on_view", realTrigger.seconds_on_view)
                        }

                        MBTriggersConstants.inactive_user -> {
                            val realTrigger = trigger as MBTriggerInactiveUser
                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("days", realTrigger.days)
                        }

                        MBTriggersConstants.event -> {
                            val realTrigger = trigger as MBTriggerEvent
                            var jMetadata: JSONObject? = null
                            if (realTrigger.metadata != null) {
                                jMetadata = JSONObject()
                                jMetadata.put("key", realTrigger.metadata!!.key)
                                jMetadata.put("value", realTrigger.metadata!!.value)
                            }

                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("times", realTrigger.times)
                                    .put("event_name", realTrigger.event_name)
                                    .put("metadata", jMetadata)
                        }

                        MBTriggersConstants.tag_change -> {
                            val realTrigger = trigger as MBTriggerTagChange
                            JSONObject().put("history", jsonizeHistory(realTrigger.history))
                                    .put("operator", realTrigger.operator.operator)
                                    .put("value", realTrigger.value)
                                    .put("tag", realTrigger.tag)
                        }

                        else -> {
                            null
                        }
                    }

                    if (jTrigger != null) {
                        jTrigger.put("type", trigger.type)
                        jTrigger.put("solved", trigger.solved)
                        jTriggers.put(jTrigger)
                    }
                }

                jString.put("triggers", jTriggers)
                return jString
            }

            return null
        }

        fun deJSONIZEMessages(jsonObject: JSONObject): MBMessageWithTriggers {
            val id = jsonObject.getLong("id")
            val message_id = jsonObject.getLong("message_id")
            val message_title = MBCommonMethods.getJSONField(jsonObject, "message_title") as String?
            val message_description = MBCommonMethods.getJSONField(jsonObject, "message_description") as String?
            val message_type = MBCommonMethods.getJSONField(jsonObject, "message_type") as String?
            val message_send_after_days = MBCommonMethods.getJSONField(jsonObject, "message_send_after_days") as Int
            val message_repeat = MBCommonMethods.getJSONField(jsonObject, "message_repeat") as Int
            val message_starts_at = jsonObject.getLong("message_starts_at")
            val message_ends_at = jsonObject.getLong("message_ends_at")
            val message_automation = MBCommonMethods.getJSONField(jsonObject, "message_automation") as Int
            val message_sTriggers = MBCommonMethods.getJSONField(jsonObject, "message_sTriggers") as String?
            val message_created_at = jsonObject.getLong("message_created_at")
            val message_updated_at = jsonObject.getLong("message_updated_at")
            val jContent = MBCommonMethods.getJSONField(jsonObject, "message_content") as JSONObject

            val content = when (message_type) {
                MBIAMConstants.CAMPAIGN_PUSH -> deJsonizePush(jContent)
                else -> deJsonizeIAM(jContent)
            }

            val message = MBMessage(message_id, message_title, message_description, message_type, message_send_after_days,
                    message_repeat, message_starts_at, message_ends_at, message_automation, message_sTriggers, message_created_at,
                    message_updated_at, content)

            val message_triggers = MBCommonMethods.getJSONField(jsonObject, "message_triggers") as JSONObject?

            if (message_triggers != null) {
                return MBMessageWithTriggers(id, message, parseTriggers(message_triggers))
            } else {
                return MBMessageWithTriggers(id, message, null)
            }
        }

        fun deJsonizePush(jContent: JSONObject): MBMessagePush {
            val id = MBCommonMethods.getJSONField(jContent, "id") as String?
            val title = MBCommonMethods.getJSONField(jContent, "title") as String?
            val body = MBCommonMethods.getJSONField(jContent, "body") as String?
            val date = MBCommonMethods.getJSONField(jContent, "date") as String?
            val sent = MBCommonMethods.getJSONField(jContent, "sent") as Int
            val topics = MBCommonMethods.getJSONField(jContent, "topics") as String?
            val total = MBCommonMethods.getJSONField(jContent, "total") as Int
            val created_at = jContent.getLong("created_at")
            val updated_at = jContent.getLong("updated_at")
            return MBMessagePush(id, title, body, date, sent, topics, total, created_at, updated_at)
        }

        fun deJsonizeIAM(jContent: JSONObject): MBMessageIAM {
            val id = jContent.getLong("id")
            val type = MBCommonMethods.getJSONField(jContent, "type") as String
            val title = MBCommonMethods.getJSONField(jContent, "title") as String?
            val content = MBCommonMethods.getJSONField(jContent, "content") as String?
            val title_color = MBCommonMethods.getJSONField(jContent, "title_color") as Int?
            val content_color = MBCommonMethods.getJSONField(jContent, "content_color") as Int?
            val backgroundColor = MBCommonMethods.getJSONField(jContent, "backgroundColor") as Int?
            val closeButtonColor = MBCommonMethods.getJSONField(jContent, "closeButtonColor") as Int?
            val closeButtonBGColor = MBCommonMethods.getJSONField(jContent, "closeButtonBGColor") as Int?
            val cta1 = deJsonizeCTA(MBCommonMethods.getJSONField(jContent, "cta1") as JSONObject?)
            val cta2 = deJsonizeCTA(MBCommonMethods.getJSONField(jContent, "cta2") as JSONObject?)
            val durationInSeconds = MBCommonMethods.getJSONField(jContent, "durationInSeconds") as Int
            val expiresAt = jContent.getLong("expiresAt")
            val image = MBCommonMethods.getJSONField(jContent, "image") as String?

            return MBMessageIAM(id, type, title, content, title_color, content_color, backgroundColor, closeButtonColor, closeButtonBGColor,
                    cta1, cta2, durationInSeconds, expiresAt, image)
        }

        fun deJsonizeCTA(jContent: JSONObject?): CTA? {
            if (jContent != null) {
                val text = MBCommonMethods.getJSONField(jContent, "text") as String
                val text_color = MBCommonMethods.getJSONField(jContent, "text_color") as Int?
                val background_color = MBCommonMethods.getJSONField(jContent, "background_color") as Int?
                val action_type = MBCommonMethods.getJSONField(jContent, "action_type") as String
                val action = MBCommonMethods.getJSONField(jContent, "action") as String
                return CTA(text, text_color, background_color, action_type, action)
            }

            return null
        }

        fun jsonizeHistory(history: ArrayList<Long>): String {
            val jArr = JSONArray()
            for (h in history) {
                jArr.put(h)
            }

            return jArr.toString()
        }

        fun deJsonizeHistory(jString: String): ArrayList<Long> {
            val jArr = JSONArray(jString)
            val arr = ArrayList<Long>()
            for (i in 0 until jArr.length()) {
                arr.add(jArr.getLong(i))
            }

            return arr
        }

        fun parseTriggers(jsonObject: JSONObject): MBMessageTriggers {
            var triggers = ArrayList<MBTrigger>()

            val sTriggerMethod = jsonObject.getString("method")
            var method = when (sTriggerMethod) {
                "any" -> TriggerMethod.ANY
                else -> TriggerMethod.ALL
            }

            val jTriggers = jsonObject.getJSONArray("triggers")
            for (i in 0 until jTriggers.length()) {
                val jTr = jTriggers.getJSONObject(i)
                val type = jTr.getString("type")
                var trigger: MBTrigger = when (type) {
                    MBTriggersConstants.location -> {

                        var after = -1
                        var radius = -1
                        var address: String? = null
                        var latitude = (-1).toDouble()
                        var longitude = (-1).toDouble()

                        if (MBCommonMethods.isJSONOk(jTr, "after")) {
                            after = jTr.getInt("after")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "radius")) {
                            radius = jTr.getInt("radius")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "address")) {
                            address = jTr.getString("address")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "latitude")) {
                            latitude = jTr.getDouble("latitude")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "longitude")) {
                            longitude = jTr.getDouble("longitude")
                        }

                        MBTriggerLocation(after = after, radius = radius, address = address,
                                latitude = latitude, longitude = longitude)
                    }

                    MBTriggersConstants.app_opening -> {
                        MBTriggerAppOpening(times = jTr.getInt("times"))
                    }

                    MBTriggersConstants.view -> {
                        MBTriggerView(times = jTr.getInt("times"), view_name = jTr.getString("view_name"),
                                seconds_on_view = jTr.getInt("seconds_on_view"))
                    }

                    MBTriggersConstants.inactive_user -> {
                        MBTriggerInactiveUser(days = jTr.getInt("days"))
                    }

                    MBTriggersConstants.event -> {
                        var times = -1
                        var event_name: String? = null
                        var metadata: MBEventMetadata? = null

                        if (MBCommonMethods.isJSONOk(jTr, "times")) {
                            times = jTr.getInt("times")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "event_name")) {
                            event_name = jTr.getString("event_name")
                        }

                        if (MBCommonMethods.isJSONOk(jTr, "metadata")) {
                            val jMtD = jTr.getJSONObject("metadata")
                            val keys = jMtD.keys()

                            while (keys.hasNext()) {
                                val key: String = keys.next()
                                try {
                                    val value: String = jMtD.getString(key)
                                    metadata = MBEventMetadata(key, value)
                                } catch (e: JSONException) {
                                }
                            }
                        }

                        MBTriggerEvent(times = times, event_name = event_name, metadata = metadata)
                    }

                    MBTriggersConstants.tag_change -> {
                        val operator = when (jTr.getString("operator")) {
                            "=" -> TagChangeOperator.EQUALS
                            else -> TagChangeOperator.NOT_EQUAL
                        }

                        MBTriggerTagChange(tag = jTr.getString("tag"), value = jTr.getString("value"), operator = operator)
                    }

                    else -> {
                        MBTrigger("null")
                    }
                }

                if (MBCommonMethods.isJSONOk(jTr, "history")) {
                    trigger.history = deJsonizeHistory(jTr.getString("history"))
                }

                if (MBCommonMethods.isJSONOk(jTr, "id")) {
                    trigger.id = jTr.getLong("id")
                }

                triggers.add(trigger)
            }

            return MBMessageTriggers(method, triggers)
        }
    }
}