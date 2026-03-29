package sevak.hovhannisyan.myproject.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sevak.hovhannisyan.myproject.api.AlphaVantageService;
import sevak.hovhannisyan.myproject.api.CoinGeckoService;
import sevak.hovhannisyan.myproject.api.FinnhubService;
import sevak.hovhannisyan.myproject.api.FreeCryptoService;
import sevak.hovhannisyan.myproject.api.OpenRouterService;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    public static final String GOAL_PREFS = "goal_prefs";
    public static final String THEME_PREFS = "theme_prefs";
    private static final String ALPHAVANTAGE_BASE_URL = "https://www.alphavantage.co/";
    private static final String FINNHUB_BASE_URL = "https://finnhub.io/";
    private static final String COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/";
    
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/";
    
    private static final String FREE_CRYPTO_BASE_URL = "https://api.freecryptoapi.com/v1/";

    @Provides
    @Singleton
    @Named(GOAL_PREFS)
    public SharedPreferences provideGoalSharedPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(GOAL_PREFS, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    @Named(THEME_PREFS)
    public SharedPreferences provideThemeSharedPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseFirestore provideFirebaseFirestore() {
        return FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public AlphaVantageService provideAlphaVantageService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(ALPHAVANTAGE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AlphaVantageService.class);
    }

    @Provides
    @Singleton
    public FinnhubService provideFinnhubService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(FINNHUB_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FinnhubService.class);
    }

    @Provides
    @Singleton
    public CoinGeckoService provideCoinGeckoService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(COINGECKO_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CoinGeckoService.class);
    }

    @Provides
    @Singleton
    public OpenRouterService provideOpenRouterService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(OPENROUTER_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterService.class);
    }

    @Provides
    @Singleton
    public FreeCryptoService provideFreeCryptoService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(FREE_CRYPTO_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FreeCryptoService.class);
    }
}
