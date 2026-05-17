package sevak.hovhannisyan.myproject.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import sevak.hovhannisyan.myproject.data.converter.ChatTypeConverter;
import sevak.hovhannisyan.myproject.data.converter.DateConverter;
import sevak.hovhannisyan.myproject.data.dao.ChatDao;
import sevak.hovhannisyan.myproject.data.dao.RecurringTransactionDao;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.ChatSession;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;
import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Main Room database class for the app.
 * Version incremented to 11 to resolve schema mismatch crashes after recent changes.
 */
@Database(entities = {Transaction.class, ChatMessage.class, ChatSession.class, RecurringTransaction.class}, version = 11, exportSchema = false)
@TypeConverters({DateConverter.class, ChatTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract ChatDao chatDao();
    public abstract RecurringTransactionDao recurringTransactionDao();
}
