package sevak.hovhannisyan.myproject.di;

import android.content.Context;

import androidx.room.Room;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.database.AppDatabase;

/**
 * Dagger Hilt module for providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    
    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                "save_database"
        ).build();
    }
    
    @Provides
    public static TransactionDao provideTransactionDao(AppDatabase database) {
        return database.transactionDao();
    }
}
