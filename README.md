# HollowRift

**A competitive survival SMP plugin for Paper 1.21.4+ (Paper 26.3+)**

[![Paper Version](https://img.shields.io/badge/Paper-1.21.4%2B-blue)](https://papermc.io)
[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/temurin/releases/?version=21)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Modrinth](https://img.shields.io/badge/Modrinth-Available-purple)](https://modrinth.com/plugin/hollowrift)

---

## 🎯 Overview

HollowRift transforms your SMP into a contested battlefield. Forge the legendary **Riftblade**, build altars to summon the **Hollow Warden**, and survive server-wide tracking that reveals your location every 5 minutes.

Built with modern Paper APIs: Adventure Components, PersistentDataContainer, BukkitRunnables.

---

## ⚔️ Features

### Custom Items
| Item | Type | Description |
|------|------|-------------|
| **Echoing Core** | Echo Shard (glowing) | Rare crafting component. PDC: `hollowrift:echoing_core`. Blocked from vanilla recipes. |
| **Riftblade** | Netherite Sword + Sharpness V, Unbreaking III, Mending | Signature weapon. PDC: `hollowrift:riftblade`. Two unique abilities. |

**Crafting Recipe (Shaped):**
```
E B E
B S B
E C E
```
E = Echo Shard, B = Breeze Rod, S = Netherite Sword, C = Echoing Core

### Riftblade Abilities
| Ability | Type | Details |
|---------|------|---------|
| **Resonance Stacks** | Passive | Consecutive hits on same target build stacks (reset on target switch / 4s). Every 3rd hit = sonic boom + 6 armor-piercing damage + knockback. |
| **Phase Slash** | Active (Right-click) | Dash 8 blocks forward (safe Y). SCULK_SOUL trail. Damage enemies within 2 blocks for 8 damage. Speed II (2s). 15s cooldown with action-bar countdown. |

### The Hollow Warden (Boss)
**Base**: Warden, **600 HP**, persistent, custom name, BossBar (60 block range).

| Mechanic | Trigger | Behavior |
|----------|---------|----------|
| **Sonic Shield** | Always | 90% melee reduction + 3 shield nodes. Destroy all 3 → disabled 20s → regenerates. |
| **Vibration Slam** | ≤50% HP | Every 12s: 3s warning → grounded players within 12 blocks launched up + Darkness + Slowness II (4s). |
| **Soul Tether** | ≤20% HP | Every 15s: tethers to 2 nearest players. Heals 10 HP/s while within 15 blocks. Break distance to stop. |
| **Death** | — | Drops 1–2 Echoing Cores. Server-wide announcement. Full cleanup. |

### Altar Summoning (Ritual)
1. Place **Sculk Shrieker** as center
2. Place **4 Chiseled Deepslate** at N/S/E/W (2 blocks out)
3. **Right-click Shrieker with Soul Lantern** → consumes lantern, lightning, broadcasts coords, spawns boss

### Competitive Tracking
Every **5 minutes**, players with a Riftblade (inventory or Ender Chest) ping their rough location:
```
📡 Riftblade resonance detected...
World: world | Approx: 500, 100, -250
```
Coordinates rounded to nearest 250 blocks.

---

## 🛠️ Installation

**Requirements:** Paper 1.21.4+ (build 26.3+), Java 21

```bash
# 1. Download latest release
wget https://github.com/your-repo/HollowRift/releases/latest/download/HollowRift-1.0.0.jar

# 2. Place in plugins folder
mv HollowRift-1.0.0.jar /path/to/server/plugins/

# 3. Restart server
./start.sh
```

Also available via Modrinth, Hangar, and SpigotMC for one-click install.

### Permissions
| Permission | Default | Description |
|------------|---------|-------------|
| `hollowrift.admin` | op | All admin commands |
| `hollowrift.give` | op | Give custom items |
| `hollowrift.spawnboss` | op | Spawn the boss |

### Commands
| Command | Description |
|---------|-------------|
| `/hollowrift give <core\|blade> [player]` | Give custom item |
| `/hollowrift spawnboss [player]` | Spawn Hollow Warden at player |
| `/hr` | Alias for `/hollowrift` |

---

## 🏗️ Architecture

- **Paper 1.21.4+** (26.3+)
- **Java 21**
- **Adventure API** for components
- **PersistentDataContainer** for item identification
- **BukkitRunnables** for timers
- **Shaded JAR** — zero runtime dependencies
- **Modular structure**: `items/`, `combat/`, `boss/`, `altar/`, `commands/`
- **MIT Licensed** — free to use, modify, distribute

---

## 🎮 Gameplay Loop

1. **Explore** Deep Dark → collect Echo Shards + Breeze Rods
2. **Craft** Echoing Core → forge **Riftblade**
3. **Build Altar** (Sculk Shrieker + 4 Chiseled Deepslate + Soul Lantern) or use `/hollowrift spawnboss`
4. **Fight** Hollow Warden → break shield nodes, dodge slams, break tethers
5. **Claim** 1–2 Echoing Cores
6. **Every 5 min**: Riftblade pings location → server converges → PvP erupts

---

## 📦 Downloads

- **Modrinth**: [modrinth.com/plugin/hollowrift](https://modrinth.com/plugin/hollowrift)
- **GitHub Releases**: [github.com/your-repo/HollowRift/releases](https://github.com/your-repo/HollowRift/releases)
- **Source Code**: [github.com/your-repo/HollowRift](https://github.com/your-repo/HollowRift)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

## 🙏 Credits

Built for the competitive SMP community. Inspired by the Deep Dark, the Warden, and the thrill of hunted progression.

**Star the repo** if you enjoy the plugin! ⭐