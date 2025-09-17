# Terminal Graphics

JLine 3 provides comprehensive support for displaying images in modern terminals through multiple graphics protocols. This feature enables rich terminal applications that can display images, charts, and visual content directly in the terminal.

## Overview

JLine supports three major terminal graphics protocols:

- **Kitty Graphics Protocol** (Priority: 90) - Modern, feature-rich protocol
- **iTerm2 Inline Images** (Priority: 70) - iTerm2's proprietary protocol  
- **Sixel Graphics** (Priority: 10) - Widely supported legacy protocol

The graphics system automatically detects which protocols are supported by the current terminal and selects the best available option.

## Quick Start

### Basic Image Display

```java
import org.jline.terminal.TerminalGraphicsManager;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

// Display an image file
File imageFile = new File("image.png");
TerminalGraphicsManager.displayImage(terminal, imageFile);

// Display a BufferedImage
BufferedImage image = ImageIO.read(imageFile);
TerminalGraphicsManager.displayImage(terminal, image);
```

### Image Display with Options

```java
import org.jline.terminal.TerminalGraphics;

// Create image options
TerminalGraphics.ImageOptions options = new TerminalGraphics.ImageOptions()
    .width(100)                    // Set width in characters
    .height(50)                    // Set height in characters
    .preserveAspectRatio(true)     // Maintain aspect ratio
    .name("my-image");             // Set image name/identifier

// Display with options
TerminalGraphicsManager.displayImage(terminal, image, options);
```

## Protocol Detection

JLine uses intelligent runtime detection to determine which graphics protocols are supported:

### Runtime Detection Methods

1. **Sixel Detection**: Sends Device Attributes query (`ESC[c`) and looks for code "4"
2. **Kitty Detection**: Sends Kitty graphics protocol query and checks for valid response
3. **Static Detection**: Falls back to environment variable and terminal type checking

**Raw Mode Protection**: All runtime detection uses raw mode to prevent escape sequences from being displayed as visible text in unsupported terminals.

### Checking Graphics Support

```java
// Check if any graphics protocol is supported
boolean supported = TerminalGraphicsManager.isGraphicsSupported(terminal);

// Get the best available protocol
Optional<TerminalGraphics> bestProtocol = TerminalGraphicsManager.getBestProtocol(terminal);
if (bestProtocol.isPresent()) {
    System.out.println("Best protocol: " + bestProtocol.get().getProtocol());
    System.out.println("Priority: " + bestProtocol.get().getPriority());
}

// List all available protocols
List<TerminalGraphics> protocols = TerminalGraphicsManager.getAvailableProtocols();
```

## Terminal Compatibility

| Terminal | Sixel | Kitty | iTerm2 | Detection Method |
|----------|-------|-------|--------|------------------|
| **Kitty** | ❌ No | ✅ Yes | ❌ No | Runtime + Static |
| **Ghostty** | ❌ No | ✅ Yes | ❌ No | Static |
| **iTerm2** | ✅ Yes | ✅ Yes | ✅ Yes | Static + Environment |
| **WezTerm** | ✅ Yes | ✅ Yes | ❌ No | Static + Environment |
| **xterm** | ✅ Yes | ❌ No | ❌ No | Runtime + Static |
| **foot** | ✅ Yes | ❌ No | ❌ No | Runtime + Static |
| **Konsole** | ✅ Yes | ❌ No | ❌ No | Runtime + Static |

## Advanced Usage

### Force Specific Protocol

```java
// Force use of Kitty protocol
TerminalGraphicsManager.forceProtocol(TerminalGraphics.Protocol.KITTY);

// Reset to automatic selection
TerminalGraphicsManager.forceProtocol(null);
```

### Configure Detection Timeouts

JLine uses configurable timeouts for runtime detection to prevent hanging in terminals that don't support graphics protocols:

```bash
# Kitty graphics protocol detection timeouts
-Djline.terminal.graphics.kitty.timeout=100          # Initial timeout (default: 100ms)
-Djline.terminal.graphics.kitty.subsequent.timeout=25  # Subsequent chars (default: 25ms)

# Sixel graphics protocol detection timeouts
-Djline.terminal.graphics.sixel.timeout=200          # Initial timeout (default: 200ms)
-Djline.terminal.graphics.sixel.subsequent.timeout=25  # Subsequent chars (default: 25ms)
```

**Example usage:**
```bash
java -Djline.terminal.graphics.kitty.timeout=50 \
     -Djline.terminal.graphics.sixel.timeout=100 \
     MyApplication
```

### Override Support Detection

```java
// Override Sixel support detection
SixelGraphics.setSixelSupportOverride(true);  // Force enable
SixelGraphics.setSixelSupportOverride(false); // Force disable
SixelGraphics.setSixelSupportOverride(null);  // Reset to automatic
```

### Direct Protocol Usage

```java
// Use specific protocol directly
SixelGraphics sixel = new SixelGraphics();
if (sixel.isSupported(terminal)) {
    sixel.displayImage(terminal, image);
}

KittyGraphics kitty = new KittyGraphics();
if (kitty.isSupported(terminal)) {
    kitty.displayImage(terminal, image);
}
```

## Image Processing

### Supported Formats

JLine supports all image formats supported by Java's ImageIO:
- PNG (recommended)
- JPEG
- GIF
- BMP
- WBMP

### Automatic Resizing

Images are automatically resized to fit within terminal constraints:

```java
// Images larger than terminal will be scaled down
// Aspect ratio is preserved by default
TerminalGraphics.ImageOptions options = new TerminalGraphics.ImageOptions()
    .preserveAspectRatio(true);  // Default: true
```

### Color Handling

Different protocols handle colors differently:
- **Kitty**: Full 24-bit RGB color support
- **iTerm2**: Full 24-bit RGB color support  
- **Sixel**: Palette-based colors (up to 256 colors)

## Error Handling

```java
try {
    TerminalGraphicsManager.displayImage(terminal, image);
} catch (UnsupportedOperationException e) {
    System.err.println("Graphics not supported in this terminal");
} catch (IOException e) {
    System.err.println("Error reading image: " + e.getMessage());
}
```

## Performance Considerations

- **Image Size**: Large images are automatically resized to reduce memory usage
- **Protocol Selection**: Kitty protocol is generally fastest, followed by iTerm2, then Sixel
- **Caching**: Consider caching converted image data for repeated displays
- **Memory**: Images are processed in memory; very large images may require more heap space

## Examples

See the `TerminalGraphicsExample` class for complete working examples:

```bash
# List supported protocols
./build example TerminalGraphicsExample -- --list-protocols

# Display an image
./build example TerminalGraphicsExample -- --display-image path/to/image.png

# Display with custom size
./build example TerminalGraphicsExample -- --display-image path/to/image.png --width 80 --height 40
```

## Troubleshooting

### Common Issues

1. **No graphics support detected**: Check if your terminal supports graphics protocols
2. **Images not displaying**: Verify image file format and permissions
3. **Poor image quality**: Try different protocols or adjust image size
4. **Performance issues**: Reduce image size or use more efficient formats
5. **Application hangs during detection**: Reduce timeout values:
   ```bash
   # Use very short timeouts for faster detection
   -Djline.terminal.graphics.kitty.timeout=50
   -Djline.terminal.graphics.sixel.timeout=50
   ```
6. **Escape sequences visible in terminal**: This should not happen with JLine 3.30.5+ as raw mode prevents echo during detection. If you see this, please report it as a bug.

### Debug Information

Enable debug logging to see protocol detection details:

```java
System.setProperty("jline.terminal.graphics.debug", "true");
```

This will output detailed information about protocol detection and selection.
