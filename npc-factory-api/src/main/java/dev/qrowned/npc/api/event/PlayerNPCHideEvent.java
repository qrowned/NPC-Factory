package dev.qrowned.npc.api.event;

import dev.qrowned.npc.api.NPC;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerNPCHideEvent extends PlayerNPCEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter
    private final Reason reason;

    /**
     * Constructs a new event instance
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     * @param reason The reason for the hide.
     */
    public PlayerNPCHideEvent(Player player, NPC npc, Reason reason) {
        super(player, npc);
        this.reason = reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * Represents a reason why a npc was hidden for a player.
     */
    public enum Reason {
        /**
         * The player has manually been excluded from seeing the npc.
         */
        EXCLUDED,
        /**
         * The distance from npc and player is now higher than the configured spawn distance.
         */
        SPAWN_DISTANCE,
        /**
         * NPC was in an unloaded chunk.
         */
        UNLOADED_CHUNK,
        /**
         * The npc was removed from the pool.
         */
        REMOVED,
        /**
         * The player seeing the npc respawned.
         */
        RESPAWNED
    }

}
