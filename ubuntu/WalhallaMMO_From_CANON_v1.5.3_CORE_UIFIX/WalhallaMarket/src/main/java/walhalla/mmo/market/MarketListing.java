package walhalla.mmo.market;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class MarketListing {

    public enum Status { ACTIVE, SOLD, CANCELLED, EXPIRED }

    public String listingId;
    public UUID sellerId;
    public ItemStack item;
    public int quantity;

    // Mixed price allowed (canon: WCraft can be partial)
    public long priceWcoin;
    public long priceWcraft;

    public long createdAtEpoch;
    public long expiresAtEpoch; // 0 = no expiry
    public Status status = Status.ACTIVE;
}
