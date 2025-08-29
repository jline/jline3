#!/bin/bash

# Snake Games Launcher for JLine3
# This script runs the various Snake game implementations

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JLINE_JAR="../jline/target/jline-4.0.0-SNAPSHOT.jar"
CLASSPATH="$JLINE_JAR:src/main/java"

echo "🐍 JLine3 Snake Games Collection 🐍"
echo "===================================="
echo ""
echo "Available games:"
echo "1. Simple Snake Game (ASCII characters)"
echo "2. Unicode Snake Game (with emojis)"
echo "3. Box Drawing Snake Game (Unicode box characters)"
echo ""
echo -n "Choose a game (1-3) or press Enter for Simple Snake: "
read choice

case $choice in
    1|"")
        echo "Starting Simple Snake Game..."
        echo "Controls: WASD to move, Q to quit"
        echo ""
        java -cp "$CLASSPATH" org.jline.demo.games.SimpleSnakeGame
        ;;
    2)
        echo "Starting Unicode Snake Game..."
        echo "Controls: WASD or arrow keys to move, Q to quit"
        echo ""
        java -cp "$CLASSPATH" org.jline.demo.games.UnicodeSnakeGame
        ;;
    3)
        echo "Starting Box Drawing Snake Game..."
        echo "Controls: WASD to move, P to pause, Q to quit"
        echo ""
        java -cp "$CLASSPATH" org.jline.demo.games.BoxDrawingSnakeGame
        ;;
    *)
        echo "Invalid choice. Starting Simple Snake Game..."
        java -cp "$CLASSPATH" org.jline.demo.games.SimpleSnakeGame
        ;;
esac

echo ""
echo "Thanks for playing! 🎮"
