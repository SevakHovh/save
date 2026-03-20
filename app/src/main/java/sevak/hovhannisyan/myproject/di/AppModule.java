package sevak.hovhannisyan.myproject.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sevak.hovhannisyan.myproject.api.AlphaVantageService;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    public static final String GOAL_PREFS = "goal_prefs";
    public static final String THEME_PREFS = "theme_prefs";
    private static final String BASE_URL = "https://www.alphavantage.co/";

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
    public AlphaVantageService provideAlphaVantageService() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AlphaVantageService.class);
    }
}
