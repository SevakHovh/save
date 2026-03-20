package sevak.hovhannisyan.myproject.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sevak.hovhannisyan.myproject.api.AlphaVantageService;
import sevak.hovhannisyan.myproject.api.StockResponse;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;

/**
 * Modernized ViewModel for MainActivity using Hilt.
 */
@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private final TransactionRepository repository;
    private final AlphaVantageService apiService;
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<Double> totalIncome;
    private final LiveData<Double> totalExpense;
    private final LiveData<Double> balance;
    
    private final MutableLiveData<List<StockResponse.GlobalQuote>> marketData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMarketLoading = new MutableLiveData<>(false);
    
    private static final String API_KEY = "1ZMFDAF3ZUAW81N7";
    
    // Dashboard preview
    private static final String[] DASHBOARD_SYMBOLS = {"AAPL", "BTCUSD"};
    
    // Expanded lists for Market Explorer
    private static final String[] STOCK_SYMBOLS = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX"};
    private static final String[] CRYPTO_SYMBOLS = {"BTCUSD", "ETHUSD", "BNBUSD", "SOLUSD", "XRPUSD", "ADAUSD", "DOGEUSD"};
    private static final String[] INDEX_SYMBOLS = {"SPY", "QQQ", "DIA", "IWM"};
    private static final String[] FOREX_SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD"};
    
    @Inject
    public MainViewModel(TransactionRepository repository, AlphaVantageService apiService) {
        this.repository = repository;
        this.apiService = apiService;
        allTransactions = repository.getAllTransactions();
        totalIncome = repository.getTotalIncome();
        totalExpense = repository.getTotalExpense();
        balance = repository.getBalance();
    }
    
    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
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
    
    public LiveData<List<StockResponse.GlobalQuote>> getMarketData() {
        return marketData;
    }

    public LiveData<Boolean> getIsMarketLoading() {
        return isMarketLoading;
    }

    public void fetchMarketData() {
        if (marketData.getValue() != null && !marketData.getValue().isEmpty()) return;
        fetchData(DASHBOARD_SYMBOLS);
    }

    public void fetchStocks() { fetchData(STOCK_SYMBOLS); }
    public void fetchCrypto() { fetchData(CRYPTO_SYMBOLS); }
    public void fetchIndices() { fetchData(INDEX_SYMBOLS); }
    public void fetchForex() { fetchData(FOREX_SYMBOLS); }

    private void fetchData(String[] symbols) {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>()); // Clear old data
        List<StockResponse.GlobalQuote> quotes = new ArrayList<>();
        final int[] count = {0};

        for (String symbol : symbols) {
            apiService.getQuote("GLOBAL_QUOTE", symbol, API_KEY).enqueue(new Callback<StockResponse>() {
                @Override
                public void onResponse(Call<StockResponse> call, Response<StockResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getGlobalQuote() != null) {
                        quotes.add(response.body().getGlobalQuote());
                    }
                    checkFinished();
                }

                @Override
                public void onFailure(Call<StockResponse> call, Throwable t) {
                    checkFinished();
                }

                private void checkFinished() {
                    count[0]++;
                    if (count[0] == symbols.length) {
                        marketData.postValue(quotes);
                        isMarketLoading.postValue(false);
                    }
                }
            });
        }
    }
    
    public void insertTransaction(Transaction transaction) {
        repository.insertTransaction(transaction);
    }
}
