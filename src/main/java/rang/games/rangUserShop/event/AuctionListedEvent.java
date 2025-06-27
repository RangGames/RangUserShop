package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.AuctionItem;

public class AuctionListedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionItem auctionItem;
    private final Player seller;

    public AuctionListedEvent(AuctionItem auctionItem, Player seller) {
        this.auctionItem = auctionItem;
        this.seller = seller;
    }

    public AuctionItem getAuctionItem() {
        return auctionItem;
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