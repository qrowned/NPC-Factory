package dev.qrowned.npc.api.event;

import com.comphenix.protocol.wrappers.EnumWrappers;
import dev.qrowned.npc.api.NPC;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerNPCInteractEvent extends PlayerNPCEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter
    private final Hand hand;
    @Getter
    private final EntityUseAction action;

    /**
     * Constructs a new event instance.
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     * @param action The action type of the interact.
     */
    public PlayerNPCInteractEvent(
            @NotNull Player player,
            @NotNull NPC npc,
            @NotNull EnumWrappers.EntityUseAction action) {
        this(player, npc, EntityUseAction.fromHandle(action), Hand.MAIN_HAND);
    }

    /**
     * Constructs a new event instance.
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     * @param action The action type of the interact.
     * @param hand   The player hand used for the interact.
     */
    public PlayerNPCInteractEvent(
            @NotNull Player player,
            @NotNull NPC npc,
            @NotNull EnumWrappers.EntityUseAction action,
            @NotNull EnumWrappers.Hand hand) {
        this(player, npc, EntityUseAction.fromHandle(action), Hand.fromHandle(hand));
    }

    /**
     * Constructs a new event instance.
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     * @param action The action type of the interact.
     */
    public PlayerNPCInteractEvent(
            @NotNull Player player,
            @NotNull NPC npc,
            @NotNull EntityUseAction action) {
        this(player, npc, action, Hand.MAIN_HAND);
    }

    /**
     * Constructs a new event instance.
     *
     * @param player The player who interacted with the npc.
     * @param npc    The npc with whom the player has interacted.
     * @param action The action type of the interact.
     * @param hand   The player hand used for the interact.
     */
    public PlayerNPCInteractEvent(
            @NotNull Player player,
            @NotNull NPC npc,
            @NotNull EntityUseAction action,
            @NotNull Hand hand) {
        super(player, npc);
        this.action = action;
        this.hand = hand;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    /**
     * A wrapper for the interact action with a npc.
     */
    public enum EntityUseAction {
        /**
         * A normal interact. (right click)
         */
        INTERACT(EnumWrappers.EntityUseAction.INTERACT),
        /**
         * An attack. (left click)
         */
        ATTACK(EnumWrappers.EntityUseAction.ATTACK),
        /**
         * A normal interact to a specific entity. (right click)
         */
        INTERACT_AT(EnumWrappers.EntityUseAction.INTERACT_AT);

        /**
         * All values of the action, to prevent a copy.
         */
        private static final EntityUseAction[] VALUES = values();
        /**
         * The entity use action as the protocol lib wrapper.
         */
        private final EnumWrappers.EntityUseAction handle;

        /**
         * Constructs an instance of the interact action.
         *
         * @param handle The protocol lib association with the action.
         */
        EntityUseAction(EnumWrappers.EntityUseAction handle) {
            this.handle = handle;
        }

        /**
         * Converts the protocol lib wrapper to the associated action.
         *
         * @param action The protocol lib wrapper of the association.
         * @return The association with the protocol lib wrapper action.
         * @throws IllegalArgumentException When no association was found.
         */
        @NotNull
        private static EntityUseAction fromHandle(@NotNull EnumWrappers.EntityUseAction action) {
            for (EntityUseAction value : VALUES) {
                if (value.handle == action) {
                    return value;
                }
            }
            throw new IllegalArgumentException("No use action for handle: " + action);
        }
    }

    /**
     * A wrapper for the hand used for interacts.
     */
    public enum Hand {
        /**
         * Main hand of the player.
         */
        MAIN_HAND(EnumWrappers.Hand.MAIN_HAND),
        /**
         * Off hand of the player.
         */
        OFF_HAND(EnumWrappers.Hand.OFF_HAND);

        /**
         * All hand enum values, to prevent a copy.
         */
        private static final Hand[] VALUES = values();

        /**
         * The hand as the protocol lib wrapper.
         */
        private final EnumWrappers.Hand handle;

        /**
         * @param handle The hand as the protocol lib wrapper.
         */
        Hand(EnumWrappers.Hand handle) {
            this.handle = handle;
        }

        /**
         * Converts the protocol lib wrapper to the associated hand.
         *
         * @param hand The protocol lib wrapper of the association.
         * @return The association with the protocol lib wrapper hand.
         * @throws IllegalArgumentException When no association was found.
         */
        @NotNull
        private static Hand fromHandle(@NotNull EnumWrappers.Hand hand) {
            for (Hand value : VALUES) {
                if (value.handle == hand) {
                    return value;
                }
            }
            throw new IllegalArgumentException("No hand for handle: " + hand);
        }
    }

}
