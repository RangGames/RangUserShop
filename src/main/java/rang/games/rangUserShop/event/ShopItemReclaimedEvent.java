package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.ShopItem;

public class ShopItemReclaimedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ShopItem shopItem;
    private final Player player;

    public ShopItemReclaimedEvent(ShopItem shopItem, Player player) {
        this.shopItem = shopItem;
        this.player = player;
    }

    public ShopItem getShopItem() {
        return shopItem;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}