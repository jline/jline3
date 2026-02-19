---
sidebar_position: 10
---

# Grapheme Cluster Mode (Mode 2027)

JLine supports terminal mode 2027 ("Unicode Core"), which tells the terminal to use [UAX #29](https://unicode.org/reports/tr29/) grapheme cluster segmentation instead of per-codepoint `wcwidth()` for cursor positioning.

## Why It Matters

Without mode 2027, terminals determine cursor movement on a per-codepoint basis. This causes incorrect positioning for characters composed of multiple Unicode code points, such as:

- **ZWJ emoji sequences**: `👨‍🌾` (farmer) is made of `👨` + ZWJ + `🌾`, but should occupy a single display cell
- **Flag sequences**: `🇫🇷` is made of two regional indicator symbols
- **Combined characters**: Characters with combining marks like `क्षि`

When mode 2027 is enabled, the terminal treats these multi-codepoint sequences as single grapheme clusters and positions the cursor accordingly.

## Checking Support

JLine probes the terminal at runtime using [DECRQM](https://vt100.net/docs/vt510-rm/DECRQM.html) (DEC Request Mode) to determine whether mode 2027 is supported. This avoids false positives from terminals that report `xterm-256color` as their type but don't actually support the mode.

```java
Terminal terminal = TerminalBuilder.terminal();

if (terminal.supportsGraphemeClusterMode()) {
    System.out.println("Terminal supports grapheme cluster mode");
} else {
    System.out.println("Terminal does not support grapheme cluster mode");
}
```

The result of the probe is cached, so subsequent calls to `supportsGraphemeClusterMode()` do not send additional escape sequences.

## Enabling and Disabling

```java
Terminal terminal = TerminalBuilder.terminal();

// Enable grapheme cluster mode
if (terminal.setGraphemeClusterMode(true)) {
    // Mode 2027 is now active
    // The terminal will use grapheme cluster segmentation

    // ... application logic ...

    // Disable when done
    terminal.setGraphemeClusterMode(false);
}
```

`setGraphemeClusterMode()` returns `true` if the terminal supports mode 2027 and the escape sequence was sent, or `false` if the terminal does not support it.

## Escape Sequences

JLine uses the following escape sequences internally:

| Operation | Sequence |
|-----------|----------|
| Enable    | `CSI ? 2027 h` |
| Disable   | `CSI ? 2027 l` |
| Query (DECRQM) | `CSI ? 2027 $ p` |
| Response (DECRPM) | `CSI ? 2027 ; Ps $ y` |

The query response `Ps` indicates the mode status:

| Ps | Meaning |
|----|---------|
| 0  | Not recognized |
| 1  | Set (enabled) |
| 2  | Reset (disabled, but recognized) |
| 3  | Permanently set |
| 4  | Permanently reset |

JLine considers the mode supported when `Ps` is 1, 2, or 3.

## Terminal Compatibility

Mode 2027 support varies across terminal emulators:

| Terminal Emulator | Mode 2027 Support |
|-------------------|-------------------|
| Contour           | Yes |
| foot              | Yes |
| WezTerm           | Yes |
| Ghostty           | Yes |
| iTerm2            | No  |
| macOS Terminal.app | No  |
| GNOME Terminal    | No  |
| Windows Terminal  | No  |
| Konsole           | No  |
| xterm             | No  |

Since JLine probes for support at runtime via DECRQM, it correctly detects whether the terminal actually supports mode 2027 regardless of the reported terminal type.

## Best Practices

1. **Always disable before exiting**: If you enable grapheme cluster mode, disable it before your application exits to leave the terminal in a clean state.

2. **Don't assume support from terminal type**: Many terminals report `xterm-256color` but don't support mode 2027. Always use `supportsGraphemeClusterMode()` which probes the terminal.

3. **Graceful fallback**: Applications should work correctly without mode 2027. Use it as an enhancement for better emoji and complex script rendering when available.
