package rang.games.rangUserShop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class EconomyManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private Economy economy = null;

    public EconomyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * Vault 경제 서비스 제공자를 찾아 economy 객체를 설정합니다.
     * @return 설정 성공 여부
     */
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * 플레이어의 잔액을 확인합니다.
     * @param player 잔액을 확인할 플레이어
     * @return 플레이어의 잔액
     */
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    /**
     * 플레이어의 계좌에서 돈을 인출합니다.
     * @param player 돈을 인출할 플레이어
     * @param amount 인출할 금액
     * @return 거래 성공 여부
     */
    public boolean withdrawPlayer(Player player, double amount) {
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    /**
     * 플레이어의 계좌에 돈을 입금합니다.
     * @param playerUUID 돈을 입금할 플레이어의 UUID
     * @param amount 입금할 금액
     * @return 거래 성공 여부
     */
    public boolean depositPlayer(java.util.UUID playerUUID, double amount) {
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerUUID);
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            log.warning(playerUUID + " 플레이어에게 돈을 입금할 수 없습니다. (존재하지 않거나 플레이 기록 없음)");
            return false;
        }
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        return response.transactionSuccess();
    }
}