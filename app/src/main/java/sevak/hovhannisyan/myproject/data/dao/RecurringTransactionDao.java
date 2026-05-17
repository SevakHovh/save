package sevak.hovhannisyan.myproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;

@Dao
public interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND active = 1")
    LiveData<List<RecurringTransaction>> getActiveRecurringTransactions(String userId);

    @Query("SELECT * FROM recurring_transactions WHERE active = 1")
    List<RecurringTransaction> getAllActiveSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecurringTransaction transaction);

    @Update
    void update(RecurringTransaction transaction);

    @Delete
    void delete(RecurringTransaction transaction);
}
