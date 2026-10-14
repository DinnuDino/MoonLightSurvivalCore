# MoonlightVanillaSurvivalCore
> Official Clean-Room Paper 26.2 / Java 21 Integration Plugin

This plugin integrates **MoonlightHomes** with safe-teleport validation, **MoonlightTeleport (TPA)** with 3-second warmup/cancellation, backed by **HikariCP Asynchronous Connection Pools**, **Point-in-Time Audit & Item Refund Hub**, and a **Strict Anti-Auto-OP Safeguard**.

---

## 🚀 Quick Start & Compilation

### Requirements
- **Java 21 LTS** or newer (`java -version`)
- **Paper 26.2** server jar
- Gradle 8.5+ or Maven 3.9+

### Option A: Gradle (Recommended)
```bash
# Build the shaded JAR
./gradlew shadowJar

# The compiled JAR will be located at:
# build/libs/MoonlightVanillaSurvivalCore-1.0.0-SNAPSHOT.jar
```

### Option B: Maven
```bash
# Build the shaded JAR
mvn clean package

# The compiled JAR will be located at:
# target/MoonlightVanillaSurvivalCore-1.0.0-SNAPSHOT.jar
```

---

## 📦 Installation to Paper 26.2 Server
1. Stop your Paper 26.2 Minecraft server.
2. Copy `MoonlightVanillaSurvivalCore-1.0.0-SNAPSHOT.jar` into your server's `plugins/` folder.
3. Start the server:
   ```bash
   java -Xms4G -Xmx4G -XX:+UseG1GC -jar paper-26.2.jar --nogui
   ```
4. Check the console output:
   ```
   [MoonlightVanillaSurvivalCore] [Database] HikariCP connection pool active and ready.
   [MoonlightVanillaSurvivalCore] [SecurityAudit] Strict OP audit passed clean.
   ```

---

## ⚙️ In-Game Commands
- `/sethome [name]` - Create a home with safe air/block validation.
- `/home [name]` - Teleport to your home.
- `/delhome <name>` - Remove an existing home.
- `/homes` - List all saved homes.
- `/tpa <player>` - Send teleport request with 3s warmup.
- `/tpahere <player>` - Request player teleport to your location.
- `/tpaccept` - Accept incoming TPA request.
- `/tpdeny` - Deny incoming TPA request.
- `/tpacancel` - Cancel your pending outgoing TPA request.
- `/logs <player>` - Point-in-time audit and snapshot inspector.
- `/shelp` - Staff command handbook and administrative guide.

---

## 🛠️ Troubleshooting & Server Setup Notes
1. **Paper 26.2 Command Registration**: Paper 26.2 does not allow legacy YAML command lookups via `getCommand()`. This plugin uses modern Paper Brigadier `BasicCommand` programmatic registration with automatic fallback to `CommandMap`.
2. **Remove Fabric Mods from `plugins/`**: If your console shows `Error loading plugin: Directory 'plugins/fabric-api-*.jar' does not contain a paper-plugin.yml`, make sure to delete `fabric-api-*.jar` from your `plugins/` folder. Paper runs Paper/Bukkit plugins; Fabric mods only run on Fabric loaders.
3. **Database File**: On startup, SQLite automatically creates `plugins/MoonlightVanillaSurvivalCore/moonlight_core.db`. Ensure your server process has read/write filesystem permissions.
