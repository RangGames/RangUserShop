package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.AuctionItem;

public class AuctionEndedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionItem auctionItem;
    private final Reason reason;
    private final Player winner;

    public enum Reason {
        SOLD,
        BUY_NOW,
        NO_BIDS,
        CANCELLED
    }

    public AuctionEndedEvent(AuctionItem auctionItem, Reason reason, Player winner) {
        this.auctionItem = auctionItem;
        this.reason = reason;
        this.winner = winner;
    }

    public AuctionItem getAuctionItem() {
        return auctionItem;
    }

    public Reason getReason() {
        return reason;
    }

    public Player getWinner() {
        return winner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}