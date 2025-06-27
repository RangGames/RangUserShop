package rang.games.rangUserShop.data;

import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.util.ItemHashUtil;

import java.util.UUID;

public class Transaction {
    private final int id;
    private final int itemId;
    private final UUID buyerUuid;
    private final String buyerName;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final String itemHash;
    private final double pricePerItem;
    private final int amount;
    private final long transactionTimestamp;
    private final String type;

    public Transaction(int id, int itemId, UUID buyerUuid, String buyerName, UUID sellerUuid, String sellerName, ItemStack itemStack, double pricePerItem, int amount, long transactionTimestamp, String type) {
        this.id = id;
        this.itemId = itemId;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        this.pricePerItem = pricePerItem;
        this.amount = amount;
        this.transactionTimestamp = transactionTimestamp;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public int getItemId() {
        return itemId;
    }

    public UUID getBuyerUuid() {
        return buyerUuid;
    }

    public String getBuyerName() {
        return buyerName;
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

    public double getPricePerItem() {
        return pricePerItem;
    }

    public int getAmount() {
        return amount;
    }

    public long getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public String getType() {
        return type;
    }
}