package sevak.hovhannisyan.myproject.api;

import com.google.gson.annotations.SerializedName;

public class CoinGeckoResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("symbol")
    private String symbol;
    @SerializedName("name")
    private String name;
    @SerializedName("current_price")
    private double currentPrice;
    @SerializedName("price_change_24h")
    private double priceChange24h;
    @SerializedName("price_change_percentage_24h")
    private double priceChangePercentage24h;
    @SerializedName("image")
    private String image;

    public String getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getPriceChange24h() { return priceChange24h; }
    public double getPriceChangePercentage24h() { return priceChangePercentage24h; }
    public String getImage() { return image; }
}
