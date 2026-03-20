package sevak.hovhannisyan.myproject.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides
    @Singleton
    public TransactionRepository provideTransactionRepository(TransactionDao transactionDao) {
        return new TransactionRepository(transactionDao);
    }
}
