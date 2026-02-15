/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Optional;

import org.jline.curses.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.TerminalGraphics;
import org.jline.terminal.impl.TerminalGraphicsManager;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A component that displays an image using terminal graphics protocols.
 *
 * <p>This component uses {@link TerminalGraphicsManager} to detect the best
 * available graphics protocol (Sixel, Kitty, iTerm2) and renders a
 * {@link BufferedImage} using terminal escape sequences. If no graphics
 * protocol is available, a fallback text message is displayed instead.</p>
 */
public class ImageComponent extends AbstractComponent {

    private static final int BASE_CELL_WIDTH_PX = 8;
    private static final int BASE_CELL_HEIGHT_PX = 16;
    private static final int CELL_WIDTH_PX;
    private static final int CELL_HEIGHT_PX;

    static {
        int scale = getDisplayScaleFactor();
        CELL_WIDTH_PX = BASE_CELL_WIDTH_PX * scale;
        CELL_HEIGHT_PX = BASE_CELL_HEIGHT_PX * scale;
    }

    private static int getDisplayScaleFactor() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            AffineTransform tx = gc.getDefaultTransform();
            return (int) Math.round(tx.getScaleX());
        } catch (Exception e) {
            return 1;
        }
    }

    private final BufferedImage image;
    private final String fallbackText;
    private String cachedImageData;
    private int cachedWidth = -1;
    private int cachedHeight = -1;

    /**
     * Creates an image component.
     *
     * @param image the image to display
     * @param fallbackText text to display if terminal graphics are not supported
     */
    public ImageComponent(BufferedImage image, String fallbackText) {
        this.image = image;
        this.fallbackText = fallbackText != null ? fallbackText : "Image display not supported";
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        if (size == null) {
            return;
        }
        Position pos = getScreenPosition();
        if (pos == null) {
            return;
        }

        AttributedStyle bgStyle = resolveStyle(".label.normal", AttributedStyle.DEFAULT);
        screen.fill(pos.x(), pos.y(), size.w(), size.h(), bgStyle);

        Terminal terminal = getTerminal();
        if (terminal == null || image == null) {
            drawFallback(screen, pos, size, bgStyle);
            return;
        }

        Optional<TerminalGraphics> protocol = TerminalGraphicsManager.getBestProtocol(terminal);
        if (protocol.isEmpty()) {
            drawFallback(screen, pos, size, bgStyle);
            return;
        }

        try {
            String imageData = getImageData(protocol.get(), size);
            if (imageData != null) {
                screen.image(pos.x(), pos.y(), size.w(), size.h(), imageData);
            } else {
                drawFallback(screen, pos, size, bgStyle);
            }
        } catch (IOException | RuntimeException e) {
            drawFallback(screen, pos, size, bgStyle);
        }
    }

    private String getImageData(TerminalGraphics graphics, Size size) throws IOException {
        if (cachedImageData != null && cachedWidth == size.w() && cachedHeight == size.h()) {
            return cachedImageData;
        }
        // Scale the image to fill the component area.
        // We estimate pixel dimensions from cell count; the terminal will render
        // the resulting image at its natural pixel size, filling the allocated cells.
        int targetW = size.w() * CELL_WIDTH_PX;
        int targetH = size.h() * CELL_HEIGHT_PX;
        if (targetW <= 0 || targetH <= 0) {
            return null;
        }
        BufferedImage scaled = scaleImage(image, targetW, targetH);
        cachedImageData = graphics.convertImage(scaled, new TerminalGraphics.ImageOptions());
        cachedWidth = size.w();
        cachedHeight = size.h();
        return cachedImageData;
    }

    private static BufferedImage scaleImage(BufferedImage src, int targetW, int targetH) {
        if (src.getWidth() == targetW && src.getHeight() == targetH) {
            return src;
        }
        int type = src.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage scaled = new BufferedImage(targetW, targetH, type);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return scaled;
    }

    private void drawFallback(Screen screen, Position pos, Size size, AttributedStyle style) {
        String text = fallbackText;
        if (text.length() > size.w()) {
            text = text.substring(0, size.w());
        }
        int x = pos.x() + Math.max(0, (size.w() - text.length()) / 2);
        int y = pos.y() + size.h() / 2;
        screen.text(x, y, new AttributedString(text, style));
    }

    private Terminal getTerminal() {
        Window window = getWindow();
        if (window == null) {
            return null;
        }
        GUI gui = window.getGUI();
        if (gui == null) {
            return null;
        }
        return gui.getTerminal();
    }

    @Override
    protected Size doGetPreferredSize() {
        if (image == null) {
            return new Size(fallbackText.length(), 1);
        }
        int cellW = Math.max(1, (image.getWidth() + CELL_WIDTH_PX - 1) / CELL_WIDTH_PX);
        int cellH = Math.max(1, (image.getHeight() + CELL_HEIGHT_PX - 1) / CELL_HEIGHT_PX);
        return new Size(cellW, cellH);
    }
}
