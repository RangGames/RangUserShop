package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.ShopItem;

public class ShopItemCancelledEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ShopItem shopItem;
    private final Player canceller;

    public ShopItemCancelledEvent(ShopItem shopItem, Player canceller) {
        this.shopItem = shopItem;
        this.canceller = canceller;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }

    public Player getCanceller() {
        return canceller;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}