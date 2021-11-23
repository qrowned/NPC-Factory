package dev.qrowned.npc.api.utils;

import dev.qrowned.npc.api.NPC;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a modifier for the spawn of a {@link NPC} for a player.
 */
public interface SpawnModifier {

    /**
     * Being executed on the spawn of a NPC to a specific player.
     *
     * @param npc    the spawned NPC
     * @param player the player the NPC has been spawned for
     */
    void handleSpawn(@NotNull NPC npc, @NotNull Player player);

}
