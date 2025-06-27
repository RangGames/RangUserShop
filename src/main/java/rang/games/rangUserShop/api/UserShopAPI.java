package rang.games.rangUserShop.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.data.*;

import java.util.List;
import java.util.UUID;

public interface UserShopAPI {

    enum ItemType {
        SHOP_ITEM,
        AUCTION_ITEM
    }

    ShopItem getListedShopItem(int id);

    List<ShopItem> getAllListedShopItems();

    List<ShopItem> getShopItemsBySeller(UUID sellerUuid);

    boolean listItem(Player seller, ItemStack itemStack, double price, int amount);

    boolean purchaseItem(Player buyer, int shopItemId);

    boolean cancelListing(Player player, int shopItemId);

    AuctionItem getAuctionItem(int id);

    List<AuctionItem> getAllActiveAuctionItems();

    boolean listAuction(Player seller, ItemStack itemStack, double startPrice, double buyNowPrice);

    boolean placeBid(Player bidder, int auctionId, double bidAmount);

    boolean buyNowAuction(Player buyer, int auctionId);

    BuyRequest getBuyRequest(int id);

    List<BuyRequest> getAllActiveBuyRequests();

    boolean createBuyRequest(Player requester, ItemStack itemStack, double pricePerItem, int amountRequested);

    boolean fulfillBuyRequest(Player seller, int buyRequestId, int amount);

    double getLowestPrice(ItemStack itemStack);

    double getAveragePriceLast24Hours(ItemStack itemStack);

    int getTransactionVolumeLast24Hours(ItemStack itemStack);

    List<ShopItem> getShopItemsByItem(ItemStack itemStack);

    List<AuctionItem> getAuctionItemsByItem(ItemStack itemStack);

    List<BuyRequest> getBuyRequestsByItem(ItemStack itemStack);

    List<Transaction> getPlayerPurchaseHistory(UUID playerUuid, int limit);

    List<Transaction> getPlayerSalesHistory(UUID playerUuid, int limit);

    List<DailyPriceInfo> getDailyPriceInfo(ItemStack itemStack, int days);

    boolean reclaimSoldOrExpiredItem(Player player, int itemId, ItemType type);
}