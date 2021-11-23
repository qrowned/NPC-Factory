package dev.qrowned.npc.api.event;

import dev.qrowned.npc.api.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerNPCShowEvent extends PlayerNPCEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    /**
     * Constructs a new event instance
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     */
    public PlayerNPCShowEvent(Player player, NPC npc) {
        super(player, npc);
    }


    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

}
