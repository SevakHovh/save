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
import sevak.hovhannisyan.myproject.api.CoinGeckoResponse;
import sevak.hovhannisyan.myproject.api.CoinGeckoService;
import sevak.hovhannisyan.myproject.api.FinnhubResponse;
import sevak.hovhannisyan.myproject.api.FinnhubService;
import sevak.hovhannisyan.myproject.api.FreeCryptoService;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;
import sevak.hovhannisyan.myproject.data.repository.UserRepository;

@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private final TransactionRepository repository;
    private final UserRepository userRepository;
    private final FinnhubService finnhubService;
    private final CoinGeckoService coinGeckoService;
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
    
    private static final String[] DASHBOARD_STOCKS = {"AAPL", "MSFT", "GOOGL"};
    // CoinGecko uses IDs for markets: bitcoin, ethereum, solana
    private static final String DASHBOARD_CRYPTO_IDS = "bitcoin,ethereum,solana";
    
    private static final String[] STOCK_SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "INTC", 
            "DIS", "PYPL", "COST", "SBUX", "V", "MA", "JPM", "BAC", "WMT", "KO"
    };
    
    private static final String ALL_CRYPTO_IDS = "bitcoin,ethereum,solana,binancecoin,ripple,cardano,dogecoin,polkadot,matic-network,litecoin,avalanche-2,chainlink,stellar,uniswap";
    
    @Inject
    public MainViewModel(TransactionRepository repository, UserRepository userRepository, 
                         FinnhubService finnhubService, CoinGeckoService coinGeckoService, FreeCryptoService cryptoService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.finnhubService = finnhubService;
        this.coinGeckoService = coinGeckoService;
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
        fetchData(DASHBOARD_STOCKS, DASHBOARD_CRYPTO_IDS);
    }

    public void fetchStocks() {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>());
        fetchData(STOCK_SYMBOLS, "");
    }

    public void fetchCrypto() {
        isMarketLoading.setValue(true);
        marketData.setValue(new ArrayList<>());
        fetchData(new String[]{}, ALL_CRYPTO_IDS);
    }

    private synchronized void fetchData(String[] stocks, String cryptoIds) {
        final List<FinnhubResponse> combinedResults = Collections.synchronizedList(new ArrayList<>());
        final int totalStockRequests = stocks.length;
        final boolean hasCrypto = cryptoIds != null && !cryptoIds.isEmpty();
        final int totalExpectedRequests = totalStockRequests + (hasCrypto ? 1 : 0);
        final int[] finishedRequests = {0};

        if (totalExpectedRequests == 0) {
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
                    checkFinished(finishedRequests, totalExpectedRequests, combinedResults);
                }

                @Override
                public void onFailure(Call<FinnhubResponse> call, Throwable t) {
                    checkFinished(finishedRequests, totalExpectedRequests, combinedResults);
                }
            });
        }

        if (hasCrypto) {
            coinGeckoService.getMarkets("usd", cryptoIds, "market_cap_desc", 100, 1, false, "24h")
                .enqueue(new Callback<List<CoinGeckoResponse>>() {
                @Override
                public void onResponse(Call<List<CoinGeckoResponse>> call, Response<List<CoinGeckoResponse>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (CoinGeckoResponse coin : response.body()) {
                            combinedResults.add(mapCoinToFinnhub(coin));
                        }
                    }
                    checkFinished(finishedRequests, totalExpectedRequests, combinedResults);
                }

                @Override
                public void onFailure(Call<List<CoinGeckoResponse>> call, Throwable t) {
                    checkFinished(finishedRequests, totalExpectedRequests, combinedResults);
                }
            });
        }
    }

    private FinnhubResponse mapCoinToFinnhub(CoinGeckoResponse coin) {
        FinnhubResponse res = new FinnhubResponse();
        res.setSymbol(coin.getSymbol().toUpperCase());
        res.setCurrentPrice(coin.getCurrentPrice());
        res.setChange(coin.getPriceChange24h());
        res.setPercentChange(coin.getPriceChangePercentage24h());
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

    public void markDateAsCompleted(long dateInMillis) {
        userRepository.markDateAsCompleted(dateInMillis);
    }

    public void saveUserFinancialData(double salary, double fixedExpenses) {
        userRepository.saveUserFinancialData(salary, fixedExpenses);
    }
}
