package sevak.hovhannisyan.myproject.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import sevak.hovhannisyan.myproject.data.converter.ChatTypeConverter;
import sevak.hovhannisyan.myproject.data.converter.DateConverter;
import sevak.hovhannisyan.myproject.data.dao.ChatDao;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.ChatSession;
import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Main Room database class for the app.
 * Version incremented to 7 to resolve schema integrity crash.
 */
@Database(entities = {Transaction.class, ChatMessage.class, ChatSession.class}, version = 7, exportSchema = false)
@TypeConverters({DateConverter.class, ChatTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract ChatDao chatDao();
}
