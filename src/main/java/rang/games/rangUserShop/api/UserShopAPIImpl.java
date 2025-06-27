package rang.games.rangUserShop.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.EconomyManager;
import rang.games.rangUserShop.RangUserShop;
import rang.games.rangUserShop.data.*;
import rang.games.rangUserShop.event.*;
import rang.games.rangUserShop.util.ItemHashUtil;

import java.util.List;
import java.util.UUID;

public class UserShopAPIImpl implements UserShopAPI {

    private final RangUserShop plugin;
    private final DatabaseManager dbManager;
    private final EconomyManager economyManager;

    public UserShopAPIImpl(RangUserShop plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
        this.economyManager = plugin.getEconomyManager();
    }

    @Override
    public ShopItem getListedShopItem(int id) {
        ShopItem item = dbManager.getShopItemById(id);
        if (item != null && item.getStatus().equals("LISTED") && item.getExpiryTimestamp() > System.currentTimeMillis()) {
            return item;
        }
        return null;
    }

    @Override
    public List<ShopItem> getAllListedShopItems() {
        return dbManager.getAllListedShopItems();
    }

    @Override
    public List<ShopItem> getShopItemsBySeller(UUID sellerUuid) {
        return dbManager.getShopItemsBySeller(sellerUuid);
    }

    @Override
    public boolean listItem(Player seller, ItemStack itemStack, double price, int amount) {
        if (itemStack == null || itemStack.getType().isAir() || price <= 0 || amount <= 0) {
            return false;
        }
        if (!seller.getInventory().containsAtLeast(itemStack, amount)) {
            return false;
        }

        ItemStack itemToSell = itemStack.clone();
        itemToSell.setAmount(1);

        long listTime = System.currentTimeMillis();
        long expiryTime = listTime + (48 * 60 * 60 * 1000L);

        ShopItem shopItem = new ShopItem(0, seller.getUniqueId(), seller.getName(), itemToSell, price, amount, listTime, expiryTime);
        int itemId = dbManager.saveShopItem(shopItem);

        if (itemId != -1) {
            itemStack.setAmount(itemStack.getAmount() - amount);
            seller.getInventory().removeItem(new ItemStack(itemStack.getType(), amount));
            seller.updateInventory();
            Bukkit.getPluginManager().callEvent(new ShopItemListedEvent(shopItem, seller));
            return true;
        }
        return false;
    }

    @Override
    public boolean purchaseItem(Player buyer, int shopItemId) {
        ShopItem shopItem = getListedShopItem(shopItemId);
        if (shopItem == null) {
            return false;
        }

        double totalPrice = shopItem.getPrice() * shopItem.getAmount();
        if (economyManager.getBalance(buyer) < totalPrice) {
            return false;
        }

        double finalSellerAmount = totalPrice;
        if (!buyer.hasPermission("usershop.tax.exempt")) {
            finalSellerAmount *= 0.95;
        }

        if (!economyManager.withdrawPlayer(buyer, totalPrice)) {
            return false;
        }
        if (!economyManager.depositPlayer(shopItem.getSellerUuid(), finalSellerAmount)) {
            economyManager.depositPlayer(buyer.getUniqueId(), totalPrice);
            return false;
        }

        ItemStack purchasedItem = shopItem.getItemStack().clone();
        purchasedItem.setAmount(shopItem.getAmount());
        if (!buyer.getInventory().addItem(purchasedItem).isEmpty()) {
            buyer.getWorld().dropItemNaturally(buyer.getLocation(), purchasedItem);
        }

        if (dbManager.updateShopItemStatus(shopItem.getId(), "SOLD")) {
            dbManager.saveTransaction(new Transaction(0, shopItem.getId(), buyer.getUniqueId(), buyer.getName(), shopItem.getSellerUuid(), shopItem.getSellerName(), shopItem.getItemStack(), shopItem.getPrice(), shopItem.getAmount(), System.currentTimeMillis(), "SALE"));
            Bukkit.getPluginManager().callEvent(new ShopItemPurchasedEvent(shopItem, buyer, shopItem.getAmount(), totalPrice));
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelListing(Player player, int shopItemId) {
        ShopItem shopItem = dbManager.getShopItemById(shopItemId);
        if (shopItem == null || !shopItem.getSellerUuid().equals(player.getUniqueId()) || !shopItem.getStatus().equals("LISTED")) {
            return false;
        }

        if (dbManager.updateShopItemStatus(shopItem.getId(), "CANCELLED")) {
            ItemStack cancelledItem = shopItem.getItemStack().clone();
            cancelledItem.setAmount(shopItem.getAmount());
            if (!player.getInventory().addItem(cancelledItem).isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), cancelledItem);
            }
            Bukkit.getPluginManager().callEvent(new ShopItemCancelledEvent(shopItem, player));
            return true;
        }
        return false;
    }

    @Override
    public AuctionItem getAuctionItem(int id) {
        AuctionItem item = dbManager.getAuctionItemById(id);
        if (item != null && item.getStatus().equals("ACTIVE") && item.getEndTimestamp() > System.currentTimeMillis()) {
            return item;
        }
        return null;
    }

    @Override
    public List<AuctionItem> getAllActiveAuctionItems() {
        return dbManager.getAllActiveAuctionItems();
    }

    @Override
    public boolean listAuction(Player seller, ItemStack itemStack, double startPrice, double buyNowPrice) {
        if (itemStack == null || itemStack.getType().isAir() || startPrice <= 0 || itemStack.getAmount() > 1) {
            return false;
        }
        if (buyNowPrice > 0 && buyNowPrice <= startPrice) {
            return false;
        }
        if (!seller.getInventory().containsAtLeast(itemStack, 1)) {
            return false;
        }

        ItemStack toAuction = itemStack.clone();
        toAuction.setAmount(1);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (24 * 60 * 60 * 1000L);

        AuctionItem auctionItem = new AuctionItem(0, seller.getUniqueId(), seller.getName(), toAuction, startPrice, buyNowPrice, startPrice, null, null, startTime, endTime, "ACTIVE");
        int auctionId = dbManager.saveAuctionItem(auctionItem);

        if (auctionId != -1) {
            itemStack.setAmount(itemStack.getAmount() - 1);
            seller.getInventory().removeItem(new ItemStack(itemStack.getType(), 1));
            seller.updateInventory();
            Bukkit.getPluginManager().callEvent(new AuctionListedEvent(auctionItem, seller));
            return true;
        }
        return false;
    }

    @Override
    public boolean placeBid(Player bidder, int auctionId, double bidAmount) {
        AuctionItem auctionItem = getAuctionItem(auctionId);
        if (auctionItem == null) {
            return false;
        }
        if (bidAmount <= auctionItem.getCurrentBid()) {
            return false;
        }
        if (economyManager.getBalance(bidder) < bidAmount) {
            return false;
        }

        if (dbManager.updateAuctionItemBid(auctionItem.getId(), bidAmount, bidder.getUniqueId(), bidder.getName())) {
            dbManager.saveBid(new Bid(0, auctionItem.getId(), bidder.getUniqueId(), bidder.getName(), bidAmount, System.currentTimeMillis()));
            Bukkit.getPluginManager().callEvent(new AuctionBidEvent(auctionItem, bidder, bidAmount));
            if (auctionItem.getHighestBidderUuid() != null && !auctionItem.getHighestBidderUuid().equals(bidder.getUniqueId())) {
                economyManager.depositPlayer(auctionItem.getHighestBidderUuid(), auctionItem.getCurrentBid());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean buyNowAuction(Player buyer, int auctionId) {
        AuctionItem auctionItem = getAuctionItem(auctionId);
        if (auctionItem == null || auctionItem.getBuyNowPrice() <= 0) {
            return false;
        }
        double buyNowPrice = auctionItem.getBuyNowPrice();
        if (economyManager.getBalance(buyer) < buyNowPrice) {
            return false;
        }

        double finalSellerAmount = buyNowPrice;
        if (!buyer.hasPermission("usershop.tax.exempt")) {
            finalSellerAmount *= 0.95;
        }

        if (!economyManager.withdrawPlayer(buyer, buyNowPrice)) {
            return false;
        }
        if (!economyManager.depositPlayer(auctionItem.getSellerUuid(), finalSellerAmount)) {
            economyManager.depositPlayer(buyer.getUniqueId(), buyNowPrice);
            return false;
        }

        ItemStack purchasedItem = auctionItem.getItemStack().clone();
        purchasedItem.setAmount(1);
        if (!buyer.getInventory().addItem(purchasedItem).isEmpty()) {
            buyer.getWorld().dropItemNaturally(buyer.getLocation(), purchasedItem);
        }

        if (dbManager.updateAuctionItemStatus(auctionItem.getId(), "ENDED")) {
            dbManager.saveTransaction(new Transaction(0, auctionItem.getId(), buyer.getUniqueId(), buyer.getName(), auctionItem.getSellerUuid(), auctionItem.getSellerName(), auctionItem.getItemStack(), buyNowPrice, 1, System.currentTimeMillis(), "AUCTION_PURCHASE"));
            Bukkit.getPluginManager().callEvent(new AuctionEndedEvent(auctionItem, AuctionEndedEvent.Reason.BUY_NOW, buyer));
            return true;
        }
        return false;
    }

    @Override
    public BuyRequest getBuyRequest(int id) {
        BuyRequest request = dbManager.getBuyRequestById(id);
        if (request != null && request.getStatus().equals("ACTIVE") && request.getAmountFulfilled() < request.getAmountRequested()) {
            return request;
        }
        return null;
    }

    @Override
    public List<BuyRequest> getAllActiveBuyRequests() {
        return dbManager.getAllActiveBuyRequests();
    }

    @Override
    public boolean createBuyRequest(Player requester, ItemStack itemStack, double pricePerItem, int amountRequested) {
        if (itemStack == null || itemStack.getType().isAir() || pricePerItem <= 0 || amountRequested <= 0) {
            return false;
        }
        double totalCost = pricePerItem * amountRequested;
        if (economyManager.getBalance(requester) < totalCost) {
            return false;
        }

        ItemStack requestedItem = itemStack.clone();
        requestedItem.setAmount(1);

        long requestTime = System.currentTimeMillis();

        BuyRequest buyRequest = new BuyRequest(0, requester.getUniqueId(), requester.getName(), requestedItem, pricePerItem, amountRequested, 0, requestTime, "ACTIVE");
        int requestId = dbManager.saveBuyRequest(buyRequest);

        if (requestId != -1) {
            economyManager.withdrawPlayer(requester, totalCost);
            Bukkit.getPluginManager().callEvent(new BuyRequestCreatedEvent(buyRequest, requester));
            return true;
        }
        return false;
    }

    @Override
    public boolean fulfillBuyRequest(Player seller, int buyRequestId, int amount) {
        BuyRequest buyRequest = getBuyRequest(buyRequestId);
        if (buyRequest == null || amount <= 0) {
            return false;
        }

        int availableInInventory = 0;
        for (ItemStack item : seller.getInventory().getContents()) {
            if (item != null && ItemHashUtil.generateItemHashForComparison(item).equals(buyRequest.getItemHash())) {
                availableInInventory += item.getAmount();
            }
        }
        if (availableInInventory < amount) {
            return false;
        }

        int remainingToFulfill = buyRequest.getAmountRequested() - buyRequest.getAmountFulfilled();
        int actualAmountToSell = Math.min(amount, remainingToFulfill);
        if (actualAmountToSell <= 0) {
            return false;
        }

        double totalPrice = buyRequest.getPricePerItem() * actualAmountToSell;
        if (!economyManager.depositPlayer(seller.getUniqueId(), totalPrice)) {
            return false;
        }

        ItemStack itemToRemove = buyRequest.getItemStack().clone();
        itemToRemove.setAmount(actualAmountToSell);
        seller.getInventory().removeItem(itemToRemove);
        seller.updateInventory();

        int newFulfilledAmount = buyRequest.getAmountFulfilled() + actualAmountToSell;
        boolean fullyFulfilled = newFulfilledAmount >= buyRequest.getAmountRequested();

        if (dbManager.updateBuyRequestAmountFulfilled(buyRequest.getId(), newFulfilledAmount)) {
            if (fullyFulfilled) {
                dbManager.updateBuyRequestStatus(buyRequest.getId(), "FULFILLED");
            }
            dbManager.saveTransaction(new Transaction(0, buyRequest.getId(), buyRequest.getRequesterUuid(), buyRequest.getRequesterName(), seller.getUniqueId(), seller.getName(), buyRequest.getItemStack(), buyRequest.getPricePerItem(), actualAmountToSell, System.currentTimeMillis(), "BUY_REQUEST_FULFILLMENT"));
            Bukkit.getPluginManager().callEvent(new BuyRequestFulfilledEvent(buyRequest, seller, actualAmountToSell, totalPrice));
            return true;
        }
        return false;
    }

    @Override
    public double getLowestPrice(ItemStack itemStack) {
        String itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        ShopItem lowestPriceItem = dbManager.getLowestPriceItem(itemHash);
        return lowestPriceItem != null ? lowestPriceItem.getPrice() : -1.0;
    }

    @Override
    public double getAveragePriceLast24Hours(ItemStack itemStack) {
        String itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
        List<Transaction> transactions = dbManager.getTransactionsByItemHash(itemHash, twentyFourHoursAgo, System.currentTimeMillis());

        double totalVolume = 0;
        double totalAmount = 0;
        for (Transaction tx : transactions) {
            totalVolume += tx.getPricePerItem() * tx.getAmount();
            totalAmount += tx.getAmount();
        }
        return totalAmount > 0 ? totalVolume / totalAmount : 0;
    }

    @Override
    public int getTransactionVolumeLast24Hours(ItemStack itemStack) {
        String itemHash = ItemHashUtil.generateItemHashForComparison(itemStack);
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
        List<Transaction> transactions = dbManager.getTransactionsByItemHash(itemHash, twentyFourHoursAgo, System.currentTimeMillis());

        int totalAmount = 0;
        for (Transaction tx : transactions) {
            totalAmount += tx.getAmount();
        }
        return totalAmount;
    }
}