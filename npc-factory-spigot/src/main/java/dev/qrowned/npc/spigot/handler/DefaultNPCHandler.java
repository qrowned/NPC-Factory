package dev.qrowned.npc.spigot.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.qrowned.npc.api.NPC;
import dev.qrowned.npc.api.event.PlayerNPCHideEvent;
import dev.qrowned.npc.api.event.PlayerNPCInteractEvent;
import dev.qrowned.npc.api.handler.NPCHandler;
import dev.qrowned.npc.api.modifier.AbstractModifier;
import dev.qrowned.npc.api.modifier.AnimationModifier;
import dev.qrowned.npc.api.modifier.MetadataModifier;
import dev.qrowned.npc.spigot.NPCFactoryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultNPCHandler implements NPCHandler {

    private static final Random RANDOM = new Random();

    private final double spawnDistance;
    private final double actionDistance;
    private final long tabListRemoveTicks;

    private final Map<Integer, NPC> npcMap = new ConcurrentHashMap<>();

    private DefaultNPCHandler(double spawnDistance, double actionDistance, long tabListRemoveTicks) {
        Preconditions.checkArgument(spawnDistance > 0 && actionDistance > 0, "Distance has to be > 0!");
        Preconditions.checkArgument(actionDistance <= spawnDistance,
                "Action distance cannot be higher than spawn distance!");

        this.spawnDistance = Math.min(
                spawnDistance * spawnDistance,
                Math.pow(Bukkit.getViewDistance() << 4, 2));
        this.actionDistance = actionDistance * actionDistance;
        this.tabListRemoveTicks = tabListRemoveTicks;

        Bukkit.getPluginManager().registerEvents(this, NPCFactoryPlugin.getInstance());

        this.registerInteractHandler();
        this.startNPCTick();
    }

    public static DefaultNPCHandler create(double spawnDistance, double actionDistance, long tabListRemoveTicks) {
        return new DefaultNPCHandler(spawnDistance, actionDistance, tabListRemoveTicks);
    }

    private void registerInteractHandler() {
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PacketAdapter(NPCFactoryPlugin.getInstance(), PacketType.Play.Client.USE_ENTITY) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        PacketContainer container = event.getPacket();
                        int targetId = container.getIntegers().read(0);

                        if (npcMap.containsKey(targetId)) {
                            NPC npc = npcMap.get(targetId);

                            EnumWrappers.Hand usedHand;
                            EnumWrappers.EntityUseAction action;

                            if (AbstractModifier.MINECRAFT_VERSION >= 17) {
                                WrappedEnumEntityUseAction useAction = container.getEnumEntityUseActions().read(0);
                                action = useAction.getAction();
                                usedHand = action == EnumWrappers.EntityUseAction.ATTACK
                                        ? EnumWrappers.Hand.MAIN_HAND
                                        : useAction.getHand();
                            } else {
                                action = container.getEntityUseActions().read(0);
                                usedHand = action == EnumWrappers.EntityUseAction.ATTACK
                                        ? EnumWrappers.Hand.MAIN_HAND
                                        : container.getHands().optionRead(0).orElse(EnumWrappers.Hand.MAIN_HAND);
                            }

                            Bukkit.getScheduler().runTask(
                                    NPCFactoryPlugin.getInstance(),
                                    () -> Bukkit.getPluginManager().callEvent(
                                            new PlayerNPCInteractEvent(
                                                    event.getPlayer(),
                                                    npc,
                                                    action,
                                                    usedHand))
                            );
                        }
                    }
                });
    }

    private void startNPCTick() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(NPCFactoryPlugin.getInstance(), () -> {
            for (Player player : ImmutableList.copyOf(Bukkit.getOnlinePlayers())) {
                for (NPC npc : this.npcMap.values()) {
                    Location npcLoc = npc.getLocation();
                    Location playerLoc = player.getLocation();
                    if (!npcLoc.getWorld().equals(playerLoc.getWorld())) {
                        if (npc.isShownFor(player)) {
                            npc.hide(player, NPCFactoryPlugin.getInstance(), PlayerNPCHideEvent.Reason.SPAWN_DISTANCE);
                        }
                        continue;
                    } else if (!npcLoc.getWorld().isChunkLoaded(npcLoc.getBlockX() >> 4, npcLoc.getBlockZ() >> 4)) {
                        if (npc.isShownFor(player)) {
                            npc.hide(player, NPCFactoryPlugin.getInstance(), PlayerNPCHideEvent.Reason.UNLOADED_CHUNK);
                        }
                        continue;
                    }

                    double distance = npcLoc.distanceSquared(playerLoc);
                    boolean inRange = distance <= this.spawnDistance;

                    if ((npc.isExcluded(player) || !inRange) && npc.isShownFor(player)) {
                        npc.hide(player, NPCFactoryPlugin.getInstance(), PlayerNPCHideEvent.Reason.SPAWN_DISTANCE);
                    } else if ((!npc.isExcluded(player) && inRange) && !npc.isShownFor(player)) {
                        npc.show(player, NPCFactoryPlugin.getInstance(), this.tabListRemoveTicks);
                    }

                    if (npc.isShownFor(player) && npc.isLookAtPlayers() && distance <= this.actionDistance) {
                        npc.rotation().queueLookAt(playerLoc).send(player);
                    }
                }
            }
        }, 20, 2);
    }

    @Override
    public int getFreeEntityId() {
        int id;

        do {
            id = RANDOM.nextInt(Integer.MAX_VALUE);
        } while (this.npcMap.containsKey(id));

        return id;
    }

    @Override
    public void handleNPC(@NotNull NPC npc) {
        this.npcMap.put(npc.getEntityId(), npc);
    }

    @Override
    public Optional<NPC> getNpc(int entityId) {
        return Optional.ofNullable(this.npcMap.get(entityId));
    }

    @Override
    public void removeNPC(int entityId) {
        this.getNpc(entityId).ifPresent(npc -> {
            this.npcMap.remove(entityId);
            npc.getShowedPlayers()
                    .forEach(uuid -> npc.hide(Bukkit.getPlayer(uuid), NPCFactoryPlugin.getInstance(), PlayerNPCHideEvent.Reason.REMOVED));
        });
    }

    @Override
    public @Unmodifiable Collection<NPC> getNPCs() {
        return Collections.unmodifiableCollection(this.npcMap.values());
    }

    @EventHandler
    public void handleRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isShownFor(player))
                .forEach(npc -> npc.hide(player, NPCFactoryPlugin.getInstance(), PlayerNPCHideEvent.Reason.RESPAWNED));
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isShownFor(player) || npc.isExcluded(player))
                .forEach(npc -> {
                    npc.getShowedPlayers().remove(player.getUniqueId());
                    npc.getExcludedPlayers().remove(player.getUniqueId());
                });
    }

    @EventHandler
    public void handleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        this.npcMap.values().stream()
                .filter(npc -> npc.isImitatePlayers() && npc.isShownFor(player))
                .filter(npc -> npc.getLocation().getWorld().equals(player.getWorld())
                        && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance)
                .forEach(npc -> npc.metadata()
                        .queue(MetadataModifier.EntityMetadata.SNEAKING, event.isSneaking()).send(player));
    }

    @EventHandler
    public void handleClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            this.npcMap.values().stream()
                    .filter(npc -> npc.isImitatePlayers() && npc.isShownFor(player))
                    .filter(npc -> npc.getLocation().getWorld().equals(player.getWorld())
                            && npc.getLocation().distanceSquared(player.getLocation()) <= this.actionDistance)
                    .forEach(npc -> npc.animation().queue(AnimationModifier.EntityAnimation.SWING_MAIN_ARM)
                            .send(player));
        }
    }

}
