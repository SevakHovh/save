package sevak.hovhannisyan.myproject.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Repository class that abstracts data access operations.
 * Provides a clean API for the ViewModel layer, now supporting user-specific data.
 */
@Singleton
public class TransactionRepository {
    
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    
    @Inject
    public TransactionRepository(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<Transaction>> getAllTransactions(String userId) {
        return transactionDao.getAllTransactions(userId);
    }
    
    public LiveData<List<Transaction>> getTransactionsByType(String userId, String type) {
        return transactionDao.getTransactionsByType(userId, type);
    }
    
    public LiveData<List<Transaction>> getTransactionsByCategory(String userId, String category) {
        return transactionDao.getTransactionsByCategory(userId, category);
    }
    
    public LiveData<Transaction> getTransactionById(long id) {
        return transactionDao.getTransactionById(id);
    }
    
    public LiveData<Double> getTotalIncome(String userId) {
        return transactionDao.getTotalIncome(userId);
    }
    
    public LiveData<Double> getTotalExpense(String userId) {
        return transactionDao.getTotalExpense(userId);
    }
    
    public LiveData<Double> getBalance(String userId) {
        return transactionDao.getBalance(userId);
    }
    
    public void insertTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.insertTransaction(transaction));
    }
    
    public void insertAllTransactions(List<Transaction> transactions) {
        executorService.execute(() -> transactionDao.insertAllTransactions(transactions));
    }
    
    public void updateTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.updateTransaction(transaction));
    }
    
    public void deleteTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.deleteTransaction(transaction));
    }
    
    public void deleteTransactionById(long id) {
        executorService.execute(() -> transactionDao.deleteTransactionById(id));
    }
    
    public void deleteAllTransactions(String userId) {
        executorService.execute(() -> transactionDao.deleteAllTransactions(userId));
    }
}
