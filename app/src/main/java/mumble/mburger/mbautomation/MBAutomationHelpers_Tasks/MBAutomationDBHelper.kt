package mumble.mburger.mbautomation.MBAutomationHelpers_Tasks

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import mumble.mburger.mbautomation.MBAutomationData.MBMessageWithTriggers
import mumble.mburger.mbautomation.MBAutomationParserConverter
import mumble.mburger.mbmessages.triggers.MBMessageTriggers
import org.json.JSONObject

val DATABASE_VERSION = 1
val DATABASE_NAME = "iam.db"

class MBAutomationDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val TABLE_MESSAGES = "MESSAGES"
    val COLUMN_MESSAGE_ID = "id"
    val COLUMN_MESSAGE_CONTENT = "content"

    val CREATE_CAMPAIGNS_TABLE = "CREATE TABLE " + TABLE_MESSAGES + " ( " +
            COLUMN_MESSAGE_ID + " LONG PRIMARY KEY, " +
            COLUMN_MESSAGE_CONTENT + " TEXT)"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_CAMPAIGNS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
    }

    fun dataIsAlreadyIn(db: SQLiteDatabase, id: Long): Boolean {
        val query = "Select * from $TABLE_MESSAGES where $COLUMN_MESSAGE_ID = $id"
        val cursor: Cursor = db.rawQuery(query, null)
        if (cursor.count <= 0) {
            cursor.close()
            return false
        }

        cursor.close()
        return true
    }

    fun addAMessage(id: Long, ms: MBMessageTriggers?, campaign_content: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        if (dataIsAlreadyIn(db, id)) {
            val localMess = getAMessage(id)
            if (localMess != null) {
                if ((localMess.triggers != null) && (ms != null)) {
                    var oneDifferent = false
                    for (trigger in localMess.triggers!!.triggers) {
                        for (localTrigger in ms.triggers) {
                            if (trigger.type == localTrigger.type) {
                                if (trigger.id != localTrigger.id) {
                                    oneDifferent = true
                                    break
                                }
                            }
                        }
                    }

                    if (oneDifferent) {
                        updateAMessage(db, id, campaign_content)
                    }
                } else {
                    updateAMessage(db, id, campaign_content)
                }
            } else {
                updateAMessage(db, id, campaign_content)
            }
        } else {
            values.put(COLUMN_MESSAGE_ID, id)
            values.put(COLUMN_MESSAGE_CONTENT, DatabaseUtils.sqlEscapeString(campaign_content))
            db.insert(TABLE_MESSAGES, null, values)
        }

        db.close()
    }

    fun updateMessage(id: Long, message_content: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_MESSAGE_CONTENT, DatabaseUtils.sqlEscapeString(message_content))
        db.update(TABLE_MESSAGES, values, "$COLUMN_MESSAGE_ID = $id", null)
        db.close()
    }

    fun getMessages(): ArrayList<MBMessageWithTriggers> {
        val campaigns = ArrayList<MBMessageWithTriggers>()
        val query = "SELECT * FROM $TABLE_MESSAGES"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                campaigns.add(getSingleMessage(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return campaigns
    }

    fun getAMessage(id: Long): MBMessageWithTriggers? {
        var message: MBMessageWithTriggers? = null
        val query = "SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = $id"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                message = getSingleMessage(cursor)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return message
    }

    fun getSingleMessage(cursor: Cursor): MBMessageWithTriggers {
        val jContent = cursor.getString(1)
        return MBAutomationParserConverter.deJSONIZEMessages(JSONObject(unescapeString(jContent)))
    }

    fun updateAMessage(db: SQLiteDatabase, id: Long, message_content: String) {
        val values = ContentValues()
        values.put(COLUMN_MESSAGE_CONTENT, DatabaseUtils.sqlEscapeString(message_content))
        db.update(TABLE_MESSAGES, values, "$COLUMN_MESSAGE_ID = $id", null)
    }

    fun deleteCampaign(id: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_MESSAGE_ID = $id", null)
        db.close()
    }

    fun unescapeString(string: String): String {
        var sb = StringBuilder(string)
        sb = sb.deleteCharAt(0)
        sb = sb.deleteCharAt(sb.length - 1)
        return sb.toString().replace("''".toRegex(), "'")
    }
}