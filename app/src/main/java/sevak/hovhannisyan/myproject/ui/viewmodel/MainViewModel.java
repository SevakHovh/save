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
import sevak.hovhannisyan.myproject.data.model.ChatMessage;
import sevak.hovhannisyan.myproject.data.model.ChatSession;
import sevak.hovhannisyan.myproject.data.model.RecurringTransaction;
import sevak.hovhannisyan.myproject.data.model.Transaction;
import sevak.hovhannisyan.myproject.data.repository.ChatRepository;
import sevak.hovhannisyan.myproject.data.repository.RecurringTransactionRepository;
import sevak.hovhannisyan.myproject.data.repository.TransactionRepository;
import sevak.hovhannisyan.myproject.data.repository.UserRepository;

@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final ChatRepository chatRepo;
    private final RecurringTransactionRepository recurringRepo;
    
    private final FinnhubService finnhubApi;
    private final CoinGeckoService geckoApi;
    private final FreeCryptoService cryptoApi;
    
    private final MutableLiveData<String> uid = new MutableLiveData<>();
    
    private final LiveData<List<Transaction>> transactionsList;
    private final LiveData<Double> incomeVal;
    private final LiveData<Double> expenseVal;
    private final LiveData<Double> totalBalance;
    private final LiveData<Map<String, Object>> userMap;
    
    private final MutableLiveData<List<FinnhubResponse>> marketList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMarket = new MutableLiveData<>(false);

    private final LiveData<List<ChatSession>> chatSessions;
    private final MutableLiveData<Long> currentSessionId = new MutableLiveData<>(-1L);
    private final LiveData<List<ChatMessage>> currentSessionMessages;
    
    private final LiveData<List<RecurringTransaction>> recurringTransactions;
    
    private final MutableLiveData<Boolean> aiIsTyping = new MutableLiveData<>(false);
    
    private static final String API_KEY = "d701p69r01qjh1odtingd701p69r01qjh1odtio0";
    
    private static final String[] STOCK_SYMBOLS = {
            "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "AMD", "INTC", 
            "DIS", "PYPL", "COST", "SBUX", "V", "MA", "JPM", "BAC", "WMT", "KO"
    };
    
    private static final String ALL_CRYPTO_IDS = "bitcoin,ethereum,solana,binancecoin,ripple,cardano,dogecoin,polkadot,matic-network,litecoin,avalanche-2,chainlink,stellar,uniswap";

    @Inject
    public MainViewModel(TransactionRepository repository, UserRepository userRepository, ChatRepository chatRepository,
                         RecurringTransactionRepository recurringRepository,
                         FinnhubService finnhubService, CoinGeckoService coinGeckoService, FreeCryptoService cryptoService) {
        this.transactionRepo = repository;
        this.userRepo = userRepository;
        this.chatRepo = chatRepository;
        this.recurringRepo = recurringRepository;
        this.finnhubApi = finnhubService;
        this.geckoApi = coinGeckoService;
        this.cryptoApi = cryptoService;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid.setValue(user.getUid());
        }

        transactionsList = Transformations.switchMap(uid, transactionRepo::getAllTransactions);
        incomeVal = Transformations.switchMap(uid, transactionRepo::getTotalIncome);
        expenseVal = Transformations.switchMap(uid, transactionRepo::getTotalExpense);
        totalBalance = Transformations.switchMap(uid, transactionRepo::getBalance);
        userMap = Transformations.switchMap(uid, id -> userRepo.getUserData());
        
        chatSessions = Transformations.switchMap(uid, chatRepo::getSessions);
        currentSessionMessages = Transformations.switchMap(currentSessionId, id -> {
            if (id == -1L) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return chatRepo.getMessagesForSession(id);
        });
        
        recurringTransactions = Transformations.switchMap(uid, recurringRepo::getActiveRecurringTransactions);
    }
    
    public LiveData<List<Transaction>> getAllTransactions() { return transactionsList; }
    public LiveData<Double> getTotalIncome() { return incomeVal; }
    public LiveData<Double> getTotalExpense() { return expenseVal; }
    public LiveData<Double> getBalance() { return totalBalance; }
    public LiveData<Map<String, Object>> getUserData() { return userMap; }
    public LiveData<List<FinnhubResponse>> getMarketData() { return marketList; }
    public LiveData<Boolean> getIsMarketLoading() { return loadingMarket; }

    // Chat Session methods
    public LiveData<List<ChatSession>> getChatSessions() { return chatSessions; }
    public void setCurrentSessionId(long id) { currentSessionId.setValue(id); }
    public LiveData<Long> getCurrentSessionId() { return currentSessionId; }
    public LiveData<List<ChatMessage>> getCurrentSessionMessages() { return currentSessionMessages; }

    public void createNewSession(String title, ChatRepository.OnSessionCreatedListener listener) {
        String myId = uid.getValue();
        if (myId != null) {
            ChatSession session = new ChatSession(title, myId);
            chatRepo.createSession(session, id -> {
                currentSessionId.postValue(id);
                if (listener != null) listener.onCreated(id);
            });
        }
    }

    public void addChatMessage(ChatMessage msg) {
        String myId = uid.getValue();
        Long sessionId = currentSessionId.getValue();
        if (myId != null && sessionId != null && sessionId != -1L) {
            msg.setUserId(myId);
            msg.setSessionId(sessionId);
            chatRepo.insertMessage(msg);
        }
    }

    public void removeLastChatMessage() {
        Long sessionId = currentSessionId.getValue();
        if (sessionId != null && sessionId != -1L) {
            chatRepo.deleteLastMessage(sessionId);
        }
    }

    public void deleteSession(long sessionId) {
        chatRepo.deleteSession(sessionId);
    }

    // Recurring Transactions
    public LiveData<List<RecurringTransaction>> getRecurringTransactions() { return recurringTransactions; }
    public void addRecurringTransaction(RecurringTransaction rt) {
        String myId = uid.getValue();
        if (myId != null) {
            rt.setUserId(myId);
            recurringRepo.insert(rt);
        }
    }
    public void deleteRecurringTransaction(RecurringTransaction rt) {
        recurringRepo.delete(rt);
    }

    // Market data methods
    public void fetchMarketData() {
        loadingMarket.setValue(true);
        marketList.setValue(new ArrayList<>());
        String[] stocks = {"AAPL", "MSFT", "GOOGL"};
        String cryptoIds = "bitcoin,ethereum,solana";
        loadDataFromApis(stocks, cryptoIds);
    }

    public void fetchStocks() {
        loadingMarket.setValue(true);
        marketList.setValue(new ArrayList<>());
        loadDataFromApis(STOCK_SYMBOLS, "");
    }

    public void fetchCrypto() {
        loadingMarket.setValue(true);
        marketList.setValue(new ArrayList<>());
        loadDataFromApis(new String[]{}, ALL_CRYPTO_IDS);
    }

    private synchronized void loadDataFromApis(String[] stocks, String crypto) {
        final List<FinnhubResponse> results = Collections.synchronizedList(new ArrayList<>());
        final int expected = stocks.length + (crypto != null && !crypto.isEmpty() ? 1 : 0);
        final int[] count = {0};

        if (expected == 0) {
            loadingMarket.postValue(false);
            return;
        }

        for (final String s : stocks) {
            finnhubApi.getQuote(s, API_KEY).enqueue(new Callback<FinnhubResponse>() {
                @Override
                public void onResponse(Call<FinnhubResponse> call, Response<FinnhubResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        FinnhubResponse item = response.body();
                        item.setSymbol(s);
                        if (item.getCurrentPrice() != 0) results.add(item);
                    }
                    checkIfDone(count, expected, results);
                }

                @Override
                public void onFailure(Call<FinnhubResponse> call, Throwable t) {
                    checkIfDone(count, expected, results);
                }
            });
        }

        if (crypto != null && !crypto.isEmpty()) {
            geckoApi.getMarkets("usd", crypto, "market_cap_desc", 100, 1, false, "24h")
                .enqueue(new Callback<List<CoinGeckoResponse>>() {
                @Override
                public void onResponse(Call<List<CoinGeckoResponse>> call, Response<List<CoinGeckoResponse>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (CoinGeckoResponse c : response.body()) {
                            results.add(mapGeckoToHub(c));
                        }
                    }
                    checkIfDone(count, expected, results);
                }

                @Override
                public void onFailure(Call<List<CoinGeckoResponse>> call, Throwable t) {
                    checkIfDone(count, expected, results);
                }
            });
        }
    }

    private FinnhubResponse mapGeckoToHub(CoinGeckoResponse coin) {
        FinnhubResponse f = new FinnhubResponse();
        f.setSymbol(coin.getSymbol().toUpperCase());
        f.setCurrentPrice(coin.getCurrentPrice());
        f.setChange(coin.getPriceChange24h());
        f.setPercentChange(coin.getPriceChangePercentage24h());
        return f;
    }

    private synchronized void checkIfDone(int[] current, int total, List<FinnhubResponse> list) {
        current[0]++;
        if (current[0] >= total) {
            marketList.postValue(new ArrayList<>(list));
            loadingMarket.postValue(false);
        }
    }
    
    public void addNewTransaction(Transaction t) {
        String myId = uid.getValue();
        if (myId != null) {
            t.setUserId(myId);
            transactionRepo.insertTransaction(t);
        }
    }

    public void clearAllData() {
        String myId = uid.getValue();
        if (myId != null) {
            transactionRepo.deleteAllTransactions(myId);
            chatRepo.clearAll(myId);
        }
    }

    public void saveGoal(double amount, long time) {
        Double balance = totalBalance.getValue();
        userRepo.saveGoalAmount(amount, balance != null ? balance : 0.0, time);
    }

    public void updateNoSaveDays(List<Long> dates) {
        userRepo.updateExcludedDates(dates);
    }

    public void markDayDone(long time) {
        userRepo.markDateAsCompleted(time);
    }

    public void saveProfileInfo(double salary, double fixed) {
        userRepo.saveUserFinancialData(salary, fixed);
    }
}
