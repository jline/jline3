# 🐍 JLine3 Snake Games Collection

A collection of Snake games demonstrating JLine3's terminal capabilities using Unicode characters instead of VT-specific fonts for maximum compatibility.

## Games Included

### 1. SimpleSnakeGame
- **Graphics**: Basic ASCII characters (`#`, `O`, `o`, `*`)
- **Compatibility**: Works on all terminals
- **Features**: Basic Snake gameplay with colored output
- **Controls**: WASD keys, Q to quit

### 2. UnicodeSnakeGame  
- **Graphics**: Unicode emojis and symbols (🐍, 🍎, ╔═╗)
- **Compatibility**: Requires Unicode support
- **Features**: Enhanced visual appeal with emojis
- **Controls**: WASD or arrow keys, Q to quit

### 3. BoxDrawingSnakeGame
- **Graphics**: Unicode box-drawing characters (┌─┐│└┘)
- **Compatibility**: Widely supported Unicode characters
- **Features**: Retro terminal aesthetic, pause functionality
- **Controls**: WASD keys, P to pause, Q to quit

## How to Run

### Quick Start
```bash
cd demo
./run-snake-games.sh
```

### Individual Games
```bash
# Compile first (if not already done)
javac -cp "../jline/target/jline-4.0.0-SNAPSHOT.jar" src/main/java/org/jline/demo/games/*.java

# Run specific games
java -cp "../jline/target/jline-4.0.0-SNAPSHOT.jar:src/main/java" org.jline.demo.games.SimpleSnakeGame
java -cp "../jline/target/jline-4.0.0-SNAPSHOT.jar:src/main/java" org.jline.demo.games.UnicodeSnakeGame
java -cp "../jline/target/jline-4.0.0-SNAPSHOT.jar:src/main/java" org.jline.demo.games.BoxDrawingSnakeGame
```

## Game Features

### Core Gameplay
- **Snake Movement**: Continuous movement in the chosen direction
- **Food Collection**: Eat food to grow and increase score
- **Collision Detection**: Game ends when hitting walls or self
- **Score Tracking**: Points awarded for each food item consumed

### JLine3 Features Demonstrated
- **Terminal Control**: Raw mode input, cursor manipulation
- **Non-blocking Input**: Real-time key detection without blocking game loop
- **ANSI Colors**: Colored output for enhanced visual experience
- **Screen Management**: Clear screen, cursor positioning
- **Cross-platform**: Works on Windows, macOS, and Linux terminals

### Visual Elements
- **Colored Borders**: Blue walls/borders
- **Snake Representation**: Green head and body with different characters
- **Food Display**: Red food items
- **Score Display**: Yellow score and statistics
- **Game Over Screen**: Formatted end-game information

## Technical Implementation

### Architecture
- **Game Loop**: Fixed timestep with configurable speed
- **Input Handling**: Non-blocking keyboard input processing
- **Rendering**: Efficient screen updates using terminal capabilities
- **State Management**: Clean separation of game logic and display

### JLine3 APIs Used
- `Terminal` and `TerminalBuilder` for terminal access
- `NonBlockingReader` for real-time input
- `InfoCmp.Capability` for terminal control sequences
- `Attributes` for terminal mode management

### Unicode Compatibility
- **Fallback Strategy**: ASCII version for maximum compatibility
- **Progressive Enhancement**: Unicode versions for better visuals
- **Wide Character Support**: Proper handling of multi-byte characters

## Controls

| Key | Action |
|-----|--------|
| W/↑ | Move Up |
| S/↓ | Move Down |
| A/← | Move Left |
| D/→ | Move Right |
| P   | Pause (BoxDrawing version only) |
| Q   | Quit Game |
| ESC | Quit Game |

## Requirements

- **Java**: JDK 21 or higher
- **JLine3**: Version 4.0.0-SNAPSHOT or compatible
- **Terminal**: Any terminal with ANSI color support
- **Unicode**: UTF-8 support recommended for Unicode versions

## Inspiration

These games were created as a modern alternative to VT-specific terminal games, demonstrating how Unicode characters can provide rich visual experiences without relying on terminal-specific font features. The implementation showcases JLine3's powerful terminal manipulation capabilities while maintaining broad compatibility across different terminal environments.

## Future Enhancements

Potential improvements could include:
- **High Score Persistence**: Save and load high scores
- **Difficulty Levels**: Adjustable game speed
- **Sound Effects**: Terminal bell integration
- **Multiplayer**: Two-player snake game
- **Custom Themes**: User-selectable color schemes
- **Sixel Graphics**: Integration with JLine3's Sixel support for terminals that support it
