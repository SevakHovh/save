package sevak.hovhannisyan.myproject.api;

import androidx.annotation.Keep;
import com.google.gson.annotations.SerializedName;

/**
 * Data model for Finnhub API response.
 * The @Keep annotation prevents ProGuard/R8 from obfuscating these fields,
 * which ensures JSON parsing works correctly in release builds.
 */
@Keep
public class FinnhubResponse {
    @SerializedName("c")
    private double currentPrice;
    
    @SerializedName("d")
    private double change;
    
    @SerializedName("dp")
    private double percentChange;
    
    @SerializedName("h")
    private double highPrice;
    
    @SerializedName("l")
    private double lowPrice;
    
    @SerializedName("o")
    private double openPrice;
    
    @SerializedName("pc")
    private double previousClose;

    @SerializedName("v")
    private String volume;

    private String symbol; // Helper field to store the symbol manually after fetching

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public double getPercentChange() { return percentChange; }
    public void setPercentChange(double percentChange) { this.percentChange = percentChange; }

    public double getHighPrice() { return highPrice; }
    public void setHighPrice(double highPrice) { this.highPrice = highPrice; }

    public double getLowPrice() { return lowPrice; }
    public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }

    public double getOpenPrice() { return openPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }

    public double getPreviousClose() { return previousClose; }
    public void setPreviousClose(double previousClose) { this.previousClose = previousClose; }

    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
}
