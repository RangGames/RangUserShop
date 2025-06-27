package rang.games.rangUserShop.listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import rang.games.rangUserShop.GuiManager;
import rang.games.rangUserShop.RangUserShop;

public class GuiListener implements Listener {

    private final RangUserShop plugin;
    private final GuiManager guiManager;

    public GuiListener(RangUserShop plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGuiManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.startsWith(GuiManager.GUI_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int slot = event.getRawSlot();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        String rawTitle = ChatColor.stripColor(title.replace(GuiManager.GUI_PREFIX, ""));

        if (rawTitle.startsWith("상점") || rawTitle.startsWith("경매장") || rawTitle.startsWith("구매 요청")) {
            handleMainShopClick(player, clickedItem, slot, event.getClick());
        } else if (rawTitle.startsWith("물품 관리")) {
            handleManagementGuiClick(player, clickedItem, slot, event.getClick());
        } else if (rawTitle.startsWith("구매 확인")) {
            handlePurchaseConfirmClick(player, clickedItem, slot);
        } else if (rawTitle.startsWith("경매 입찰")) {
            handleAuctionBidGuiClick(player, clickedItem, slot);
        } else if (rawTitle.startsWith("구매 요청 이행")) {
            handleBuyRequestFulfillGuiClick(player, clickedItem, slot);
        } else if (rawTitle.startsWith("최저가 목록")) {
        }
    }

    private void handleMainShopClick(Player player, ItemStack clickedItem, int slot, ClickType clickType) {
        if (slot >= GuiManager.GUI_ITEM_SLOTS_START && slot <= GuiManager.GUI_ITEM_SLOTS_END) {
            guiManager.handleMainShopItemClick(player, clickedItem, clickType);
            return;
        }

        RangUserShop.MainGuiTab currentTab = plugin.getPlayerCurrentMainTab(player.getUniqueId());
        int currentPage = plugin.getPlayerCurrentPage(player.getUniqueId());

        switch (slot) {
            case GuiManager.MAIN_TAB_SHOP_SLOT:
                guiManager.openMainShop(player, RangUserShop.MainGuiTab.SHOP, 1);
                break;
            case GuiManager.MAIN_TAB_AUCTION_SLOT:
                guiManager.openMainShop(player, RangUserShop.MainGuiTab.AUCTION, 1);
                break;
            case GuiManager.MAIN_TAB_BUY_REQUEST_SLOT:
                guiManager.openMainShop(player, RangUserShop.MainGuiTab.BUY_REQUESTS, 1);
                break;
            case GuiManager.MAIN_PREV_PAGE_SLOT:
                if (clickedItem.getType() == Material.ARROW) {
                    guiManager.openMainShop(player, currentTab, currentPage - 1);
                }
                break;
            case GuiManager.MAIN_NEXT_PAGE_SLOT:
                if (clickedItem.getType() == Material.ARROW) {
                    guiManager.openMainShop(player, currentTab, currentPage + 1);
                }
                break;
            case GuiManager.MAIN_REFRESH_SLOT:
                guiManager.openMainShop(player, currentTab, currentPage);
                player.sendMessage(ChatColor.GREEN + "상점을 새로고침했습니다.");
                break;
            case GuiManager.MAIN_SEARCH_HELP_SLOT:
                if (clickType.isLeftClick()) {
                    guiManager.openSearchGui(player);
                } else if (clickType.isRightClick()) {
                    guiManager.displayHelp(player);
                }
                break;
            case GuiManager.MAIN_SORT_SLOT:
                plugin.toggleSortOrder();
                guiManager.openMainShop(player, currentTab, 1);
                player.sendMessage(ChatColor.GREEN + "정렬 방식이 " + plugin.getCurrentSortOrder().getDisplayName() + "로 변경되었습니다.");
                break;
            case GuiManager.MAIN_MANAGE_ITEMS_SLOT:
                guiManager.openManagementGui(player, RangUserShop.ManagementTab.SELLING, 1);
                break;
        }
    }

    private void handleManagementGuiClick(Player player, ItemStack clickedItem, int slot, ClickType clickType) {
        if (slot >= GuiManager.GUI_ITEM_SLOTS_START && slot <= GuiManager.GUI_ITEM_SLOTS_END) {
            guiManager.handleManagementItemClick(player, clickedItem, clickType);
            return;
        }

        RangUserShop.ManagementTab currentTab = guiManager.getPlayerCurrentManagementTab(player.getUniqueId());
        int currentPage = plugin.getPlayerCurrentPage(player.getUniqueId());

        switch (slot) {
            case GuiManager.MANAGEMENT_PREV_PAGE_SLOT:
                if (clickedItem.getType() == Material.ARROW) {
                    guiManager.openManagementGui(player, currentTab, currentPage - 1);
                }
                break;
            case GuiManager.MANAGEMENT_NEXT_PAGE_SLOT:
                if (clickedItem.getType() == Material.ARROW) {
                    guiManager.openManagementGui(player, currentTab, currentPage + 1);
                }
                break;
            case GuiManager.MANAGEMENT_TAB_SELLING_SLOT:
                guiManager.openManagementGui(player, RangUserShop.ManagementTab.SELLING, 1);
                break;
            case GuiManager.MANAGEMENT_TAB_SOLD_EXPIRED_SLOT:
                guiManager.openManagementGui(player, RangUserShop.ManagementTab.SOLD_EXPIRED, 1);
                break;
            case GuiManager.MANAGEMENT_TAB_STORAGE_INFO_SLOT:
                guiManager.openManagementGui(player, RangUserShop.ManagementTab.STORAGE_INFO, 1);
                break;
            case GuiManager.MANAGEMENT_BACK_TO_MAIN_SLOT:
                guiManager.openMainShop(player, plugin.getPlayerCurrentMainTab(player.getUniqueId()), plugin.getPlayerCurrentPage(player.getUniqueId()));
                break;
        }
    }

    private void handlePurchaseConfirmClick(Player player, ItemStack clickedItem, int slot) {
        ItemStack centralItem = player.getOpenInventory().getTopInventory().getItem(GuiManager.PURCHASE_DISPLAY_ITEM_SLOT);
        if (centralItem == null) {
            player.sendMessage(ChatColor.RED + "구매 정보를 찾을 수 없습니다.");
            player.closeInventory();
            return;
        }

        switch (slot) {
            case GuiManager.CONFIRM_PURCHASE_SLOT:
                if (clickedItem.getType() == Material.LIME_WOOL) {
                    guiManager.handlePurchase(player, centralItem);
                }
                break;
            case GuiManager.CANCEL_PURCHASE_SLOT:
                if (clickedItem.getType() == Material.RED_WOOL) {
                    player.sendMessage(ChatColor.YELLOW + "구매를 취소했습니다.");
                    guiManager.openMainShop(player, plugin.getPlayerCurrentMainTab(player.getUniqueId()), plugin.getPlayerCurrentPage(player.getUniqueId()));
                }
                break;
        }
    }

    private void handleAuctionBidGuiClick(Player player, ItemStack clickedItem, int slot) {
        ItemStack centralItem = player.getOpenInventory().getTopInventory().getItem(GuiManager.AUCTION_DISPLAY_ITEM_SLOT);
        if (centralItem == null) {
            player.sendMessage(ChatColor.RED + "경매 정보를 찾을 수 없습니다.");
            player.closeInventory();
            return;
        }

        switch (slot) {
            case GuiManager.AUCTION_PLACE_BID_SLOT:
                if (clickedItem.getType() == Material.LIME_WOOL) {
                    guiManager.openBidSignGui(player, centralItem);
                }
                break;
            case GuiManager.AUCTION_BUY_NOW_SLOT:
                if (clickedItem.getType() == Material.GOLD_BLOCK) {
                    guiManager.handleAuctionBuyNow(player, centralItem);
                }
                break;
            case GuiManager.AUCTION_CANCEL_SLOT:
                if (clickedItem.getType() == Material.RED_WOOL) {
                    player.sendMessage(ChatColor.YELLOW + "입찰을 취소했습니다.");
                    guiManager.openMainShop(player, RangUserShop.MainGuiTab.AUCTION, plugin.getPlayerCurrentPage(player.getUniqueId()));
                }
                break;
        }
    }

    private void handleBuyRequestFulfillGuiClick(Player player, ItemStack clickedItem, int slot) {
        ItemStack centralItem = player.getOpenInventory().getTopInventory().getItem(GuiManager.BUY_REQUEST_DISPLAY_ITEM_SLOT);
        if (centralItem == null) {
            player.sendMessage(ChatColor.RED + "구매 요청 정보를 찾을 수 없습니다.");
            player.closeInventory();
            return;
        }

        switch (slot) {
            case GuiManager.BUY_REQUEST_FULFILL_ALL_SLOT:
                if (clickedItem.getType() == Material.LIME_WOOL) {
                    guiManager.handleFulfillBuyRequest(player, centralItem, -1);
                }
                break;
            case GuiManager.BUY_REQUEST_FULFILL_PARTIAL_SLOT:
                if (clickedItem.getType() == Material.YELLOW_WOOL) {
                    guiManager.openFulfillPartialSignGui(player, centralItem);
                }
                break;
            case GuiManager.BUY_REQUEST_CANCEL_SLOT:
                if (clickedItem.getType() == Material.RED_WOOL) {
                    player.sendMessage(ChatColor.YELLOW + "판매를 취소했습니다.");
                    guiManager.openMainShop(player, RangUserShop.MainGuiTab.BUY_REQUESTS, plugin.getPlayerCurrentPage(player.getUniqueId()));
                }
                break;
        }
    }
}