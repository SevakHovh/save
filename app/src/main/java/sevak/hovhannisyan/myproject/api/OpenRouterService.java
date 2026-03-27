package sevak.hovhannisyan.myproject.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface OpenRouterService {
    /**
     * Using @Url or a full path in @POST ensures the URL is exactly as intended,
     * overriding any baseUrl issues.
     */
    @POST("https://openrouter.ai/api/v1/chat/completions")
    Call<OpenRouterResponse> analyzeBill(
        @Header("Authorization") String authorization,
        @Header("HTTP-Referer") String siteUrl,
        @Header("X-Title") String siteName,
        @Body OpenRouterRequest request
    );
}
