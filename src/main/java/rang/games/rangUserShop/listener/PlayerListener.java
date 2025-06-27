package rang.games.rangUserShop.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import rang.games.rangUserShop.GuiManager;
import rang.games.rangUserShop.RangUserShop;

public class PlayerListener implements Listener {

    private final RangUserShop plugin;

    public PlayerListener(RangUserShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.clearPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        if (!event.getView().getTitle().startsWith(GuiManager.GUI_PREFIX)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == null ||
                    !player.getOpenInventory().getTitle().startsWith(GuiManager.GUI_PREFIX)) {
                plugin.setPlayerSearchTerm(player.getUniqueId(), null);
            }
        }, 1L);
    }
}