# Walhalla MMO - Minecraft RPG Plugin Suite

A comprehensive RPG plugin system for Minecraft Paper 1.21.8 servers.

## 📁 Repository Structure

```
walhalla-mmo/
├── WalhallaMMO/                          # Source code for all plugins
│   ├── WalhallaCore/                     # Core API and shared services
│   ├── WalhallaCombat/                   # Combat system and damage
│   ├── WalhallaSkills/                   # Skill system and progression
│   ├── WalhallaQuests/                   # Quest system
│   ├── WalhallaProfessions/              # Profession system
│   ├── WalhallaSpells/                   # Magic and spellcasting
│   ├── WalhallaWeapons/                  # Custom weapons
│   ├── WalhallaWeaponMods/               # Weapon modifications
│   ├── WalhallaBosses/                   # Boss encounters
│   ├── WalhallaDungeons/                 # Dungeon system
│   ├── WalhallaEvents/                   # Server events
│   ├── WalhallaGuilds/                   # Guild system
│   ├── WalhallaMarket/                   # Economy and marketplace
│   ├── WalhallaMenu/                     # UI menus
│   ├── WalhallaParty/                    # Party system
│   ├── WalhallaRPGMenu/                  # RPG interface
│   └── WalhallaZones/                    # Zone management
│
├── COMPLETE_SERVER_PACKAGE/              # Deployment package
│   ├── plugins/                          # All 17 compiled plugin JARs
│   ├── bukkit.yml                        # Server configurations
│   ├── server.properties
│   ├── spigot.yml
│   └── start scripts                     # Windows/Linux startup scripts
│
├── WALHALLA_MMO_COMPLETE_SERVER_v1.5.0.zip  # Ready-to-deploy package
│
└── Documentation/
    ├── PROJECT_GUIDE.txt                 # Development guide
    ├── QUICK_REFERENCE.txt               # Quick command reference
    ├── README_QUICK_START.txt            # Deployment quickstart
    ├── README_SERVER.txt                 # Server setup guide
    ├── START_HERE.txt                    # Getting started
    └── STACK_OVERFLOW_FIX_SUMMARY.txt    # Bug fix documentation

```

## 🚀 Quick Start

### For Server Admins (Deployment)

1. **Download the complete package:**
   - Use `WALHALLA_MMO_COMPLETE_SERVER_v1.5.0.zip`

2. **Extract to your server:**
   - Unzip to your server directory
   - Download Paper 1.21.8 from https://papermc.io/downloads/paper
   - Place `paper-1.21.8.jar` in the root directory

3. **Start the server:**
   - Windows: Double-click `start.bat`
   - Linux: Run `./start.sh`

4. **Configure:**
   - Edit `server.properties` for basic settings
   - Plugin configs are in `plugins/WalhallaCore/`, etc.

See [README_QUICK_START.txt](README_QUICK_START.txt) for detailed instructions.

### For Developers (Building from Source)

1. **Prerequisites:**
   - Java 21 JDK
   - Maven 3.9+
   - Git

2. **Clone and build:**
   ```bash
   cd WalhallaMMO
   mvn clean package -DskipTests
   ```

3. **Find compiled JARs:**
   - Each plugin's JAR is in `{module}/target/`
   - Copy JARs to your test server's `plugins/` folder

See [PROJECT_GUIDE.txt](PROJECT_GUIDE.txt) for development workflow.

## 📦 What's Included

### 17 Plugins (All Version 1.5.0-SNAPSHOT)

1. **walhalla-core** - Core API and services
2. **walhalla-combat** - RPG combat system
3. **walhalla-skills** - Skill trees and progression
4. **walhalla-quests** - Quest system
5. **walhalla-professions** - Gathering and crafting
6. **walhalla-spells** - Magic system
7. **walhalla-weapons** - Custom weapons
8. **walhalla-weapon-mods** - Weapon enhancements
9. **walhalla-bosses** - Boss encounters
10. **walhalla-dungeons** - Instanced dungeons
11. **walhalla-events** - World events
12. **walhalla-guilds** - Player guilds
13. **walhalla-market** - Economy system
14. **walhalla-menu** - UI framework
15. **walhalla-party** - Group system
16. **walhalla-rpgmenu** - Character interface
17. **walhalla-zones** - Zone management

## 🔧 System Requirements

- **Minecraft Version:** Paper 1.21.8 or higher
- **Java:** 21 or higher
- **RAM:** Minimum 4GB, recommended 8GB+
- **CPU:** Multi-core recommended
- **OS:** Windows, Linux, or macOS

## 📝 Recent Updates

### v1.5.0-SNAPSHOT (January 28, 2026)

**Critical Fixes:**
- ✅ Fixed StackOverflowError in combat system
- ✅ Combat now uses direct health manipulation (no recursive events)
- ✅ All 17 plugins load successfully
- ✅ Server stable under load

**Known Issues:**
- None currently

See [STACK_OVERFLOW_FIX_SUMMARY.txt](STACK_OVERFLOW_FIX_SUMMARY.txt) for technical details.

## 🆘 Support

### Common Issues

**Server won't start:**
- Verify Java 21 is installed: `java -version`
- Check you have Paper 1.21.8 (not Spigot or Bukkit)
- Review `logs/latest.log` for errors

**Plugins not loading:**
- Ensure all 17 JARs are in `plugins/` folder
- Check for errors in console during startup
- Verify walhalla-core loads first (dependency for others)

**Combat crashes:**
- Update to latest v1.5.0-SNAPSHOT build
- See [STACK_OVERFLOW_FIX_SUMMARY.txt](STACK_OVERFLOW_FIX_SUMMARY.txt)

### Documentation

- **Deployment:** [README_QUICK_START.txt](README_QUICK_START.txt)
- **Development:** [PROJECT_GUIDE.txt](PROJECT_GUIDE.txt)
- **Commands:** [QUICK_REFERENCE.txt](QUICK_REFERENCE.txt)
- **Server Setup:** [README_SERVER.txt](README_SERVER.txt)

## 📜 License

All rights reserved. This is a private project.

## 🏗️ Development Status

**Phase:** Production-ready v1.5.0
**Status:** ✅ All systems operational
**Last Build:** January 28, 2026
