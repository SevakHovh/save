package sevak.hovhannisyan.myproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.ChatSession;

@Dao
public interface ChatDao {
    // Sessions
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY lastTimestamp DESC")
    LiveData<List<ChatSession>> getSessions(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSession(ChatSession session);

    @Update
    void updateSession(ChatSession session);

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    void deleteSession(long sessionId);

    // Messages
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesForSession(long sessionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessage message);

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    void deleteMessagesForSession(long sessionId);

    @Query("DELETE FROM chat_messages WHERE id = (SELECT MAX(id) FROM chat_messages WHERE sessionId = :sessionId)")
    void deleteLastMessage(long sessionId);
    
    @Query("DELETE FROM chat_sessions WHERE userId = :userId")
    void deleteAllSessions(String userId);

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    void deleteAllMessages(String userId);
}
