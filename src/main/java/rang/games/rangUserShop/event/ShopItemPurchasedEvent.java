package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.ShopItem;

public class ShopItemPurchasedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ShopItem shopItem;
    private final Player buyer;
    private final int amount;
    private final double totalPrice;

    public ShopItemPurchasedEvent(ShopItem shopItem, Player buyer, int amount, double totalPrice) {
        this.shopItem = shopItem;
        this.buyer = buyer;
        this.amount = amount;
        this.totalPrice = totalPrice;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }

    public Player getBuyer() {
        return buyer;
    }

    public int getAmount() {
        return amount;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}