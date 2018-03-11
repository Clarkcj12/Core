package network.palace.core.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import network.palace.core.Core;
import network.palace.core.economy.CurrencyType;
import network.palace.core.events.EconomyUpdateEvent;
import network.palace.core.honor.HonorMapping;
import network.palace.core.honor.TopHonorReport;
import network.palace.core.npc.mob.MobPlayerTexture;
import network.palace.core.player.CPlayer;
import network.palace.core.player.Rank;
import network.palace.core.resource.ResourcePack;
import network.palace.core.tracking.GameType;
import network.palace.core.tracking.StatisticType;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;

/**
 * @author Marc
 * @since 9/23/17
 */
public class MongoHandler {
    private String username;
    private String password;
    private String hostname;

    private MongoClient client = null;
    @Getter private MongoDatabase database = null;
    private MongoCollection<Document> playerCollection = null;
    private MongoCollection<Document> friendsCollection = null;
    private MongoCollection<Document> permissionCollection = null;
    private MongoCollection<Document> cosmeticsCollection = null;
    private MongoCollection<Document> resourcePackCollection = null;
    private MongoCollection<Document> honorMappingCollection = null;
    private MongoCollection<Document> outfitsCollection = null;
    private MongoCollection<Document> hotelCollection = null;
    private MongoCollection<Document> warpsCollection = null;

    public MongoHandler() {
        connect();
    }

    /**
     * Connect to the MongoDB database
     */
    public void connect() {
        username = Core.getCoreConfig().getString("db.user");
        password = Core.getCoreConfig().getString("db.password");
        hostname = Core.getCoreConfig().getString("db.hostname");
        if (username == null || password == null || hostname == null) {
            Core.logMessage("Mongo Handler", ChatColor.RED + "" + ChatColor.BOLD + "Error with mongo config!");
            Bukkit.shutdown();
        }
        MongoClientURI connectionString = new MongoClientURI("mongodb://" + username + ":" + password + "@" + hostname);
        client = new MongoClient(connectionString);
        database = client.getDatabase("palace");
        playerCollection = database.getCollection("players");
        friendsCollection = database.getCollection("friends");
        permissionCollection = database.getCollection("permissions");
        cosmeticsCollection = database.getCollection("cosmetics");
        resourcePackCollection = database.getCollection("resourcepacks");
        honorMappingCollection = database.getCollection("honormapping");
        outfitsCollection = database.getCollection("outfits");
        hotelCollection = database.getCollection("hotels");
        warpsCollection = database.getCollection("warps");
    }

    /* Player Methods */

    /**
     * Create a new player in the database
     *
     * @param player the CPlayer object
     */
    public void createPlayer(CPlayer player) {
        if (getPlayer(player.getUniqueId()) != null) return;

        Document newDocument = new Document("uuid", player.getUniqueId())
                .append("username", player.getName())
                .append("ip", "localhost")
                .append("tokens", 1)
                .append("currency", 1)
                .append("currentServer", "Hub1")
                .append("isp", "localhost")
                .append("rank", player.getRank().getDBName())
                .append("lastOnline", System.currentTimeMillis())
                .append("isVisible", true)
                .append("currentMute", null)
                .append("bans", new HashMap<String, Object>() {{
                    put("isBanned", false);
                    put("currentBan", null);
                }})
                .append("kicks", new ArrayList<>());
        playerCollection.insertOne(newDocument);
    }

    /**
     * Get a player's full document from the database
     *
     * @param uuid the uuid
     * @return the <b>full</b> document
     * @implNote This method shouldn't be used frequently, use {@link #getPlayer(UUID, Document)} to get specific data
     */
    public Document getPlayer(UUID uuid) {
        return playerCollection.find(MongoFilter.UUID.getFilter(uuid.toString())).first();
    }

    /**
     * Get a specific set of a player's data from the database
     *
     * @param uuid  the uuid
     * @param limit a Document specifying which keys to return from the database
     * @return a Document with the limited data
     */
    public Document getPlayer(UUID uuid, Document limit) {
        FindIterable<Document> doc = playerCollection.find(MongoFilter.UUID.getFilter(uuid.toString())).projection(limit);
        if (doc == null) return null;
        return doc.first();
    }

    /**
     * Tell if a player exists in the database
     *
     * @param username the username
     * @return true if exists, otherwise false
     */
    public boolean playerExists(String username) {
        return playerCollection.find(MongoFilter.USERNAME.getFilter(username)) != null;
    }

    /**
     * Get rank from uuid
     *
     * @param uuid the uuid
     * @return the rank, or settler if doesn't exist
     */
    public Rank getRank(UUID uuid) {
        if (uuid == null) return Rank.SETTLER;
        Document result = playerCollection.find(MongoFilter.UUID.getFilter(uuid.toString())).first();
        if (result == null) return Rank.SETTLER;
        return Rank.fromString(result.getString("rank"));
    }

    /**
     * Get rank from username
     *
     * @param username the username
     * @return the rank, or settler if doesn't exist
     */
    public Rank getRank(String username) {
        return Rank.fromString(playerCollection.find(MongoFilter.USERNAME.getFilter(username)).first().getString("rank"));
    }

    /**
     * Get UUID from player's username
     *
     * @param username the username
     * @return their UUID or null if isn't formatted like UUID
     */
    public UUID usernameToUUID(String username) {
        try {
            FindIterable<Document> list = playerCollection.find(MongoFilter.USERNAME.getFilter(username));
            if (list.first() == null) return null;
            return UUID.fromString(list.first().getString("uuid"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Cache a skin for later use
     *
     * @param uuid      UUID of the player
     * @param value     Value of the skin
     * @param signature Signature of the skin
     */
    public void cacheSkin(UUID uuid, String value, String signature) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("skin", new Document("hash", value).append("signature", signature)));
    }

    /**
     * Get the cached skin for a player's uuid
     *
     * @param uuid The uuid to find
     * @return The texture
     */
    public MobPlayerTexture getPlayerTextureHash(UUID uuid) {
        BasicDBObject skin = (BasicDBObject) getPlayer(uuid, new Document("skin", 1)).get("skin");
        return new MobPlayerTexture(skin.getString("hash"), skin.getString("signature"));
    }

    /**
     * Get the language the player has selected
     *
     * @param uuid the player's uuid
     * @return the language the player uses
     */
    public String getLanguage(UUID uuid) {
        return "en_us";
    }

    /* Warp Methods */

    public FindIterable<Document> getWarps() {
        return warpsCollection.find();
    }

    public void deleteWarp(String name) {
        warpsCollection.deleteOne(Filters.eq("name", name));
    }

    public void createWarp(String name, String server, double x, double y, double z, float yaw, float pitch, String world) {
        warpsCollection.insertOne(new Document("name", name).append("server", server).append("x", x).append("y", y)
                .append("z", z).append("yaw", (int) yaw).append("pitch", (int) pitch).append("world", world));
    }

    /* Achievement Methods */

    /**
     * Record achievement for player in database
     *
     * @param uuid          the uuid of the player
     * @param achievementID the achievement ID
     */
    public void addAchievement(UUID uuid, int achievementID) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.push("achievements", new BasicDBObject("id", achievementID).append("time", System.currentTimeMillis() / 1000)));
    }

    /**
     * Get all achievements for a player
     *
     * @param uuid The player's uuid
     * @return list of the players achievements
     */
    public List<Integer> getAchievements(UUID uuid) {
        List<Integer> list = new ArrayList<>();
        Document player = getPlayer(uuid, new Document("achievements", 1));
        if (player == null) return list;
        List array = (ArrayList) player.get("achievements");
        for (Object obj : array) {
            Document doc = (Document) obj;
            list.add(doc.getInteger("id"));
        }
        return list;
    }

    /* Cosmetics */

    /**
     * Earn a cosmetic for a player
     *
     * @param player the player that earned it
     * @param id     the id of the cosmetic they earned
     */
    public void earnCosmetic(CPlayer player, int id) {
        earnCosmetic(player.getUniqueId(), id);
    }

    /**
     * Earn a cosmetic for a player
     *
     * @param uuid the uuid that earned it
     * @param id   the id of the cosmetic they earned
     */
    public void earnCosmetic(UUID uuid, int id) {

    }

    /**
     * Does a player have a cosmetic item?
     *
     * @param player the player to check
     * @param id     the id to check
     * @return if the player has the cosmetic
     */
    public boolean hasCosmetic(CPlayer player, int id) {
        return hasCosmetic(player.getUniqueId(), id);
    }

    /**
     * Does a player have a cosmetic item?
     *
     * @param uuid the uuid to check
     * @param id   the id to check
     * @return if the player has the cosmetic
     */
    public boolean hasCosmetic(UUID uuid, int id) {
        //TODO do this
        return false;
    }

    /* Economy Methods */

    /**
     * Get a player's amount of a certain currency
     *
     * @param uuid the uuid
     * @param type the currency type (balance, tokens)
     * @return the amount
     */
    public int getCurrency(UUID uuid, CurrencyType type) {
        Document player = getPlayer(uuid, new Document(type.getName(), 1));
        if (player == null) return 0;
        return (int) player.getOrDefault(type.getName(), 0);
    }

    /**
     * Change a player's currency amount
     *
     * @param uuid   the uuid
     * @param amount the amount
     * @param type   the currency type
     * @param set    true if the value should be set to amount, false if existing value should be incremented
     */
    public void changeAmount(UUID uuid, int amount, CurrencyType type, boolean set) {
        changeAmount(uuid, amount, "plugin", type, set);
    }

    /**
     * Change a player's currency amount
     *
     * @param uuid   the uuid
     * @param amount the amount
     * @param source the source of the transaction
     * @param type   the currency type
     * @param set    true if the value should be set to amount, false if existing value should be incremented
     */
    public void changeAmount(UUID uuid, int amount, String source, CurrencyType type, boolean set) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), set ? Updates.set(type.getName(), amount) : Updates.inc(type.getName(), amount));
        Document doc = new Document("amount", amount).append("type", type.getName()).append("source", source)
                .append("server", Core.getInstanceName()).append("timestamp", System.currentTimeMillis() / 1000);
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.push("transactions", doc));
        new EconomyUpdateEvent(uuid, getCurrency(uuid, type), type).call();
    }

    /**
     * Log a transaction in the database
     *
     * @param uuid   the uuid
     * @param amount the amount
     * @param source the source of the transaction
     * @param type   the currency type
     * @param set    whether or not the transaction was a set
     */
    public void logTransaction(UUID uuid, int amount, String source, CurrencyType type, boolean set) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.push("transactions", new BasicDBObject("amount", amount)
                .append("type", (set ? "set " : "add ") + type.getName())
                .append("source", source)
                .append("server", Core.getInstanceName())
                .append("timestamp", System.currentTimeMillis() / 1000)));
    }

    /* Game Methods */

    /**
     * Get a players statistic in a game
     *
     * @param game   the game to get the statistic from
     * @param type   the type of statistic to get
     * @param player the player to get the statistic from
     * @return the amount of the statistic they have
     */
    public int getGameStat(GameType game, StatisticType type, CPlayer player) {
        return getGameStat(game, type, player.getUniqueId());
    }

    /**
     * Get a players statistic in a game
     *
     * @param game the game to get the statistic from
     * @param type the type of statistic to get
     * @param uuid the player to get the statistic from
     * @return the amount of the statistic they have
     */
    public int getGameStat(GameType game, StatisticType type, UUID uuid) {
        Document player = getPlayer(uuid, new Document("gameData", 1));
        BasicDBObject obj = (BasicDBObject) player.get(game.getDbName());
        if (!obj.containsField(type.getType()) || (!(obj.get(type.getType()) instanceof Number) && !(obj.get(type.getType()) instanceof Boolean))) {
            return 0;
        }
        return obj.getInt(type.getType());
    }

    /**
     * Add a game statistic to a player.
     *
     * @param game      the game that this happened in
     * @param statistic the statistic to add
     * @param amount    the amount to give the player
     * @param player    the player who earned this stat
     */
    public void addGameStat(GameType game, StatisticType statistic, int amount, CPlayer player) {
        addGameStat(game, statistic, amount, player.getUuid());
    }

    /**
     * Add a game statistic to a player
     *
     * @param game      the game that this happened in
     * @param statistic the statistic to add
     * @param uuid      the player who earned this stat
     * @param amount    the amount to give the player
     */
    public void addGameStat(GameType game, StatisticType statistic, int amount, UUID uuid) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("gameData", new BasicDBObject(game.getDbName(), new BasicDBObject(statistic.getType(), amount))));
    }

    /* Honor Methods */

    /**
     * Get the honor mappings from mysql.
     *
     * @return the mappings
     */
    public List<HonorMapping> getHonorMappings() {
        List<HonorMapping> list = new ArrayList<>();
        FindIterable<Document> iter = honorMappingCollection.find();
        for (Document doc : iter) {
            HonorMapping map = new HonorMapping(doc.getInteger("level"), doc.getInteger("honor"));
            list.add(map);
        }
        return list;
    }

    /**
     * Add honor to a player
     * To remove honor, make amount negative
     *
     * @param uuid   the player's uuid
     * @param amount the amount to add
     */
    public void addHonor(UUID uuid, int amount) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.inc("honor", amount));
    }

    /**
     * Set a player's honor
     *
     * @param uuid   the player's uuid
     * @param amount the amount
     */
    public void setHonor(UUID uuid, int amount) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("honor", amount));
    }

    /**
     * Get a player's honor
     *
     * @param uuid the player's uuid
     * @return the player's honor
     */
    public int getHonor(UUID uuid) {
        Document player = getPlayer(uuid, new Document("honor", 1));
        if (player == null) return 0;
        return (int) player.getOrDefault("honor", 1);
    }

    /**
     * Get honor leaderboard
     *
     * @param limit amount to get (max 10)
     * @return leaderboard map
     */
    public HashMap<Integer, TopHonorReport> getTopHonor(int limit) {
        HashMap<Integer, TopHonorReport> map = new HashMap<>();
        if (limit > 10) {
            limit = 10;
        }
        FindIterable<Document> list = playerCollection.find().projection(new Document("uuid", 1).append("username", 1)
                .append("rank", 1).append("honor", 1)).sort(new Document("honor", -1)).limit(limit);
        int place = 1;
        for (Document doc : list) {
            map.put(doc.getInteger("honor"), new TopHonorReport(UUID.fromString(doc.getString("uuid")),
                    doc.getString("username"), place++, doc.getInteger("honor")));
        }
        return map;
    }

    /* Resource Pack Methods */

    /**
     * Get all resource packs in the database
     *
     * @return a List of ResourcePack containing resource pack information
     */
    public List<ResourcePack> getResourcePacks() {
        List<ResourcePack> list = new ArrayList<>();
        resourcePackCollection.find().forEach((Block<? super Document>) doc ->
                list.add(new ResourcePack(doc.getString("name"), doc.getString("url"), doc.getString("hash"))));
        return list;
    }

    /* Permission Methods */

    /**
     * Gets members.
     *
     * @param rank the rank
     * @return the members
     */
    public List<String> getMembers(Rank rank) {
        List<String> list = new ArrayList<>();
        playerCollection.find(MongoFilter.RANK.getFilter(rank.getDBName())).projection(new Document("username", 1))
                .forEach((Block<Document>) d -> list.add(d.getString("username")));
        return list;
    }

    /**
     * Sets rank.
     *
     * @param uuid the uuid
     * @param rank the rank
     */
    public void setRank(UUID uuid, Rank rank) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("rank", rank.getDBName()));
    }

    /**
     * Gets permissions.
     *
     * @param player the player
     * @return the permissions
     */
    public Map<String, Boolean> getPermissions(CPlayer player) {
        return getPermissions(player.getRank());
    }

    /**
     * Gets permissions.
     *
     * @param rank the rank
     * @return the permissions
     */
    public Map<String, Boolean> getPermissions(Rank rank) {
        Map<String, Boolean> map = new HashMap<>();
        for (Document main : permissionCollection.find(new Document("rank", rank.getDBName()))) {
            ArrayList allowed = (ArrayList) main.get("allowed");
            ArrayList denied = (ArrayList) main.get("denied");
            for (Object o : denied) {
                String s = (String) o;
                map.put(MongoUtil.commaToPeriod(s), false);
            }
            for (Object o : allowed) {
                String s = (String) o;
                map.put(MongoUtil.commaToPeriod(s), true);
            }
//            ArrayList list = (ArrayList) main.get(rank.getDBName());
//            for (Object o : list) {
//                Document doc = (Document) o;
//                if (doc == null || doc.isEmpty() || doc.getString("node") == null || doc.getBoolean("value") == null)
//                    continue;
//                map.put(MongoUtil.commaToPeriod(doc.getString("node")), doc.getBoolean("value"));
//            }
        }
        return map;
    }

    /**
     * Sets permission.
     *
     * @param node  the node
     * @param rank  the rank
     * @param value the value
     */
    public void setPermission(String node, Rank rank, boolean value) {
        node = MongoUtil.periodToComma(node);
        boolean removeFromOtherList = false;
        String other = value ? "denied" : "allowed";
        for (Document d : permissionCollection.find(MongoFilter.RANK.getFilter(rank.getDBName())).projection(new Document(other, 1))) {
            for (Object o : d.get(other, ArrayList.class)) {
                String s = (String) o;
                if (s != null && s.equals(node)) {
                    permissionCollection.updateOne(MongoFilter.RANK.getFilter(rank.getDBName()), Updates.pull(other, node));
                }
            }
        }
        permissionCollection.updateOne(MongoFilter.RANK.getFilter(rank.getDBName()), Updates.addToSet(value ? "allowed" : "denied", node));
    }

    /**
     * Unset permission.
     *
     * @param node the node
     * @param rank the rank
     */
    public void unsetPermission(String node, Rank rank) {
        node = MongoUtil.periodToComma(node);
        permissionCollection.updateOne(MongoFilter.RANK.getFilter(rank.getDBName()), Updates.pull("allowed", node));
        permissionCollection.updateOne(MongoFilter.RANK.getFilter(rank.getDBName()), Updates.pull("denied", node));
//        permissionCollection.updateOne(new Document(), Updates.pull(rank.getDBName(), new Document("node", node)));
//        permissionCollection.updateOne(new Document(), new Document("$pull", new Document(rank.getDBName(), new Document("node", node))));
    }

    /**
     * Get a document storing monthly rewards data for a specific player
     *
     * @param uuid the uuid of the player
     * @return a document with monthly rewards data
     */
    public Document getMonthlyRewards(UUID uuid) {
        return getPlayer(uuid, new Document("monthlyRewards", 1));
    }

    /**
     * Get a document storing voting data for a specific player
     *
     * @param uuid the uuid of the player
     * @return a document with voting data
     */
    public Document getVoteData(UUID uuid) {
        return getPlayer(uuid, new Document("vote", 1));
    }

    /**
     * Get a list of UUIDs a player is friends with
     *
     * @param uuid the uuid of the player
     * @return a list of UUIDs the player is friends with
     */
    public List<UUID> getFriendList(UUID uuid) {
        return getList(uuid, true);
    }

    /**
     * Get a list of the player's friend request UUIDs
     *
     * @param uuid the uuid of the player
     * @return a list of UUIDs from friend requests
     */
    public List<UUID> getRequestList(UUID uuid) {
        return getList(uuid, false);
    }

    /**
     * Base method for getFriendList and getRequestList, not recommended to call this directly in case of API changes
     *
     * @param uuid    the uuid of the player
     * @param friends true if getting a friend list, false if a request list
     * @return a list of UUIDs
     */
    public List<UUID> getList(UUID uuid, boolean friends) {
        List<UUID> list = new ArrayList<>();
        List<String> prelist = new ArrayList<>();
        friendsCollection.find(Filters.eq(new Document("sender", uuid.toString()))).projection(new Document("receiver", 1).append("started", 1))
                .forEach((Block<Document>) d -> {
                    if (friends ? d.getLong("started") > 0 : d.getLong("started") <= 0) {
                        prelist.add(d.getString("receiver"));
                    }
                });
        friendsCollection.find(Filters.eq(new Document("receiver", uuid.toString()))).projection(new Document("sender", 1).append("started", 1))
                .forEach((Block<Document>) d -> {
                    if (friends ? d.getLong("started") > 0 : d.getLong("started") <= 0) {
                        prelist.add(d.getString("sender"));
                    }
                });
        for (String s : prelist) {
            if (s.equals(uuid.toString())) continue;
            list.add(UUID.fromString(s));
        }
        return list;
    }

    /**
     * Update monthly reward data for a player
     *
     * @param uuid      the uuid of the player
     * @param settler   the timestamp settler was last claimed
     * @param dweller   the timestamp dweller was last claimed
     * @param noble     the timestamp noble was last claimed
     * @param majestic  the timestamp majestic was last claimed
     * @param honorable the timestamp honorable was last claimed
     */
    public void updateMonthlyRewardData(UUID uuid, long settler, long dweller, long noble, long majestic, long honorable) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("monthlyRewards",
                new Document("settler", settler).append("dweller", dweller).append("noble", noble)
                        .append("majestic", majestic).append("honorable", honorable)));
    }

    public Document getHotels() {
        return hotelCollection.find().projection(new Document("hotels", 1)).first();
    }

    public Document getHotelMessages() {
        return hotelCollection.find().projection(new Document("messages", 1)).first();
    }

    public FindIterable<Document> getHotelMessages(UUID uuid) {
        return hotelCollection.find(Filters.eq("messages.target", uuid.toString()));
    }

    /*
    Park Methods
     */

    /**
     * Get data for a specific section of park data. If no limit is provided, the entire parks section is returned.
     *
     * @param uuid  the uuid of the player
     * @param limit a document specifying the limits of the search
     * @return a document with the requested data
     */
    public Document getParkData(UUID uuid, Document limit) {
        return getPlayer(uuid, new Document("parks", limit == null ? 1 : limit));
    }

    /**
     * Get the specific value of a string inside the parks document
     *
     * @param uuid the uuid of the player
     * @param key  the string to search for
     * @return the value of that string
     */
    public String getParkData(UUID uuid, String key) {
        return getPlayer(uuid, new Document("parks", 1)).getString(key);
    }

    /**
     * Get the namecolor data for a player's MagicBand
     *
     * @param uuid the uuid of the player
     * @return the namecolor
     */
    public String getMagicBandNameColor(UUID uuid) {
        Document data = getMagicBandData(uuid);
        return data.getString("namecolor");
    }

    /**
     * Get the bandtype data for a player's MagicBand
     *
     * @param uuid the uuid of the player
     * @return the bandtype
     */
    public String getMagicBandType(UUID uuid) {
        Document data = getMagicBandData(uuid);
        return data.getString("bandtype");
    }

    /**
     * Base method for getMagicBandNameColor and getMagicBandType, not recommended to call this directly in case of API changes
     *
     * @param uuid the uuid of the player
     * @return a document with MagicBand data
     */
    public Document getMagicBandData(UUID uuid) {
        return getParkData(uuid, new Document("magicband", 1));
    }

    /**
     * Get a specific park setting for a player
     *
     * @param uuid    the uuid of the player
     * @param setting the setting
     * @return the value of the setting
     */
    public String getParkSetting(UUID uuid, String setting) {
        return getParkData(uuid, new Document("settings", 1)).getString(setting);
    }

    /**
     * Get a document with ride counter data for a player
     *
     * @param uuid the uuid of the player
     * @return a document with ride counter data
     */
    public Document getRideCounterData(UUID uuid) {
        return getParkData(uuid, new Document("rides", 1));
    }

    /**
     * Log a ride counter entry into the player's document
     *
     * @param uuid the uuid of the player
     * @param name the name of the ride
     */
    public void logRideCounter(UUID uuid, String name) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()),
                Updates.push("parks.rides", new Document("name", name)
                        .append("server", Core.getInstanceName())
                        .append("time", System.currentTimeMillis() / 1000)));
    }

    /**
     * Get all autographs for a player
     *
     * @param uuid the uuid of the player
     * @return an iterable of the player's autographs
     */
    public FindIterable<Document> getAutographs(UUID uuid) {
        return playerCollection.find(MongoFilter.UUID.getFilter(uuid.toString())).projection(new Document("autgraphs", 1));
    }

    /**
     * Sign a player's book
     *
     * @param player  the player's book being signed
     * @param sender  the player signing the book
     * @param message the message
     */
    public void signBook(UUID player, String sender, String message) {
        Document doc = new Document("author", sender).append("message", message).append("time", System.currentTimeMillis() / 1000);
        playerCollection.updateOne(MongoFilter.UUID.getFilter(player.toString()), Updates.push("autographs", doc));
    }

    /**
     * Delete an autograph from a player's book
     *
     * @param uuid   the uuid of the player
     * @param sender the author of the autograph
     * @param time   the time the autograph was written
     */
    public void deleteAutograph(UUID uuid, String sender, long time) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.pull("autographs",
                new Document("sender", sender).append("time", time)));
    }

    /**
     * Charge a player a specific type of FastPass
     *
     * @param uuid   the uuid of the player
     * @param type   the FP type
     * @param amount the amount to charge (usually 1)
     */
    public void chargeFastPass(UUID uuid, String type, int amount) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.inc("parks.fastpass." + type, -amount));
    }

    /**
     * Get all outfits from the database
     *
     * @return an iterable of outfit data
     */
    public FindIterable<Document> getOutfits() {
        return getOutfits(-1);
    }

    /**
     * Get all outfits for a specific resort
     *
     * @param resort the resort id
     * @return an iterable of outfit data
     */
    public FindIterable<Document> getOutfits(int resort) {
        if (resort < 0) {
            return outfitsCollection.find();
        } else {
            return outfitsCollection.find(Filters.eq("resort", resort));
        }
    }

    /**
     * Get a document with the outfit purchases of a player
     *
     * @param uuid the uuid of the player
     * @return a document with outfit purchases data
     */
    public Document getOutfitPurchases(UUID uuid) {
        return getParkData(uuid, new Document("outfitPurchases", 1));
    }

    /**
     * Set a player's outfit code value
     *
     * @param uuid the uuid of the player
     * @param code the value of the code
     */
    public void setOutfitCode(UUID uuid, String code) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("parks.outfit", code));
    }

    /**
     * Log the purchase of an outfit
     *
     * @param uuid the uuid of the player
     * @param id   the id of the outfit
     */
    public void purchaseOutfit(UUID uuid, int id) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.push("parks.outfitPurchases", new Document("id", id).append("time", System.currentTimeMillis() / 1000)));
    }

    /**
     * Create a new outfit (with a lot of variables)
     *
     * @param name       the name
     * @param hid        the helmet ID
     * @param hdata      the helmet data
     * @param head       the helmet nbt
     * @param cid        the chestplate ID
     * @param cdata      the chestplate data
     * @param chestplate the chestplate nbt
     * @param lid        the leggings ID
     * @param ldata      the leggings data
     * @param leggings   the leggings nbt
     * @param bid        the boots ID
     * @param bdata      the boots data
     * @param boots      the boots nbt
     * @param resort     the resort id
     */
    public void createOutfit(String name, int hid, byte hdata, String head, int cid, byte cdata, String chestplate,
                             int lid, byte ldata, String leggings, int bid, byte bdata, String boots, int resort) {
        Document doc = new Document("id", getNextSequence());
        doc.append("name", name);
        doc.append("headID", hid);
        doc.append("headData", hdata);
        doc.append("head", head);
        doc.append("chestID", cid);
        doc.append("chestData", cdata);
        doc.append("chest", chestplate);
        doc.append("leggingsID", lid);
        doc.append("leggingsData", ldata);
        doc.append("leggings", leggings);
        doc.append("bootsID", bid);
        doc.append("bootsData", bdata);
        doc.append("boots", boots);
        outfitsCollection.insertOne(doc);
    }

    /**
     * Used for creating new outfits
     *
     * @return the value of that field plus one
     */
    private Object getNextSequence() {
        BasicDBObject find = new BasicDBObject();
        find.put("_id", "userid");
        BasicDBObject update = new BasicDBObject();
        update.put("$inc", new BasicDBObject("seq", 1));
        Document obj = outfitsCollection.findOneAndUpdate(find, update);
        return obj.get("seq");
    }

    /**
     * Delete an outfit
     *
     * @param id the id of the outfit to delete
     */
    public void deleteOutfit(int id) {
        outfitsCollection.deleteOne(Filters.eq("id", id));
    }

    /**
     * Set a park setting for a player
     *
     * @param uuid    the uuid of the player
     * @param setting the setting to set
     * @param value   the value of the setting
     */
    public void setParkSetting(UUID uuid, String setting, Object value) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("parks.settings." + setting, value));
    }

    /**
     * Set MagicBand data for a player
     *
     * @param uuid  the uuid of the player
     * @param key   the key to set
     * @param value the value to set the key to
     */
    public void setMagicBandData(UUID uuid, String key, String value) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("parks.magicband." + key, value));
    }

    /**
     * Set a player's build mode status
     *
     * @param uuid  the uuid of the player
     * @param value the value to set it to
     */
    public void setBuildMode(UUID uuid, boolean value) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("parks.buildmode", value));
    }

    /**
     * Update a player's inventory size for a specific resort
     *
     * @param uuid   the uuid of the player
     * @param type   the type of inventory (packsize or lockersize)
     * @param size   the size 0 (small) or 1 (large)
     * @param resort the resort
     */
    public void setInventorySize(UUID uuid, String type, int size, int resort) {
        playerCollection.updateOne(Filters.and(MongoFilter.UUID.getFilter(uuid.toString()), Filters.eq("parks.inventories.resort", resort)),
                new Document("parks.inventories." + type, size));
    }

    /**
     * Update the FastPass data for a specific UUID
     *
     * @param uuid        the uuid of the player
     * @param slow        the amount of slow FPs
     * @param moderate    the amount of moderate FPs
     * @param thrill      the amount of thrill FPs
     * @param slowday     the day of the year a slow FP was last claimed
     * @param moderateday the day of the year a moderate FP was last claimed
     * @param thrillday   the day of the year a thrill FP was last claimed
     */
    public void updateFPData(UUID uuid, int slow, int moderate, int thrill, int slowday, int moderateday, int thrillday) {
        playerCollection.updateOne(MongoFilter.UUID.getFilter(uuid.toString()), Updates.set("parks.fastpass",
                new Document("slow", slow).append("moderate", moderate).append("thrill", thrill).append("slowday", slowday)
                        .append("moderateday", moderateday).append("thrillday", thrillday)));
    }

    /**
     * Close the connection with the MongoDB database
     */
    public void close() {
        client.close();
    }

    public enum MongoFilter {
        UUID, USERNAME, RANK;

        public Bson getFilter(String s) {
            switch (this) {
                case UUID:
                    return Filters.eq("uuid", s);
                case USERNAME:
                    return Filters.regex("username", s, "i");
                case RANK:
                    return Filters.eq("rank", s);
            }
            return null;
        }
    }
}
