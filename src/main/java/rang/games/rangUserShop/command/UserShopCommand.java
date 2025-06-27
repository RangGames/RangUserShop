package rang.games.rangUserShop.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rang.games.languageUtil.LanguageAPI;
import rang.games.rangUserShop.RangUserShop;
import rang.games.rangUserShop.data.AuctionItem;
import rang.games.rangUserShop.data.BuyRequest;
import rang.games.rangUserShop.data.DatabaseManager;
import rang.games.rangUserShop.data.ShopItem;
import rang.games.rangUserShop.event.AuctionListedEvent;
import rang.games.rangUserShop.event.BuyRequestCreatedEvent;
import rang.games.rangUserShop.event.ShopItemListedEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UserShopCommand implements CommandExecutor, TabCompleter {

    private final RangUserShop plugin;
    private final DatabaseManager dbManager;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public UserShopCommand(RangUserShop plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            if (!player.hasPermission("usershop.open")) {
                player.sendMessage(ChatColor.RED + "상점을 열 권한이 없습니다.");
                return true;
            }
            plugin.getGuiManager().openMainShop(player, plugin.getPlayerCurrentMainTab(player.getUniqueId()), 1);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "판매":
                return handleSellCommand(player, args);
            case "검색":
                player.sendMessage(ChatColor.YELLOW + "이 기능은 GUI의 검색 버튼을 통해 사용할 수 있습니다.");
                return true;
            case "시세":
                return handlePriceCheckCommand(player);
            case "관리":
                if (!player.hasPermission("usershop.manage")) {
                    player.sendMessage(ChatColor.RED + "물품을 관리할 권한이 없습니다.");
                    return true;
                }
                plugin.getGuiManager().openManagementGui(player, RangUserShop.ManagementTab.SELLING, 1);
                return true;
            case "입찰":
                return handleAuctionCommand(player, args);
            case "구매요청":
                return handleBuyRequestCommand(player, args);
            case "관리자":
                return handleAdminCommand(player, args);
            case "도움말":
            case "help":
                plugin.getGuiManager().displayHelp(player);
                return true;
            default:
                player.sendMessage(ChatColor.RED + "알 수 없는 명령어입니다. /유저상점 도움말");
                return true;
        }
    }

    private boolean handleSellCommand(Player player, String[] args) {
        if (!player.hasPermission("usershop.sell")) {
            player.sendMessage(ChatColor.RED + "아이템을 판매할 권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "사용법: /유저상점 판매 <개당 가격> [수량]");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "판매할 아이템을 손에 들어주세요.");
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
            if (price <= 0) {
                player.sendMessage(ChatColor.RED + "가격은 0보다 커야 합니다.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 가격을 입력해주세요.");
            return true;
        }

        int amount = itemInHand.getAmount();
        if (args.length >= 3) {
            try {
                int specifiedAmount = Integer.parseInt(args[2]);
                if (specifiedAmount <= 0 || specifiedAmount > amount) {
                    player.sendMessage(ChatColor.RED + "판매할 수량을 1개부터 " + amount + "개까지 지정할 수 있습니다.");
                    return true;
                }
                amount = specifiedAmount;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "올바른 수량을 입력해주세요.");
                return true;
            }
        }

        ItemStack toSell = itemInHand.clone();
        toSell.setAmount(1);

        long listTime = System.currentTimeMillis();
        long expiryTime = listTime + TimeUnit.HOURS.toMillis(48);

        ShopItem shopItem = new ShopItem(0, player.getUniqueId(), player.getName(), toSell, price, amount, listTime, expiryTime);
        int itemId = dbManager.saveShopItem(shopItem);

        if (itemId != -1) {
            itemInHand.setAmount(itemInHand.getAmount() - amount);
            player.sendMessage(ChatColor.GREEN + LanguageAPI.getItemName(toSell) + " " + amount + "개를 개당 " + formatter.format(price) + "원에 판매 등록했습니다.");
            if (player.hasPermission("usershop.tax.exempt")) {
                player.sendMessage(ChatColor.AQUA + "(수수료 면제 혜택이 적용됩니다!)");
            }
            plugin.getServer().getPluginManager().callEvent(new ShopItemListedEvent(shopItem, player));
        } else {
            player.sendMessage(ChatColor.RED + "아이템 등록에 실패했습니다. 서버 관리자에게 문의하세요.");
        }
        return true;
    }

    private boolean handleAuctionCommand(Player player, String[] args) {
        if (!player.hasPermission("usershop.bid.sell")) {
            player.sendMessage(ChatColor.RED + "경매를 등록할 권한이 없습니다.");
            return true;
        }

        if (args.length < 2 || args.length > 3) {
            player.sendMessage(ChatColor.RED + "사용법: /유저상점 입찰 <시작가> [즉시구매가]");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "경매에 등록할 아이템을 손에 들어주세요.");
            return true;
        }
        if (itemInHand.getAmount() > 1) {
            player.sendMessage(ChatColor.RED + "경매는 한 번에 1개의 아이템만 등록할 수 있습니다.");
            return true;
        }

        double startPrice;
        double buyNowPrice = 0;
        try {
            startPrice = Double.parseDouble(args[1]);
            if (startPrice <= 0) {
                player.sendMessage(ChatColor.RED + "시작 가격은 0보다 커야 합니다.");
                return true;
            }
            if (args.length == 3) {
                buyNowPrice = Double.parseDouble(args[2]);
                if (buyNowPrice <= startPrice) {
                    player.sendMessage(ChatColor.RED + "즉시 구매가는 시작가보다 높아야 합니다.");
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 가격을 입력해주세요.");
            return true;
        }

        ItemStack toAuction = itemInHand.clone();
        toAuction.setAmount(1);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.HOURS.toMillis(24);

        AuctionItem auctionItem = new AuctionItem(0, player.getUniqueId(), player.getName(), toAuction, startPrice, buyNowPrice, startPrice, null, null, startTime, endTime, "ACTIVE");
        int auctionId = dbManager.saveAuctionItem(auctionItem);

        if (auctionId != -1) {
            itemInHand.setAmount(0);
            player.sendMessage(ChatColor.GREEN + LanguageAPI.getItemName(toAuction) + " 아이템을 경매에 등록했습니다. 시작가: " + formatter.format(startPrice) + "원" + (buyNowPrice > 0 ? ", 즉시구매가: " + formatter.format(buyNowPrice) + "원" : ""));
            plugin.getServer().getPluginManager().callEvent(new AuctionListedEvent(auctionItem, player));
        } else {
            player.sendMessage(ChatColor.RED + "경매 등록에 실패했습니다. 서버 관리자에게 문의하세요.");
        }
        return true;
    }

    private boolean handleBuyRequestCommand(Player player, String[] args) {
        if (!player.hasPermission("usershop.buyorder")) {
            player.sendMessage(ChatColor.RED + "구매 요청을 생성할 권한이 없습니다.");
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "사용법: /유저상점 구매요청 <개당가격> <수량>");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "구매 요청할 아이템을 손에 들어주세요.");
            return true;
        }

        double pricePerItem;
        int amountRequested;
        try {
            pricePerItem = Double.parseDouble(args[1]);
            amountRequested = Integer.parseInt(args[2]);
            if (pricePerItem <= 0 || amountRequested <= 0) {
                player.sendMessage(ChatColor.RED + "가격과 수량은 0보다 커야 합니다.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "올바른 가격 또는 수량을 입력해주세요.");
            return true;
        }

        double totalCost = pricePerItem * amountRequested;
        if (plugin.getEconomyManager().getBalance(player) < totalCost) {
            player.sendMessage(ChatColor.RED + "잔액이 부족합니다. 총 " + formatter.format(totalCost) + "원이 필요합니다.");
            return true;
        }

        ItemStack requestedItem = itemInHand.clone();
        requestedItem.setAmount(1);

        long requestTime = System.currentTimeMillis();
        long expiryTime = requestTime + TimeUnit.DAYS.toMillis(7);

        BuyRequest buyRequest = new BuyRequest(0, player.getUniqueId(), player.getName(), requestedItem, pricePerItem, amountRequested, 0, requestTime, expiryTime, "ACTIVE");

        if (!plugin.getEconomyManager().withdrawPlayer(player, totalCost)) {
            player.sendMessage(ChatColor.RED + "금액을 인출하는 데 실패했습니다. 다시 시도해주세요.");
            return true;
        }

        int requestId = dbManager.saveBuyRequest(buyRequest);

        if (requestId != -1) {
            player.sendMessage(ChatColor.GREEN + LanguageAPI.getItemName(requestedItem) + " " + amountRequested + "개를 개당 " + formatter.format(pricePerItem) + "원에 구매 요청했습니다. 총 " + formatter.format(totalCost) + "원이 예치되었습니다.");
            plugin.getServer().getPluginManager().callEvent(new BuyRequestCreatedEvent(buyRequest, player));
        } else {
            plugin.getEconomyManager().depositPlayer(player.getUniqueId(), totalCost);
            player.sendMessage(ChatColor.RED + "구매 요청 등록에 실패했습니다. 서버 관리자에게 문의하세요.");
        }
        return true;
    }

    private boolean handlePriceCheckCommand(Player player) {
        if (!player.hasPermission("usershop.price")) {
            player.sendMessage(ChatColor.RED + "시세를 조회할 권한이 없습니다.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "시세를 조회할 아이템을 손에 들어주세요.");
            return true;
        }

        plugin.getGuiManager().openDetailedPriceInfoGui(player, itemInHand, 1);
        return true;
    }

    private boolean handleAdminCommand(Player player, String[] args) {
        if (!player.hasPermission("usershop.admin")) {
            player.sendMessage(ChatColor.RED + "관리자 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "사용법: /유저상점 관리자 <reload|cleanup|auctioncleanup>");
            return true;
        }

        String adminSubCommand = args[1].toLowerCase();
        switch (adminSubCommand) {
            case "reload":
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "유저상점 플러그인 설정이 새로고침되었습니다.");
                break;
            case "cleanup":
                player.sendMessage(ChatColor.YELLOW + "만료된 상점 아이템 정리를 시작합니다...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    int cleaned = dbManager.cleanupExpiredItems();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.GREEN + String.valueOf(cleaned) + "개의 만료된 일반 상점 아이템이 정리되었습니다.");
                    });
                });
                break;
            case "auctioncleanup":
                player.sendMessage(ChatColor.YELLOW + "만료된 경매 아이템 처리를 시작합니다...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getGuiManager().processExpiredAuctions();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.GREEN + "만료된 경매 아이템 처리가 완료되었습니다.");
                    });
                });
                break;
            default:
                player.sendMessage(ChatColor.RED + "알 수 없는 관리자 명령어입니다. 사용법: /유저상점 관리자 <reload|cleanup|auctioncleanup>");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (player.hasPermission("usershop.sell")) completions.add("판매");
            if (player.hasPermission("usershop.bid.sell")) completions.add("입찰");
            if (player.hasPermission("usershop.buyorder")) completions.add("구매요청");
            if (player.hasPermission("usershop.price")) completions.add("시세");
            if (player.hasPermission("usershop.manage")) completions.add("관리");
            if (player.hasPermission("usershop.admin")) completions.add("관리자");
            completions.add("도움말");
            completions.add("help");
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "판매":
                    if (player.hasPermission("usershop.sell")) {
                        completions.add("<개당가격>");
                    }
                    break;
                case "입찰":
                    if (player.hasPermission("usershop.bid.sell")) {
                        completions.add("<시작가>");
                    }
                    break;
                case "구매요청":
                    if (player.hasPermission("usershop.buyorder")) {
                        completions.add("<개당가격>");
                    }
                    break;
                case "관리자":
                    if (player.hasPermission("usershop.admin")) {
                        completions.add("reload");
                        completions.add("cleanup");
                        completions.add("auctioncleanup");
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "판매":
                    if (player.hasPermission("usershop.sell")) {
                        ItemStack itemInHand = player.getInventory().getItemInMainHand();
                        if (itemInHand.getType() != Material.AIR) {
                            completions.add(String.valueOf(itemInHand.getAmount()));
                        }
                        completions.add("<수량>");
                    }
                    break;
                case "입찰":
                    if (player.hasPermission("usershop.bid.sell")) {
                        completions.add("[즉시구매가]");
                    }
                    break;
                case "구매요청":
                    if (player.hasPermission("usershop.buyorder")) {
                        completions.add("<수량>");
                    }
                    break;
            }
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}