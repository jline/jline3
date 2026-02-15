/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
public class SnakeDemo {

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
        FAST(100),
        EXTREME(50);
        final long delay;

        Difficulty(long d) {
            this.delay = d;
        }
    }

    public enum PowerUpType {
        SPEED_BOOST("\u00bb", "Speed Boost", AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW)),
        DOUBLE_POINTS("\u00a4", "Double Points", AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN)),
        SHRINK("\u25cb", "Shrink Snake", AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.MAGENTA)),
        INVINCIBLE("\u2666", "Invincible", AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));

        final String symbol;
        final String name;
        final AttributedStyle style;

        PowerUpType(String symbol, String name, AttributedStyle style) {
            this.symbol = symbol;
            this.name = name;
            this.style = style;
        }
    }

    private static class PowerUp {
        Point position;
        PowerUpType type;
        int duration;
        int remainingTime;

        PowerUp(Point position, PowerUpType type, int duration) {
            this.position = position;
            this.type = type;
            this.duration = duration;
            this.remainingTime = duration;
        }
    }

    private static final int CRASH_ANIM_FRAMES = 8;

    private enum Operation {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        PAUSE,
        MENU,
        HELP,
        RESET,
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
    public SnakeDemo colorMode(ColorMode cm) {
        this.colorMode = cm;
        return this;
    }

    public SnakeDemo wallsMode(WallsMode wm) {
        this.wallsMode = wm;
        return this;
    }

    public SnakeDemo difficulty(Difficulty d) {
        this.difficulty = d;
        this.gameDelay = d.delay;
        return this;
    }

    public SnakeDemo wrapAround(boolean w) {
        this.wrapAround = w;
        return this;
    }

    private List<Point> walls = new ArrayList<>();

    private final Terminal terminal;
    private final Display display;
    private final Size size = new Size();
    private final Random random = new Random();

    // Persistence
    private Preferences prefs;
    private int highScore = 0;
    private boolean newHighAchieved = false;

    // Effects
    private int eatEffectFrames = 0;
    private Point lastEatPos = new Point(0, 0);
    private int headPulseTick = 0;
    private int crashFrames = 0;
    private Point crashPos = null;

    // UI state
    private boolean helpOverlay = false;

    // Game state
    private List<Point> snake;
    private Direction direction;
    private Point food;

    // Power-ups
    private List<PowerUp> activePowerUps = new ArrayList<>();
    private PowerUp currentPowerUp = null;
    private int powerUpSpawnCounter = 0;
    private boolean doublePointsActive = false;
    private boolean invincibleActive = false;
    private int speedBoostFrames = 0;

    // Achievements
    private int totalPowerUpsCollected = 0;
    private int maxLengthAchieved = 0;
    private boolean speedDemonAchieved = false;

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

    private void beep() {
        try {
            terminal.puts(Capability.bell);
            terminal.flush();
        } catch (Exception ignore) {
            // ignore if terminal does not support bell
        }
    }

    private long currentDelay() {
        long baseDelay = gameDelay;

        // Apply speed boost if active
        if (speedBoostFrames > 0) {
            baseDelay = Math.max(20L, baseDelay / 2);
        }

        // Compensate for terminal cells being taller than wide: horizontal moves look slower.
        // Speed up when moving LEFT/RIGHT to match perceived vertical speed.
        if (direction == Direction.LEFT || direction == Direction.RIGHT) {
            return Math.max(20L, (long) (baseDelay * 0.67));
        }
        return baseDelay;
    }

    private void setCrash(Point p) {
        gameOver = true;
        crashPos = new Point(Math.max(0, Math.min(gameWidth - 1, p.x)), Math.max(0, Math.min(gameHeight - 1, p.y)));
        crashFrames = CRASH_ANIM_FRAMES;
        beep();
        if (score > highScore) {
            highScore = score;
            newHighAchieved = true;
            if (prefs != null) {
                try {
                    prefs.putInt("highScore", highScore);
                } catch (Exception ignore) {
                    // ignore
                }
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

    public SnakeDemo(Terminal terminal) {
        this.terminal = terminal;
        this.display = new Display(terminal, true);
        this.prefs = Preferences.userNodeForPackage(SnakeDemo.class);
        this.highScore = this.prefs.getInt("highScore", 0);
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
        headPulseTick = 0;
        crashFrames = 0;
        crashPos = null;
        newHighAchieved = false;

        // Reset power-up states
        activePowerUps.clear();
        currentPowerUp = null;
        powerUpSpawnCounter = 0;
        doublePointsActive = false;
        invincibleActive = false;
        speedBoostFrames = 0;

        updateGameDimensions();
        buildWalls();
        spawnFood();

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
        } while (snake.contains(food) || walls.contains(food) || isPowerUpAt(food));
    }

    private boolean isPowerUpAt(Point point) {
        return activePowerUps.stream().anyMatch(p -> p.position.equals(point));
    }

    private void spawnPowerUp() {
        if (activePowerUps.size() >= 2) return; // Limit active power-ups

        Point position;
        do {
            position = new Point(random.nextInt(gameWidth), random.nextInt(gameHeight));
        } while (snake.contains(position)
                || walls.contains(position)
                || position.equals(food)
                || isPowerUpAt(position));

        PowerUpType type = PowerUpType.values()[random.nextInt(PowerUpType.values().length)];
        int duration = 300 + random.nextInt(200); // 5-8 seconds at 60fps
        activePowerUps.add(new PowerUp(position, type, duration));
    }

    private void updatePowerUps() {
        // Update power-up spawn counter
        powerUpSpawnCounter++;
        if (powerUpSpawnCounter > 600 && random.nextInt(100) < 5) { // 5% chance every 10 seconds
            spawnPowerUp();
            powerUpSpawnCounter = 0;
        }

        // Update active power-ups
        activePowerUps.removeIf(powerUp -> {
            powerUp.remainingTime--;
            return powerUp.remainingTime <= 0;
        });

        // Update active effects
        if (speedBoostFrames > 0) {
            speedBoostFrames--;
        }

        if (currentPowerUp != null) {
            currentPowerUp.remainingTime--;
            if (currentPowerUp.remainingTime <= 0) {
                deactivatePowerUp(currentPowerUp);
                currentPowerUp = null;
            }
        }
    }

    private void activatePowerUp(PowerUpType type) {
        switch (type) {
            case SPEED_BOOST:
                speedBoostFrames = 300; // 5 seconds
                break;
            case DOUBLE_POINTS:
                doublePointsActive = true;
                currentPowerUp = new PowerUp(null, type, 600); // 10 seconds
                break;
            case SHRINK:
                if (snake.size() > 3) {
                    snake.remove(snake.size() - 1);
                    snake.remove(snake.size() - 1);
                }
                break;
            case INVINCIBLE:
                invincibleActive = true;
                currentPowerUp = new PowerUp(null, type, 300); // 5 seconds
                break;
        }
    }

    private void deactivatePowerUp(PowerUp powerUp) {
        switch (powerUp.type) {
            case DOUBLE_POINTS:
                doublePointsActive = false;
                break;
            case INVINCIBLE:
                invincibleActive = false;
                break;
        }
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

            outer:
            while (running) {
                long lastUpdate = System.currentTimeMillis();
                gameOver = false;

                while (running && !gameOver) {
                    display();

                    if (!paused) {
                        long now = System.currentTimeMillis();
                        long delay = currentDelay();
                        if (now - lastUpdate >= delay) {
                            updateGame();
                            lastUpdate = now;
                        }
                    }

                    // Handle input with timeout
                    long timeToNextUpdate = paused
                            ? Long.MAX_VALUE
                            : Math.max(1, currentDelay() - (System.currentTimeMillis() - lastUpdate));

                    int ch = bindingReader.peekCharacter(timeToNextUpdate);
                    if (ch != NonBlockingReader.READ_EXPIRED && ch != -1) {
                        Operation op = bindingReader.readBinding(keyMap, null, false);
                        handleInput(op);
                    }
                }

                // Crash animation frames (if any)
                if (crashFrames > 0) {
                    long frameDelay = Math.max(50, gameDelay / 2);
                    while (crashFrames > 0) {
                        display();
                        crashFrames--;
                        try {
                            Thread.sleep(frameDelay);
                        } catch (InterruptedException ignore) {
                            // ignore
                        }
                    }
                }

                // Show game over screen and wait for action
                if (gameOver) {
                    boolean again = displayGameOverAndAsk();
                    if (again) {
                        initializeGame();
                        continue outer;
                    } else {
                        running = false;
                        break;
                    }
                } else {
                    // not game over, but loop ended
                    break;
                }
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
        keyMap.bind(Operation.HELP, "h", "H");
        keyMap.bind(Operation.RESET, "r", "R");
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
            case HELP:
                helpOverlay = !helpOverlay;
                break;
            case RESET:
                initializeGame();
                break;
            case QUIT:
                running = false;
                break;
        }
    }

    private void updateGame() {
        if (paused || gameOver) return;

        // Update power-ups
        updatePowerUps();

        // Calculate new head position
        Point head = snake.get(0);
        Point newHead = new Point(head.x + direction.dx, head.y + direction.dy);

        // Wall collision or wrap-around
        if (newHead.x < 0 || newHead.x >= gameWidth || newHead.y < 0 || newHead.y >= gameHeight) {
            if (wrapAround) {
                if (newHead.x < 0) newHead.x = gameWidth - 1;
                if (newHead.x >= gameWidth) newHead.x = 0;
                if (newHead.y < 0) newHead.y = gameHeight - 1;
                if (newHead.y >= gameHeight) newHead.y = 0;
            } else {
                setCrash(newHead);
                return;
            }
        }

        // Check self collision (unless invincible)
        if (!invincibleActive && snake.contains(newHead)) {
            setCrash(newHead);
            return;
        }

        // Check walls collision (unless invincible)
        if (!invincibleActive && walls.contains(newHead)) {
            setCrash(newHead);
            return;
        }

        // Add new head
        snake.add(0, newHead);

        // Check food collision
        boolean ateFood = false;
        if (newHead.equals(food)) {
            int points = doublePointsActive ? 20 : 10;
            score += points;
            eatEffectFrames = 6;
            lastEatPos = new Point(food.x, food.y);
            beep();
            spawnFood();
            ateFood = true;
        }

        // Check power-up collision
        PowerUp collectedPowerUp = null;
        for (PowerUp powerUp : activePowerUps) {
            if (powerUp.position.equals(newHead)) {
                collectedPowerUp = powerUp;
                break;
            }
        }

        if (collectedPowerUp != null) {
            activePowerUps.remove(collectedPowerUp);
            activatePowerUp(collectedPowerUp.type);
            totalPowerUpsCollected++;
            beep();
        }

        if (!ateFood) {
            // Remove tail if no food eaten
            snake.remove(snake.size() - 1);
        }

        // advance head pulse
        headPulseTick = (headPulseTick + 1) % 12;

        // Track achievements
        maxLengthAchieved = Math.max(maxLengthAchieved, snake.size());
        if (difficulty == Difficulty.EXTREME && snake.size() >= 20) {
            speedDemonAchieved = true;
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
                board[w.y][w.x] = '\u2588';
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
        header.append("\ud83d\udc0d JLine Snake");
        header.style(AttributedStyle.DEFAULT);
        header.append(" | Score: ");
        header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
        header.append(String.valueOf(score));
        header.style(AttributedStyle.DEFAULT);
        header.append(" | Length: ");
        header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
        header.append(String.valueOf(snake.size()));
        header.style(AttributedStyle.DEFAULT);
        header.append(" | High: ");
        if (newHighAchieved) {
            header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
        } else {
            header.style(AttributedStyle.DEFAULT.bold());
        }
        header.append(String.valueOf(highScore));
        header.style(AttributedStyle.DEFAULT);

        // Show active power-up effects
        if (speedBoostFrames > 0) {
            header.append(" | ");
            header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
            header.append("\u00bbSPEED");
        }
        if (doublePointsActive) {
            header.append(" | ");
            header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
            header.append("\u00a4x2");
        }
        if (invincibleActive) {
            header.append(" | ");
            header.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
            header.append("\u2666SHIELD");
        }
        header.style(AttributedStyle.DEFAULT);
        // Head glow pulse: apply a faint halo around head
        Point head = snake.isEmpty() ? new Point(0, 0) : snake.get(0);
        int pulseRadius = (headPulseTick / 3) % 2 + 1; // 1..2
        for (int dy = -pulseRadius; dy <= pulseRadius; dy++) {
            for (int dx = -pulseRadius; dx <= pulseRadius; dx++) {
                if (dx == 0 && dy == 0) continue;
                int xx = head.x + dx;
                int yy = head.y + dy;
                if (xx >= 0 && xx < gameWidth && yy >= 0 && yy < gameHeight && board[yy][xx] == ' ') {
                    board[yy][xx] = ','; // halo
                }
            }
        }

        // Crash animation overlay
        if (crashFrames > 0 && crashPos != null) {
            int radius = 1 + (CRASH_ANIM_FRAMES - crashFrames) / 2;
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    int xx = crashPos.x + dx;
                    int yy = crashPos.y + dy;
                    if (xx >= 0 && xx < gameWidth && yy >= 0 && yy < gameHeight) {
                        board[yy][xx] = 'x';
                    }
                }
            }
        }

        // Precompute help overlay box if needed
        boolean showHelp = helpOverlay;
        int boxWidth = Math.max(10, Math.min(gameWidth - 4, 40));
        String[] helpLines = new String[] {
            " How to play ",
            "Move  : Arrow keys or WASD",
            "Pause : P or Space",
            "Reset : R",
            "Help  : H (toggle)",
            "Menu  : M (options)",
            "Quit  : Q or Ctrl+C"
        };
        int boxHeight = helpLines.length + 2;
        int boxY0 = (gameHeight - boxHeight) / 2;
        int boxX0 = (gameWidth - boxWidth) / 2;

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
        topBorder.append("\u250c");
        for (int x = 0; x < gameWidth; x++) {
            topBorder.append("\u2500");
        }
        topBorder.append("\u2510");
        lines.add(topBorder.toAttributedString());

        // Build game area with side borders
        for (int y = 0; y < gameHeight; y++) {
            AttributedStringBuilder line = new AttributedStringBuilder();
            line.append("\u2502");

            for (int x = 0; x < gameWidth; x++) {
                // Help overlay has priority over board content
                if (showHelp && y >= boxY0 && y < boxY0 + boxHeight && x >= boxX0 && x < boxX0 + boxWidth) {
                    int li = y - boxY0;
                    int ci = x - boxX0;
                    if (li == 0) {
                        // top border
                        if (ci == 0) {
                            line.append("\u250c");
                        } else if (ci == boxWidth - 1) {
                            line.append("\u2510");
                        } else {
                            line.append("\u2500");
                        }
                    } else if (li == boxHeight - 1) {
                        // bottom border
                        if (ci == 0) {
                            line.append("\u2514");
                        } else if (ci == boxWidth - 1) {
                            line.append("\u2518");
                        } else {
                            line.append("\u2500");
                        }
                    } else if (ci == 0 || ci == boxWidth - 1) {
                        line.append("\u2502");
                    } else {
                        // inside: render help text centered
                        int textIdx = li - 1;
                        String text = helpLines[textIdx];
                        int avail = boxWidth - 2;
                        int start = Math.max(0, (avail - text.length()) / 2);
                        int pos = ci - 1;
                        if (pos >= start && pos < start + text.length()) {
                            char tch = text.charAt(pos - start);
                            if (textIdx == 0) {
                                line.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
                                line.append(tch);
                                line.style(AttributedStyle.DEFAULT);
                            } else {
                                line.style(AttributedStyle.DEFAULT.faint());
                                line.append(tch);
                                line.style(AttributedStyle.DEFAULT);
                            }
                        } else {
                            line.append(' ');
                        }
                    }
                    continue;
                }

                // Check for power-up at this position (rendered directly to preserve style)
                PowerUp puAt = null;
                for (PowerUp pu : activePowerUps) {
                    if (pu.position.x == x && pu.position.y == y) {
                        puAt = pu;
                        break;
                    }
                }
                if (puAt != null) {
                    line.style(puAt.type.style);
                    line.append(puAt.type.symbol);
                    line.style(AttributedStyle.DEFAULT);
                    continue;
                }

                char ch = board[y][x];
                if (ch == '@') {
                    // Snake head - bright color
                    line.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == ',') {
                    // glow halo
                    line.style(AttributedStyle.DEFAULT.faint().foreground(AttributedStyle.YELLOW));
                    line.append('\u00b7');
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
                } else if (ch == '\u2588') {
                    // Walls
                    line.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
                    line.append(ch);
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == 'x') {
                    // Crash animation
                    line.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
                    line.append('X');
                    line.style(AttributedStyle.DEFAULT);
                } else if (ch == '.') {
                    line.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA));
                    line.append('*');
                    line.style(AttributedStyle.DEFAULT);
                } else {
                    line.append(ch);
                }
            }

            line.append("\u2502");
            lines.add(line.toAttributedString());
        }

        // Build bottom border
        AttributedStringBuilder bottomBorder = new AttributedStringBuilder();
        bottomBorder.append("\u2514");
        for (int x = 0; x < gameWidth; x++) {
            bottomBorder.append("\u2500");
        }
        bottomBorder.append("\u2518");
        lines.add(bottomBorder.toAttributedString());

        // Build footer with controls
        lines.add(new AttributedString(""));
        AttributedStringBuilder footer = new AttributedStringBuilder();
        footer.style(AttributedStyle.DEFAULT.faint());
        footer.append("Controls: Arrow/WASD=Move  P=Pause  H=Help  R=Reset  M=Menu  Q=Quit");
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
                            difficulty = Difficulty.EXTREME;
                            break;
                        case EXTREME:
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

    private boolean displayGameOverAndAsk() {
        // Reuse displayGameOver layout plus a question line
        List<AttributedString> lines = new ArrayList<>();
        int centerY = size.getRows() / 2;
        for (int i = 0; i < centerY - 6; i++) {
            lines.add(new AttributedString(""));
        }
        AttributedStringBuilder gameOverTitle = new AttributedStringBuilder();
        String title = "\ud83d\udc0d GAME OVER \ud83d\udc0d";
        int titlePadding = Math.max(0, (size.getColumns() - title.length()) / 2);
        for (int i = 0; i < titlePadding; i++) gameOverTitle.append(" ");
        gameOverTitle.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
        gameOverTitle.append(title);
        lines.add(gameOverTitle.toAttributedString());
        lines.add(new AttributedString(""));

        AttributedStringBuilder scoreMsg = new AttributedStringBuilder();
        String scoreText = "Final Score: " + score + " | Snake Length: " + snake.size();
        int scorePadding = Math.max(0, (size.getColumns() - scoreText.length()) / 2);
        for (int i = 0; i < scorePadding; i++) scoreMsg.append(" ");
        scoreMsg.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
        scoreMsg.append(scoreText);
        lines.add(scoreMsg.toAttributedString());
        lines.add(new AttributedString(""));

        // Achievements
        AttributedStringBuilder achMsg = new AttributedStringBuilder();
        String achText = "Max Length: " + maxLengthAchieved
                + " | Power-ups: " + totalPowerUpsCollected
                + (speedDemonAchieved ? " | Speed Demon!" : "");
        int achPadding = Math.max(0, (size.getColumns() - achText.length()) / 2);
        for (int i = 0; i < achPadding; i++) achMsg.append(" ");
        achMsg.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
        achMsg.append(achText);
        lines.add(achMsg.toAttributedString());
        lines.add(new AttributedString(""));

        String ask = "Play again? [Y/n]  (R=Reset, Q=Quit)";
        AttributedStringBuilder askMsg = new AttributedStringBuilder();
        int askPadding = Math.max(0, (size.getColumns() - ask.length()) / 2);
        for (int i = 0; i < askPadding; i++) askMsg.append(" ");
        askMsg.style(AttributedStyle.DEFAULT.faint());
        askMsg.append(ask);
        lines.add(askMsg.toAttributedString());
        display.update(lines, -1);

        try {
            NonBlockingReader reader = terminal.reader();
            while (true) {
                int ch = reader.read(0);
                if (ch == -1) break;
                if (ch == 'y' || ch == 'Y' || ch == '\r' || ch == '\n') return true;
                if (ch == 'n' || ch == 'N' || ch == 'q' || ch == 'Q') return false;
                if (ch == 'r' || ch == 'R') return true; // treat reset as play again
            }
        } catch (IOException e) {
            // ignore
        }
        return false;
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

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            new SnakeDemo(terminal).run();
        }
    }
}
