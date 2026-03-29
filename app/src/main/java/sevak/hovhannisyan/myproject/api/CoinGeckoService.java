package sevak.hovhannisyan.myproject.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CoinGeckoService {
    @GET("coins/markets")
    Call<List<CoinGeckoResponse>> getMarkets(
        @Query("vs_currency") String vsCurrency,
        @Query("ids") String ids,
        @Query("order") String order,
        @Query("per_page") int perPage,
        @Query("page") int page,
        @Query("sparkline") boolean sparkline,
        @Query("price_change_percentage") String priceChangePercentage
    );
}
