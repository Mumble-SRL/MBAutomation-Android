package mumble.mburger.mbautomation

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import mumble.mburger.mbautomation.MBAutomationData.MBTriggers.MBUserEvent
import java.util.*

val EVENTS_DATABASE_VERSION = 1
val EVENTS_DATABASE_NAME = "iam.db"

internal class MBAutomationEventsDBHelper(context: Context?) : SQLiteOpenHelper(context, EVENTS_DATABASE_NAME, null, EVENTS_DATABASE_VERSION) {

    val TABLE_MBUSER_EVENTS = "events"

    val COLUMN_ID = "_id"
    val COLUMN_EVENT = "event"
    val COLUMN_METADATA = "metadata"
    val COLUMN_TIMESTAMP = "timestamp"
    val COLUMN_SENDING = "sending"

    val DATABASE_CREATE_MBUSER_EVENTS = "CREATE TABLE " + TABLE_MBUSER_EVENTS + " ( " +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_EVENT + " TEXT, " +
            COLUMN_METADATA + " TEXT, " +
            COLUMN_TIMESTAMP + " LONG, " +
            COLUMN_SENDING + " INTEGER)"

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(DATABASE_CREATE_MBUSER_EVENTS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MBUSER_EVENTS")
        onCreate(db)
    }

    fun clearAll(context: Context) {
        context.deleteDatabase(DATABASE_NAME)
    }

    val allEvents: ArrayList<MBUserEvent>
        get() {
            val events = ArrayList<MBUserEvent>()
            val query = ("SELECT * FROM " + TABLE_MBUSER_EVENTS
                    + " WHERE " + COLUMN_SENDING + " = 0"
                    + " ORDER BY " + COLUMN_TIMESTAMP)
            val db = this.writableDatabase
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    events.add(getSingleEvent(cursor))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return events
        }

    fun setSending(events: ArrayList<MBUserEvent>) {
        val db = this.writableDatabase
        for (i in events.indices) {
            val event = events[i]
            val cv = ContentValues()
            cv.put(COLUMN_SENDING, 1)
            db.update(TABLE_MBUSER_EVENTS, cv, COLUMN_ID + " = " + event.id, null)
        }
        db.close()
    }

    fun removeSending(events: ArrayList<MBUserEvent>) {
        val db = this.writableDatabase
        for (i in events.indices) {
            val event = events[i]
            val cv = ContentValues()
            cv.put(COLUMN_SENDING, 0)
            db.update(TABLE_MBUSER_EVENTS, cv, COLUMN_ID + " = " + event.id, null)
        }
        db.close()
    }

    fun getSingleEvent(cursor: Cursor): MBUserEvent {
        val id = cursor.getInt(0)
        val event = cursor.getString(1)
        val metadata = cursor.getString(2)
        val timestamp = cursor.getLong(3)
        val sending = cursor.getInt(4) == 1
        return MBUserEvent(id, event, metadata, timestamp, sending)
    }

    fun addEvent(event: String, metadata: String?, sending: Boolean) {
        val db = this.writableDatabase
        val values = createContentValuesMBUserEvent(event, metadata, sending)
        db.insert(TABLE_MBUSER_EVENTS, null, values)
        db.close()
    }

    fun deleteEvents(events: ArrayList<MBUserEvent>) {
        val db = this.writableDatabase
        val builder = StringBuilder("(")
        for (i in events.indices) {
            builder.append(events[i].id)
            if (i + 1 != events.size) {
                builder.append(",")
            }
        }
        builder.append(")")
        db.delete(TABLE_MBUSER_EVENTS, "$COLUMN_ID IN $builder", null)
        db.close()
    }

    private fun createContentValuesMBUserEvent(event: String, metadata: String?, sending: Boolean): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_EVENT, event)
        values.put(COLUMN_METADATA, metadata)
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        if (sending) {
            values.put(COLUMN_SENDING, 1)
        } else {
            values.put(COLUMN_SENDING, 0)
        }
        return values
    }
}