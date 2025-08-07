# JLine3 Snake Game

A classic Snake game implementation for JLine3, demonstrating advanced terminal UI capabilities.

## Overview

This implementation provides a fully functional Snake game as requested in [GitHub Issue #1317](https://github.com/jline/jline3/issues/1317). The game is implemented as a single class in the builtins subproject and uses JLine3's `Display` class for fullscreen rendering.

## Features

### Game Features
- **Classic Snake Gameplay**: Control a growing snake to collect food
- **Score Tracking**: Points awarded for each food item collected
- **Collision Detection**: Game ends on wall collision or self-collision
- **Pause/Resume**: Pause and resume gameplay at any time
- **Dynamic Food Spawning**: Food appears randomly on the game board
- **Growing Snake**: Snake grows longer with each food item consumed

### Technical Features
- **Fullscreen Display**: Uses JLine3's `Display` class for efficient screen updates
- **Responsive UI**: Adapts to terminal resize events
- **Keyboard Input**: Advanced input handling with `BindingReader` and `KeyMap`
- **Colored Output**: Beautiful visual presentation using `AttributedString`
- **Terminal Capabilities**: Proper fullscreen mode with cursor management
- **Signal Handling**: Graceful handling of window resize and interrupt signals

## Controls

| Key | Action |
|-----|--------|
| Arrow Keys | Move snake (Up, Down, Left, Right) |
| W, A, S, D | Alternative movement controls |
| P, Space | Pause/Resume game |
| Q, Ctrl+C | Quit game |

## Implementation Details

### Architecture
- **Single Class Design**: Implemented as `org.jline.builtins.Snake`
- **Command Integration**: Integrated into `Commands.java` as the `snake` command
- **Display-Based Rendering**: Uses JLine3's `Display` class for efficient updates
- **Event-Driven Input**: Non-blocking input handling with timeout support

### Key Components
1. **Game State Management**: Snake position, direction, food location, score
2. **Input Processing**: Keyboard event handling with direction validation
3. **Game Logic**: Movement, collision detection, food consumption
4. **Rendering Engine**: Efficient screen updates with colored output
5. **Signal Handling**: Window resize and interrupt signal management

### Visual Design
- **Bordered Game Area**: Clean visual boundaries with Unicode box characters
- **Colored Elements**: 
  - Snake head: Bold green (`@`)
  - Snake body: Green (`#`)
  - Food: Bold red (`*`)
- **Status Display**: Score, snake length, and pause status
- **Game Over Screen**: Centered final score display

## Usage

### As a Command
```bash
# Run the snake game
snake

# Show help
snake --help
```

### In JLine3 Applications
```java
import org.jline.builtins.Snake;
import org.jline.terminal.Terminal;

// Create and run the game
Snake game = new Snake(terminal);
game.run();
```

### Demo Script
A demo script is available at `demo/src/main/scripts/snake-demo.jline` that provides an interactive introduction to the game.

## Building and Testing

### Prerequisites
- JDK 22 or higher
- Maven 4.0.0-rc-4 or higher

### Build Commands
```bash
# Full build
mvn install

# Build with formatting
mvn spotless:apply
mvn install

# Build specific module
mvn install -pl builtins
```

## Technical Implementation

### Game Loop
The game uses a timed update loop with non-blocking input:
1. **Display Update**: Render current game state
2. **Input Processing**: Handle keyboard events with timeout
3. **Game Logic**: Update snake position and check collisions
4. **State Management**: Update score, food position, and game status

### Display Rendering
Efficient rendering using JLine3's `Display` class:
- **Differential Updates**: Only changed content is redrawn
- **Attributed Strings**: Colored and styled text output
- **Terminal Capabilities**: Proper fullscreen mode management

### Input Handling
Advanced keyboard input processing:
- **Key Mapping**: Configurable key bindings for all controls
- **Direction Validation**: Prevents invalid moves (e.g., reversing direction)
- **Non-blocking**: Game continues running while waiting for input

## Code Quality

- **Formatted Code**: Follows project's Spotless formatting rules
- **Documentation**: Comprehensive JavaDoc comments
- **Error Handling**: Graceful handling of terminal and I/O errors
- **Signal Safety**: Proper cleanup on interruption or window resize

## Future Enhancements

Potential improvements for future versions:
- **High Score Persistence**: Save and display high scores
- **Difficulty Levels**: Adjustable game speed
- **Sound Effects**: Terminal bell for events
- **Multiplayer Mode**: Two-player snake game
- **Custom Themes**: Configurable colors and characters

## Contributing

This implementation demonstrates JLine3's capabilities for creating interactive terminal applications. It serves as both a fun game and a reference implementation for:
- Fullscreen terminal applications
- Real-time input handling
- Efficient display updates
- Signal handling
- Terminal capability management

The code is designed to be educational and can serve as a starting point for other terminal-based games or interactive applications using JLine3.
