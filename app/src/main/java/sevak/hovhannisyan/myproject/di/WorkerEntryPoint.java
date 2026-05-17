package sevak.hovhannisyan.myproject.di;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import sevak.hovhannisyan.myproject.data.dao.RecurringTransactionDao;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface WorkerEntryPoint {
    RecurringTransactionDao recurringTransactionDao();
    TransactionDao transactionDao();
}
