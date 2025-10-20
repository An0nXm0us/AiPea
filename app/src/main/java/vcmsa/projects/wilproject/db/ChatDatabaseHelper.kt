package vcmsa.projects.wilproject.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import vcmsa.projects.wilproject.models.ChatSession
import vcmsa.projects.wilproject.models.Message

class ChatDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ChatHistory.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_CHAT_SESSIONS = "chat_sessions"
        const val TABLE_MESSAGES = "messages"

        // Common columns
        const val COLUMN_ID = "id"
        const val COLUMN_CHAT_SESSION_ID = "chat_session_id"

        // Chat sessions table columns
        const val COLUMN_TITLE = "title"
        const val COLUMN_LAST_UPDATED = "last_updated"

        // Messages table columns
        const val COLUMN_MESSAGE_TEXT = "message_text"
        const val COLUMN_IS_USER = "is_user"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create chat sessions table
        val createChatSessionsTable = """
            CREATE TABLE $TABLE_CHAT_SESSIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_LAST_UPDATED INTEGER NOT NULL
            )
        """.trimIndent()

        // Create messages table
        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CHAT_SESSION_ID INTEGER NOT NULL,
                $COLUMN_MESSAGE_TEXT TEXT NOT NULL,
                $COLUMN_IS_USER INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_CHAT_SESSION_ID) REFERENCES $TABLE_CHAT_SESSIONS($COLUMN_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createChatSessionsTable)
        db.execSQL(createMessagesTable)

        // Create index for better performance
        db.execSQL("CREATE INDEX idx_chat_session_id ON $TABLE_MESSAGES($COLUMN_CHAT_SESSION_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAT_SESSIONS")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // Chat Session operations
    fun insertChatSession(title: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_LAST_UPDATED, System.currentTimeMillis())
        }
        return db.insert(TABLE_CHAT_SESSIONS, null, values)
    }

    fun getAllChatSessionsWithMessages(): List<ChatSession> {
        val chatSessions = mutableListOf<ChatSession>()
        val db = readableDatabase

        // First get all chat sessions
        val sessionsQuery = """
            SELECT * FROM $TABLE_CHAT_SESSIONS 
            ORDER BY $COLUMN_LAST_UPDATED DESC
        """.trimIndent()

        val sessionsCursor: Cursor = db.rawQuery(sessionsQuery, null)

        if (sessionsCursor.moveToFirst()) {
            do {
                val chatSession = cursorToChatSession(sessionsCursor)
                // Load messages for this chat session
                chatSession.messages.addAll(getMessagesForChatSession(chatSession.id))
                chatSessions.add(chatSession)
            } while (sessionsCursor.moveToNext())
        }
        sessionsCursor.close()
        return chatSessions
    }

    fun getAllChatSessions(): List<ChatSession> {
        val chatSessions = mutableListOf<ChatSession>()
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_CHAT_SESSIONS 
            ORDER BY $COLUMN_LAST_UPDATED DESC
        """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                chatSessions.add(cursorToChatSession(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return chatSessions
    }

    fun getChatSession(id: Long): ChatSession? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_CHAT_SESSIONS WHERE $COLUMN_ID = ?"
        val cursor: Cursor = db.rawQuery(query, arrayOf(id.toString()))

        return if (cursor.moveToFirst()) {
            val chatSession = cursorToChatSession(cursor)
            // Load messages for this chat session
            chatSession.messages.addAll(getMessagesForChatSession(chatSession.id))
            cursor.close()
            chatSession
        } else {
            cursor.close()
            null
        }
    }

    fun updateChatSessionTitle(id: Long, newTitle: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, newTitle)
            put(COLUMN_LAST_UPDATED, System.currentTimeMillis())
        }
        db.update(TABLE_CHAT_SESSIONS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteChatSession(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_CHAT_SESSIONS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // Message operations
    fun insertMessage(chatSessionId: Long, message: Message): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CHAT_SESSION_ID, chatSessionId)
            put(COLUMN_MESSAGE_TEXT, message.text)
            put(COLUMN_IS_USER, if (message.isUser) 1 else 0)
            put(COLUMN_TIMESTAMP, message.timestamp)
        }

        // Update the chat session's last updated time
        updateChatSessionLastUpdated(chatSessionId)

        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun getMessagesForChatSession(chatSessionId: Long): MutableList<Message> {
        val messages = mutableListOf<Message>()
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MESSAGES 
            WHERE $COLUMN_CHAT_SESSION_ID = ? 
            ORDER BY $COLUMN_TIMESTAMP ASC
        """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, arrayOf(chatSessionId.toString()))

        if (cursor.moveToFirst()) {
            do {
                messages.add(cursorToMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return messages
    }

    fun getAllMessages(): List<Message> {
        val messages = mutableListOf<Message>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_MESSAGES ORDER BY $COLUMN_TIMESTAMP ASC"
        val cursor: Cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                messages.add(cursorToMessage(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return messages
    }

    fun getLastMessageForChatSession(chatSessionId: Long): Message? {
        val db = readableDatabase
        val query = """
            SELECT * FROM $TABLE_MESSAGES 
            WHERE $COLUMN_CHAT_SESSION_ID = ? 
            ORDER BY $COLUMN_TIMESTAMP DESC 
            LIMIT 1
        """.trimIndent()

        val cursor: Cursor = db.rawQuery(query, arrayOf(chatSessionId.toString()))

        return if (cursor.moveToFirst()) {
            val message = cursorToMessage(cursor)
            cursor.close()
            message
        } else {
            cursor.close()
            null
        }
    }

    fun deleteMessagesForChatSession(chatSessionId: Long) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_CHAT_SESSION_ID = ?", arrayOf(chatSessionId.toString()))
    }

    internal fun updateChatSessionLastUpdated(chatSessionId: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LAST_UPDATED, System.currentTimeMillis())
        }
        db.update(TABLE_CHAT_SESSIONS, values, "$COLUMN_ID = ?", arrayOf(chatSessionId.toString()))
    }

    private fun cursorToChatSession(cursor: Cursor): ChatSession {
        return ChatSession(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            lastUpdated = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_UPDATED))
        )
    }

    private fun cursorToMessage(cursor: Cursor): Message {
        return Message(
            text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT)),
            isUser = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_USER)) == 1,
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
        )
    }
}