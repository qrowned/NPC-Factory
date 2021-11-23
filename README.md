<br />
<p align="center">
  <a href="https://github.com/qrowned/NPC-Factory">
    <img src="https://i.imgur.com/iGyQ997.png" alt="Logo" width="117" height="121">
  </a>

<h3 align="center">NPC-Factory</h3>
<br>
  <p align="center">
    This spigot plugin is a easy to use NPC API called NPC-Factory. <br />
    Creating non-player-characters was never so easy!
    <br />
  </p>
  
## 💻 Built With

* []() <img src="https://www.vectorlogo.zone/logos/java/java-icon.svg" alt="java" width="20" height="20"/> Java
* []() <img src="https://www.vectorlogo.zone/logos/minecraft/minecraft-icon.svg" alt="mongodb" width="20" height="20"/> Spigot

## Installation

1. Download the latest [release](https://github.com/qrowned/NPC-Factory/releases).
2. Put the .jar file in your plugins folder.
3. Restart the server.

## Developing

Add the dependency to your project:


### Maven

```xml

<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.qrowned</groupId>
    <artifactId>NPC-Factory</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

### Gradle

```groovy
maven {
    name 'jitpack.io'
    url 'https://jitpack.io'
}

implementation only: 'com.github.qrowned:NPC-Factory:1.0.0-RELEASE'
```

### Code Example

```java
public class NPCExample {

    private final NPCFactoryPlugin npcFactoryPlugin;

    public NPCExample(NPCFactoryPlugin npcFactoryPlugin) {
        this.npcFactoryPlugin = npcFactoryPlugin;
    }

    /**
     * Spawn sample npc
     *
     * @param location        location the npc should spawn
     * @param excludedPlayers list of players which should not see the npc
     */
    public void spawnNPC(Location location, List<Player> excludedPlayers) {
        final NPC npc = NPC.builder()
                .data(this.createNPCData())
                .location(location)
                .lookAtPlayers(false)
                .imitatePlayers(false)
                .build(this.npcFactoryPlugin.getNpcHandler());

        excludedPlayers.forEach(npc::exclude);
    }

    /**
     * Create a sample npc data
     *
     * @return created npc data
     */
    private NPCData createNPCData() {
        // Creating data and fetching data from mojang
        final NPCData npcData = NPCData.create(UUID.fromString("a7f2ffe9-cf32-46be-8453-9c73eb3be00b"));
        npcData.complete();

        // Change UUID & Name
        npcData.setUniqueId(UUID.randomUUID());
        npcData.setName("NPC-Factory");

        return npcData;
    }

}
```
