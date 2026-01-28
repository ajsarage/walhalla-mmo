package walhalla.mmo.professions;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    public enum Type { PROFESSIONS, CRAFTING, SELL }

    private final Type type;

    public MenuHolder(Type type) {
        this.type = type;
    }

    public Type type() { return type; }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
