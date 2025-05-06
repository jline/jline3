---
sidebar_position: 6
---

# Mouse Support

import CodeSnippet from '@site/src/components/CodeSnippet';

JLine provides support for mouse events in terminal applications, allowing you to create more interactive and user-friendly command-line interfaces. This feature is particularly useful for applications that require point-and-click interactions, such as text editors, file browsers, or interactive menus.

## Mouse Support Basics

Terminal mouse support works by capturing mouse events (clicks, movements, wheel scrolling) and translating them into escape sequences that can be processed by your application. JLine provides an abstraction layer that makes it easy to work with these events.

### Enabling Mouse Support

To enable mouse support in your JLine application:

<CodeSnippet name="MouseSupportBasicsExample" />

## Mouse Event Types

JLine can capture several types of mouse events:

- **Mouse Clicks**: Left, middle, and right button clicks
- **Mouse Movement**: Movement with buttons pressed or released
- **Mouse Wheel**: Scrolling up or down
- **Mouse Position**: Coordinates of the mouse pointer

## Handling Mouse Events

To properly handle mouse events, you need to parse the escape sequences that represent mouse actions. JLine's `Terminal.MouseEvent` class helps with this:

<CodeSnippet name="MouseEventHandlingExample" />

## Mouse Tracking Modes

JLine supports different mouse tracking modes, which determine what types of events are reported:

<CodeSnippet name="MouseTrackingModesExample" />

## Creating Interactive UI Elements

With mouse support, you can create interactive UI elements like buttons, menus, and selection lists:

<CodeSnippet name="MouseInteractiveUIExample" />

## Mouse Support in LineReader

JLine's `LineReader` can also be configured to handle mouse events:

<CodeSnippet name="LineReaderMouseExample" />

## Terminal Compatibility

Mouse support depends on the capabilities of the terminal emulator being used. Not all terminals support mouse events, and those that do may support different features or use different escape sequences.

Here's a compatibility table for common terminal emulators:

| Terminal Emulator | Basic Clicks | Mouse Movement | Mouse Wheel | Extended Reporting |
|-------------------|--------------|----------------|-------------|-------------------|
| xterm             | Yes          | Yes            | Yes         | Yes               |
| iTerm2            | Yes          | Yes            | Yes         | Yes               |
| GNOME Terminal    | Yes          | Yes            | Yes         | Yes               |
| Konsole           | Yes          | Yes            | Yes         | Yes               |
| PuTTY             | Yes          | Yes            | Yes         | Partial           |
| Windows Terminal  | Yes          | Yes            | Yes         | Yes               |
| CMD.exe           | No           | No             | No          | No                |
| MinTTY (Git Bash) | Yes          | Yes            | Yes         | Yes               |

To check if a terminal supports mouse events:

```java
boolean supportsMouseTracking = terminal.getStringCapability(Capability.enter_mouse_mode) != null;
```

## Best Practices

When implementing mouse support in your JLine applications:

1. **Always disable mouse tracking before exiting**: Failing to do so can leave the terminal in an inconsistent state.

2. **Provide keyboard alternatives**: Not all users can or want to use a mouse, so always provide keyboard shortcuts for mouse actions.

3. **Check for terminal capabilities**: Before enabling mouse support, check if the terminal supports it.

4. **Handle terminal resizing**: Mouse coordinates can change when the terminal is resized, so be prepared to update your UI accordingly.

5. **Use clear visual indicators**: Make it obvious which elements are clickable and provide visual feedback on mouse hover and click.

6. **Test in different terminals**: Mouse support varies across terminal emulators, so test your application in multiple environments.

7. **Consider accessibility**: Some users rely on screen readers or other assistive technologies that may not work well with mouse-based interfaces.

## Conclusion

Mouse support in JLine allows you to create more interactive and user-friendly terminal applications. By capturing and processing mouse events, you can implement clickable buttons, menus, selection lists, and other UI elements that would be cumbersome to navigate using only the keyboard.

Remember that mouse support is an enhancement, not a replacement for keyboard navigation. Always ensure your application is fully usable without a mouse for maximum accessibility and compatibility.
