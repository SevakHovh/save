package sevak.hovhannisyan.myproject.di;

import android.content.Context;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import sevak.hovhannisyan.myproject.data.dao.ChatDao;
import sevak.hovhannisyan.myproject.data.dao.RecurringTransactionDao;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.db.AppDatabase;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "my-project-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    @Singleton
    public TransactionDao provideTransactionDao(AppDatabase appDatabase) {
        return appDatabase.transactionDao();
    }

    @Provides
    @Singleton
    public ChatDao provideChatDao(AppDatabase appDatabase) {
        return appDatabase.chatDao();
    }

    @Provides
    @Singleton
    public RecurringTransactionDao provideRecurringTransactionDao(AppDatabase appDatabase) {
        return appDatabase.recurringTransactionDao();
    }
}
