package rang.games.rangUserShop.util;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemHashUtil {

    public static String generateItemHash(ItemStack itemStack) {
        if (itemStack == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(itemStack.getType().name()).append(":");
        sb.append(itemStack.getAmount()).append(":");

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                sb.append("name:").append(meta.getDisplayName()).append(":");
            }
            if (meta.hasLore()) {
                sb.append("lore:").append(String.join("|", meta.getLore())).append(":");
            }
            if (meta.hasEnchants()) {
                String enchantments = meta.getEnchants().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().getKey().toString()))
                        .map(e -> e.getKey().getKey().toString() + "=" + e.getValue())
                        .collect(Collectors.joining(","));
                sb.append("ench:").append(enchantments).append(":");
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.getKeys().isEmpty()) {
                String persistentData = pdc.getKeys().stream()
                        .sorted(Comparator.comparing(NamespacedKey::toString))
                        .map(key -> {
                            if (pdc.has(key, PersistentDataType.STRING)) return key.getKey() + "=" + pdc.get(key, PersistentDataType.STRING);
                            if (pdc.has(key, PersistentDataType.INTEGER)) return key.getKey() + "=" + pdc.get(key, PersistentDataType.INTEGER);
                            return key.getKey() + "=data";
                        })
                        .collect(Collectors.joining(","));
                sb.append("pdc:").append(persistentData).append(":");
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String generateItemHashForComparison(ItemStack itemStack) {
        if (itemStack == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(itemStack.getType().name()).append(":");

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                sb.append("name:").append(meta.getDisplayName()).append(":");
            }
            if (meta.hasLore()) {
                sb.append("lore:").append(String.join("|", meta.getLore())).append(":");
            }
            if (meta.hasEnchants()) {
                String enchantments = meta.getEnchants().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().getKey().toString()))
                        .map(e -> e.getKey().getKey().toString() + "=" + e.getValue())
                        .collect(Collectors.joining(","));
                sb.append("ench:").append(enchantments).append(":");
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.getKeys().isEmpty()) {
                String persistentData = pdc.getKeys().stream()
                        .sorted(Comparator.comparing(NamespacedKey::toString))
                        .map(key -> {
                            if (pdc.has(key, PersistentDataType.STRING)) return key.getKey() + "=" + pdc.get(key, PersistentDataType.STRING);
                            if (pdc.has(key, PersistentDataType.INTEGER)) return key.getKey() + "=" + pdc.get(key, PersistentDataType.INTEGER);
                            return key.getKey() + "=data";
                        })
                        .collect(Collectors.joining(","));
                sb.append("pdc:").append(persistentData).append(":");
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}