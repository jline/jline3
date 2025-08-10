/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

/**
 * A terminal-based Snake game implementation.
 * <p>
 * This class provides a classic Snake game experience in the terminal, featuring:
 * </p>
 * <ul>
 *   <li>Arrow key controls for snake movement</li>
 *   <li>Food collection and score tracking</li>
 *   <li>Collision detection (walls and self)</li>
 *   <li>Pause/resume functionality</li>
 *   <li>Responsive terminal UI with borders and status</li>
 * </ul>
 * <p>
 * Controls:
 * </p>
 * <ul>
 *   <li>Arrow keys - Change snake direction</li>
 *   <li>WASD keys - Alternative movement controls</li>
 *   <li>P - Pause/Resume game</li>
 *   <li>Q - Quit game</li>
 * </ul>
 */
public class Snake {

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);
        final int dx, dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        Direction opposite() {
            switch (this) {
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                case LEFT:
                    return RIGHT;
                case RIGHT:
                    return LEFT;
                default:
                    return this;
            }
        }
    }

    public enum ColorMode {
        CLASSIC,
        RAINBOW
    }

    public enum WallsMode {
        NONE,
        BORDER,
        RANDOM
    }

    public enum Difficulty {
        SLOW(200),
        NORMAL(150),
        FAST(100);
        final long delay;

        Difficulty(long d) {
            this.delay = d;
        }
    }

    private enum Operation {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        PAUSE,
        MENU,
        QUIT,
        NONE
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Point)) return false;
            Point p = (Point) obj;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return x * 31 + y;
        }
    }

    // Config
    private ColorMode colorMode = ColorMode.RAINBOW;
    private WallsMode wallsMode = WallsMode.BORDER;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean wrapAround = false;

    // Walls
    /**
     * Optional start configuration setters
     */
    public Snake colorMode(ColorMode cm) {
        this.colorMode = cm;
        return this;
    }

    public Snake wallsMode(WallsMode wm) {
        this.wallsMode = wm;
        return this;
    }

    public Snake difficulty(Difficulty d) {
        this.difficulty = d;
        this.gameDelay = d.delay;
        return this;
    }

    public Snake wrapAround(boolean w) {
        this.wrapAround = w;
        return this;
    }

    private List<Point> walls = new ArrayList<>();

    private final Terminal terminal;
    private final Display display;
    private final Size size = new Size();
    private final Random random = new Random();

    // Effects
    private int eatEffectFrames = 0;
    private Point lastEatPos = new Point(0, 0);

    // Game state
    private List<Point> snake;
    private Direction direction;
    private Point food;

    private void buildWalls() {
        walls.clear();
        if (wallsMode == WallsMode.NONE) return;
        if (wallsMode == WallsMode.BORDER) {
            for (int x = 0; x < gameWidth; x++) {
                walls.add(new Point(x, 0));
                walls.add(new Point(x, gameHeight - 1));
            }
            for (int y = 0; y < gameHeight; y++) {
                walls.add(new Point(0, y));
                walls.add(new Point(gameWidth - 1, y));
            }
        } else if (wallsMode == WallsMode.RANDOM) {
            int count = Math.max(5, (gameWidth * gameHeight) / 40);
            for (int i = 0; i < count; i++) {
                walls.add(new Point(random.nextInt(gameWidth), random.nextInt(gameHeight)));
            }
        }
    }

    private int score;
    private boolean gameOver;

    private AttributedStyle tailStyle(int idx) {
        if (colorMode == ColorMode.RAINBOW) {
            int base = (idx * 20) % 360;
            int r = (int) (Math.max(0, Math.min(255, Math.abs(((base + 0) % 360) - 180) - 60)) * 4.25);
            int g = (int) (Math.max(0, Math.min(255, Math.abs(((base + 120) % 360) - 180) - 60)) * 4.25);
            int b = (int) (Math.max(0, Math.min(255, Math.abs(((base + 240) % 360) - 180) - 60)) * 4.25);
            return AttributedStyle.DEFAULT.foreground(r, g, b);
        } else {
            return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
        }
    }

    private boolean paused;
    private boolean running;

    // Game area dimensions (excluding borders)
    // Timing
    private long gameDelay = Difficulty.NORMAL.delay;

    private int gameWidth;
    private int gameHeight;
    private int gameStartX = 2; // Leave space for border
    private int gameStartY = 3; // Leave space for border and header

    // Removed fixed delay; using configurable difficulty instead

    public Snake(Terminal terminal) {
        this.terminal = terminal;
        this.display = new Display(terminal, true);
        initializeGame();
    }

    private void initializeGame() {
        snake = new ArrayList<>();
        snake.add(new Point(10, 10)); // Start position
        snake.add(new Point(9, 10));
        snake.add(new Point(8, 10));

        direction = Direction.RIGHT;
        score = 0;
        gameOver = false;
        paused = false;
        running = true;

        updateGameDimensions();
        buildWalls();
        spawnFood();
        // center start
        // Color helpers for tails
        java.util.function.IntFunction<AttributedStyle> tailColor = idx -> {
            if (colorMode == ColorMode.RAINBOW) {
                int base = (idx * 20) % 360;
                int r = (int) (Math.max(0, Math.min(255, Math.abs(((base + 0) % 360) - 180) - 60)) * 4.25);
                int g = (int) (Math.max(0, Math.min(255, Math.abs(((base + 120) % 360) - 180) - 60)) * 4.25);
                int b = (int) (Math.max(0, Math.min(255, Math.abs(((base + 240) % 360) - 180) - 60)) * 4.25);
                return AttributedStyle.DEFAULT.foreground(r, g, b);
            } else {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
            }
        };

        Point head = snake.get(0);
        head.x = Math.max(1, gameWidth / 2);
        head.y = Math.max(1, gameHeight / 2);
        // adjust rest
        for (int i = 1; i < snake.size(); i++) {
            snake.get(i).x = head.x - i;
            snake.get(i).y = head.y;
        }
    }

    private void updateGameDimensions() {
        size.copy(terminal.getSize());
        gameWidth = size.getColumns() - 4; // Account for borders
        gameHeight = size.getRows() - 5; // Account for borders and header/footer

        // Ensure minimum game area
        if (gameWidth < 20 || gameHeight < 10) {
            gameWidth = Math.max(gameWidth, 20);
            gameHeight = Math.max(gameHeight, 10);
        }
    }

    private void spawnFood() {
        do {
            food = new Point(random.nextInt(gameWidth), random.nextInt(gameHeight));
        } while (snake.contains(food) || walls.contains(food));
    }

    public void run() throws IOException {
        Terminal.SignalHandler prevWinchHandler = terminal.handle(Terminal.Signal.WINCH, this::handleResize);
        Terminal.SignalHandler prevIntHandler = terminal.handle(Terminal.Signal.INT, this::handleInterrupt);
        Attributes attr = terminal.enterRawMode();

        try {
            // Enter fullscreen mode
            terminal.puts(Capability.enter_ca_mode);
            terminal.puts(Capability.keypad_xmit);
            terminal.puts(Capability.cursor_invisible);
            terminal.flush();

            // Setup key bindings
            KeyMap<Operation> keyMap = createKeyMap();
            BindingReader bindingReader = new BindingReader(terminal.reader());

            long lastUpdate = System.currentTimeMillis();

            while (running && !gameOver) {
                display();

                if (!paused) {
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate >= gameDelay) {
                        updateGame();
                        lastUpdate = now;
                    }
                }

                // Handle input with timeout
                long timeToNextUpdate =
                        paused ? Long.MAX_VALUE : Math.max(1, gameDelay - (System.currentTimeMillis() - lastUpdate));

                int ch = bindingReader.peekCharacter(timeToNextUpdate);
                if (ch != NonBlockingReader.READ_EXPIRED && ch != -1) {
                    Operation op = bindingReader.readBinding(keyMap, null, false);
                    handleInput(op);
                }
            }

            // Show game over screen
            if (gameOver) {
                displayGameOver();
                // Wait for any key to exit
                bindingReader.readBinding(keyMap);
            }

        } finally {
            terminal.setAttributes(attr);
            if (prevWinchHandler != null) {
                terminal.handle(Terminal.Signal.WINCH, prevWinchHandler);
            }
            if (prevIntHandler != null) {
                terminal.handle(Terminal.Signal.INT, prevIntHandler);
            }

            // Exit fullscreen mode
            terminal.puts(Capability.exit_ca_mode);
            terminal.puts(Capability.keypad_local);
            terminal.puts(Capability.cursor_visible);
            terminal.writer().flush();
        }
    }

    private KeyMap<Operation> createKeyMap() {
        KeyMap<Operation> keyMap = new KeyMap<>();

        // Arrow keys
        keyMap.bind(Operation.UP, KeyMap.key(terminal, Capability.key_up));
        keyMap.bind(Operation.DOWN, KeyMap.key(terminal, Capability.key_down));
        keyMap.bind(Operation.LEFT, KeyMap.key(terminal, Capability.key_left));
        keyMap.bind(Operation.RIGHT, KeyMap.key(terminal, Capability.key_right));

        // WASD keys
        keyMap.bind(Operation.UP, "w", "W");
        keyMap.bind(Operation.DOWN, "s", "S");
        keyMap.bind(Operation.LEFT, "a", "A");
        keyMap.bind(Operation.RIGHT, "d", "D");

        // Control keys
        keyMap.bind(Operation.PAUSE, "p", "P", " ");
        keyMap.bind(Operation.MENU, "m", "M", "o", "O");
        keyMap.bind(Operation.QUIT, "q", "Q", KeyMap.ctrl('C'));

        // Default to NONE for unbound keys
        keyMap.setUnicode(Operation.NONE);
        keyMap.setNomatch(Operation.NONE);

        return keyMap;
    }

    private void handleInput(Operation op) {
        if (op == null) return;

        switch (op) {
            case UP:
                if (direction != Direction.DOWN) direction = Direction.UP;
                break;
            case DOWN:
                if (direction != Direction.UP) direction = Direction.DOWN;
                break;
            case LEFT:
                if (direction != Direction.RIGHT) direction = Direction.LEFT;
                break;
            case RIGHT:
                if (direction != Direction.LEFT) direction = Direction.RIGHT;
                break;
            case PAUSE:
                paused = !paused;
                break;
            case MENU:
                showMenu();
                break;
            case QUIT:
                running = false;
                break;
        }
    }

    private void updateGame() {
        if (paused || gameOver) return;

        // Calculate new head position
        Point head = snake.get(0);
        // Color helpers for tails
        java.util.function.IntFunction<AttributedStyle> tailColor = idx -> {
            if (colorMode == ColorMode.RAINBOW) {
                int base = (idx * 20) % 360;
                int r = (int) (Math.max(0, Math.min(255, Math.abs(((base + 0) % 360) - 180) - 60)) * 4.25);
                int g = (int) (Math.max(0, Math.min(255, Math.abs(((base + 120) % 360) - 180) - 60)) * 4.25);
                int b = (int) (Math.max(0, Math.min(255, Math.abs(((base + 240) % 360) - 180) - 60)) * 4.25);
                return AttributedStyle.DEFAULT.foreground(r, g, b);
            } else {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
            }
        };

        Point newHead = new Point(head.x + direction.dx, head.y + direction.dy);

        // Wall collision or wrap-around
        if (newHead.x < 0 || newHead.x >= gameWidth || newHead.y < 0 || newHead.y >= gameHeight) {
            if (wrapAround) {
                if (newHead.x < 0) newHead.x = gameWidth - 1;
                if (newHead.x >= gameWidth) newHead.x = 0;
                if (newHead.y < 0) newHead.y = gameHeight - 1;
                if (newHead.y >= gameHeight) newHead.y = 0;
            } else {
                gameOver = true;
                return;
            }
        }

        // Check self collision
        if (snake.contains(newHead)) {
            gameOver = true;
            return;
        }

        // Check walls collision
        if (walls.contains(newHead)) {
            gameOver = true;
            return;
        }

        // Add new head
        snake.add(0, newHead);

        // Check food collision
        if (newHead.equals(food)) {
            score += 10;
            eatEffectFrames = 6;
            lastEatPos = new Point(food.x, food.y);
            spawnFood();
        } else {
            // Remove tail if no food eaten
            snake.remove(snake.size() - 1);
        }
    }

    private void display() {
        display.resize(size.getRows(), size.getColumns());

        List<AttributedString> lines = new ArrayList<>();

        // Create game board
        char[][] board = new char[gameHeight][gameWidth];
        for (int y = 0; y < gameHeight; y++) {
            for (int x = 0; x < gameWidth; x++) {
                board[y][x] = ' ';
            }
        }

        // Place walls
        for (Point w : walls) {
            if (w.x >= 0 && w.x < gameWidth && w.y >= 0 && w.y < gameHeight) {
                board[w.y][w.x] = '█';
            }
        }

        // Place snake
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            if (p.x >= 0 && p.x < gameWidth && p.y >= 0 && p.y < gameHeight) {
                board[p.y][p.x] = i == 0 ? '@' : '#'; // Head is @, body is #
            }
        }

        // Place food
        if (food.x >= 0 && food.x < gameWidth && food.y >= 0 && food.y < gameHeight) {
            board[food.y][food.x] = '*';
        }

        // Build header
        AttributedStringBuilder header = new AttributedStringBuilder();
        // Eating effect: highlight around the eaten pill
        if (eatEffectFrames > 0) {
            int radius = 1 + (6 - eatEffectFrames) / 2;
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int xx = lastEatPos.x + dx;
                    int yy = lastEatPos.y + dy;
                    if (xx >= 0 && xx < gameWidth && yy >= 0 && yy < gameHeight) {
                        if (board[yy][xx] == ' ') board[yy][xx] = '.'; // sparkle
                    }
                }
            }
            eatEffectFrames--;
        }

        header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
        header.append("🐍 JLine Snake Game");
        header.style(AttributedStyle.DEFAULT);
        header.append(" | Score: ");
        header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
        header.append(String.valueOf(score));
        header.style(AttributedStyle.DEFAULT);
        header.append(" | Length: ");
        header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
        header.append(String.valueOf(snake.size()));

        if (paused) {
            header.style(AttributedStyle.DEFAULT);
            header.append(" | ");
            header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
            header.append("PAUSED");
        }

        lines.add(header.toAttributedString());
        lines.add(new AttributedString(""));

        // Build top border
        AttributedStringBuilder topBorder = new AttributedStringBuilder();
        topBorder.append("┌");
        for (int x = 0; x < gameWidth; x++) {
            topBorder.append("─");
        }
        topBorder.append("┐");
        lines.add(topBorder.toAttributedString());

        // Build game area with side borders
        for (int y = 0; y < gameHeight; y++) {
            AttributedStringBuilder line = new AttributedStringBuilder();
            line.append("│");

            for (int x = 0; x < gameWidth; x++) {
                char ch = board[y][x];
                if (ch == '@') {
                    // Snake head - bright color
                    line.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == '#') {
                    // Snake body - gradient/rainbow
                    int idxFromHead = 0;
                    // find index from head by scanning snake list
                    for (int si = 1; si < snake.size(); si++) {
                        if (snake.get(si).x == x && snake.get(si).y == y) {
                            idxFromHead = si;
                            break;
                        }
                    }
                    line.style(tailStyle(idxFromHead));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == '*') {
                    // Food - red blinking-ish (bold)
                    line.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == '█') {
                    // Walls
                    line.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == '.') {
                    line.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA));
                    line.append('*');
                    line.style(AttributedStyle.DEFAULT);
                } else {
                    line.append(ch);
                }
            }

            line.append("│");
            lines.add(line.toAttributedString());
        }

        // Build bottom border
        AttributedStringBuilder bottomBorder = new AttributedStringBuilder();
        bottomBorder.append("└");
        for (int x = 0; x < gameWidth; x++) {
            bottomBorder.append("─");
        }
        bottomBorder.append("┘");
        lines.add(bottomBorder.toAttributedString());

        // Build footer with controls
        lines.add(new AttributedString(""));
        AttributedStringBuilder footer = new AttributedStringBuilder();
        footer.style(AttributedStyle.DEFAULT.faint());
        footer.append("Controls: Arrow keys/WASD=Move, P=Pause, Q=Quit");
        lines.add(footer.toAttributedString());

        display.update(lines, -1);
    }

    private void showMenu() {
        List<AttributedString> lines = new ArrayList<>();
        lines.add(new AttributedString(""));
        AttributedStringBuilder title = new AttributedStringBuilder();
        title.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
        title.append("Snake Options");
        lines.add(title.toAttributedString());
        lines.add(new AttributedString(""));

        lines.add(new AttributedString("  1) Color Mode: " + colorMode));
        lines.add(new AttributedString("  2) Walls Mode:  " + wallsMode));
        lines.add(new AttributedString("  3) Difficulty:  " + difficulty));
        lines.add(new AttributedString("  4) Wrap Around: " + (wrapAround ? "ON" : "OFF")));
        lines.add(new AttributedString(""));
        lines.add(new AttributedString("  Use keys 1-4 to toggle options, ESC to exit menu"));

        display.update(lines, 0);

        // Simple key handling for options
        try {
            terminal.handle(Terminal.Signal.WINCH, null);
            terminal.enterRawMode();
            NonBlockingReader reader = terminal.reader();
            int ch;
            while ((ch = reader.read(1000)) != -1) {
                if (ch == 27) { // ESC
                    break;
                } else if (ch == '1') {
                    colorMode = (colorMode == ColorMode.CLASSIC) ? ColorMode.RAINBOW : ColorMode.CLASSIC;
                } else if (ch == '2') {
                    switch (wallsMode) {
                        case NONE:
                            wallsMode = WallsMode.BORDER;
                            break;
                        case BORDER:
                            wallsMode = WallsMode.RANDOM;
                            break;
                        case RANDOM:
                            wallsMode = WallsMode.NONE;
                            break;
                    }
                    buildWalls();
                } else if (ch == '3') {
                    switch (difficulty) {
                        case SLOW:
                            difficulty = Difficulty.NORMAL;
                            break;
                        case NORMAL:
                            difficulty = Difficulty.FAST;
                            break;
                        case FAST:
                            difficulty = Difficulty.SLOW;
                            break;
                    }
                    gameDelay = difficulty.delay;
                } else if (ch == '4') {
                    wrapAround = !wrapAround;
                }
                // Refresh menu with updated options
                showMenu();
                return;
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private void displayGameOver() {
        List<AttributedString> lines = new ArrayList<>();

        // Center the game over message
        int centerY = size.getRows() / 2;
        int centerX = size.getColumns() / 2;

        // Add empty lines to center vertically
        for (int i = 0; i < centerY - 3; i++) {
            lines.add(new AttributedString(""));
        }

        // Game Over title
        AttributedStringBuilder gameOverTitle = new AttributedStringBuilder();
        String title = "🐍 GAME OVER 🐍";
        int titlePadding = Math.max(0, (size.getColumns() - title.length()) / 2);
        for (int i = 0; i < titlePadding; i++) {
            gameOverTitle.append(" ");
        }
        gameOverTitle.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
        gameOverTitle.append(title);
        lines.add(gameOverTitle.toAttributedString());

        lines.add(new AttributedString(""));

        // Final score
        AttributedStringBuilder scoreMsg = new AttributedStringBuilder();
        String scoreText = "Final Score: " + score + " | Snake Length: " + snake.size();
        int scorePadding = Math.max(0, (size.getColumns() - scoreText.length()) / 2);
        for (int i = 0; i < scorePadding; i++) {
            scoreMsg.append(" ");
        }
        scoreMsg.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
        scoreMsg.append(scoreText);
        lines.add(scoreMsg.toAttributedString());

        lines.add(new AttributedString(""));

        // Instructions
        AttributedStringBuilder instructions = new AttributedStringBuilder();
        String instructText = "Press any key to exit...";
        int instructPadding = Math.max(0, (size.getColumns() - instructText.length()) / 2);
        for (int i = 0; i < instructPadding; i++) {
            instructions.append(" ");
        }
        instructions.style(AttributedStyle.DEFAULT.faint());
        instructions.append(instructText);
        lines.add(instructions.toAttributedString());

        display.update(lines, -1);
    }

    private void handleResize(Terminal.Signal signal) {
        updateGameDimensions();
        // Ensure snake and food are still within bounds
        snake.removeIf(p -> p.x >= gameWidth || p.y >= gameHeight);
        if (snake.isEmpty()) {
            gameOver = true;
            return;
        }
        if (food.x >= gameWidth || food.y >= gameHeight) {
            spawnFood();
        }
        buildWalls();
    }

    private void handleInterrupt(Terminal.Signal signal) {
        running = false;
    }

    /**
     * Main entry point for the Snake game.
     *
     * @param terminal the terminal to use for the game
     * @throws IOException if an I/O error occurs
     */
    public static void main(Terminal terminal) throws IOException {
        new Snake(terminal).run();
    }
}
