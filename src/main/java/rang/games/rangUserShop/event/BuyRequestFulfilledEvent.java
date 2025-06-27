package rang.games.rangUserShop.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import rang.games.rangUserShop.data.BuyRequest;

public class BuyRequestFulfilledEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BuyRequest buyRequest;
    private final Player seller;
    private final int amountFulfilled;
    private final double totalPrice;

    public BuyRequestFulfilledEvent(BuyRequest buyRequest, Player seller, int amountFulfilled, double totalPrice) {
        this.buyRequest = buyRequest;
        this.seller = seller;
        this.amountFulfilled = amountFulfilled;
        this.totalPrice = totalPrice;
    }

    public BuyRequest getBuyRequest() {
        return buyRequest;
    }

    public Player getSeller() {
        return seller;
    }

    public int getAmountFulfilled() {
        return amountFulfilled;
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