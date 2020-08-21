package mumble.mburger.mbautomation

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import mumble.mburger.mbautomation.MBAutomationData.MBUserEvent
import mumble.mburger.mbautomation.MBAutomationData.MBUserView
import java.util.*

val EVENTS_DATABASE_VERSION = 1
val EVENTS_DATABASE_NAME = "eventview.db"

internal class MBAutomationEventsDBHelper(context: Context?) : SQLiteOpenHelper(context, EVENTS_DATABASE_NAME, null, EVENTS_DATABASE_VERSION) {

    val TABLE_MBUSER_EVENTS = "events"
    val TABLE_MBUSER_VIEWS = "views"

    val COLUMN_EVENT_ID = "_id"
    val COLUMN_EVENT = "event"
    val COLUMN_EVENT_NAME = "event_name"
    val COLUMN_EVENT_METADATA = "metadata"
    val COLUMN_EVENT_TIMESTAMP = "timestamp"
    val COLUMN_EVENT_SENDING = "sending"

    val COLUMN_VIEW_ID = "_id"
    val COLUMN_VIEW = "event"
    val COLUMN_VIEW_TIMESTAMP = "timestamp"
    val COLUMN_VIEW_SENDING = "sending"

    val DATABASE_CREATE_MBUSER_EVENTS = "CREATE TABLE " + TABLE_MBUSER_EVENTS + " ( " +
            COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_EVENT + " TEXT, " +
            COLUMN_EVENT_NAME + " TEXT, " +
            COLUMN_EVENT_METADATA + " TEXT, " +
            COLUMN_EVENT_TIMESTAMP + " LONG, " +
            COLUMN_EVENT_SENDING + " INTEGER)"

    val DATABASE_CREATE_MBUSER_VIEWS = "CREATE TABLE " + TABLE_MBUSER_VIEWS + " ( " +
            COLUMN_VIEW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_VIEW + " TEXT, " +
            COLUMN_VIEW_TIMESTAMP + " LONG, " +
            COLUMN_VIEW_SENDING + " INTEGER)"

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(DATABASE_CREATE_MBUSER_EVENTS)
        database.execSQL(DATABASE_CREATE_MBUSER_VIEWS)
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
                    + " WHERE " + COLUMN_EVENT_SENDING + " = 0"
                    + " ORDER BY " + COLUMN_EVENT_TIMESTAMP)
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

    fun setSendingEvents(events: ArrayList<MBUserEvent>) {
        val db = this.writableDatabase
        for (i in events.indices) {
            val event = events[i]
            val cv = ContentValues()
            cv.put(COLUMN_EVENT_SENDING, 1)
            db.update(TABLE_MBUSER_EVENTS, cv, COLUMN_EVENT_ID + " = " + event.id, null)
        }
        db.close()
    }

    fun removeSendingEvents(events: ArrayList<MBUserEvent>) {
        val db = this.writableDatabase
        for (i in events.indices) {
            val event = events[i]
            val cv = ContentValues()
            cv.put(COLUMN_EVENT_SENDING, 0)
            db.update(TABLE_MBUSER_EVENTS, cv, COLUMN_EVENT_ID + " = " + event.id, null)
        }
        db.close()
    }

    fun getSingleEvent(cursor: Cursor): MBUserEvent {
        val id = cursor.getInt(0)
        val event = cursor.getString(1)
        val event_name = cursor.getString(2)
        val metadata = cursor.getString(3)
        val timestamp = cursor.getLong(4)
        val sending = cursor.getInt(5) == 1
        return MBUserEvent(id, event, event_name, metadata, timestamp, sending)
    }

    fun addEvent(event: String, event_name: String?, metadata: String?, sending: Boolean) {
        val db = this.writableDatabase
        val values = createContentValuesMBUserEvent(event, event_name, metadata, sending)
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
        db.delete(TABLE_MBUSER_EVENTS, "$COLUMN_EVENT_ID IN $builder", null)
        db.close()
    }

    private fun createContentValuesMBUserEvent(event: String, event_name: String?, metadata: String?, sending: Boolean): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_EVENT, event)
        values.put(COLUMN_EVENT_NAME, event_name)
        values.put(COLUMN_EVENT_METADATA, metadata)
        values.put(COLUMN_EVENT_TIMESTAMP, System.currentTimeMillis())
        if (sending) {
            values.put(COLUMN_EVENT_SENDING, 1)
        } else {
            values.put(COLUMN_EVENT_SENDING, 0)
        }
        return values
    }

    //VIEWS
    val allViews: ArrayList<MBUserView>
        get() {
            val views = ArrayList<MBUserView>()
            val query = ("SELECT * FROM " + TABLE_MBUSER_VIEWS
                    + " WHERE " + COLUMN_VIEW_SENDING + " = 0"
                    + " ORDER BY " + COLUMN_VIEW_TIMESTAMP)
            val db = this.writableDatabase
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    views.add(getSingleView(cursor))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return views
        }

    fun setSendingViews(views: ArrayList<MBUserView>) {
        val db = this.writableDatabase
        for (i in views.indices) {
            val view = views[i]
            val cv = ContentValues()
            cv.put(COLUMN_VIEW_SENDING, 1)
            db.update(TABLE_MBUSER_VIEWS, cv, COLUMN_VIEW_ID + " = " + view.id, null)
        }
        db.close()
    }

    fun removeSendingViews(views: ArrayList<MBUserView>) {
        val db = this.writableDatabase
        for (i in views.indices) {
            val view = views[i]
            val cv = ContentValues()
            cv.put(COLUMN_VIEW_SENDING, 0)
            db.update(TABLE_MBUSER_VIEWS, cv, COLUMN_VIEW_ID + " = " + view.id, null)
        }
        db.close()
    }

    fun getSingleView(cursor: Cursor): MBUserView {
        val id = cursor.getInt(0)
        val view = cursor.getString(1)
        val timestamp = cursor.getLong(2)
        val sending = cursor.getInt(3) == 1
        return MBUserView(id, view, timestamp, sending)
    }

    fun addView(name:String, sending: Boolean) {
        val db = this.writableDatabase
        val values = createContentValuesMBUserView(name, sending)
        db.insert(TABLE_MBUSER_VIEWS, null, values)
        db.close()
    }

    fun deleteViews(views: ArrayList<MBUserView>) {
        val db = this.writableDatabase
        val builder = StringBuilder("(")
        for (i in views.indices) {
            builder.append(views[i].id)
            if (i + 1 != views.size) {
                builder.append(",")
            }
        }
        builder.append(")")
        db.delete(TABLE_MBUSER_VIEWS, "$COLUMN_VIEW_ID IN $builder", null)
        db.close()
    }

    private fun createContentValuesMBUserView(name: String, sending: Boolean): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_VIEW, name)
        values.put(COLUMN_VIEW_TIMESTAMP, System.currentTimeMillis())
        if (sending) {
            values.put(COLUMN_VIEW_SENDING, 1)
        } else {
            values.put(COLUMN_VIEW_SENDING, 0)
        }
        return values
    }
}