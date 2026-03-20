package sevak.hovhannisyan.myproject.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import sevak.hovhannisyan.myproject.data.converter.DateConverter;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Main Room database class for the app.
 */
@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
}
