/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.games;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

/**
 * A Unicode-based Snake game using JLine3 terminal capabilities.
 * Uses Unicode characters instead of VT-specific fonts for maximum compatibility.
 */
public class UnicodeSnakeGame {

    // Game constants
    private static final int GAME_WIDTH = 40;
    private static final int GAME_HEIGHT = 20;
    private static final int GAME_SPEED = 150; // milliseconds between moves

    // Unicode characters for game elements
    private static final String WALL_HORIZONTAL = "═";
    private static final String WALL_VERTICAL = "║";
    private static final String WALL_TOP_LEFT = "╔";
    private static final String WALL_TOP_RIGHT = "╗";
    private static final String WALL_BOTTOM_LEFT = "╚";
    private static final String WALL_BOTTOM_RIGHT = "╝";
    private static final String SNAKE_HEAD = "🐍";
    private static final String SNAKE_BODY = "●";
    private static final String FOOD = "🍎";
    private static final String EMPTY = " ";

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    // Game state
    private Terminal terminal;
    private NonBlockingReader reader;
    private List<Point> snake;
    private Point food;
    private Direction direction;
    private boolean gameRunning;
    private int score;
    private Random random;

    // Direction enum
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
    }

    // Point class for coordinates
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

        Point move(Direction dir) {
            return new Point(x + dir.dx, y + dir.dy);
        }
    }

    public UnicodeSnakeGame() throws IOException {
        terminal = TerminalBuilder.builder().system(true).build();
        reader = terminal.reader();
        snake = new ArrayList<>();
        random = new Random();

        // Initialize snake in the center
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        snake.add(new Point(GAME_WIDTH / 2 - 1, GAME_HEIGHT / 2));
        snake.add(new Point(GAME_WIDTH / 2 - 2, GAME_HEIGHT / 2));

        direction = Direction.RIGHT;
        gameRunning = true;
        score = 0;

        spawnFood();
    }

    public void run() throws IOException, InterruptedException {
        try {
            // Enter raw mode for immediate key response
            Attributes originalAttributes = terminal.getAttributes();
            terminal.enterRawMode();

            // Hide cursor
            terminal.puts(Capability.cursor_invisible);

            // Clear screen
            terminal.puts(Capability.clear_screen);

            drawInstructions();

            long lastMoveTime = System.currentTimeMillis();

            while (gameRunning) {
                // Handle input
                handleInput();

                // Move snake at regular intervals
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime >= GAME_SPEED) {
                    moveSnake();
                    lastMoveTime = currentTime;
                }

                // Draw game
                drawGame();

                // Small delay to prevent excessive CPU usage
                TimeUnit.MILLISECONDS.sleep(10);
            }

            // Game over
            drawGameOver();

            // Wait for any key to exit
            terminal.writer().println("\n" + WHITE + "Press any key to exit..." + RESET);
            terminal.flush();
            reader.read();

            // Restore terminal
            terminal.setAttributes(originalAttributes);
            terminal.puts(Capability.cursor_visible);

        } finally {
            terminal.close();
        }
    }

    private void handleInput() throws IOException {
        if (reader.available() > 0) {
            int ch = reader.read();

            switch (ch) {
                case 'w':
                case 'W':
                case 65: // Up arrow
                    if (direction != Direction.DOWN) direction = Direction.UP;
                    break;
                case 's':
                case 'S':
                case 66: // Down arrow
                    if (direction != Direction.UP) direction = Direction.DOWN;
                    break;
                case 'a':
                case 68: // Left arrow
                    if (direction != Direction.RIGHT) direction = Direction.LEFT;
                    break;
                case 'd':
                case 67: // Right arrow
                    if (direction != Direction.LEFT) direction = Direction.RIGHT;
                    break;
                case 'q':
                case 'Q':
                case 27: // ESC
                    gameRunning = false;
                    break;
            }
        }
    }

    private void moveSnake() {
        Point head = snake.get(0);
        Point newHead = head.move(direction);

        // Check wall collision
        if (newHead.x < 1 || newHead.x >= GAME_WIDTH - 1 || newHead.y < 1 || newHead.y >= GAME_HEIGHT - 1) {
            gameRunning = false;
            return;
        }

        // Check self collision
        if (snake.contains(newHead)) {
            gameRunning = false;
            return;
        }

        snake.add(0, newHead);

        // Check food collision
        if (newHead.equals(food)) {
            score += 10;
            spawnFood();
        } else {
            // Remove tail if no food eaten
            snake.remove(snake.size() - 1);
        }
    }

    private void spawnFood() {
        do {
            food = new Point(1 + random.nextInt(GAME_WIDTH - 2), 1 + random.nextInt(GAME_HEIGHT - 2));
        } while (snake.contains(food));
    }

    private void drawInstructions() throws IOException {
        terminal.puts(Capability.cursor_address, 0, 0);
        terminal.writer().println(CYAN + "🐍 Unicode Snake Game 🐍" + RESET);
        terminal.writer().println(WHITE + "Use WASD or arrow keys to move, Q to quit" + RESET);
        terminal.writer().println();
        terminal.flush();
    }

    private void drawGame() throws IOException {
        // Position cursor at game area
        terminal.puts(Capability.cursor_address, 3, 0);

        // Draw game board
        for (int y = 0; y < GAME_HEIGHT; y++) {
            for (int x = 0; x < GAME_WIDTH; x++) {
                String cell = getCellContent(x, y);
                terminal.writer().print(cell);
            }
            terminal.writer().println();
        }

        // Draw score
        terminal.writer().println(YELLOW + "Score: " + score + RESET);
        terminal.flush();
    }

    private String getCellContent(int x, int y) {
        // Draw walls
        if (y == 0) {
            if (x == 0) return BLUE + WALL_TOP_LEFT + RESET;
            if (x == GAME_WIDTH - 1) return BLUE + WALL_TOP_RIGHT + RESET;
            return BLUE + WALL_HORIZONTAL + RESET;
        }
        if (y == GAME_HEIGHT - 1) {
            if (x == 0) return BLUE + WALL_BOTTOM_LEFT + RESET;
            if (x == GAME_WIDTH - 1) return BLUE + WALL_BOTTOM_RIGHT + RESET;
            return BLUE + WALL_HORIZONTAL + RESET;
        }
        if (x == 0 || x == GAME_WIDTH - 1) {
            return BLUE + WALL_VERTICAL + RESET;
        }

        Point current = new Point(x, y);

        // Draw snake head
        if (snake.get(0).equals(current)) {
            return GREEN + SNAKE_HEAD + RESET;
        }

        // Draw snake body
        if (snake.contains(current)) {
            return GREEN + SNAKE_BODY + RESET;
        }

        // Draw food
        if (food.equals(current)) {
            return RED + FOOD + RESET;
        }

        // Empty space
        return EMPTY;
    }

    private void drawGameOver() throws IOException {
        terminal.puts(Capability.cursor_address, GAME_HEIGHT + 5, 0);
        terminal.writer().println(RED + "💀 GAME OVER! 💀" + RESET);
        terminal.writer().println(YELLOW + "Final Score: " + score + RESET);
        terminal.flush();
    }

    public static void main(String[] args) {
        try {
            UnicodeSnakeGame game = new UnicodeSnakeGame();
            game.run();
        } catch (Exception e) {
            System.err.println("Error running game: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
