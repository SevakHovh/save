package sevak.hovhannisyan.myproject.api;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

/**
 * Highly resilient data model for FreeCryptoAPI.
 * Uses @Keep to prevent obfuscation and @SerializedName alternates to match various API versions.
 */
@Keep
public class FreeCryptoResponse {
    @SerializedName(value = "symbol", alternate = {"s", "ticker", "symbol_id", "id", "name"})
    private String symbol;
    
    @SerializedName(value = "price", alternate = {"p", "price_usd", "priceUsd", "rate", "last", "current_price", "c"})
    private String price;
    
    @SerializedName(value = "dailyChange", alternate = {"dc", "change", "change_24h", "daily_change", "d", "diff"})
    private String dailyChange;
    
    @SerializedName(value = "dailyPercentChange", alternate = {"dp", "percent_change", "change_pct", "daily_percent_change", "pc", "cp"})
    private String dailyPercentChange;

    @SerializedName(value = "high", alternate = {"h", "high_24h", "day_high"})
    private String high;

    @SerializedName(value = "low", alternate = {"l", "low_24h", "day_low"})
    private String low;

    @SerializedName(value = "volume", alternate = {"v", "volume_24h", "total_volume"})
    private String volume;

    public String getSymbol() { return symbol; }

    public double getPrice() { 
        return parseSafe(price); 
    }

    public double getDailyChange() { 
        return parseSafe(dailyChange); 
    }

    public double getDailyPercentChange() { 
        return parseSafe(dailyPercentChange); 
    }

    public double getHigh() { return parseSafe(high); }
    public double getLow() { return parseSafe(low); }
    public String getVolume() { return volume != null ? volume : "-"; }

    private double parseSafe(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            // Handles cases like "65,000.50" or quoted strings from the API
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
