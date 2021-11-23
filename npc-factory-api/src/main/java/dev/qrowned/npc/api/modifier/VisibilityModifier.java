package dev.qrowned.npc.api.modifier;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.qrowned.npc.api.NPC;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisibilityModifier extends AbstractModifier {

    private VisibilityModifier(@NotNull NPC npc) {
        super(npc);
    }

    /**
     * Creates a new npc modifier.
     *
     * @param npc The npc this modifier is for.
     */
    public static VisibilityModifier create(@NotNull NPC npc) {
        return new VisibilityModifier(npc);
    }

    /**
     * Enqueues the change of the player list for the wrapped npc.
     *
     * @param action The action of the player list change.
     * @return The same instance of this class, for chaining.
     * @since 2.5-SNAPSHOT
     */
    @NotNull
    public VisibilityModifier queuePlayerListChange(@NotNull PlayerInfoAction action) {
        return this.queuePlayerListChange(action.handle);
    }

    /**
     * Enqueues the change of the player list for the wrapped npc.
     *
     * @param action The action of the player list change as a protocol lib wrapper.
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public VisibilityModifier queuePlayerListChange(@NotNull EnumWrappers.PlayerInfoAction action) {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.PLAYER_INFO, false);
        packetContainer.getPlayerInfoAction().write(0, action);

        PlayerInfoData playerInfoData = new PlayerInfoData(
                super.npc.getWrappedGameProfile(),
                20,
                EnumWrappers.NativeGameMode.CREATIVE,
                WrappedChatComponent.fromText(super.npc.getWrappedGameProfile().getName()));
        packetContainer.getPlayerInfoDataLists().write(0, new ArrayList<>(List.of(playerInfoData)));

        return this;
    }

    /**
     * Enqueues the spawn of the wrapped npc.
     *
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public VisibilityModifier queueSpawn() {
        PacketContainer packetContainer = super.newContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        packetContainer.getUUIDs().write(0, super.npc.getNpcData().getUniqueId());

        double x = super.npc.getLocation().getX();
        double y = super.npc.getLocation().getY();
        double z = super.npc.getLocation().getZ();

        if (MINECRAFT_VERSION < 9) {
            packetContainer.getIntegers()
                    .write(1, (int) Math.floor(x * 32.0D))
                    .write(2, (int) Math.floor(y * 32.0D))
                    .write(3, (int) Math.floor(z * 32.0D));
        } else {
            packetContainer.getDoubles()
                    .write(0, x)
                    .write(1, y)
                    .write(2, z);
        }

        packetContainer.getBytes()
                .write(0, (byte) (super.npc.getLocation().getYaw() * 256F / 360F))
                .write(1, (byte) (super.npc.getLocation().getPitch() * 256F / 360F));

        if (MINECRAFT_VERSION < 15) {
            packetContainer.getDataWatcherModifier().write(0, new WrappedDataWatcher());
        }

        return this;
    }

    /**
     * Enqueues the de-spawn of the wrapped npc.
     *
     * @return The same instance of this class, for chaining.
     */
    @NotNull
    public VisibilityModifier queueDestroy() {
        PacketContainer packetContainer = super
                .newContainer(PacketType.Play.Server.ENTITY_DESTROY, false);

        if (MINECRAFT_VERSION >= 17) {
            packetContainer.getIntLists().write(0, new ArrayList<>(List.of(super.npc.getEntityId())));
        } else {
            packetContainer.getIntegerArrays().write(0, new int[]{super.npc.getEntityId()});
        }
        return this;
    }

    /**
     * A wrapper for all available player info actions.
     */
    public enum PlayerInfoAction {
        /**
         * Adds a player to the player list.
         */
        ADD_PLAYER(EnumWrappers.PlayerInfoAction.ADD_PLAYER),
        /**
         * Updates the game mode of a player.
         */
        UPDATE_GAME_MODE(EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE),
        /**
         * Updates the latency of a player.
         */
        UPDATE_LATENCY(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY),
        /**
         * Updates the display name of a player.
         */
        UPDATE_DISPLAY_NAME(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME),
        /**
         * Removes a specific player from the player list.
         */
        REMOVE_PLAYER(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        /**
         * The protocol lib wrapper for the action.
         */
        private final EnumWrappers.PlayerInfoAction handle;

        /**
         * Creates a new action.
         *
         * @param handle The protocol lib wrapper for the action.
         */
        PlayerInfoAction(EnumWrappers.PlayerInfoAction handle) {
            this.handle = handle;
        }
    }

}
