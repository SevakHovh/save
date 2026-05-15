package sevak.hovhannisyan.myproject.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    public enum Type { TEXT, TYPING, CARD }

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long sessionId;
    private String text;
    private boolean isUser;
    private long timestamp;
    private Type type;
    private String userId;

    public ChatMessage(String text, boolean isUser) {
        this(text, isUser, Type.TEXT);
    }

    public ChatMessage(String text, boolean isUser, Type type) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
        this.type = type;
    }

    public ChatMessage() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
