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
 * A Snake game using Unicode box-drawing characters for a retro terminal aesthetic.
 * Uses widely-supported Unicode characters for better visual appeal than ASCII.
 */
public class BoxDrawingSnakeGame {

    // Game constants
    private static final int GAME_WIDTH = 35;
    private static final int GAME_HEIGHT = 18;
    private static final int GAME_SPEED = 180; // milliseconds between moves

    // Unicode box-drawing characters (widely supported)
    private static final String WALL_H = "─";
    private static final String WALL_V = "│";
    private static final String WALL_TL = "┌";
    private static final String WALL_TR = "┐";
    private static final String WALL_BL = "└";
    private static final String WALL_BR = "┘";
    private static final String SNAKE_HEAD = "█";
    private static final String SNAKE_BODY = "▓";
    private static final String FOOD = "◆";
    private static final String EMPTY = " ";

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String RED = "\u001B[31m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String MAGENTA = "\u001B[35m";

    // Game state
    private Terminal terminal;
    private NonBlockingReader reader;
    private List<Point> snake;
    private Point food;
    private Direction direction;
    private boolean gameRunning;
    private int score;
    private Random random;
    private String[][] gameBoard;

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

    public BoxDrawingSnakeGame() throws IOException {
        terminal = TerminalBuilder.builder().system(true).build();
        reader = terminal.reader();
        snake = new ArrayList<>();
        random = new Random();
        gameBoard = new String[GAME_HEIGHT][GAME_WIDTH];

        // Initialize snake in the center
        snake.add(new Point(GAME_WIDTH / 2, GAME_HEIGHT / 2));
        snake.add(new Point(GAME_WIDTH / 2 - 1, GAME_HEIGHT / 2));
        snake.add(new Point(GAME_WIDTH / 2 - 2, GAME_HEIGHT / 2));

        direction = Direction.RIGHT;
        gameRunning = true;
        score = 0;

        initializeBoard();
        spawnFood();
    }

    private void initializeBoard() {
        // Fill board with walls and empty spaces
        for (int y = 0; y < GAME_HEIGHT; y++) {
            for (int x = 0; x < GAME_WIDTH; x++) {
                if (y == 0) {
                    if (x == 0) gameBoard[y][x] = WALL_TL;
                    else if (x == GAME_WIDTH - 1) gameBoard[y][x] = WALL_TR;
                    else gameBoard[y][x] = WALL_H;
                } else if (y == GAME_HEIGHT - 1) {
                    if (x == 0) gameBoard[y][x] = WALL_BL;
                    else if (x == GAME_WIDTH - 1) gameBoard[y][x] = WALL_BR;
                    else gameBoard[y][x] = WALL_H;
                } else if (x == 0 || x == GAME_WIDTH - 1) {
                    gameBoard[y][x] = WALL_V;
                } else {
                    gameBoard[y][x] = EMPTY;
                }
            }
        }
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

            drawTitle();

            long lastMoveTime = System.currentTimeMillis();

            while (gameRunning) {
                // Handle input
                handleInput();

                // Move snake at regular intervals
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime >= GAME_SPEED) {
                    moveSnake();
                    updateBoard();
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
                case 65: // Up arrow (if supported)
                    if (direction != Direction.DOWN) direction = Direction.UP;
                    break;
                case 's':
                case 'S':
                case 66: // Down arrow (if supported)
                    if (direction != Direction.UP) direction = Direction.DOWN;
                    break;
                case 'a':
                case 68: // Left arrow (if supported)
                    if (direction != Direction.RIGHT) direction = Direction.LEFT;
                    break;
                case 'd':
                case 67: // Right arrow (if supported)
                    if (direction != Direction.LEFT) direction = Direction.RIGHT;
                    break;
                case 'q':
                case 'Q':
                case 27: // ESC
                    gameRunning = false;
                    break;
                case 'p':
                case 'P':
                    // Pause functionality
                    pause();
                    break;
            }
        }
    }

    private void pause() throws IOException {
        terminal.puts(Capability.cursor_address, GAME_HEIGHT + 4, 0);
        terminal.writer().println(YELLOW + "PAUSED - Press any key to continue..." + RESET);
        terminal.flush();
        reader.read();
    }

    private void moveSnake() {
        Point head = snake.get(0);
        Point newHead = head.move(direction);

        // Check wall collision
        if (newHead.x <= 0 || newHead.x >= GAME_WIDTH - 1 || newHead.y <= 0 || newHead.y >= GAME_HEIGHT - 1) {
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

    private void updateBoard() {
        // Reset inner board (keep walls)
        for (int y = 1; y < GAME_HEIGHT - 1; y++) {
            for (int x = 1; x < GAME_WIDTH - 1; x++) {
                gameBoard[y][x] = EMPTY;
            }
        }

        // Place food
        gameBoard[food.y][food.x] = FOOD;

        // Place snake
        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);
            if (i == 0) {
                gameBoard[segment.y][segment.x] = SNAKE_HEAD;
            } else {
                gameBoard[segment.y][segment.x] = SNAKE_BODY;
            }
        }
    }

    private void drawTitle() throws IOException {
        terminal.puts(Capability.cursor_address, 0, 0);
        terminal.writer().println(CYAN + "┌─────────────────────────────────────┐" + RESET);
        terminal.writer()
                .println(CYAN + "│" + RESET + BRIGHT_GREEN + "          UNICODE SNAKE GAME         " + RESET + CYAN
                        + "│" + RESET);
        terminal.writer().println(CYAN + "└─────────────────────────────────────┘" + RESET);
        terminal.writer().println(WHITE + "Controls: WASD to move, P to pause, Q to quit" + RESET);
        terminal.writer().println();
        terminal.flush();
    }

    private void drawGame() throws IOException {
        // Position cursor at game area
        terminal.puts(Capability.cursor_address, 5, 0);

        // Draw game board
        for (int y = 0; y < GAME_HEIGHT; y++) {
            for (int x = 0; x < GAME_WIDTH; x++) {
                String cell = gameBoard[y][x];
                String coloredCell = getColoredCell(cell);
                terminal.writer().print(coloredCell);
            }
            terminal.writer().println();
        }

        // Draw game info
        terminal.writer().println();
        terminal.writer()
                .println(YELLOW + "Score: " + score + RESET + WHITE
                        + "  │  " + RESET + GREEN
                        + "Length: " + snake.size() + RESET + WHITE
                        + "  │  " + RESET + MAGENTA
                        + "Speed: " + (300 - GAME_SPEED) + "ms" + RESET);
        terminal.flush();
    }

    private String getColoredCell(String cell) {
        switch (cell) {
            case WALL_H:
            case WALL_V:
            case WALL_TL:
            case WALL_TR:
            case WALL_BL:
            case WALL_BR:
                return BLUE + cell + RESET;
            case SNAKE_HEAD:
                return BRIGHT_GREEN + cell + RESET;
            case SNAKE_BODY:
                return GREEN + cell + RESET;
            case FOOD:
                return BRIGHT_RED + cell + RESET;
            default:
                return cell;
        }
    }

    private void drawGameOver() throws IOException {
        terminal.puts(Capability.cursor_address, GAME_HEIGHT + 8, 0);
        terminal.writer().println(RED + "┌─────────────────────┐" + RESET);
        terminal.writer().println(RED + "│" + RESET + BRIGHT_RED + "     GAME OVER!      " + RESET + RED + "│" + RESET);
        terminal.writer().println(RED + "└─────────────────────┘" + RESET);
        terminal.writer().println(YELLOW + "Final Score: " + score + RESET);
        terminal.writer().println(GREEN + "Snake Length: " + snake.size() + RESET);

        // Show a simple "trophy" based on score
        if (score >= 100) {
            terminal.writer().println(YELLOW + "Achievement: Snake Master! " + "★★★" + RESET);
        } else if (score >= 50) {
            terminal.writer().println(YELLOW + "Achievement: Good Player! " + "★★" + RESET);
        } else if (score >= 20) {
            terminal.writer().println(YELLOW + "Achievement: Getting Started! " + "★" + RESET);
        }

        terminal.flush();
    }

    public static void main(String[] args) {
        try {
            BoxDrawingSnakeGame game = new BoxDrawingSnakeGame();
            game.run();
        } catch (Exception e) {
            System.err.println("Error running game: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
