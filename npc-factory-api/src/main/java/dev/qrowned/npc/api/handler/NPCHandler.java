package dev.qrowned.npc.api.handler;

import dev.qrowned.npc.api.NPC;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents the handler of a npc. Needed e.g. for {@link NPC.NPCBuilder}.
 */
public interface NPCHandler extends Listener {

    /**
     * Get a free entity id for a npc
     *
     * @return the free entity id
     */
    int getFreeEntityId();

    /**
     * Let this handler handle a specific npc
     *
     * @param npc the npc to be handled
     */
    void handleNPC(@NotNull NPC npc);

    /**
     * Get a npc handled by this handler
     *
     * @param entityId the entity id of the npc
     * @return the npc
     */
    Optional<NPC> getNpc(int entityId);

    /**
     * Remove a npc from this handler
     *
     * @param entityId the entity id of the npc
     */
    void removeNPC(int entityId);

    /**
     * Get all npcs handled by this handler
     *
     * @return an unmodifiable list of all npcs
     */
    @Unmodifiable
    Collection<NPC> getNPCs();

}
