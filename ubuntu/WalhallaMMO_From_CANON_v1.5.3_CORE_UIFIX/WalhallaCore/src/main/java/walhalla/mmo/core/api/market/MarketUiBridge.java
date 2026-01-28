package walhalla.mmo.core.api.market;

import org.bukkit.entity.Player;

/**
 * Player UI bridge for market + trade (Phase 14).
 * No player commands: everything must be reachable via UI.
 */
public interface MarketUiBridge {

    /**
     * Open the market main menu for the player.
     */
    void openMarketMenu(Player player);
}
