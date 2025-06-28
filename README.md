# RangUserShop

## Overview

RangUserShop is a comprehensive user-to-user shopping plugin for Spigot-based Minecraft servers. It provides a feature-rich platform for players to buy, sell, and trade items through a user-friendly graphical interface. The plugin includes a standard shop, an auction house, and a buy request system to facilitate a dynamic player-driven economy.

## Features

  * **Player Shops:** Players can list their items for sale at a fixed price for other players to purchase.
  * **Auction House:** A complete auction system where players can list items for bidding, with options for a starting price and a "buy now" price.
  * **Buy Requests:** Players can post requests to buy specific items at a certain price, allowing others to fulfill their orders.
  * **Advanced GUI:** An intuitive and easy-to-use graphical interface for all shop, auction, and buy request interactions. The GUI is managed by the `GuiManager` class.
  * **Search and Sort:** Players can search for items by name or seller and sort listings by various criteria such as price, date listed, and alphabetically.
  * **Price Information:** Players can check the detailed price history of items, including the lowest current price, 24-hour average price, and transaction volume.
  * **Item Management:** A dedicated management GUI for players to view their listed items, sold/expired items, and reclaim items.
  * **Tax System:** A configurable tax system on sales, with a permission to exempt certain players from taxes.
  * **Developer API:** A clean and simple API (`UserShopAPI.java`) for other plugins to interact with RangUserShop.
  * **GiftBox Integration:** Integrates with `RangGiftBox` to deliver items to offline players or when a player's inventory is full.

## Commands

The main command for this plugin is `/usershop`, with the aliases `/shop` and `/유저상점`.

  * `/usershop` - Opens the main shop GUI.
  * `/usershop sell <price> [amount]` - Puts the item held in the main hand up for sale.
  * `/usershop auction <start_price> [buy_now_price]` - Puts the item held in the main hand up for auction.
  * `/usershop buyrequest <price_per_item> <amount>` - Creates a request to buy the item held in the main hand.
  * `/usershop price` - Checks the price history of the item in the main hand.
  * `/usershop manage` - Opens the item management GUI.
  * `/usershop help` - Displays the help message.
  * `/usershop admin <reload|cleanup|auctioncleanup>` - Admin commands to manage the plugin.

## Permissions

  * `usershop.open`: Allows opening the shop GUI. (default: true)
  * `usershop.sell`: Allows listing items for sale. (default: true)
  * `usershop.search`: Allows searching for items in the shop. (default: true)
  * `usershop.price`: Allows checking the price of items. (default: true)
  * `usershop.manage`: Allows managing personal items. (default: true)
  * `usershop.tax.exempt`: Exempts the player from sales tax. (default: op)
  * `usershop.bid.sell`: Allows listing items for auction. (default: op)
  * `usershop.buyorder`: Allows creating buy requests. (default: op)
  * `usershop.admin`: Allows access to admin commands. (default: op)

## Dependencies

  * **Vault:** Required for all economy interactions.
  * **RangGiftBox:** Required for the item mailing feature.
  * **LanguageUtil:** A library for handling item names.
  * **An economy plugin supported by Vault** (e.g., EssentialsX)

## Configuration

The `config.yml` file contains the database connection details.

```yaml
database:
  host: "localhost"
  port: 3306
  database: "usershop"
  username: "user"
  password: "password"
```

Please make sure to configure this with your MariaDB/MySQL database credentials. The plugin will automatically create the necessary tables on first startup.

## Installation

1.  Download the latest version of the plugin.
2.  Make sure you have all the dependencies (Vault, RangGiftBox, LanguageUtil, and an economy plugin) installed on your server.
3.  Place the `RangUserShop.jar` file into your server's `plugins` folder.
4.  Restart or reload your server.
5.  Configure the database settings in the `config.yml` file located in the `plugins/RangUserShop` folder.
6.  Restart the server again for the database connection to be established.

## For Developers

RangUserShop provides an API for developers to integrate their plugins. You can access the API through the `UserShopAPI` interface.

**Example of getting the API:**

```java
import rang.games.rangUserShop.api.UserShopAPI;
import org.bukkit.plugin.RegisteredServiceProvider;

// ...

RegisteredServiceProvider<UserShopAPI> rsp = getServer().getServicesManager().getRegistration(UserShopAPI.class);
if (rsp != null) {
    UserShopAPI api = rsp.getProvider();
    // You can now use the API methods
}
```
