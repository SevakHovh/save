package sevak.hovhannisyan.myproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import sevak.hovhannisyan.myproject.data.model.Transaction;

/**
 * Data Access Object for Transaction entity.
 * Provides methods to interact with the transactions table.
 */
@Dao
public interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByType(String type);
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(String category);
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    LiveData<Transaction> getTransactionById(long id);
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    LiveData<Double> getTotalIncome();
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    LiveData<Double> getTotalExpense();
    
    @Query("SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) FROM transactions")
    LiveData<Double> getBalance();
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransaction(Transaction transaction);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllTransactions(List<Transaction> transactions);
    
    @Update
    void updateTransaction(Transaction transaction);
    
    @Delete
    void deleteTransaction(Transaction transaction);
    
    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteTransactionById(long id);
    
    @Query("DELETE FROM transactions")
    void deleteAllTransactions();
}
