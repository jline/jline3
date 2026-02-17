---
sidebar_position: 7
---

# Components Module

The JLine Components module (`jline-components`) provides composable, output-only UI components for building polished terminal interfaces. Inspired by [Ink](https://github.com/vadimdemedes/ink) (React for CLIs), it offers flexbox-inspired layout, animation support, and true-color rendering — all built on JLine's existing `Display` + `AttributedString` infrastructure.

## Overview

Unlike the full-screen [Curses TUI](../advanced/curses-tui.md) module, the Components module is designed for **inline, composable output** — progress bars, spinners, status messages, and styled text that render below the prompt without taking over the terminal.

Key features:

- **Composable components** with a flexbox-inspired layout engine
- **Animation framework** with 60fps rendering (spinners, progress sweeps, gradient shimmer)
- **True-color (24-bit RGB)** gradients and styled text
- **Dirty tracking** for efficient re-rendering (only redraws when content changes)
- **Builder pattern** for all components

## Quick Start

Add the dependency:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-components</artifactId>
    <version>4.0.0</version>
</dependency>
```

A minimal example showing a spinner with a progress bar:

```java
Terminal terminal = TerminalBuilder.builder().build();
ComponentRenderer renderer = ComponentRenderer.create(terminal);

Spinner spinner = Spinner.builder()
        .frames(SpinnerFrames.DOTS)
        .label("Loading...")
        .build();

ProgressBar bar = ProgressBar.builder()
        .progress(0.0)
        .width(30)
        .build();

Box root = Box.builder()
        .direction(FlexDirection.COLUMN)
        .borderStyle(Box.BorderStyle.ROUNDED)
        .padding(Insets.of(0, 1))
        .child(spinner)
        .child(bar)
        .build();

renderer.setRoot(root);
renderer.startAnimations();

for (int i = 0; i <= 100; i++) {
    bar.setProgress(i / 100.0);
    renderer.render();
    Thread.sleep(50);
}

renderer.close();
```

## Architecture

The module follows a clean separation of concerns:

```
Component (interface)       — what to render
Canvas (interface)          — where to render (character grid)
ComponentRenderer           — how to render (manages Display + animations)
LayoutEngine                — how to arrange children (flexbox)
```

Components never touch the Terminal directly. They render to a `Canvas`, which produces `List<AttributedString>` for JLine's `Display` class. This makes components testable without a real terminal.

## Components

### Text

Styled text with optional word wrapping and alignment.

```java
Text text = Text.builder()
        .text("Hello, World!")
        .style(AttributedStyle.BOLD)
        .build();

// With wrapping
Text wrapped = Text.builder()
        .text("This is a long message that will wrap")
        .wrap(true)
        .maxWidth(20)
        .alignment(FlexAlign.CENTER)
        .build();
```

### Box

Flexbox container with optional borders, padding, and gap.

```java
Box box = Box.builder()
        .direction(FlexDirection.COLUMN)    // or ROW
        .borderStyle(Box.BorderStyle.ROUNDED) // SINGLE, DOUBLE, ROUNDED
        .borderColor(AttributedStyle.DEFAULT.foreground(34, 197, 94))
        .padding(Insets.of(0, 1))
        .gap(1)
        .child(text1)
        .child(separator)
        .child(text2)
        .build();
```

Border styles: `NONE`, `SINGLE` (`┌─┐`), `DOUBLE` (`╔═╗`), `ROUNDED` (`╭─╮`).

### Spinner

Animated loading indicator with 25+ built-in frame sets.

```java
Spinner spinner = Spinner.builder()
        .frames(SpinnerFrames.DOTS)    // DOTS, ARC, ARROW, LINE, TRIANGLE, ...
        .label("Processing...")
        .build();

// Update the label dynamically
spinner.setLabel("Processing... 42 items done");
```

Available frame sets include: `DOTS`, `LINE`, `ARC`, `ARROW`, `TRIANGLE`, `CIRCLE`, `SQUARE`, `BOUNCE`, `CLOCK`, `EARTH`, `MOON`, `HEARTS`, and many more.

### ProgressBar

Determinate progress bar with gradient fills.

```java
ProgressBar bar = ProgressBar.builder()
        .progress(0.0)
        .width(40)
        .filledGradient(59, 130, 246, 147, 197, 253)  // blue gradient
        .emptyColor(30, 41, 59)                        // dark track
        .showPercentage(true)
        .build();

bar.setProgress(0.75);  // Update to 75%
```

### IndeterminateProgressBar

Animated sweep bar for unknown-duration operations.

```java
IndeterminateProgressBar bar = IndeterminateProgressBar.builder()
        .width(50)
        .glowRadius(12)
        .cycleDuration(2000)
        .trackColor(15, 23, 42)
        .glowColor(96, 165, 250)
        .build();
```

The glow sweeps back and forth using gaussian falloff for a smooth light effect.

### Gradient

Gradient-colored text with optional shimmer animation.

```java
// Static gradient
Gradient title = Gradient.builder()
        .text("My App")
        .colors(new int[]{255, 0, 0}, new int[]{0, 0, 255})
        .build();

// Animated shimmer
Gradient shimmer = Gradient.builder()
        .text("Loading")
        .baseColor(59, 130, 246)
        .highlightColor(219, 234, 254)
        .glowWidth(3)
        .animate(true)
        .cycleDuration(3000)
        .build();
```

### StatusMessage

Colored status indicators with prefix icons.

```java
StatusMessage.success("42 tests passed");    // ✔ green
StatusMessage.warning("2 deprecations");     // ⚠ yellow
StatusMessage.error("Build failed");         // ✖ red
StatusMessage.info("Code coverage: 94%");    // ℹ blue
```

### Separator

Horizontal line with optional centered title.

```java
Separator.builder().build();                           // ──────────
Separator.builder().title("Results").build();           // ── Results ──
```

### Hyperlink

Styled text that stores a URL for programmatic access.

```java
Hyperlink link = Hyperlink.builder()
        .url("https://github.com/jline/jline3")
        .text("JLine on GitHub")
        .style(AttributedStyle.DEFAULT.foreground(96, 165, 250).underline())
        .build();
```

## Canvas

The `Canvas` interface provides a character grid buffer that components render into. You never need to create a `Canvas` when using `ComponentRenderer` (it manages one internally), but it's useful for **testing components** or integrating with `Display` directly:

```java
// Create a canvas and render a component
Canvas canvas = Canvas.create(40, 5);
component.render(canvas, 40, 5);

// Convert to Display-compatible format
List<AttributedString> lines = canvas.toLines();
display.update(lines, -1);
```

Key methods:

- `Canvas.create(width, height)` — creates a new canvas
- `text(col, row, AttributedString)` — write styled text at a position
- `put(col, row, char, style)` — write a single character
- `fill(col, row, w, h, char, style)` — fill a rectangular region
- `subRegion(col, row, w, h)` — create a nested sub-canvas with coordinate translation and clipping
- `toLines()` — convert to `List<AttributedString>` for `Display.update()`

Sub-regions are how the layout engine gives each child component its own coordinate space within a parent Box.

## Layout

The layout engine implements a simplified flexbox model operating on character cells:

- **Direction**: `FlexDirection.ROW` (horizontal) or `COLUMN` (vertical)
- **Alignment**: `FlexAlign.START`, `CENTER`, `END`, `STRETCH` (cross-axis)
- **Justification**: `FlexJustify.START`, `CENTER`, `END`, `SPACE_BETWEEN`, `SPACE_AROUND`
- **Padding**: `Insets.of(top, right, bottom, left)` or `Insets.of(vertical, horizontal)`
- **Gap**: Space between children (in character cells)

```java
Box layout = Box.builder()
        .direction(FlexDirection.ROW)
        .justify(FlexJustify.SPACE_BETWEEN)
        .align(FlexAlign.CENTER)
        .gap(2)
        .child(leftContent)
        .child(rightContent)
        .build();
```

## Animation

Components implementing `Animatable` are automatically animated when `renderer.startAnimations()` is called. The animation timer runs on a daemon thread at each component's preferred interval (typically 16ms for 60fps).

Built-in animated components:
- `Spinner` — frame cycling
- `IndeterminateProgressBar` — glow sweep
- `Gradient` — shimmer effect (when `animate(true)` is set)

When you call `renderer.setRoot(newRoot)`, animatable components in the new tree are automatically discovered and registered.

`stopAnimations()` blocks until the animation thread has fully terminated, so it is safe to modify the component tree immediately after calling it.

### Component Tree Discovery

The animation framework discovers `Animatable` components by recursively walking the component tree via `Component.getChildren()`. If you create a custom composite component (a component that wraps or contains other components), you **must** override `getChildren()` to return your child components — otherwise nested animations will not be discovered:

```java
public class MyWrapper extends AbstractComponent {
    private final Component inner;

    @Override
    public List<Component> getChildren() {
        return Collections.singletonList(inner);
    }

    // ... render, getPreferredSize, etc.
}
```

### Custom Components

To create a custom component, extend `AbstractComponent` (which provides dirty tracking) and implement `getPreferredSize()` and `render()`:

```java
public class Badge extends AbstractComponent {
    private String text;
    private AttributedStyle style;

    @Override
    public Size getPreferredSize() {
        return new Size(text.length() + 4, 1); // "[ text ]"
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(style);
        sb.append("[ ").append(text).append(" ]");
        canvas.text(0, 0, sb.toAttributedString());
        markClean(); // clear dirty flag after rendering
    }

    public void setText(String text) {
        this.text = text;
        invalidate(); // mark dirty so renderer knows to re-render
    }
}
```

To make a custom component **animatable**, also implement the `Animatable` interface:

```java
public class Blinker extends AbstractComponent implements Animatable {
    private boolean visible = true;

    @Override
    public boolean onTick(long elapsedMs) {
        boolean newVisible = (elapsedMs / 500) % 2 == 0;
        if (newVisible != visible) {
            visible = newVisible;
            invalidate();
            return true; // changed
        }
        return false;
    }

    @Override
    public long getIntervalMs() {
        return 500;
    }

    // ... getPreferredSize(), render()
}
```

## Dirty Tracking

Components track whether they need re-rendering via `isDirty()` and `invalidate()`:

- Call `invalidate()` when a component's state changes (e.g., `setText()`, `setProgress()`, `onTick()`)
- The `render()` method should clear the dirty flag after rendering by calling `markClean()`
- Composite components should override `isDirty()` to also check their children's dirty state
- `ComponentRenderer.render()` creates a new canvas and renders the entire tree on each call; dirty tracking is primarily used by the animation timer to know when to trigger a re-render

The animation timer calls `onTick()` on each registered `Animatable`. If any returns `true` (indicating the component changed), the timer triggers `renderer.render()` via the `onDirty` callback.

## ComponentRenderer

`ComponentRenderer` manages the lifecycle of rendering a component tree to a terminal:

```java
// Inline rendering (below the prompt)
ComponentRenderer renderer = ComponentRenderer.create(terminal);

// Full-screen rendering
ComponentRenderer renderer = ComponentRenderer.fullScreen(terminal);
```

Key methods:

- `setRoot(component)` — sets the root component; re-registers animatables if animations are running
- `render()` — renders the component tree to the display (equivalent to `renderToDisplay(-1)`)
- `renderToDisplay(cursorPos)` — renders with a specific cursor position; pass `-1` to hide the cursor
- `startAnimations()` — walks the component tree, registers all `Animatable` components, starts the timer thread
- `stopAnimations()` — stops the animation timer
- `close()` — stops animations and restores the terminal's previous WINCH signal handler

## Convenience Factory

The `Components` class provides shorthand factory methods for common configurations:

```java
import org.jline.components.Components;

// Text
Text t = Components.text("hello");
Text bold = Components.boldText("Important");
Text styled = Components.text("colored", myStyle);

// Spinner and ProgressBar
Spinner s = Components.spinner("Loading...");
Spinner s2 = Components.spinner(SpinnerFrames.ARC, "Processing...");
ProgressBar bar = Components.progressBar(0.5);
IndeterminateProgressBar sweep = Components.indeterminateProgressBar(40);

// Status messages
StatusMessage ok = Components.success("All tests passed");
StatusMessage warn = Components.warning("2 deprecations");
StatusMessage err = Components.error("Build failed");
StatusMessage note = Components.info("Coverage: 94%");

// Layout shortcuts
Separator sep = Components.separator("Results");
Gradient grad = Components.gradient("Title", new int[]{255, 0, 0}, new int[]{0, 0, 255});
Hyperlink link = Components.link("https://example.com", "Click here");

// Box containers
Box vbox = Components.vbox(t, sep, s);              // vertical column
Box hbox = Components.hbox(ok, note);                // horizontal row
Box bordered = Components.borderedBox(Box.BorderStyle.ROUNDED, t, sep, bar);
```

## JPMS

The module descriptor exports four public packages:

```java
module org.jline.components {
    requires transitive org.jline.terminal;

    exports org.jline.components;
    exports org.jline.components.layout;
    exports org.jline.components.ui;
    exports org.jline.components.animation;
}
```

The `org.jline.components.impl` package is internal and not exported.
