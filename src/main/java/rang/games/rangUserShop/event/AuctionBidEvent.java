package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.AuctionItem;

public class AuctionBidEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final AuctionItem auctionItem;
    private final Player bidder;
    private final double bidAmount;

    public AuctionBidEvent(AuctionItem auctionItem, Player bidder, double bidAmount) {
        this.auctionItem = auctionItem;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
    }

    public AuctionItem getAuctionItem() {
        return auctionItem;
    }

    public Player getBidder() {
        return bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}