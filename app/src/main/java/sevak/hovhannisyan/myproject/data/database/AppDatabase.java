package sevak.hovhannisyan.myproject.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import sevak.hovhannisyan.myproject.data.converter.DateConverter;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Room database class for the SAVE app.
 * Manages the database instance and provides access to DAOs.
 */
@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;
    
    private static final String DATABASE_NAME = "save_database";
    
    public abstract TransactionDao transactionDao();
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
