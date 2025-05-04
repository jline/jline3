---
sidebar_position: 4
---

# Terminal Attributes and Modes

JLine provides fine-grained control over terminal attributes and modes, allowing you to customize how the terminal behaves. Understanding these attributes is essential for creating sophisticated terminal applications.

## Terminal Attributes

Terminal attributes control various aspects of terminal behavior, such as input processing, output processing, and control characters.

```java title="TerminalAttributesExample.java" showLineNumbers
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TerminalAttributesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // highlight-start
            // Get current terminal attributes
            Attributes attributes = terminal.getAttributes();
            // highlight-end
            
            // Display current attributes
            terminal.writer().println("Current terminal attributes:");
            terminal.writer().println("  ECHO: " + attributes.getInputFlag(InputFlag.ECHO));
            terminal.writer().println("  ICANON: " + attributes.getInputFlag(InputFlag.ICANON));
            terminal.writer().println("  ICRNL: " + attributes.getInputFlag(InputFlag.ICRNL));
            terminal.writer().println("  INLCR: " + attributes.getInputFlag(InputFlag.INLCR));
            terminal.writer().println("  ISIG: " + attributes.getInputFlag(InputFlag.ISIG));
            terminal.writer().println("  OPOST: " + attributes.getOutputFlag(OutputFlag.OPOST));
            terminal.writer().println("  ONLCR: " + attributes.getOutputFlag(OutputFlag.ONLCR));
            terminal.writer().println("  OCRNL: " + attributes.getOutputFlag(OutputFlag.OCRNL));
            terminal.writer().println("  IEXTEN: " + attributes.getLocalFlag(LocalFlag.IEXTEN));
            terminal.writer().println("  VEOF: " + attributes.getControlChar(ControlChar.VEOF));
            terminal.writer().println("  VERASE: " + attributes.getControlChar(ControlChar.VERASE));
            terminal.writer().println("  VINTR: " + attributes.getControlChar(ControlChar.VINTR));
            terminal.writer().println("  VSUSP: " + attributes.getControlChar(ControlChar.VSUSP));
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Input Flags

Input flags control how input is processed:

```java title="InputFlagsExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class InputFlagsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();
            
            // Display current input flags
            terminal.writer().println("Current input flags:");
            
            // highlight-start
            // Common input flags
            for (InputFlag flag : new InputFlag[] {
                    InputFlag.IGNBRK,  // Ignore break condition
                    InputFlag.BRKINT,  // Map break to interrupt
                    InputFlag.IGNPAR,  // Ignore parity errors
                    InputFlag.PARMRK,  // Mark parity errors
                    InputFlag.INPCK,   // Enable parity check
                    InputFlag.ISTRIP,  // Strip 8th bit
                    InputFlag.INLCR,   // Map NL to CR
                    InputFlag.IGNCR,   // Ignore CR
                    InputFlag.ICRNL,   // Map CR to NL
                    InputFlag.IXON,    // Enable XON/XOFF flow control
                    InputFlag.IXOFF,   // Enable sending XON/XOFF
                    InputFlag.IXANY,   // Any character restarts output
                    InputFlag.IMAXBEL, // Ring bell on input queue full
                    InputFlag.IUCLC,   // Map uppercase to lowercase
                    InputFlag.ECHO,    // Echo input characters
                    InputFlag.ECHOE,   // Echo erase character as BS-SP-BS
                    InputFlag.ECHOK,   // Echo NL after kill character
                    InputFlag.ECHONL,  // Echo NL
                    InputFlag.NOFLSH,  // Disable flush after interrupt
                    InputFlag.TOSTOP,  // Stop background jobs that write to terminal
                    InputFlag.IEXTEN,  // Enable extended functions
                    InputFlag.ECHOCTL, // Echo control characters in hat notation
                    InputFlag.ECHOKE,  // BS-SP-BS entire line on line kill
                    InputFlag.PENDIN,  // Retype pending input at next read
                    InputFlag.ICANON,  // Enable canonical mode
                    InputFlag.ISIG,    // Enable signals
            }) {
                try {
                    boolean value = attributes.getInputFlag(flag);
                    terminal.writer().println("  " + flag + ": " + value);
                } catch (Exception e) {
                    terminal.writer().println("  " + flag + ": unsupported");
                }
            }
            // highlight-end
            
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Output Flags

Output flags control how output is processed:

```java title="OutputFlagsExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class OutputFlagsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();
            
            // Display current output flags
            terminal.writer().println("Current output flags:");
            
            // highlight-start
            // Common output flags
            for (OutputFlag flag : new OutputFlag[] {
                    OutputFlag.OPOST,  // Post-process output
                    OutputFlag.ONLCR,  // Map NL to CR-NL
                    OutputFlag.OCRNL,  // Map CR to NL
                    OutputFlag.ONOCR,  // Don't output CR at column 0
                    OutputFlag.ONLRET, // NL performs CR function
                    OutputFlag.OFILL,  // Use fill characters for delay
                    OutputFlag.OFDEL,  // Fill is DEL
                    OutputFlag.NLDLY,  // NL delay
                    OutputFlag.CRDLY,  // CR delay
                    OutputFlag.TABDLY, // Tab delay
                    OutputFlag.BSDLY,  // Backspace delay
                    OutputFlag.VTDLY,  // Vertical tab delay
                    OutputFlag.FFDLY,  // Form feed delay
            }) {
                try {
                    int value = attributes.getOutputFlag(flag);
                    terminal.writer().println("  " + flag + ": " + value);
                } catch (Exception e) {
                    terminal.writer().println("  " + flag + ": unsupported");
                }
            }
            // highlight-end
            
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Control Characters

Control characters define special characters that have specific functions:

```java title="ControlCharsExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class ControlCharsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();
            
            // Display current control characters
            terminal.writer().println("Current control characters:");
            
            // highlight-start
            // Common control characters
            for (ControlChar cc : new ControlChar[] {
                    ControlChar.VEOF,    // EOF character (usually Ctrl-D)
                    ControlChar.VEOL,    // EOL character
                    ControlChar.VERASE,  // Erase character (usually Backspace)
                    ControlChar.VINTR,   // Interrupt character (usually Ctrl-C)
                    ControlChar.VKILL,   // Kill character (usually Ctrl-U)
                    ControlChar.VMIN,    // Minimum number of characters for non-canonical read
                    ControlChar.VQUIT,   // Quit character (usually Ctrl-\\)
                    ControlChar.VSTART,  // Start character (usually Ctrl-Q)
                    ControlChar.VSTOP,   // Stop character (usually Ctrl-S)
                    ControlChar.VSUSP,   // Suspend character (usually Ctrl-Z)
                    ControlChar.VTIME,   // Timeout in deciseconds for non-canonical read
            }) {
                try {
                    int value = attributes.getControlChar(cc);
                    terminal.writer().println("  " + cc + ": " + value + 
                            (value > 0 ? " ('" + (char)value + "')" : ""));
                } catch (Exception e) {
                    terminal.writer().println("  " + cc + ": unsupported");
                }
            }
            // highlight-end
            
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Terminal Modes

JLine supports different terminal modes that affect how input and output are processed.

### Canonical Mode vs. Raw Mode

The most important distinction is between canonical mode and raw mode:

```java title="TerminalModesExample.java" showLineNumbers
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class TerminalModesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Save original attributes
            Attributes originalAttributes = terminal.getAttributes();
            
            // Display current mode
            boolean canonicalMode = originalAttributes.getInputFlag(InputFlag.ICANON);
            terminal.writer().println("Terminal is currently in " + 
                    (canonicalMode ? "canonical" : "raw") + " mode");
            terminal.writer().flush();
            
            // highlight-start
            // Switch to raw mode
            Attributes rawAttributes = new Attributes(originalAttributes);
            rawAttributes.setInputFlag(InputFlag.ICANON, false); // Disable canonical mode
            rawAttributes.setInputFlag(InputFlag.ECHO, false);   // Disable echo
            rawAttributes.setInputFlag(InputFlag.ISIG, false);   // Disable signals
            rawAttributes.setInputFlag(InputFlag.IEXTEN, false); // Disable extended functions
            
            // Set control characters for non-canonical mode
            rawAttributes.setControlChar(Attributes.ControlChar.VMIN, 1);  // Read at least 1 character
            rawAttributes.setControlChar(Attributes.ControlChar.VTIME, 0); // No timeout
            
            // Apply raw mode attributes
            terminal.setAttributes(rawAttributes);
            // highlight-end
            
            terminal.writer().println("Switched to raw mode. Press any key to continue...");
            terminal.writer().flush();
            
            // Read a single character
            int c = terminal.reader().read();
            
            // Restore original attributes
            terminal.setAttributes(originalAttributes);
            
            terminal.writer().println("\nSwitched back to canonical mode");
            terminal.writer().println("You pressed: " + (char)c + " (ASCII: " + c + ")");
            terminal.writer().flush();
            
            terminal.writer().println("\nPress Enter to exit...");
            terminal.writer().flush();
            terminal.reader().readLine();
        } finally {
            terminal.close();
        }
    }
}
```

### Mode Comparison

| Feature | Canonical Mode | Raw Mode |
|---------|---------------|----------|
| Input Processing | Line-buffered (until Enter) | Character-by-character |
| Special Characters | Processed (Ctrl+C, Ctrl+Z, etc.) | Not processed (unless configured) |
| Echoing | Enabled by default | Disabled by default |
| Backspace | Processed | Not processed |
| Line Editing | Enabled | Disabled |
| Use Case | Command-line interfaces | Interactive applications, games |

## Customizing Terminal Behavior

You can customize terminal behavior by modifying attributes:

```java title="CustomTerminalBehaviorExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class CustomTerminalBehaviorExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // Save original attributes
            Attributes originalAttributes = terminal.getAttributes();
            
            // highlight-start
            // Create custom attributes
            Attributes customAttributes = new Attributes(originalAttributes);
            
            // Customize input behavior
            customAttributes.setInputFlag(InputFlag.ECHO, true);     // Enable echo
            customAttributes.setInputFlag(InputFlag.ICANON, true);   // Enable canonical mode
            customAttributes.setInputFlag(InputFlag.ICRNL, true);    // Map CR to NL
            
            // Customize control characters
            customAttributes.setControlChar(ControlChar.VINTR, 3);   // Set Ctrl+C as interrupt
            customAttributes.setControlChar(ControlChar.VEOF, 4);    // Set Ctrl+D as EOF
            customAttributes.setControlChar(ControlChar.VSUSP, 26);  // Set Ctrl+Z as suspend
            
            // Apply custom attributes
            terminal.setAttributes(customAttributes);
            // highlight-end
            
            terminal.writer().println("Terminal configured with custom attributes");
            terminal.writer().println("Type some text and press Enter (Ctrl+D to exit):");
            terminal.writer().flush();
            
            // Read lines until EOF
            String line;
            while ((line = terminal.reader().readLine()) != null) {
                terminal.writer().println("You typed: " + line);
                terminal.writer().println("Type another line (Ctrl+D to exit):");
                terminal.writer().flush();
            }
            
            // Restore original attributes
            terminal.setAttributes(originalAttributes);
        } finally {
            terminal.close();
        }
    }
}
```

## Saving and Restoring Attributes

It's important to save and restore terminal attributes:

```java title="SaveRestoreAttributesExample.java"
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class SaveRestoreAttributesExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        try {
            // highlight-start
            // Save original attributes
            Attributes originalAttributes = terminal.getAttributes();
            // highlight-end
            
            terminal.writer().println("Original terminal attributes saved");
            terminal.writer().println("Temporarily changing terminal attributes...");
            terminal.writer().flush();
            
            // Change attributes (enter raw mode)
            terminal.enterRawMode();
            
            terminal.writer().println("Terminal is now in raw mode");
            terminal.writer().println("Press any key to restore original attributes...");
            terminal.writer().flush();
            
            // Wait for a keypress
            terminal.reader().read();
            
            // highlight-start
            // Restore original attributes
            terminal.setAttributes(originalAttributes);
            // highlight-end
            
            terminal.writer().println("\nOriginal terminal attributes restored");
            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
}
```

## Best Practices

When working with terminal attributes and modes, consider these best practices:

1. **Always Save Original Attributes**: Save the original terminal attributes before making changes.

2. **Always Restore Attributes**: Restore the original attributes before exiting, even in error cases.

3. **Use try-finally Blocks**: Ensure attributes are restored even if exceptions occur.

4. **Document Mode Changes**: Clearly document when your application changes terminal modes.

5. **Provide User Feedback**: Inform users when the terminal mode changes.

6. **Check Terminal Capabilities**: Not all terminals support all attributes and modes.

7. **Test on Different Platforms**: Terminal behavior can vary across platforms.

8. **Use Helper Methods**: Use helper methods like `enterRawMode()` when available.

9. **Consider User Experience**: Choose modes appropriate for your application's user experience.

10. **Handle Signals Appropriately**: Be careful when disabling signal processing (ISIG flag).
