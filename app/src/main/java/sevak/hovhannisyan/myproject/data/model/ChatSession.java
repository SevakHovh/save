package sevak.hovhannisyan.myproject.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions")
public class ChatSession {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private long lastTimestamp;
    private String userId;

    public ChatSession() {}

    public ChatSession(String title, String userId) {
        this.title = title;
        this.userId = userId;
        this.lastTimestamp = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
