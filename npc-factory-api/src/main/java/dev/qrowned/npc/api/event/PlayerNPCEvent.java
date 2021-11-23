package dev.qrowned.npc.api.event;

import dev.qrowned.npc.api.NPC;
import dev.qrowned.npc.api.modifier.AbstractModifier;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

public abstract class PlayerNPCEvent extends PlayerEvent {

    @Getter
    private final NPC npc;

    /**
     * Constructs a new event instance
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     */
    public PlayerNPCEvent(Player player, NPC npc) {
        super(player);
        this.npc = npc;
    }

    /**
     * Sends the queued data in the provided {@link AbstractModifier}s to the player involved in this
     * event.
     *
     * @param npcModifiers The {@link AbstractModifier}s whose data should be sent
     */
    public void send(AbstractModifier... npcModifiers) {
        for (AbstractModifier npcModifier : npcModifiers) {
            npcModifier.send(super.getPlayer());
        }
    }

}
