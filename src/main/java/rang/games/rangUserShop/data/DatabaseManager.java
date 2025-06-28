package rang.games.rangUserShop.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import rang.games.rangUserShop.util.ItemSerializer;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final Logger log;
    private Connection connection;

    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;

    public DatabaseManager(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.log = plugin.getLogger();

        this.host = config.getString("database.host", "localhost");
        this.port = config.getInt("database.port", 3306);
        this.databaseName = config.getString("database.name", "minecraft_shop");
        this.username = config.getString("database.username", "user");
        this.password = config.getString("database.password", "password");
    }

    public void connect() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    log.info("데이터베이스 연결이 이미 설정되어 있습니다.");
                    return;
                }
            } catch (SQLException e) {
                log.log(Level.WARNING, "기존 데이터베이스 연결 상태 확인 중 오류 발생", e);
            }
        }

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, databaseName);
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, username, password);
            log.info("MariaDB 데이터베이스가 성공적으로 연결되었습니다: " + jdbcUrl);
            createTables();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "MariaDB 데이터베이스에 연결할 수 없습니다! 연결 정보를 확인해주세요.", e);
            log.log(Level.SEVERE, "JDBC URL: " + jdbcUrl + ", User: " + username);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "MariaDB JDBC 드라이버를 찾을 수 없습니다! pom.xml에 의존성이 올바른지 확인해주세요.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                log.info("MariaDB 데이터베이스 연결이 성공적으로 해제되었습니다.");
            } catch (SQLException e) {
                log.log(Level.SEVERE, "MariaDB 데이터베이스 연결을 닫을 수 없습니다!", e);
            }
        }
    }

    private void createTables() {
        String createShopItemsTable = "CREATE TABLE IF NOT EXISTS shop_items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "seller_name VARCHAR(16) NOT NULL," +
                "item_stack LONGTEXT NOT NULL," +
                "item_hash VARCHAR(64) NOT NULL," +
                "price DOUBLE NOT NULL," +
                "amount INT NOT NULL," +
                "list_timestamp BIGINT NOT NULL," +
                "expiry_timestamp BIGINT NOT NULL," +
                "status VARCHAR(15) NOT NULL DEFAULT 'LISTED'," +
                "INDEX (seller_uuid), INDEX (status), INDEX (expiry_timestamp), INDEX (item_hash)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createAuctionItemsTable = "CREATE TABLE IF NOT EXISTS shop_auctions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "seller_name VARCHAR(16) NOT NULL," +
                "item_stack LONGTEXT NOT NULL," +
                "item_hash VARCHAR(64) NOT NULL," +
                "start_price DOUBLE NOT NULL," +
                "buy_now_price DOUBLE," +
                "current_bid DOUBLE NOT NULL," +
                "highest_bidder_uuid VARCHAR(36)," +
                "highest_bidder_name VARCHAR(16)," +
                "start_timestamp BIGINT NOT NULL," +
                "end_timestamp BIGINT NOT NULL," +
                "status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE'," +
                "INDEX (seller_uuid), INDEX (status), INDEX (end_timestamp), INDEX (item_hash)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createBidsTable = "CREATE TABLE IF NOT EXISTS shop_bids (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "auction_id INT NOT NULL," +
                "bidder_uuid VARCHAR(36) NOT NULL," +
                "bidder_name VARCHAR(16) NOT NULL," +
                "bid_amount DOUBLE NOT NULL," +
                "bid_timestamp BIGINT NOT NULL," +
                "FOREIGN KEY (auction_id) REFERENCES shop_auctions(id) ON DELETE CASCADE," +
                "INDEX (auction_id), INDEX (bidder_uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createBuyRequestsTable = "CREATE TABLE IF NOT EXISTS shop_buy_requests (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "requester_uuid VARCHAR(36) NOT NULL," +
                "requester_name VARCHAR(16) NOT NULL," +
                "item_stack LONGTEXT NOT NULL," +
                "item_hash VARCHAR(64) NOT NULL," +
                "price_per_item DOUBLE NOT NULL," +
                "amount_requested INT NOT NULL," +
                "amount_fulfilled INT NOT NULL DEFAULT 0," +
                "request_timestamp BIGINT NOT NULL," +
                "expiry_timestamp BIGINT NOT NULL," +
                "status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE'," +
                "INDEX (requester_uuid), INDEX (status), INDEX (item_hash), INDEX (expiry_timestamp)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String createTransactionsTable = "CREATE TABLE IF NOT EXISTS shop_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "item_id INT NOT NULL," +
                "buyer_uuid VARCHAR(36) NOT NULL," +
                "buyer_name VARCHAR(16) NOT NULL," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "seller_name VARCHAR(16) NOT NULL," +
                "item_stack LONGTEXT NOT NULL," +
                "item_hash VARCHAR(64) NOT NULL," +
                "price_per_item DOUBLE NOT NULL," +
                "amount INT NOT NULL," +
                "transaction_timestamp BIGINT NOT NULL," +
                "type VARCHAR(32) NOT NULL," +
                "INDEX (item_id), INDEX (buyer_uuid), INDEX (seller_uuid), INDEX (transaction_timestamp), INDEX (item_hash)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createShopItemsTable);
            stmt.execute(createAuctionItemsTable);
            stmt.execute(createBidsTable);
            stmt.execute(createBuyRequestsTable);
            stmt.execute(createTransactionsTable);
            log.info("MariaDB 데이터베이스 테이블이 확인/생성되었습니다.");
            addColumnIfNotExists("shop_buy_requests", "expiry_timestamp", "BIGINT NOT NULL DEFAULT 0 AFTER request_timestamp");
        } catch (SQLException e) {
            log.log(Level.SEVERE, "MariaDB 데이터베이스 테이블을 생성할 수 없습니다!", e);
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getColumns(null, null, tableName, columnName);
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                    log.info("테이블 '" + tableName + "'에 '" + columnName + "' 컬럼을 추가했습니다.");
                }
            }
        } catch (SQLException e) {
            log.log(Level.WARNING, "테이블 '" + tableName + "'에 '" + columnName + "' 컬럼을 추가하는 데 실패했습니다.", e);
        }
    }

    public int saveShopItem(ShopItem item) {
        String sql = "INSERT INTO shop_items (seller_uuid, seller_name, item_stack, item_hash, price, amount, list_timestamp, expiry_timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getSellerUuid().toString());
            pstmt.setString(2, item.getSellerName());
            pstmt.setString(3, ItemSerializer.itemStackToBase64(item.getItemStack()));
            pstmt.setString(4, item.getItemHash());
            pstmt.setDouble(5, item.getPrice());
            pstmt.setInt(6, item.getAmount());
            pstmt.setLong(7, item.getListTimestamp());
            pstmt.setLong(8, item.getExpiryTimestamp());
            pstmt.setString(9, "LISTED");

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "상점 아이템을 MariaDB에 저장할 수 없습니다!", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "아이템 스택을 직렬화하는 데 실패했습니다!", e);
        }
        return generatedId;
    }

    public ShopItem getShopItemById(int id) {
        String sql = "SELECT * FROM shop_items WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return shopItemFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "로 상점 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return null;
    }

    public List<ShopItem> getShopItemsBySeller(UUID sellerUuid) {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items WHERE seller_uuid = ? AND status = 'LISTED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ShopItem item = shopItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "판매자 " + sellerUuid + "의 상점 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public List<ShopItem> getShopItemsBySellerAndStatus(UUID sellerUuid, String status) {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items WHERE seller_uuid = ? AND status = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUuid.toString());
            pstmt.setString(2, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ShopItem item = shopItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "판매자 " + sellerUuid + "의 상태 " + status + "인 상점 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public List<ShopItem> getAllListedShopItems() {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items WHERE status = 'LISTED' AND expiry_timestamp > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ShopItem item = shopItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "모든 등록된 상점 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public boolean updateShopItemStatus(int id, String newStatus) {
        String sql = "UPDATE shop_items SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 상점 아이템 상태를 MariaDB에서 업데이트할 수 없습니다!", e);
        }
        return false;
    }

    public boolean deleteShopItem(int id) {
        String sql = "DELETE FROM shop_items WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 상점 아이템을 MariaDB에서 삭제할 수 없습니다!", e);
        }
        return false;
    }

    public int cleanupExpiredItems() {
        String sql = "UPDATE shop_items SET status = 'EXPIRED' WHERE expiry_timestamp <= ? AND status = 'LISTED'";
        int affectedRows = 0;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                log.info(affectedRows + "개의 상점 아이템이 EXPIRED로 표시되었습니다.");
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "만료된 상점 아이템을 MariaDB에서 정리할 수 없습니다!", e);
        }
        return affectedRows;
    }

    public ShopItem getLowestPriceItem(String itemHash) {
        String sql = "SELECT * FROM shop_items WHERE item_hash = ? AND status = 'LISTED' AND expiry_timestamp > ? ORDER BY price ASC LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return shopItemFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 최저가 아이템을 MariaDB에서 찾을 수 없습니다!", e);
        }
        return null;
    }

    public List<ShopItem> getListedItemsByHash(String itemHash) {
        List<ShopItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_items WHERE item_hash = ? AND status = 'LISTED' AND expiry_timestamp > ? ORDER BY price ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ShopItem item = shopItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 등록된 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public int getListedItemCount(String itemHash) {
        int totalCount = 0;
        String sql = "SELECT SUM(amount) FROM shop_items WHERE item_hash = ? AND status = 'LISTED' AND expiry_timestamp > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalCount = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 등록된 아이템 수량을 MariaDB에서 가져올 수 없습니다!", e);
        }
        return totalCount;
    }

    private ShopItem shopItemFromResultSet(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
            String sellerName = rs.getString("seller_name");
            ItemStack itemStack = ItemSerializer.base64ToItemStack(rs.getString("item_stack"));
            double price = rs.getDouble("price");
            int amount = rs.getInt("amount");
            long listTimestamp = rs.getLong("list_timestamp");
            long expiryTimestamp = rs.getLong("expiry_timestamp");
            String status = rs.getString("status");
            return new ShopItem(id, sellerUuid, sellerName, itemStack, price, amount, listTimestamp, expiryTimestamp, status);
        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "ShopItem ID: " + rs.getInt("id") + "의 ItemStack을 데이터베이스에서 역직렬화하는 데 실패했습니다!", e);
            return null;
        }
    }

    public int saveAuctionItem(AuctionItem item) {
        String sql = "INSERT INTO shop_auctions (seller_uuid, seller_name, item_stack, item_hash, start_price, buy_now_price, current_bid, highest_bidder_uuid, highest_bidder_name, start_timestamp, end_timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getSellerUuid().toString());
            pstmt.setString(2, item.getSellerName());
            pstmt.setString(3, ItemSerializer.itemStackToBase64(item.getItemStack()));
            pstmt.setString(4, item.getItemHash());
            pstmt.setDouble(5, item.getStartPrice());
            if (item.getBuyNowPrice() > 0) {
                pstmt.setDouble(6, item.getBuyNowPrice());
            } else {
                pstmt.setNull(6, Types.DOUBLE);
            }
            pstmt.setDouble(7, item.getCurrentBid());
            if (item.getHighestBidderUuid() != null) {
                pstmt.setString(8, item.getHighestBidderUuid().toString());
                pstmt.setString(9, item.getHighestBidderName());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.VARCHAR);
            }
            pstmt.setLong(10, item.getStartTimestamp());
            pstmt.setLong(11, item.getEndTimestamp());
            pstmt.setString(12, item.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "경매 아이템을 MariaDB에 저장할 수 없습니다!", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "아이템 스택을 직렬화하는 데 실패했습니다!", e);
        }
        return generatedId;
    }

    public AuctionItem getAuctionItemById(int id) {
        String sql = "SELECT * FROM shop_auctions WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return auctionItemFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "로 경매 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return null;
    }

    public List<AuctionItem> getAllActiveAuctionItems() {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_auctions WHERE status = 'ACTIVE' AND end_timestamp > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuctionItem item = auctionItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "모든 활성 경매 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public List<AuctionItem> getAuctionItemsBySeller(UUID sellerUuid) {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_auctions WHERE seller_uuid = ? ORDER BY end_timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuctionItem item = auctionItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "판매자 " + sellerUuid + "의 경매 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public boolean updateAuctionItemBid(int id, double newBid, UUID bidderUuid, String bidderName) {
        String sql = "UPDATE shop_auctions SET current_bid = ?, highest_bidder_uuid = ?, highest_bidder_name = ? WHERE id = ? AND current_bid < ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, newBid);
            pstmt.setString(2, bidderUuid.toString());
            pstmt.setString(3, bidderName);
            pstmt.setInt(4, id);
            pstmt.setDouble(5, newBid);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 경매 아이템 입찰을 MariaDB에서 업데이트할 수 없습니다!", e);
        }
        return false;
    }

    public boolean updateAuctionItemStatus(int id, String newStatus) {
        String sql = "UPDATE shop_auctions SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 경매 아이템 상태를 MariaDB에서 업데이트할 수 없습니다!", e);
        }
        return false;
    }

    public List<AuctionItem> getExpiredActiveAuctions() {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_auctions WHERE status = 'ACTIVE' AND end_timestamp <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuctionItem item = auctionItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "만료된 활성 경매 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    private AuctionItem auctionItemFromResultSet(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
            String sellerName = rs.getString("seller_name");
            ItemStack itemStack = ItemSerializer.base64ToItemStack(rs.getString("item_stack"));
            double startPrice = rs.getDouble("start_price");
            double buyNowPrice = rs.getDouble("buy_now_price");
            double currentBid = rs.getDouble("current_bid");
            UUID highestBidderUuid = rs.getString("highest_bidder_uuid") != null ? UUID.fromString(rs.getString("highest_bidder_uuid")) : null;
            String highestBidderName = rs.getString("highest_bidder_name");
            long startTimestamp = rs.getLong("start_timestamp");
            long endTimestamp = rs.getLong("end_timestamp");
            String status = rs.getString("status");
            return new AuctionItem(id, sellerUuid, sellerName, itemStack, startPrice, buyNowPrice, currentBid, highestBidderUuid, highestBidderName, startTimestamp, endTimestamp, status);
        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "AuctionItem ID: " + rs.getInt("id") + "의 ItemStack을 데이터베이스에서 역직렬화하는 데 실패했습니다!", e);
            return null;
        }
    }

    public int saveBid(Bid bid) {
        String sql = "INSERT INTO shop_bids (auction_id, bidder_uuid, bidder_name, bid_amount, bid_timestamp) VALUES (?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, bid.getAuctionId());
            pstmt.setString(2, bid.getBidderUuid().toString());
            pstmt.setString(3, bid.getBidderName());
            pstmt.setDouble(4, bid.getBidAmount());
            pstmt.setLong(5, bid.getBidTimestamp());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "입찰을 MariaDB에 저장할 수 없습니다!", e);
        }
        return generatedId;
    }

    private Bid bidFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int auctionId = rs.getInt("auction_id");
        UUID bidderUuid = UUID.fromString(rs.getString("bidder_uuid"));
        String bidderName = rs.getString("bidder_name");
        double bidAmount = rs.getDouble("bid_amount");
        long bidTimestamp = rs.getLong("bid_timestamp");
        return new Bid(id, auctionId, bidderUuid, bidderName, bidAmount, bidTimestamp);
    }

    public int saveBuyRequest(BuyRequest request) {
        String sql = "INSERT INTO shop_buy_requests (requester_uuid, requester_name, item_stack, item_hash, price_per_item, amount_requested, amount_fulfilled, request_timestamp, expiry_timestamp, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, request.getRequesterUuid().toString());
            pstmt.setString(2, request.getRequesterName());
            pstmt.setString(3, ItemSerializer.itemStackToBase64(request.getItemStack()));
            pstmt.setString(4, request.getItemHash());
            pstmt.setDouble(5, request.getPricePerItem());
            pstmt.setInt(6, request.getAmountRequested());
            pstmt.setInt(7, request.getAmountFulfilled());
            pstmt.setLong(8, request.getRequestTimestamp());
            pstmt.setLong(9, request.getExpiryTimestamp());
            pstmt.setString(10, request.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "구매 요청을 MariaDB에 저장할 수 없습니다!", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "아이템 스택을 직렬화하는 데 실패했습니다!", e);
        }
        return generatedId;
    }

    public BuyRequest getBuyRequestById(int id) {
        String sql = "SELECT * FROM shop_buy_requests WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return buyRequestFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "로 구매 요청을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return null;
    }

    public List<BuyRequest> getAllActiveBuyRequests() {
        List<BuyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM shop_buy_requests WHERE status = 'ACTIVE' AND amount_fulfilled < amount_requested AND expiry_timestamp > ? ORDER BY price_per_item DESC, request_timestamp ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BuyRequest request = buyRequestFromResultSet(rs);
                    if (request != null) {
                        requests.add(request);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "모든 활성 구매 요청을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return requests;
    }

    public List<BuyRequest> getExpiredActiveBuyRequests() {
        List<BuyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM shop_buy_requests WHERE status = 'ACTIVE' AND expiry_timestamp <= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BuyRequest request = buyRequestFromResultSet(rs);
                    if (request != null) {
                        requests.add(request);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "만료된 활성 구매 요청을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return requests;
    }


    public boolean updateBuyRequestAmountFulfilled(int id, int fulfilledAmount) {
        String sql = "UPDATE shop_buy_requests SET amount_fulfilled = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, fulfilledAmount);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 구매 요청 이행 수량을 MariaDB에서 업데이트할 수 없습니다!", e);
        }
        return false;
    }

    public boolean updateBuyRequestStatus(int id, String newStatus) {
        String sql = "UPDATE shop_buy_requests SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "ID " + id + "의 구매 요청 상태를 MariaDB에서 업데이트할 수 없습니다!", e);
        }
        return false;
    }

    private BuyRequest buyRequestFromResultSet(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            UUID requesterUuid = UUID.fromString(rs.getString("requester_uuid"));
            String requesterName = rs.getString("requester_name");
            ItemStack itemStack = ItemSerializer.base64ToItemStack(rs.getString("item_stack"));
            double pricePerItem = rs.getDouble("price_per_item");
            int amountRequested = rs.getInt("amount_requested");
            int amountFulfilled = rs.getInt("amount_fulfilled");
            long requestTimestamp = rs.getLong("request_timestamp");
            long expiryTimestamp = rs.getLong("expiry_timestamp");
            String status = rs.getString("status");
            return new BuyRequest(id, requesterUuid, requesterName, itemStack, pricePerItem, amountRequested, amountFulfilled, requestTimestamp, expiryTimestamp, status);
        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "BuyRequest ID: " + rs.getInt("id") + "의 ItemStack을 데이터베이스에서 역직렬화하는 데 실패했습니다!", e);
            return null;
        }
    }

    public int saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO shop_transactions (item_id, buyer_uuid, buyer_name, seller_uuid, seller_name, item_stack, item_hash, price_per_item, amount, transaction_timestamp, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, transaction.getItemId());
            pstmt.setString(2, transaction.getBuyerUuid().toString());
            pstmt.setString(3, transaction.getBuyerName());
            pstmt.setString(4, transaction.getSellerUuid().toString());
            pstmt.setString(5, transaction.getSellerName());
            pstmt.setString(6, ItemSerializer.itemStackToBase64(transaction.getItemStack()));
            pstmt.setString(7, transaction.getItemHash());
            pstmt.setDouble(8, transaction.getPricePerItem());
            pstmt.setInt(9, transaction.getAmount());
            pstmt.setLong(10, transaction.getTransactionTimestamp());
            pstmt.setString(11, transaction.getType());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "거래를 MariaDB에 저장할 수 없습니다!", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "아이템 스택을 직렬화하는 데 실패했습니다!", e);
        }
        return generatedId;
    }

    public List<Transaction> getTransactionsByItemHash(String itemHash, long startTime, long endTime) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM shop_transactions WHERE item_hash = ? AND transaction_timestamp BETWEEN ? AND ? ORDER BY transaction_timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, startTime);
            pstmt.setLong(3, endTime);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = transactionFromResultSet(rs);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 거래 기록을 조회할 수 없습니다!", e);
        }
        return transactions;
    }

    public List<DailyPriceInfo> getDailyPriceInfo(String itemHash, int days) {
        List<DailyPriceInfo> dailyInfos = new ArrayList<>();
        long startTime = System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000;
        String sql = "SELECT " +
                "  DATE(FROM_UNIXTIME(transaction_timestamp / 1000)) AS transaction_date, " +
                "  AVG(price_per_item) AS avg_price, " +
                "  SUM(amount) AS total_amount " +
                "FROM shop_transactions " +
                "WHERE item_hash = ? AND transaction_timestamp >= ? " +
                "GROUP BY transaction_date " +
                "ORDER BY transaction_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, startTime);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    dailyInfos.add(new DailyPriceInfo(
                            rs.getDate("transaction_date"),
                            rs.getDouble("avg_price"),
                            rs.getInt("total_amount")
                    ));
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 일일 시세 정보를 조회할 수 없습니다!", e);
        }
        return dailyInfos;
    }

    private Transaction transactionFromResultSet(ResultSet rs) throws SQLException {
        try {
            int id = rs.getInt("id");
            int itemId = rs.getInt("item_id");
            UUID buyerUuid = UUID.fromString(rs.getString("buyer_uuid"));
            String buyerName = rs.getString("buyer_name");
            UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
            String sellerName = rs.getString("seller_name");
            ItemStack itemStack = ItemSerializer.base64ToItemStack(rs.getString("item_stack"));
            double pricePerItem = rs.getDouble("price_per_item");
            int amount = rs.getInt("amount");
            long transactionTimestamp = rs.getLong("transaction_timestamp");
            String type = rs.getString("type");
            return new Transaction(id, itemId, buyerUuid, buyerName, sellerUuid, sellerName, itemStack, pricePerItem, amount, transactionTimestamp, type);
        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.SEVERE, "Transaction ID: " + rs.getInt("id") + "의 ItemStack을 데이터베이스에서 역직렬화하는 데 실패했습니다!", e);
            return null;
        }
    }

    public List<AuctionItem> getAuctionItemsByHash(String itemHash) {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM shop_auctions WHERE item_hash = ? AND status = 'ACTIVE' AND end_timestamp > ? ORDER BY current_bid ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AuctionItem item = auctionItemFromResultSet(rs);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 활성 경매 아이템을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return items;
    }

    public List<BuyRequest> getBuyRequestsByHash(String itemHash) {
        List<BuyRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM shop_buy_requests WHERE item_hash = ? AND status = 'ACTIVE' AND amount_fulfilled < amount_requested AND expiry_timestamp > ? ORDER BY price_per_item DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, itemHash);
            pstmt.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BuyRequest request = buyRequestFromResultSet(rs);
                    if (request != null) {
                        requests.add(request);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "해시 " + itemHash + "의 활성 구매 요청을 MariaDB에서 조회할 수 없습니다!", e);
        }
        return requests;
    }

    public List<Transaction> getTransactionsByBuyer(UUID buyerUuid, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM shop_transactions WHERE buyer_uuid = ? ORDER BY transaction_timestamp DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, buyerUuid.toString());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = transactionFromResultSet(rs);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "구매자 " + buyerUuid + "의 거래 기록을 조회할 수 없습니다!", e);
        }
        return transactions;
    }

    public List<Transaction> getTransactionsBySeller(UUID sellerUuid, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM shop_transactions WHERE seller_uuid = ? ORDER BY transaction_timestamp DESC LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, sellerUuid.toString());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = transactionFromResultSet(rs);
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "판매자 " + sellerUuid + "의 거래 기록을 조회할 수 없습니다!", e);
        }
        return transactions;
    }
}