    package com.example.netardedrigger

    import android.content.ContentValues
    import android.content.Context
    import android.database.sqlite.SQLiteDatabase
    import android.database.sqlite.SQLiteOpenHelper

    class DBHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        companion object {
            private const val DATABASE_NAME = "AppDatabase.db"
            private const val DATABASE_VERSION = 1

            // User Table
            private const val TABLE_USERS = "Users"
            private const val COLUMN_USER_ID = "id"
            private const val COLUMN_USERNAME = "username"
            private const val COLUMN_PASSWORD = "password"

            // Chat Table
            private const val TABLE_CHAT_MESSAGES = "ChatMessages"
            private const val COLUMN_MESSAGE_ID = "id"
            private const val COLUMN_MESSAGE = "message"
            private const val COLUMN_IMAGE_PATH = "image_path"
        }

        override fun onCreate(db: SQLiteDatabase?) {
            // Create Users table
            val createUsersTable = ("CREATE TABLE $TABLE_USERS ("
                    + "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "$COLUMN_USERNAME TEXT, "
                    + "$COLUMN_PASSWORD TEXT)")
            db?.execSQL(createUsersTable)

            // Create ChatMessages table
            val createChatTable = ("CREATE TABLE $TABLE_CHAT_MESSAGES ("
                    + "$COLUMN_MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "$COLUMN_MESSAGE TEXT, "
                    + "$COLUMN_IMAGE_PATH TEXT)")
            db?.execSQL(createChatTable)
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            db?.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_MESSAGES")
            onCreate(db)
        }

        // Insert user into Users table
        fun insertUser(username: String, password: String): Boolean {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_USERNAME, username)
                put(COLUMN_PASSWORD, password)
            }
            val result = db.insert(TABLE_USERS, null, values)

            return result != -1L
        }

        // Check if user exists in Users table
        fun checkUser(username: String, password: String): Boolean {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(username, password)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            return exists
        }

        // Insert chat message
        fun insertMessage(message: String, imagePath: String?) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_MESSAGE, message)
                put(COLUMN_IMAGE_PATH, imagePath)
            }
            db.insert(TABLE_CHAT_MESSAGES, null, values)
            db.close()
        }

        // Fetch all chat messages
        fun getAllMessages(): MutableList<Pair<String, String?>> {
            val messageList = mutableListOf<Pair<String, String?>>()
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT * FROM $TABLE_CHAT_MESSAGES", null)
            if (cursor.moveToFirst()) {
                do {
                    val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                    val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                    messageList.add(Pair(message, imagePath))
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return messageList
        }

        // Clear chat messages
        fun clearMessages() {
            val db = writableDatabase
            db.execSQL("DELETE FROM $TABLE_CHAT_MESSAGES")
            db.close()
        }
    }
