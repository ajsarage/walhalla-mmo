╔════════════════════════════════════════════════════════════════════════════════╗
║                                                                                ║
║                  🎮 WALHALLA MMO COMPLETE SERVER PACKAGE 🎮                    ║
║                                                                                ║
║                         Version 1.5.0 - Full Server                            ║
║                                                                                ║
║                   ✅ Everything Included • Ready to Run ✅                     ║
║                                                                                ║
╚════════════════════════════════════════════════════════════════════════════════╝


📦 WHAT'S INCLUDED
════════════════════════════════════════════════════════════════════════════════

This package contains EVERYTHING needed to run a complete Walhalla MMO server:

✓ Server Configuration Files
  • server.properties (pre-configured for RPG gameplay)
  • bukkit.yml (optimized spawn rates)
  • spigot.yml (performance tuned for MMO)
  • eula.txt (pre-accepted)

✓ Start Scripts
  • start.bat (Windows - double-click to run)
  • start.sh (Linux/Mac - chmod +x start.sh first)
  • Optimized JVM flags for 4GB RAM

✓ Plugin System
  • plugins/ folder with all 17 Walhalla MMO plugins
  • 20 embedded canon data files (game balance)
  • 15+ configuration templates
  • 100% functional out of the box

✓ Documentation
  • This README
  • Installation guide (detailed steps)
  • Full functionality guide (game mechanics)


⚠️ WHAT YOU NEED TO ADD
════════════════════════════════════════════════════════════════════════════════

Only ONE file is missing (due to Mojang licensing):

❗ paper-1.21.8.jar (the server JAR)

Download from: https://papermc.io/downloads/paper

Steps:
1. Go to https://papermc.io/downloads/paper
2. Select version: 1.21.8
3. Click "Download"
4. Move paper-1.21.8.jar to this folder
5. Rename it exactly to: paper-1.21.8.jar

That's it! Everything else is ready.


🚀 QUICK START (5 MINUTES)
════════════════════════════════════════════════════════════════════════════════

Step 1: Download Paper JAR
  → https://papermc.io/downloads/paper (version 1.21.8)
  → Place in this folder as "paper-1.21.8.jar"

Step 2: Verify Java 21
  → Open terminal/command prompt
  → Type: java -version
  → Should show "21" or higher
  → If not, download from: https://adoptium.net/

Step 3: Start Server
  ┌─ Windows ────────────────────────────────────┐
  │ Double-click: start.bat                      │
  └──────────────────────────────────────────────┘

  ┌─ Linux/Mac ──────────────────────────────────┐
  │ chmod +x start.sh                            │
  │ ./start.sh                                   │
  └──────────────────────────────────────────────┘

Step 4: Wait for Server Load
  → First run: 30-60 seconds (generating world)
  → Subsequent runs: 10-20 seconds
  → Look for "Done!" message in console

Step 5: Connect & Verify
  → Join: localhost (or your server IP)
  → Run: /plugins
  → Should show 17 green Walhalla plugins
  → Test: /walhalla help


📋 FOLDER STRUCTURE
════════════════════════════════════════════════════════════════════════════════

After first run, your server will look like:

COMPLETE_SERVER_PACKAGE/
  ├── paper-1.21.8.jar            ← YOU ADD THIS
  ├── start.bat                   ✓ Start script (Windows)
  ├── start.sh                    ✓ Start script (Linux/Mac)
  ├── eula.txt                    ✓ Pre-accepted
  ├── server.properties           ✓ Server settings
  ├── bukkit.yml                  ✓ Bukkit config
  ├── spigot.yml                  ✓ Spigot config
  ├── README_SERVER.txt           ✓ This file
  │
  ├── plugins/                    ✓ All 17 Walhalla plugins
  │   ├── walhalla-core.jar
  │   ├── walhalla-combat.jar
  │   ├── walhalla-skills.jar
  │   └── ... (14 more)
  │
  ├── world/                      (generated on first run)
  ├── world_nether/               (generated on first run)
  ├── world_the_end/              (generated on first run)
  ├── logs/                       (server logs)
  └── cache/                      (server cache)


⚙️ SYSTEM REQUIREMENTS
════════════════════════════════════════════════════════════════════════════════

Minimum:
  • Java 21 LTS (required)
  • RAM: 2 GB allocated
  • Storage: 2 GB free space
  • CPU: 2 cores

Recommended (for 10+ players):
  • Java 21 LTS
  • RAM: 4 GB allocated (pre-configured)
  • Storage: 5 GB free space
  • CPU: 4 cores
  • SSD (faster world loading)

Check Java Version:
  → java -version
  → Must show version 21 or higher
  → Download: https://adoptium.net/


🎮 SERVER SETTINGS (PRE-CONFIGURED)
════════════════════════════════════════════════════════════════════════════════

Default Settings (Edit server.properties to change):
  • Max Players: 50
  • Difficulty: Normal
  • PvP: Enabled (MMO combat zones)
  • Flight: Disabled (anti-cheat)
  • View Distance: 10 chunks
  • Simulation Distance: 10 chunks
  • MOTD: "⚔ WALHALLA MMO | RPG Adventure Awaits!"
  • Port: 25565 (standard)

Performance Settings (Optimized):
  • JVM Flags: Aikar's G1GC flags (industry standard)
  • RAM: 4GB allocated (-Xmx4G -Xms2G)
  • Entity Activation Range: Balanced for RPG
  • Mob Spawn Rates: Tuned for MMO gameplay


🔧 CUSTOMIZATION
════════════════════════════════════════════════════════════════════════════════

Want to change settings?

Server Settings:
  → Edit: server.properties
  → Change max-players, difficulty, pvp, etc.
  → Restart server for changes to apply

RAM Allocation:
  → Edit: start.bat (Windows) or start.sh (Linux)
  → Change -Xmx4G and -Xms2G values
  → Example: -Xmx8G -Xms4G for 8GB

Game Balance:
  → After first run, find: plugins/WalhallaCore/canon/
  → Edit any ANNEX_*.txt file (XP, items, economy, etc.)
  → Use /walhalla reload command to apply changes

Plugin Settings:
  → After first run, find: plugins/WalhallaSkills/config.yml
  → Each plugin has its own config.yml
  → Edit and restart or use /walhalla reload


❓ FREQUENTLY ASKED QUESTIONS
════════════════════════════════════════════════════════════════════════════════

Q: Why isn't paper-1.21.8.jar included?
A: Mojang's EULA prohibits redistribution of server software.
   You must download it yourself from papermc.io (free).

Q: Can I use Spigot instead of Paper?
A: Not recommended. Paper has better performance and compatibility.
   Walhalla MMO is tested on Paper 1.21.8.

Q: How much RAM do I need?
A: Minimum 2GB, recommended 4GB (pre-configured).
   For 20+ players, use 6-8GB.

Q: Can I use this with existing plugins?
A: Yes! Walhalla plugins are compatible with most plugins.
   Just add them to the plugins/ folder.

Q: Where are player data files stored?
A: plugins/WalhallaCore/playerdata/
   YAML files, one per player UUID.

Q: Can I change the game balance?
A: Yes! Edit files in plugins/WalhallaCore/canon/
   Use /walhalla reload to apply changes without restart.

Q: What if I see errors on startup?
A: Check console logs. Most common:
   • Missing paper-1.21.8.jar → Download it
   • Wrong Java version → Install Java 21
   • Port already in use → Change server-port in server.properties

Q: How do I make myself admin?
A: In console, type: op YourUsername
   Or edit ops.json file directly.

Q: Can I use a custom world?
A: Yes! Replace world/ folder before starting server.
   Or change level-name in server.properties.


🛠️ TROUBLESHOOTING
════════════════════════════════════════════════════════════════════════════════

Server won't start:
  ✓ Check Java version: java -version (must be 21+)
  ✓ Verify paper-1.21.8.jar exists in this folder
  ✓ Check eula.txt has "eula=true"
  ✓ Ensure port 25565 is not already in use

Plugins show red in /plugins:
  ✓ Check console for errors
  ✓ Verify all 17 Walhalla JARs are in plugins/ folder
  ✓ Restart server completely
  ✓ Check paper-1.21.8.jar version (not 1.21.7 or 1.22)

Performance issues:
  ✓ Increase RAM in start.bat (change -Xmx4G to -Xmx6G)
  ✓ Reduce view-distance in server.properties
  ✓ Lower entity-activation-range in spigot.yml

Can't connect:
  ✓ Check server-ip in server.properties (leave blank for localhost)
  ✓ Verify firewall allows port 25565
  ✓ Use direct IP if on LAN
  ✓ Check online-mode setting if using offline/cracked


📞 ADDITIONAL DOCUMENTATION
════════════════════════════════════════════════════════════════════════════════

In the parent folder (walhalla-mmo/):
  → FULL_FUNCTIONALITY_GUIDE.txt (all game systems explained)
  → PRODUCTION_VERIFICATION_REPORT.txt (technical verification)
  → DEPLOYMENT_MANIFEST.txt (complete file inventory)

In this package:
  → plugins/ folder (all 17 JARs with embedded configs)


✅ VERIFICATION CHECKLIST
════════════════════════════════════════════════════════════════════════════════

Before Starting:
  □ Downloaded paper-1.21.8.jar from papermc.io
  □ Placed paper-1.21.8.jar in this folder
  □ Verified Java 21+ installed (java -version)
  □ Have 2GB+ free disk space
  □ Have 4GB+ free RAM

First Start:
  □ Double-clicked start.bat (Windows) or ran ./start.sh (Linux)
  □ Saw "Done!" message in console
  □ No errors in console
  □ World generated successfully

In-Game Verification:
  □ Connected to server (localhost or your IP)
  □ Ran /plugins command
  □ All 17 Walhalla plugins show green
  □ Ran /walhalla help successfully
  □ Game systems responsive (skills, combat, economy)


🎊 YOU'RE ALL SET!
════════════════════════════════════════════════════════════════════════════════

This is a COMPLETE server package. Once you add paper-1.21.8.jar,
you can start your Walhalla MMO server immediately.

✅ All 17 plugins included
✅ All configuration files pre-configured
✅ All game data embedded (20 canon files)
✅ Start scripts ready
✅ Optimized for performance
✅ 100% functional out of the box

Just download Paper, place it here, and run start.bat!

═══════════════════════════════════════════════════════════════════════════════

Package: WALHALLA_MMO_COMPLETE_SERVER_v1.5.0
Type: Full Server (minus Paper JAR)
Status: ✅ READY TO RUN
Date: January 28, 2026

═══════════════════════════════════════════════════════════════════════════════
