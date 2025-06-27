package rang.games.rangUserShop.data;

import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.util.ItemHashUtil;

import java.util.UUID;

public class BuyRequest {
    private final int id;
    private final UUID requesterUuid;
    private final String requesterName;
    private final ItemStack itemStack;
    private final String itemHash;
    private final double pricePerItem;
    private final int amountRequested;
    private int amountFulfilled;
    private final long requestTimestamp;
    private final long expiryTimestamp;
    private String status;

    public BuyRequest(int id, UUID requesterUuid, String requesterName, ItemStack itemStack, double pricePerItem, int amountRequested, int amountFulfilled, long requestTimestamp, long expiryTimestamp, String status) {
        this.id = id;
        this.requesterUuid = requesterUuid;
        this.requesterName = requesterName;
        this.itemStack = itemStack;
        this.itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        this.pricePerItem = pricePerItem;
        this.amountRequested = amountRequested;
        this.amountFulfilled = amountFulfilled;
        this.requestTimestamp = requestTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getItemHash() {
        return itemHash;
    }

    public double getPricePerItem() {
        return pricePerItem;
    }

    public int getAmountRequested() {
        return amountRequested;
    }

    public int getAmountFulfilled() {
        return amountFulfilled;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setAmountFulfilled(int amountFulfilled) {
        this.amountFulfilled = amountFulfilled;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}