package rang.games.rangUserShop.data;

import java.util.Date;

public class DailyPriceInfo {
    private final Date date;
    private final double averagePrice;
    private final int totalAmount;

    public DailyPriceInfo(Date date, double averagePrice, int totalAmount) {
        this.date = date;
        this.averagePrice = averagePrice;
        this.totalAmount = totalAmount;
    }

    public Date getDate() {
        return date;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public int getTotalAmount() {
        return totalAmount;
    }
}