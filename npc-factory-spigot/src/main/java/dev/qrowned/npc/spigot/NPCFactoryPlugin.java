package dev.qrowned.npc.spigot;

import dev.qrowned.npc.api.handler.NPCHandler;
import dev.qrowned.npc.spigot.handler.DefaultNPCHandler;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class NPCFactoryPlugin extends JavaPlugin {

    @Getter
    private static NPCFactoryPlugin instance;

    private NPCHandler npcHandler;

    @Override
    public void onEnable() {
        instance = this;
        this.getLogger().info("Loading NPC Factory Plugin...");

        this.npcHandler = DefaultNPCHandler.create(20, 10, 10);
    }

}
