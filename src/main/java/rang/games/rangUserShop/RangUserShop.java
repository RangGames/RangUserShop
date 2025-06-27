package rang.games.rangUserShop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rang.games.languageUtil.LanguageAPI;
import rang.games.rangGiftBox.api.GiftBoxAPI;
import rang.games.rangUserShop.api.UserShopAPI;
import rang.games.rangUserShop.api.UserShopAPIImpl;
import rang.games.rangUserShop.command.UserShopCommand;
import rang.games.rangUserShop.data.BuyRequest;
import rang.games.rangUserShop.data.DatabaseManager;
import rang.games.rangUserShop.listener.GuiListener;
import rang.games.rangUserShop.listener.PlayerListener;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RangUserShop extends JavaPlugin {

    private static RangUserShop instance;
    private Logger log;

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private GuiManager guiManager;
    private UserShopAPI userShopAPI;
    private GiftBoxAPI giftBoxAPI;
    private final DecimalFormat formatter = new DecimalFormat("#,###");


    public enum SortOrder {
        LATEST("최신순"),
        OLDEST("오래된 순"),
        PRICE_ASC("가격 낮은순"),
        PRICE_DESC("가격 높은순"),
        EXPIRING_SOON("만료 임박 순"),
        ALPHABETICAL_ASC("이름 오름차순"),
        ALPHABETICAL_DESC("이름 내림차순");

        private final String displayName;

        SortOrder(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortOrder next() {
            SortOrder[] values = SortOrder.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public enum ManagementTab {
        SELLING("판매 중인 물품", Material.CHEST),
        SOLD_EXPIRED("판매 완료/만료 물품", Material.ENDER_CHEST),
        STORAGE_INFO("보관함 안내", Material.BOOK);


        private final String displayName;
        private final Material icon;

        ManagementTab(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    public enum MainGuiTab {
        SHOP("상점", Material.DIAMOND),
        AUCTION("경매", Material.GOLD_INGOT),
        BUY_REQUESTS("구매 요청", Material.IRON_INGOT);

        private final String displayName;
        private final Material icon;

        MainGuiTab(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private SortOrder currentSortOrder = SortOrder.LATEST;
    private final Map<UUID, String> playerSearchTerms = new HashMap<>();
    private final Map<UUID, MainGuiTab> playerCurrentMainTab = new HashMap<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();
    private final Map<UUID, UUID> playerFilterSellerUuid = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        log = this.getLogger();

        saveDefaultConfig();

        this.economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            log.severe(String.format("[%s] - Vault 또는 경제 플러그인을 찾을 수 없어 플러그인을 비활성화합니다.", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupGiftBoxAPI();

        this.databaseManager = new DatabaseManager(this, getConfig());
        databaseManager.connect();

        this.guiManager = new GuiManager(this);
        this.userShopAPI = new UserShopAPIImpl(this);

        if (getCommand("유저상점") != null) {
            getCommand("유저상점").setExecutor(new UserShopCommand(this));
        } else {
            log.severe("plugin.yml에 '유저상점' 명령어가 등록되지 않았습니다!");
        }
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.cleanupExpiredItems();
            processExpiredBuyRequests();
        }, 0L, 20L * 60 * 5);

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            guiManager.processExpiredAuctions();
        }, 0L, 20L * 60);

        log.info(String.format("[%s] 플러그인이 활성화되었습니다.", getDescription().getName()));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        log.info(String.format("[%s] 플러그인이 비활성화되었습니다.", getDescription().getName()));
    }

    private void setupGiftBoxAPI() {
        if (getServer().getPluginManager().getPlugin("RangGiftBox") == null) {
            log.warning("RangGiftBox 플러그인을 찾을 수 없습니다. 우편함 기능이 비활성화됩니다.");
            this.giftBoxAPI = null;
            return;
        }
        RegisteredServiceProvider<GiftBoxAPI> rsp = getServer().getServicesManager().getRegistration(GiftBoxAPI.class);
        if (rsp == null) {
            log.warning("RangGiftBox API를 찾을 수 없습니다. 우편함 기능이 제한될 수 있습니다.");
            this.giftBoxAPI = null;
            return;
        }
        this.giftBoxAPI = rsp.getProvider();
        log.info("RangGiftBox API와 성공적으로 연동되었습니다.");
    }

    private void processExpiredBuyRequests() {
        List<BuyRequest> expiredRequests = databaseManager.getExpiredActiveBuyRequests();
        for (BuyRequest request : expiredRequests) {
            double amountToRefund = request.getPricePerItem() * (request.getAmountRequested() - request.getAmountFulfilled());

            if (amountToRefund > 0) {
                if (economyManager.depositPlayer(request.getRequesterUuid(), amountToRefund)) {
                    databaseManager.updateBuyRequestStatus(request.getId(), "EXPIRED");

                    OfflinePlayer requester = Bukkit.getOfflinePlayer(request.getRequesterUuid());
                    if (requester.isOnline()) {
                        requester.getPlayer().sendMessage(ChatColor.YELLOW + "구매 요청하신 아이템(" + LanguageAPI.getItemName(request.getItemStack()) + ")이 만료되어 " + formatter.format(amountToRefund) + "원이 환불되었습니다.");
                    }
                } else {
                    log.warning("만료된 구매 요청(ID: " + request.getId() + ")의 금액 환불에 실패했습니다. 요청자: " + request.getRequesterUuid() + ", 환불액: " + amountToRefund);
                }
            } else {
                databaseManager.updateBuyRequestStatus(request.getId(), "EXPIRED");
            }
        }
    }

    public void giveItemToPlayer(OfflinePlayer player, ItemStack item, String reason) {
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer.getInventory().addItem(item).isEmpty()) {
                onlinePlayer.sendMessage(ChatColor.GREEN + reason + "을(를) 인벤토리로 지급받았습니다.");
            } else {
                sendToGiftBoxOrDrop(onlinePlayer, item, reason);
            }
        } else {
            if (giftBoxAPI != null) {
                long sevenDaysInSeconds = 7 * 24 * 60 * 60;
                giftBoxAPI.sendGift(player.getUniqueId(), item, "서버 시스템", sevenDaysInSeconds)
                        .thenRun(() -> getLogger().info("오프라인 플레이어 " + player.getName() + "에게 " + reason + "을(를) 우편함으로 보냈습니다."))
                        .exceptionally(ex -> {
                            getLogger().log(Level.SEVERE, "우편함으로 아이템 전송 중 오류 발생 (오프라인 플레이어: " + player.getName() + "): " + ex.getMessage(), ex);
                            return null;
                        });
            } else {
                getLogger().warning("오프라인 플레이어 " + player.getName() + "에게 아이템을 지급할 수 없습니다 (RangGiftBox API 없음). 수동 지급이 필요합니다: " + item.toString());
            }
        }
    }

    public void sendToGiftBoxOrDrop(Player player, ItemStack item, String reason) {
        if (giftBoxAPI != null) {
            long sevenDaysInSeconds = 7 * 24 * 60 * 60;
            giftBoxAPI.sendGift(player.getUniqueId(), item, "서버 시스템", sevenDaysInSeconds)
                    .thenRun(() -> {
                        if (player.isOnline()) {
                            player.sendMessage(ChatColor.YELLOW + "인벤토리가 가득 차서 " + reason + "이(가) 우편함으로 지급되었습니다.");
                        }
                    })
                    .exceptionally(ex -> {
                        getLogger().log(Level.SEVERE, "우편함으로 아이템 전송 중 오류 발생 (플레이어: " + player.getName() + "): " + ex.getMessage(), ex);
                        if (player.isOnline()) {
                            player.sendMessage(ChatColor.RED + "아이템을 우편함으로 보내는 데 실패했습니다. 아이템이 바닥에 드롭됩니다.");
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                        return null;
                    });
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(ChatColor.YELLOW + "인벤토리가 가득 차서 " + reason + "이(가) 발밑에 떨어졌습니다.");
        }
    }

    public static RangUserShop getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public UserShopAPI getUserShopAPI() {
        return userShopAPI;
    }

    public GiftBoxAPI getGiftBoxAPI() {
        return giftBoxAPI;
    }

    public SortOrder getCurrentSortOrder() {
        return currentSortOrder;
    }

    public void toggleSortOrder() {
        this.currentSortOrder = this.currentSortOrder.next();
    }

    public void setPlayerSearchTerm(UUID uuid, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            playerSearchTerms.remove(uuid);
        } else {
            playerSearchTerms.put(uuid, searchTerm.trim());
        }
    }

    public String getPlayerSearchTerm(UUID uuid) {
        return playerSearchTerms.get(uuid);
    }

    public void setPlayerCurrentMainTab(UUID uuid, MainGuiTab tab) {
        playerCurrentMainTab.put(uuid, tab);
    }

    public MainGuiTab getPlayerCurrentMainTab(UUID uuid) {
        return playerCurrentMainTab.getOrDefault(uuid, MainGuiTab.SHOP);
    }

    public void setPlayerCurrentPage(UUID uuid, int page) {
        playerCurrentPage.put(uuid, page);
    }

    public int getPlayerCurrentPage(UUID uuid) {
        return playerCurrentPage.getOrDefault(uuid, 1);
    }

    public void setPlayerFilterSellerUuid(UUID playerUuid, UUID sellerUuid) {
        if (sellerUuid == null) {
            playerFilterSellerUuid.remove(playerUuid);
        } else {
            playerFilterSellerUuid.put(playerUuid, sellerUuid);
        }
    }

    public UUID getPlayerFilterSellerUuid(UUID playerUuid) {
        return playerFilterSellerUuid.get(playerUuid);
    }

    public void clearPlayerData(UUID uuid) {
        playerSearchTerms.remove(uuid);
        playerCurrentMainTab.remove(uuid);
        playerCurrentPage.remove(uuid);
        playerFilterSellerUuid.remove(uuid);
    }
}