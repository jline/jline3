/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.jline.terminal.Terminal;

/**
 * Support for displaying animated images in terminals.
 * 
 * <p>This class provides functionality to display animated GIF images and other
 * multi-frame image formats in terminals that support graphics protocols.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic frame extraction from animated GIFs</li>
 *   <li>Configurable frame timing and delays</li>
 *   <li>Loop control (finite or infinite loops)</li>
 *   <li>Pause/resume functionality</li>
 *   <li>Memory-efficient frame caching</li>
 * </ul>
 * 
 * @since 3.30.0
 */
public class AnimatedImageSupport {
    
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    
    /**
     * Represents a single frame in an animated image.
     */
    public static class AnimationFrame {
        private final BufferedImage image;
        private final int delayMs;
        
        public AnimationFrame(BufferedImage image, int delayMs) {
            this.image = image;
            this.delayMs = Math.max(50, delayMs); // Minimum 50ms delay
        }
        
        public BufferedImage getImage() {
            return image;
        }
        
        public int getDelayMs() {
            return delayMs;
        }
    }
    
    /**
     * Animation playback controller.
     */
    public static class AnimationController {
        private final List<AnimationFrame> frames;
        private final Terminal terminal;
        private final TerminalGraphics graphics;
        private final TerminalGraphics.ImageOptions options;
        
        private volatile boolean playing = false;
        private volatile boolean paused = false;
        private volatile int currentFrame = 0;
        private volatile int loopCount = 0;
        private volatile int maxLoops = -1; // -1 for infinite
        private ScheduledFuture<?> playbackTask;
        
        AnimationController(List<AnimationFrame> frames, Terminal terminal, 
                          TerminalGraphics graphics, TerminalGraphics.ImageOptions options) {
            this.frames = frames;
            this.terminal = terminal;
            this.graphics = graphics;
            this.options = options;
        }
        
        /**
         * Starts animation playback.
         */
        public synchronized void play() {
            if (playing && !paused) {
                return;
            }
            
            playing = true;
            paused = false;
            scheduleNextFrame();
        }
        
        /**
         * Pauses animation playback.
         */
        public synchronized void pause() {
            paused = true;
            if (playbackTask != null) {
                playbackTask.cancel(false);
            }
        }
        
        /**
         * Resumes animation playback.
         */
        public synchronized void resume() {
            if (playing && paused) {
                paused = false;
                scheduleNextFrame();
            }
        }
        
        /**
         * Stops animation playback.
         */
        public synchronized void stop() {
            playing = false;
            paused = false;
            if (playbackTask != null) {
                playbackTask.cancel(false);
            }
        }
        
        /**
         * Sets the maximum number of loops (-1 for infinite).
         */
        public void setMaxLoops(int maxLoops) {
            this.maxLoops = maxLoops;
        }
        
        /**
         * Gets the current frame number.
         */
        public int getCurrentFrame() {
            return currentFrame;
        }
        
        /**
         * Gets the total number of frames.
         */
        public int getFrameCount() {
            return frames.size();
        }
        
        /**
         * Checks if the animation is currently playing.
         */
        public boolean isPlaying() {
            return playing && !paused;
        }
        
        private void scheduleNextFrame() {
            if (!playing || paused || frames.isEmpty()) {
                return;
            }
            
            AnimationFrame frame = frames.get(currentFrame);
            
            // Display current frame
            try {
                graphics.displayImage(terminal, frame.getImage(), options);
                terminal.flush();
            } catch (IOException e) {
                // Stop animation on error
                stop();
                return;
            }
            
            // Schedule next frame
            playbackTask = executor.schedule(() -> {
                synchronized (this) {
                    if (!playing || paused) {
                        return;
                    }
                    
                    currentFrame++;
                    if (currentFrame >= frames.size()) {
                        currentFrame = 0;
                        loopCount++;
                        
                        // Check if we've reached max loops
                        if (maxLoops >= 0 && loopCount >= maxLoops) {
                            stop();
                            return;
                        }
                    }
                    
                    scheduleNextFrame();
                }
            }, frame.getDelayMs(), TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Loads an animated image from a file.
     * 
     * @param file the image file to load
     * @return list of animation frames
     * @throws IOException if the image cannot be loaded
     */
    public static List<AnimationFrame> loadAnimatedImage(File file) throws IOException {
        List<AnimationFrame> frames = new ArrayList<>();
        
        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            ImageReader reader = ImageIO.getImageReaders(stream).next();
            reader.setInput(stream);
            
            int numFrames = reader.getNumImages(true);
            
            for (int i = 0; i < numFrames; i++) {
                BufferedImage image = reader.read(i);
                int delay = getFrameDelay(reader, i);
                frames.add(new AnimationFrame(image, delay));
            }
            
            reader.dispose();
        }
        
        return frames;
    }
    
    /**
     * Creates an animation controller for the given frames.
     */
    public static AnimationController createController(List<AnimationFrame> frames, 
                                                     Terminal terminal, 
                                                     TerminalGraphics graphics,
                                                     TerminalGraphics.ImageOptions options) {
        return new AnimationController(frames, terminal, graphics, options);
    }
    
    /**
     * Displays an animated image with default options.
     */
    public static CompletableFuture<AnimationController> displayAnimatedImage(
            Terminal terminal, File imageFile) throws IOException {
        
        TerminalGraphics graphics = TerminalGraphicsManager.getBestProtocol(terminal)
            .orElseThrow(() -> new IOException("No graphics protocol available"));
        
        return displayAnimatedImage(terminal, graphics, imageFile, new TerminalGraphics.ImageOptions());
    }
    
    /**
     * Displays an animated image with specified options.
     */
    public static CompletableFuture<AnimationController> displayAnimatedImage(
            Terminal terminal, TerminalGraphics graphics, File imageFile, 
            TerminalGraphics.ImageOptions options) throws IOException {
        
        List<AnimationFrame> frames = loadAnimatedImage(imageFile);
        AnimationController controller = createController(frames, terminal, graphics, options);
        
        return CompletableFuture.supplyAsync(() -> {
            controller.play();
            return controller;
        });
    }
    
    /**
     * Extracts frame delay from image metadata.
     */
    private static int getFrameDelay(ImageReader reader, int frameIndex) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(frameIndex);
            String metaFormatName = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);
            
            IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
            if (graphicsControlExtensionNode != null) {
                String delayTime = graphicsControlExtensionNode.getAttribute("delayTime");
                if (delayTime != null && !delayTime.isEmpty()) {
                    return Integer.parseInt(delayTime) * 10; // Convert from centiseconds to milliseconds
                }
            }
        } catch (Exception e) {
            // Ignore and use default delay
        }
        
        return 100; // Default 100ms delay
    }
    
    /**
     * Helper method to find a node by name in metadata tree.
     */
    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        return null;
    }
    
    /**
     * Shuts down the animation executor service.
     * Should be called when the application is shutting down.
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
