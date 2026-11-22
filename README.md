# DonutSim Client (Minecraft 26.3 Fabric)

100% client-side stealth utility mod for **Minecraft 26.3** (Java 21, Fabric Loader `0.19.5`, Fabric API `0.162.0+26.3`, Fabric Loom `1.17`).

## Anti-Detection & Architecture Guarantees
- **`"environment": "client"`** in `fabric.mod.json` with zero `main` or `server` entrypoints.
- **Zero Network Traffic:** No `PayloadTypeRegistry` channels, custom packets, plugin channels, or server handshakes are ever registered or transmitted.
- **Pure Client Command Interception:** All commands use `ClientCommandRegistrationCallback` so they execute locally and never reach the multiplayer server.
- **Native Un-obfuscated 26.3 Build:** Uses Fabric Loom `1.17` with native Mojang mappings (no Yarn mappings or remapping dependencies).

## Building the `.jar`
Ensure Java 21 is installed and run:
```bash
./gradlew build
```
The compiled mod `.jar` will be output to `build/libs/donutsim-1.0.0.jar`.

## Features & Commands
- **Right Shift**: Opens the **DonutSim Config & HUD Editor** GUI (toggle HUD, drag to reposition, scale, edit balance/shards/kills/deaths/team, or simulate incoming payments).
- `/pay <player> <amount>` — Deducts `<amount>` (supports `K`, `M`, `B`, `T`), plays XP orb sound, and prints DonutSMP confirmation.
- `/payme <player> <amount>` — Simulates an incoming payment and adds `<amount>` to your balance.
- `/bal` / `/balance` — Shows your simulated balance locally.
- `/eco <set|give|take> <amount>` — Modifies your simulated balance.
- `/shards <set|give> <amount>` — Modifies your simulated shards.
- `/stats <kills|deaths|team> <value>` — Updates your sidebar stats or team tag.
- `/drop <item_id> [count]` — Spawns a glowing, client-only item entity in front of you that can be picked up locally.
- `/kit <spawner|pvp|keyall>` — Spawns a visual DonutSMP loot cluster on the ground.
