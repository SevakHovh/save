package sevak.hovhannisyan.myproject.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.data.dao.RecurringTransactionDao;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;

@Singleton
public class RecurringTransactionRepository {
    private final RecurringTransactionDao recurringDao;
    private final ExecutorService executorService;

    @Inject
    public RecurringTransactionRepository(RecurringTransactionDao recurringDao) {
        this.recurringDao = recurringDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<RecurringTransaction>> getActiveRecurringTransactions(String userId) {
        return recurringDao.getActiveRecurringTransactions(userId);
    }

    public void insert(RecurringTransaction transaction) {
        executorService.execute(() -> recurringDao.insert(transaction));
    }

    public void update(RecurringTransaction transaction) {
        executorService.execute(() -> recurringDao.update(transaction));
    }

    public void delete(RecurringTransaction transaction) {
        executorService.execute(() -> recurringDao.delete(transaction));
    }
}
