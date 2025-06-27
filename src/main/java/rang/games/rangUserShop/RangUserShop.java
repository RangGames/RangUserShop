package rang.games.rangUserShop;

import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rang.games.rangGiftBox.api.GiftBoxAPI;
import rang.games.rangUserShop.api.UserShopAPI;
import rang.games.rangUserShop.api.UserShopAPIImpl;
import rang.games.rangUserShop.command.UserShopCommand;
import rang.games.rangUserShop.data.DatabaseManager;
import rang.games.rangUserShop.listener.GuiListener;
import rang.games.rangUserShop.listener.PlayerListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class RangUserShop extends JavaPlugin {

    private static RangUserShop instance;
    private Logger log;

    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private GuiManager guiManager;
    private UserShopAPI userShopAPI;
    private GiftBoxAPI giftBoxAPI;

    public enum SortOrder {
        LATEST("최신순"),
        OLDEST("오래된 순"),
        PRICE_ASC("가격 낮은순"),
        PRICE_DESC("가격 높은순"),
        EXPIRING_SOON("만료 임박 순");

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

    public void clearPlayerData(UUID uuid) {
        playerSearchTerms.remove(uuid);
        playerCurrentMainTab.remove(uuid);
        playerCurrentPage.remove(uuid);
    }
}