package sevak.hovhannisyan.myproject.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouterService {
    @POST("api/v1/chat/completions")
    Call<OpenRouterResponse> analyzeBill(
        @Header("Authorization") String authorization,
        @Header("HTTP-Referer") String siteUrl,
        @Header("X-Title") String siteName,
        @Body OpenRouterRequest request
    );
}
