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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.imageio.ImageIO;

/**
 * Image cache for terminal graphics to improve performance and reduce memory usage.
 * 
 * <p>This cache stores processed images and their metadata to avoid reprocessing
 * the same images multiple times. It includes features like:</p>
 * <ul>
 *   <li>LRU eviction policy</li>
 *   <li>File modification time tracking</li>
 *   <li>Memory usage monitoring</li>
 *   <li>Automatic cache cleanup</li>
 * </ul>
 * 
 * @since 3.30.0
 */
public class ImageCache {
    
    private static final int DEFAULT_MAX_ENTRIES = 50;
    private static final long DEFAULT_MAX_MEMORY_MB = 100;
    
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final long maxMemoryBytes;
    private volatile long currentMemoryUsage = 0;
    
    /**
     * Cache entry containing image data and metadata.
     */
    private static class CacheEntry {
        final BufferedImage image;
        final long lastModified;
        final long size;
        volatile long lastAccessed;
        
        CacheEntry(BufferedImage image, long lastModified, long size) {
            this.image = image;
            this.lastModified = lastModified;
            this.size = size;
            this.lastAccessed = System.currentTimeMillis();
        }
        
        void updateAccess() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
    
    /**
     * Creates a new image cache with default settings.
     */
    public ImageCache() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_MEMORY_MB);
    }
    
    /**
     * Creates a new image cache with specified limits.
     * 
     * @param maxEntries maximum number of cached entries
     * @param maxMemoryMB maximum memory usage in megabytes
     */
    public ImageCache(int maxEntries, long maxMemoryMB) {
        this.maxEntries = maxEntries;
        this.maxMemoryBytes = maxMemoryMB * 1024 * 1024;
    }
    
    /**
     * Gets an image from the cache or loads it if not cached.
     * 
     * @param file the image file to load
     * @return the cached or newly loaded image
     * @throws IOException if the image cannot be loaded
     */
    public BufferedImage getImage(File file) throws IOException {
        String key = generateKey(file);
        Path path = file.toPath();
        
        // Check if file exists
        if (!Files.exists(path)) {
            throw new IOException("Image file not found: " + file.getAbsolutePath());
        }
        
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        long lastModified = attrs.lastModifiedTime().toMillis();
        
        CacheEntry entry = cache.get(key);
        
        // Check if cached entry is still valid
        if (entry != null && entry.lastModified == lastModified) {
            entry.updateAccess();
            return entry.image;
        }
        
        // Load the image
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Unable to read image file: " + file.getAbsolutePath());
        }
        
        // Calculate image size in bytes (rough estimate)
        long imageSize = (long) image.getWidth() * image.getHeight() * 4; // 4 bytes per pixel (ARGB)
        
        // Create cache entry
        CacheEntry newEntry = new CacheEntry(image, lastModified, imageSize);
        
        // Add to cache and manage memory
        cache.put(key, newEntry);
        currentMemoryUsage += imageSize;
        
        // Cleanup if necessary
        cleanupIfNeeded();
        
        return image;
    }
    
    /**
     * Generates a cache key for the given file.
     */
    private String generateKey(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(file.getAbsolutePath().getBytes());
            byte[] digest = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple path-based key
            return file.getAbsolutePath().replace(File.separator, "_");
        }
    }
    
    /**
     * Cleans up the cache if it exceeds memory or entry limits.
     */
    private void cleanupIfNeeded() {
        // Check if cleanup is needed
        if (cache.size() <= maxEntries && currentMemoryUsage <= maxMemoryBytes) {
            return;
        }
        
        // Find least recently used entries
        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().lastAccessed, e2.getValue().lastAccessed))
            .limit(Math.max(1, cache.size() - maxEntries + 10)) // Remove extra entries for buffer
            .forEach(entry -> {
                cache.remove(entry.getKey());
                currentMemoryUsage -= entry.getValue().size;
            });
    }
    
    /**
     * Clears all cached images.
     */
    public void clear() {
        cache.clear();
        currentMemoryUsage = 0;
    }
    
    /**
     * Gets the current number of cached images.
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Gets the current memory usage in bytes.
     */
    public long getMemoryUsage() {
        return currentMemoryUsage;
    }
    
    /**
     * Gets the current memory usage in megabytes.
     */
    public double getMemoryUsageMB() {
        return currentMemoryUsage / (1024.0 * 1024.0);
    }
    
    /**
     * Gets cache statistics as a formatted string.
     */
    public String getStats() {
        return String.format("ImageCache: %d entries, %.2f MB used", size(), getMemoryUsageMB());
    }
}
