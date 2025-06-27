package rang.games.rangUserShop.data;

import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.util.ItemHashUtil;

import java.util.UUID;

public class ShopItem {
    private final int id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack itemStack;
    private final String itemHash;
    private final double price;
    private int amount;
    private final long listTimestamp;
    private final long expiryTimestamp;
    private String status;

    public ShopItem(int id, UUID sellerUuid, String sellerName, ItemStack itemStack, double price, int amount, long listTimestamp, long expiryTimestamp) {
        this(id, sellerUuid, sellerName, itemStack, price, amount, listTimestamp, expiryTimestamp, "LISTED");
    }

    public ShopItem(int id, UUID sellerUuid, String sellerName, ItemStack itemStack, double price, int amount, long listTimestamp, long expiryTimestamp, String status) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        this.price = price;
        this.amount = amount;
        this.listTimestamp = listTimestamp;
        this.expiryTimestamp = expiryTimestamp;
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

    public double getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }

    public long getListTimestamp() {
        return listTimestamp;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}