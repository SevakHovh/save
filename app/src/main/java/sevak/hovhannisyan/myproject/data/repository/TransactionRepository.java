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
 * Provides a clean API for the ViewModel layer.
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
    
    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }
    
    public LiveData<List<Transaction>> getTransactionsByType(String type) {
        return transactionDao.getTransactionsByType(type);
    }
    
    public LiveData<List<Transaction>> getTransactionsByCategory(String category) {
        return transactionDao.getTransactionsByCategory(category);
    }
    
    public LiveData<Transaction> getTransactionById(long id) {
        return transactionDao.getTransactionById(id);
    }
    
    public LiveData<Double> getTotalIncome() {
        return transactionDao.getTotalIncome();
    }
    
    public LiveData<Double> getTotalExpense() {
        return transactionDao.getTotalExpense();
    }
    
    public LiveData<Double> getBalance() {
        return transactionDao.getBalance();
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
    
    public void deleteAllTransactions() {
        executorService.execute(() -> transactionDao.deleteAllTransactions());
    }
}
