package sevak.hovhannisyan.myproject.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FinnhubService {
    @GET("api/v1/quote")
    Call<FinnhubResponse> getQuote(
        @Query("symbol") String symbol,
        @Query("token") String token
    );
}
