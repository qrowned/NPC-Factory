package dev.qrowned.npc.api;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.base.Preconditions;
import dev.qrowned.npc.api.data.NPCData;
import dev.qrowned.npc.api.event.PlayerNPCHideEvent;
import dev.qrowned.npc.api.event.PlayerNPCShowEvent;
import dev.qrowned.npc.api.handler.NPCHandler;
import dev.qrowned.npc.api.modifier.*;
import dev.qrowned.npc.api.utils.SpawnModifier;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a non-player character which can be configured via {@link NPCData}
 */
@Getter
@Setter
public class NPC {

    private final List<UUID> showedPlayers = new CopyOnWriteArrayList<>();
    private final List<UUID> excludedPlayers = new CopyOnWriteArrayList<>();

    private final int entityId;

    private final NPCData npcData;
    private final WrappedGameProfile wrappedGameProfile;

    private final Location location;
    private final SpawnModifier spawnModifier;

    private boolean lookAtPlayers;
    private boolean imitatePlayers;

    public NPC(int entityId, NPCData npcData, Location location, SpawnModifier spawnModifier, boolean lookAtPlayers, boolean imitatePlayers) {
        this.entityId = entityId;
        this.npcData = npcData;
        this.location = location;
        this.spawnModifier = spawnModifier;
        this.lookAtPlayers = lookAtPlayers;
        this.imitatePlayers = imitatePlayers;
        this.wrappedGameProfile = this.convertProfile(npcData);
    }

    @NotNull
    public static NPCBuilder builder() {
        return new NPCBuilder();
    }

    /**
     * Shows this npc to a player.
     *
     * @param player      The player to show this npc to.
     * @param plugin      The plugin requesting the change.
     * @param removeTicks The ticks before removing the player from the player list after
     *                    spawning. A negative value indicates that this npc shouldn't get
     *                    removed from the player list.
     */
    public void show(@NotNull Player player, @NotNull Plugin plugin, long removeTicks) {
        this.showedPlayers.add(player.getUniqueId());

        VisibilityModifier visibilityModifier = VisibilityModifier.create(this);
        visibilityModifier.queuePlayerListChange(EnumWrappers.PlayerInfoAction.ADD_PLAYER).send(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            visibilityModifier.queueSpawn().send(player);
            this.spawnModifier.handleSpawn(this, player);

            if (removeTicks >= 0) {
                Bukkit.getScheduler().runTaskLater(
                        plugin,
                        () -> visibilityModifier
                                .queuePlayerListChange(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER).send(player),
                        removeTicks
                );
            }

            Bukkit.getPluginManager().callEvent(new PlayerNPCShowEvent(player, this));
        }, 10L);
    }

    /**
     * Hides this npc from a player.
     *
     * @param player The player to hide the npc for.
     * @param plugin The plugin requesting the change.
     * @param reason The reason why the npc was hidden for the player.
     */
    public void hide(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @NotNull PlayerNPCHideEvent.Reason reason) {
        VisibilityModifier.create(this)
                .queuePlayerListChange(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER)
                .queueDestroy()
                .send(player);

        this.showedPlayers.remove(player.getUniqueId());

        Bukkit.getScheduler().runTask(plugin,
                () -> Bukkit.getPluginManager().callEvent(new PlayerNPCHideEvent(player, this, reason)));
    }

    /**
     * Converts a {@link NPCData} to a {@link WrappedGameProfile}.
     *
     * @param npcData The {@link NPCData} to convert
     * @return The game profile
     */
    @NotNull
    public WrappedGameProfile convertProfile(@NotNull NPCData npcData) {
        WrappedGameProfile gameProfile = new WrappedGameProfile(npcData.getUniqueId(),
                npcData.getName());
        npcData.getProperties().forEach(property -> gameProfile.getProperties().put(
                property.getName(),
                new WrappedSignedProperty(property.getName(), property.getValue(), property.getSignature())
        ));
        return gameProfile;
    }

    /**
     * Exclude a player from seeing the NPC
     *
     * @param player the player to exclude
     */
    public void exclude(@NotNull Player player) {
        this.excludedPlayers.add(player.getUniqueId());
    }

    /**
     * "Unexclude" a player from seeing the NPC
     *
     * @param player the player to "unexclude"
     */
    public void unExclude(@NotNull Player player) {
        this.excludedPlayers.remove(player.getUniqueId());
    }

    /**
     * Get if this npc is shown for the given {@code player}.
     *
     * @param player The player to check.
     * @return If the npc is shown for the given {@code player}.
     */
    public boolean isShownFor(@NotNull Player player) {
        return this.showedPlayers.contains(player.getUniqueId());
    }

    /**
     * Get if the specified {@code player} is explicitly not allowed to see this npc.
     *
     * @param player The player to check.
     * @return if the specified {@code player} is explicitly not allowed to see this npc.
     */
    public boolean isExcluded(@NotNull Player player) {
        return this.excludedPlayers.contains(player.getUniqueId());
    }

    /**
     * Creates a new animation modifier which serves methods to play animations on an NPC
     *
     * @return a animation modifier modifying this NPC
     */
    @NotNull
    public AnimationModifier animation() {
        return AnimationModifier.create(this);
    }

    /**
     * Creates a new rotation modifier which serves methods related to entity rotation
     *
     * @return a rotation modifier modifying this NPC
     */
    @NotNull
    public RotationModifier rotation() {
        return RotationModifier.create(this);
    }

    /**
     * Creates a new equipment modifier which serves methods to change an NPCs equipment
     *
     * @return an equipment modifier modifying this NPC
     */
    @NotNull
    public EquipmentModifier equipment() {
        return EquipmentModifier.create(this);
    }

    /**
     * Creates a new metadata modifier which serves methods to change an NPCs metadata, including
     * sneaking etc.
     *
     * @return a metadata modifier modifying this NPC
     */
    @NotNull
    public MetadataModifier metadata() {
        return MetadataModifier.create(this);
    }

    /**
     * Creates a new visibility modifier which serves methods to change an NPCs visibility.
     *
     * @return a visibility modifier modifying this NPC
     */
    @NotNull
    public VisibilityModifier visibility() {
        return VisibilityModifier.create(this);
    }

    public static class NPCBuilder {

        private NPCData data;

        private boolean lookAtPlayer = true;
        private boolean imitatePlayer = true;

        private Location location = new Location(Bukkit.getWorlds().get(0), 0D, 0D, 0D);
        private SpawnModifier spawnCustomizer = (npc, player) -> {
        };

        /**
         * Creates a new builder instance.
         */
        private NPCBuilder() {
        }

        /**
         * Sets the profile of the npc, cannot be changed afterwards
         *
         * @param profile the profile
         * @return this builder instance
         */
        public NPCBuilder data(@NotNull NPCData profile) {
            this.data = Preconditions.checkNotNull(profile, "profile");
            return this;
        }

        /**
         * Sets the location of the npc, cannot be changed afterwards
         *
         * @param location the location
         * @return this builder instance
         */
        public NPCBuilder location(@NotNull Location location) {
            this.location = Preconditions.checkNotNull(location, "location");
            return this;
        }

        /**
         * Enables/disables looking at the player, default is true
         *
         * @param lookAtPlayer if the NPC should look at the player
         * @return this builder instance
         */
        public NPCBuilder lookAtPlayers(boolean lookAtPlayer) {
            this.lookAtPlayer = lookAtPlayer;
            return this;
        }

        /**
         * Enables/disables imitation of the player, such as sneaking and hitting the player, default is
         * true
         *
         * @param imitatePlayer if the NPC should imitate players
         * @return this builder instance
         */
        public NPCBuilder imitatePlayers(boolean imitatePlayer) {
            this.imitatePlayer = imitatePlayer;
            return this;
        }

        /**
         * Sets an executor which will be called every time the NPC is spawned for a certain player.
         * Permanent NPC modifications should be done in this method, otherwise they will be lost at the
         * next respawn of the NPC.
         *
         * @param spawnModifier the spawn customizer which will be called on every spawn
         * @return this builder instance
         */
        public NPCBuilder spawnModifier(@NotNull SpawnModifier spawnModifier) {
            this.spawnCustomizer = Preconditions.checkNotNull(spawnModifier, "spawnModifier");
            return this;
        }

        /**
         * Passes the NPC to a handler which handles events, spawning and destruction of this NPC for
         * players
         *
         * @param handler the handler the NPC will be passed to
         * @return this builder instance
         */
        @NotNull
        public NPC build(@NotNull NPCHandler handler) {
            Preconditions.checkNotNull(this.data, "A profile must be given");
            Preconditions
                    .checkArgument(this.data.isComplete(), "The provided profile has to be complete!");

            NPC npc = new NPC(
                    handler.getFreeEntityId(),
                    this.data,
                    this.location,
                    this.spawnCustomizer,
                    this.lookAtPlayer,
                    this.imitatePlayer);
            handler.handleNPC(npc);

            return npc;
        }
    }

}
