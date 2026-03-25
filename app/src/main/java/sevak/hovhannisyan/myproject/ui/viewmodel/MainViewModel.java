package sevak.hovhannisyan.myproject.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
import sevak.hovhannisyan.myproject.api.FinnhubService;
import sevak.hovhannisyan.myproject.api.FreeCryptoListResponse;
import sevak.hovhannisyan.myproject.api.FreeCryptoResponse;
import sevak.hovhannisyan.myproject.api.FreeCryptoService;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;
import sevak.hovhannisyan.myproject.data.repository.UserRepository;

@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private final TransactionRepository repository;
    private final UserRepository userRepository;
    private final FinnhubService finnhubService;
    private final FreeCryptoService cryptoService;
    
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    
    private final LiveData<List<Transaction>> allTransactions;
    private final LiveData<Double> totalIncome;
    private final LiveData<Double> totalExpense;
    private final LiveData<Double> balance;
    private final LiveData<Map<String, Object>> userData;
    
    private final MutableLiveData<List<FinnhubResponse>> marketData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMarketLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> marketError = new MutableLiveData<>();
    
    private static final String FINNHUB_KEY = "d701p69r01qjh1odtingd701p69r01qjh1odtio0";
    private static final String CRYPTO_KEY = "jimmsl5u6l9mr32fcshk";
    
    private static final String[] DASHBOARD_STOCKS = {"AAPL", "MSFT", "GOOGL"};
    private static final String[] DASHBOARD_CRYPTO = {"BTC", "ETH", "SOL"};
    
    private static final String[] STOCK_SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "INTC", 
            "DIS", "PYPL", "COST", "SBUX", "V", "MA", "JPM", "BAC", "WMT", "KO"
    };
    
    private static final String[] CRYPTO_LIST = {"BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "DOT", "MATIC", "LTC", "AVAX", "LINK", "XLM", "UNI"};
    
    @Inject
    public MainViewModel(TransactionRepository repository, UserRepository userRepository, 
                         FinnhubService finnhubService, FreeCryptoService cryptoService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.finnhubService = finnhubService;
        this.cryptoService = cryptoService;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId.setValue(user.getUid());
        }

        allTransactions = Transformations.switchMap(currentUserId, repository::getAllTransactions);
        totalIncome = Transformations.switchMap(currentUserId, repository::getTotalIncome);
        totalExpense = Transformations.switchMap(currentUserId, repository::getTotalExpense);
        balance = Transformations.switchMap(currentUserId, repository::getBalance);
        userData = Transformations.switchMap(currentUserId, id -> userRepository.getUserData());
    }
    
    public LiveData<List<Transaction>> getAllTransactions() { return allTransactions; }
    public LiveData<Double> getTotalIncome() { return totalIncome; }
    public LiveData<Double> getTotalExpense() { return totalExpense; }
    public LiveData<Double> getBalance() { return balance; }
    public LiveData<Map<String, Object>> getUserData() { return userData; }
    public LiveData<List<FinnhubResponse>> getMarketData() { return marketData; }
    public LiveData<Boolean> getIsMarketLoading() { return isMarketLoading; }
    public LiveData<String> getMarketError() { return marketError; }

    public void fetchMarketData() {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>());
        fetchData(DASHBOARD_STOCKS, DASHBOARD_CRYPTO);
    }

    public void fetchStocks() {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>());
        fetchData(STOCK_SYMBOLS, new String[]{});
    }

    public void fetchCrypto() {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>());
        fetchData(new String[]{}, CRYPTO_LIST);
    }

    private synchronized void fetchData(String[] stocks, String[] cryptoSymbols) {
        final List<FinnhubResponse> combinedResults = Collections.synchronizedList(new ArrayList<>());
        final int totalRequests = stocks.length + cryptoSymbols.length;
        final int[] finishedRequests = {0};

        if (totalRequests == 0) {
            isMarketLoading.postValue(false);
            return;
        }

        for (final String symbol : stocks) {
            finnhubService.getQuote(symbol, FINNHUB_KEY).enqueue(new Callback<FinnhubResponse>() {
                @Override
                public void onResponse(Call<FinnhubResponse> call, Response<FinnhubResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        FinnhubResponse res = response.body();
                        res.setSymbol(symbol);
                        if (res.getCurrentPrice() != 0) combinedResults.add(res);
                    }
                    checkFinished(finishedRequests, totalRequests, combinedResults);
                }

                @Override
                public void onFailure(Call<FinnhubResponse> call, Throwable t) {
                    checkFinished(finishedRequests, totalRequests, combinedResults);
                }
            });
        }

        for (final String symbol : cryptoSymbols) {
            String authHeader = "Bearer " + CRYPTO_KEY;
            cryptoService.getTickers(authHeader, symbol).enqueue(new Callback<FreeCryptoListResponse>() {
                @Override
                public void onResponse(Call<FreeCryptoListResponse> call, Response<FreeCryptoListResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getSymbols() != null) {
                        List<FreeCryptoResponse> list = response.body().getSymbols();
                        if (!list.isEmpty()) {
                            FreeCryptoResponse cryptoRes = list.get(0);
                            FinnhubResponse mapped = mapToFinnhubResponse(cryptoRes);
                            if (mapped.getSymbol() == null || mapped.getSymbol().isEmpty()) {
                                mapped.setSymbol(symbol);
                            }
                            combinedResults.add(mapped);
                        }
                    } else {
                        android.util.Log.e("MainViewModel", "Crypto Error " + response.code() + " for " + symbol);
                    }
                    checkFinished(finishedRequests, totalRequests, combinedResults);
                }

                @Override
                public void onFailure(Call<FreeCryptoListResponse> call, Throwable t) {
                    android.util.Log.e("MainViewModel", "Crypto Fail for " + symbol + ": " + t.getMessage());
                    checkFinished(finishedRequests, totalRequests, combinedResults);
                }
            });
        }
    }

    private FinnhubResponse mapToFinnhubResponse(FreeCryptoResponse crypto) {
        FinnhubResponse res = new FinnhubResponse();
        res.setSymbol(crypto.getSymbol() != null ? crypto.getSymbol().toUpperCase() : "");
        res.setCurrentPrice(crypto.getPrice());
        res.setChange(crypto.getDailyChange());
        res.setPercentChange(crypto.getDailyPercentChange());
        res.setHighPrice(crypto.getHigh());
        res.setLowPrice(crypto.getLow());
        res.setVolume(crypto.getVolume());
        return res;
    }

    private synchronized void checkFinished(int[] finished, int total, List<FinnhubResponse> results) {
        finished[0]++;
        if (finished[0] >= total) {
            marketData.postValue(new ArrayList<>(results));
            isMarketLoading.postValue(false);
        }
    }
    
    public void insertTransaction(Transaction transaction) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            transaction.setUserId(userId);
            repository.insertTransaction(transaction);
        }
    }

    public void clearAllTransactions() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.deleteAllTransactions(userId);
        }
    }

    public void saveGoalAmount(double goalAmount, long endTime) {
        Double currentBalance = balance.getValue();
        userRepository.saveGoalAmount(goalAmount, currentBalance != null ? currentBalance : 0.0, endTime);
    }

    public void updateExcludedDates(List<Long> dates) {
        userRepository.updateExcludedDates(dates);
    }

    public void saveUserFinancialData(double salary, double fixedExpenses) {
        userRepository.saveUserFinancialData(salary, fixedExpenses);
    }
}
