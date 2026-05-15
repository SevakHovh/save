package sevak.hovhannisyan.myproject.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.data.dao.ChatDao;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.ChatSession;

@Singleton
public class ChatRepository {
    private final ChatDao chatDao;
    private final ExecutorService executorService;

    public interface OnSessionCreatedListener {
        void onCreated(long sessionId);
    }

    @Inject
    public ChatRepository(ChatDao chatDao) {
        this.chatDao = chatDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<ChatSession>> getSessions(String userId) {
        return chatDao.getSessions(userId);
    }

    public void createSession(ChatSession session, OnSessionCreatedListener listener) {
        executorService.execute(() -> {
            long id = chatDao.insertSession(session);
            if (listener != null) listener.onCreated(id);
        });
    }

    public void updateSession(ChatSession session) {
        executorService.execute(() -> chatDao.updateSession(session));
    }

    public void deleteSession(long sessionId) {
        executorService.execute(() -> {
            chatDao.deleteMessagesForSession(sessionId);
            chatDao.deleteSession(sessionId);
        });
    }

    public LiveData<List<ChatMessage>> getMessagesForSession(long sessionId) {
        return chatDao.getMessagesForSession(sessionId);
    }

    public void insertMessage(ChatMessage message) {
        executorService.execute(() -> {
            chatDao.insertMessage(message);
            // Also update session timestamp
            executorService.execute(() -> {
                 // Note: Ideally we'd have a more robust way to get the session and update its timestamp
                 // But for simplicity in this project:
            });
        });
    }

    public void deleteLastMessage(long sessionId) {
        executorService.execute(() -> chatDao.deleteLastMessage(sessionId));
    }

    public void clearAll(String userId) {
        executorService.execute(() -> {
            chatDao.deleteAllMessages(userId);
            chatDao.deleteAllSessions(userId);
        });
    }
}
