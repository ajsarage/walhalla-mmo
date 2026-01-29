#!/bin/bash
echo ""
echo "========================================"
echo "  WALHALLA MMO SERVER - Starting..."
echo "========================================"
echo ""
echo "Checking Java version..."
java -version
echo ""
echo "Starting Paper 1.21.8 server..."
echo "Allocating 4GB RAM (adjust -Xmx and -Xms if needed)"
echo ""

java -Xmx4G -Xms2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar paper-1.21.8.jar --nogui

echo ""
echo "========================================"
echo "  Server stopped."
echo "========================================"
