package sevak.hovhannisyan.myproject.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface BrevoService {
    @POST("v3/smtp/email")
    Call<MailResponse> sendEmail(
        @Header("api-key") String apiKey,
        @Body RequestBody body
    );
}
