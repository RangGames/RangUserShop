package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.ShopItem;

public class ShopItemListedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ShopItem shopItem;
    private final Player seller;

    public ShopItemListedEvent(ShopItem shopItem, Player seller) {
        this.shopItem = shopItem;
        this.seller = seller;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }

    public Player getSeller() {
        return seller;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}