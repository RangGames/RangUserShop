package rang.games.rangUserShop.data;

import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.util.ItemHashUtil;

import java.util.UUID;

public class AuctionItem {
    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final String itemHash;
    private final double startPrice;
    private final double buyNowPrice;
    private double currentBid;
    private UUID highestBidderUuid;
    private String highestBidderName;
    private final long startTimestamp;
    private final long endTimestamp;
    private String status;

    public AuctionItem(int id, UUID sellerUuid, String sellerName, ItemStack itemStack, double startPrice, double buyNowPrice, double currentBid, UUID highestBidderUuid, String highestBidderName, long startTimestamp, long endTimestamp, String status) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        this.startPrice = startPrice;
        this.buyNowPrice = buyNowPrice;
        this.currentBid = currentBid;
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getItemHash() {
        return itemHash;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getBuyNowPrice() {
        return buyNowPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public UUID getHighestBidderUuid() {
        return highestBidderUuid;
    }

    public String getHighestBidderName() {
        return highestBidderName;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public void setHighestBidder(UUID highestBidderUuid, String highestBidderName) {
        this.highestBidderUuid = highestBidderUuid;
        this.highestBidderName = highestBidderName;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}