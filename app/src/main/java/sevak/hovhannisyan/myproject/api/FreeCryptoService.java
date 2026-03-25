package sevak.hovhannisyan.myproject.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface FreeCryptoService {
    /**
     * Updated to expect a wrapped list response to handle nested JSON data.
     * Endpoint: getData
     */
    @GET("getData")
    Call<FreeCryptoListResponse> getTickers(
        @Header("Authorization") String authHeader,
        @Query("symbol") String symbol
    );
}
