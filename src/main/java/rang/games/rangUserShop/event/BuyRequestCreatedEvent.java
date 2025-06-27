package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.BuyRequest;

public class BuyRequestCreatedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BuyRequest buyRequest;
    private final Player requester;

    public BuyRequestCreatedEvent(BuyRequest buyRequest, Player requester) {
        this.buyRequest = buyRequest;
        this.requester = requester;
    }

    public BuyRequest getBuyRequest() {
        return buyRequest;
    }

    public Player getRequester() {
        return requester;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}