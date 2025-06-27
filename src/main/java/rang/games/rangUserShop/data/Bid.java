package rang.games.rangUserShop.data;

import java.util.UUID;

public class Bid {
    private final int id;
    private final int auctionId;
    private final UUID bidderUuid;
    private final String bidderName;
    private final double bidAmount;
    private final long bidTimestamp;

    public Bid(int id, int auctionId, UUID bidderUuid, String bidderName, double bidAmount, long bidTimestamp) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderUuid = bidderUuid;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTimestamp = bidTimestamp;
    }

    public int getId() {
        return id;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public UUID getBidderUuid() {
        return bidderUuid;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public long getBidTimestamp() {
        return bidTimestamp;
    }
}