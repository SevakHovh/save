package sevak.hovhannisyan.myproject.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AlphaVantageService {
    @GET("query")
    Call<StockResponse> getQuote(
        @Query("function") String function,
        @Query("symbol") String symbol,
        @Query("apikey") String apiKey
    );
}
