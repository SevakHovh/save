package sevak.hovhannisyan.myproject.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import dagger.hilt.android.EntryPointAccessors;
import sevak.hovhannisyan.myproject.data.dao.RecurringTransactionDao;
import sevak.hovhannisyan.myproject.data.dao.TransactionDao;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.model.TransactionType;
import sevak.hovhannisyan.myproject.di.WorkerEntryPoint;

public class RecurringTransactionWorker extends Worker {
    private static final String TAG = "RecurringWorker";

    public RecurringTransactionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting recurring transaction processing");
        
        try {
            WorkerEntryPoint entryPoint = EntryPointAccessors.fromApplication(getApplicationContext(), WorkerEntryPoint.class);
            RecurringTransactionDao recurringDao = entryPoint.recurringTransactionDao();
            TransactionDao transactionDao = entryPoint.transactionDao();

            List<RecurringTransaction> activeList = recurringDao.getAllActiveSync();
            Date now = new Date();

            for (RecurringTransaction rt : activeList) {
                if (shouldProcess(rt, now)) {
                    processRecurring(rt, transactionDao, recurringDao, now);
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in recurring worker", e);
            return Result.retry();
        }
    }

    private boolean shouldProcess(RecurringTransaction rt, Date now) {
        if (rt.getLastProcessedDate() == null) return true;

        Calendar nextDue = Calendar.getInstance();
        nextDue.setTime(rt.getLastProcessedDate());
        
        // Use the user-defined period in days
        int days = rt.getPeriodDays() > 0 ? rt.getPeriodDays() : 30;
        nextDue.add(Calendar.DAY_OF_YEAR, days);
        
        Calendar current = Calendar.getInstance();
        current.setTime(now);

        // Process if today is the due date or we've passed it
        return current.after(nextDue) || isSameDay(current, nextDue);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void processRecurring(RecurringTransaction rt, TransactionDao tDao, RecurringTransactionDao rDao, Date now) {
        Transaction t = new Transaction();
        t.setUserId(rt.getUserId());
        t.setAmount(rt.getAmount());
        t.setCategory(rt.getCategory());
        
        // Ensure the type is correctly mapped to TransactionType constants
        String type = rt.getType();
        if (TransactionType.INCOME.equals(type)) {
            t.setType(TransactionType.INCOME);
        } else {
            t.setType(TransactionType.EXPENSE);
        }

        t.setDescription(rt.getDescription() + " (Fixed)");
        t.setDate(now);

        tDao.insertTransaction(t);

        rt.setLastProcessedDate(now);
        rDao.update(rt);
        Log.d(TAG, "Processed recurring: " + rt.getDescription());
    }
}
