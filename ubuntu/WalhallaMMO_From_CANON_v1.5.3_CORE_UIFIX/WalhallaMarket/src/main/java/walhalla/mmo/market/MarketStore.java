package walhalla.mmo.market;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple YAML-backed persistence for market listings.
 * This is NOT a database; it is sufficient for Phase 14 baseline.
 */
public final class MarketStore {

    private final JavaPlugin plugin;
    private final File file;

    private final Map<String, MarketListing> listings = new ConcurrentHashMap<>();

    public MarketStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "listings.yml");
    }

    public Collection<MarketListing> all() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public List<MarketListing> activeListings() {
        List<MarketListing> out = new ArrayList<>();
        for (MarketListing l : listings.values()) {
            if (l.status == MarketListing.Status.ACTIVE) out.add(l);
        }
        out.sort(Comparator.comparingLong(a -> a.createdAtEpoch));
        return out;
    }

    public List<MarketListing> activeForSeller(UUID seller) {
        List<MarketListing> out = new ArrayList<>();
        for (MarketListing l : listings.values()) {
            if (l.status == MarketListing.Status.ACTIVE && seller.equals(l.sellerId)) out.add(l);
        }
        out.sort(Comparator.comparingLong(a -> a.createdAtEpoch));
        return out;
    }

    public Optional<MarketListing> get(String id) {
        return Optional.ofNullable(listings.get(id));
    }

    public void put(MarketListing listing) {
        listings.put(listing.listingId, listing);
    }

    public void load() {
        listings.clear();
        if (!file.exists()) return;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("listings");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            MarketListing l = new MarketListing();
            l.listingId = id;
            l.sellerId = uuid(s.getString("sellerId"));
            l.item = s.getItemStack("item");
            l.quantity = s.getInt("quantity", l.item != null ? l.item.getAmount() : 1);
            l.priceWcoin = s.getLong("priceWcoin", 0L);
            l.priceWcraft = s.getLong("priceWcraft", 0L);
            l.createdAtEpoch = s.getLong("createdAtEpoch", Instant.now().toEpochMilli());
            l.expiresAtEpoch = s.getLong("expiresAtEpoch", 0L);
            l.status = MarketListing.Status.valueOf(s.getString("status", "ACTIVE"));
            listings.put(id, l);
        }
    }

    public void flush() {
        YamlConfiguration y = new YamlConfiguration();
        ConfigurationSection root = y.createSection("listings");
        for (MarketListing l : listings.values()) {
            ConfigurationSection s = root.createSection(l.listingId);
            s.set("sellerId", l.sellerId != null ? l.sellerId.toString() : null);
            s.set("item", l.item);
            s.set("quantity", l.quantity);
            s.set("priceWcoin", l.priceWcoin);
            s.set("priceWcraft", l.priceWcraft);
            s.set("createdAtEpoch", l.createdAtEpoch);
            s.set("expiresAtEpoch", l.expiresAtEpoch);
            s.set("status", l.status.name());
        }
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed saving listings.yml: " + e.getMessage());
        }
    }

    private UUID uuid(String s) {
        try { return s == null ? null : UUID.fromString(s); } catch (Exception ex) { return null; }
    }
}
