package sevak.hovhannisyan.myproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;

/**
 * ViewModel for MainActivity.
 * Manages UI-related data and business logic.
 */
@HiltViewModel
public class MainViewModel extends AndroidViewModel {
    
    private final TransactionRepository repository;
    private LiveData<List<Transaction>> allTransactions;
    private LiveData<Double> totalIncome;
    private LiveData<Double> totalExpense;
    private LiveData<Double> balance;
    
    private MutableLiveData<String> selectedCategory = new MutableLiveData<>();
    private MutableLiveData<String> selectedType = new MutableLiveData<>();
    
    @Inject
    public MainViewModel(@NonNull Application application, TransactionRepository repository) {
        super(application);
        this.repository = repository;
        allTransactions = repository.getAllTransactions();
        totalIncome = repository.getTotalIncome();
        totalExpense = repository.getTotalExpense();
        balance = repository.getBalance();
    }
    
    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }
    
    public LiveData<List<Transaction>> getTransactionsByType(String type) {
        return repository.getTransactionsByType(type);
    }
    
    public LiveData<List<Transaction>> getTransactionsByCategory(String category) {
        return repository.getTransactionsByCategory(category);
    }
    
    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }
    
    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }
    
    public LiveData<Double> getBalance() {
        return balance;
    }
    
    public void insertTransaction(Transaction transaction) {
        repository.insertTransaction(transaction);
    }
    
    public void updateTransaction(Transaction transaction) {
        repository.updateTransaction(transaction);
    }
    
    public void deleteTransaction(Transaction transaction) {
        repository.deleteTransaction(transaction);
    }
    
    public void deleteTransactionById(long id) {
        repository.deleteTransactionById(id);
    }
    
    public MutableLiveData<String> getSelectedCategory() {
        return selectedCategory;
    }
    
    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category);
    }
    
    public MutableLiveData<String> getSelectedType() {
        return selectedType;
    }
    
    public void setSelectedType(String type) {
        selectedType.setValue(type);
    }
}
