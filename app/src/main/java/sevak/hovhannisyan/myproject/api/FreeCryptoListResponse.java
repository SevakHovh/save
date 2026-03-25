package sevak.hovhannisyan.myproject.api;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Wrapper class for FreeCryptoAPI response which typically returns a nested list.
 */
@Keep
public class FreeCryptoListResponse {
    @SerializedName(value = "symbols", alternate = {"data", "crypto", "results"})
    private List<FreeCryptoResponse> symbols;

    public List<FreeCryptoResponse> getSymbols() {
        return symbols;
    }
}
