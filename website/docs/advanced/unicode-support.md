---
sidebar_position: 6
---

# Unicode and Wide Character Support

JLine provides robust support for Unicode and wide characters, allowing you to create applications that work with international text and special symbols. This guide covers how to handle Unicode characters, wide characters (like CJK characters), and emoji in your JLine applications.

## Unicode Basics in JLine

JLine is built on Java's Unicode support, which means it can handle the full range of Unicode characters:

```java title="UnicodeBasicsExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class UnicodeBasicsExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display Unicode examples
        terminal.writer().println("JLine Unicode Support Examples:");
        terminal.writer().println();
        
        // highlight-start
        // Basic Latin and Latin-1 Supplement
        terminal.writer().println("Latin characters: Hello, World! Café Français");
        
        // Greek
        terminal.writer().println("Greek: Γειά σου Κόσμε");
        
        // Cyrillic
        terminal.writer().println("Cyrillic: Привет, мир");
        
        // Hebrew
        terminal.writer().println("Hebrew: שלום עולם");
        
        // Arabic
        terminal.writer().println("Arabic: مرحبا بالعالم");
        
        // CJK (Chinese, Japanese, Korean)
        terminal.writer().println("Chinese: 你好，世界");
        terminal.writer().println("Japanese: こんにちは世界");
        terminal.writer().println("Korean: 안녕하세요 세계");
        
        // Emoji
        terminal.writer().println("Emoji: 😀 🚀 🌍 🎉 👍");
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Try typing some Unicode characters:");
        terminal.writer().flush();
        
        // Read a line with Unicode support
        String line = reader.readLine("unicode> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Wide Characters

Wide characters, such as CJK (Chinese, Japanese, Korean) characters, take up two columns in the terminal. JLine handles these characters correctly for cursor positioning and line editing:

```java title="WideCharactersExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.WCWidth;

import java.io.IOException;

public class WideCharactersExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display wide character examples
        terminal.writer().println("Wide Character Support in JLine:");
        terminal.writer().println();
        
        // highlight-start
        // Examples of wide characters
        String[] examples = {
            "你好",         // Chinese
            "こんにちは",   // Japanese
            "안녕하세요",   // Korean
            "你好，世界！", // Chinese with punctuation
            "Hello 世界",   // Mixed Latin and CJK
            "🀄 🈴 🈺 🉐"   // Wide emoji and symbols
        };
        
        // Display examples with character width information
        for (String example : examples) {
            terminal.writer().println("Text: " + example);
            
            terminal.writer().print("Width: ");
            int totalWidth = 0;
            for (int i = 0; i < example.length(); i++) {
                char c = example.charAt(i);
                int width = WCWidth.wcwidth(c);
                totalWidth += width;
                terminal.writer().print(width + " ");
            }
            terminal.writer().println("\nTotal display width: " + totalWidth);
            terminal.writer().println();
        }
        // highlight-end
        
        terminal.writer().println("Try typing some wide characters:");
        terminal.writer().flush();
        
        // Read a line with wide character support
        String line = reader.readLine("wide> ");
        
        // Display the entered text with width information
        terminal.writer().println("You entered: " + line);
        terminal.writer().print("Character widths: ");
        for (int i = 0; i < line.length(); i++) {
            terminal.writer().print(WCWidth.wcwidth(line.charAt(i)) + " ");
        }
        terminal.writer().println();
        
        terminal.close();
    }
}
```

## Emoji Support

JLine can handle emoji characters, which are becoming increasingly common in modern applications:

```java title="EmojiSupportExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmojiSupportExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display emoji examples
        terminal.writer().println("Emoji Support in JLine:");
        terminal.writer().println();
        
        // highlight-start
        // Common emoji categories
        Map<String, String[]> emojiCategories = new HashMap<>();
        emojiCategories.put("Smileys", new String[] {"😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇"});
        emojiCategories.put("People", new String[] {"👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👋", "🖐️"});
        emojiCategories.put("Animals", new String[] {"🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯"});
        emojiCategories.put("Food", new String[] {"🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍈", "🍒"});
        emojiCategories.put("Travel", new String[] {"🚗", "✈️", "🚀", "🛸", "🚂", "🚢", "🚁", "🛵", "🚲", "🛴"});
        emojiCategories.put("Objects", new String[] {"⌚", "📱", "💻", "⌨️", "🖥️", "🖱️", "🖨️", "🖋️", "📞", "📟"});
        emojiCategories.put("Symbols", new String[] {"❤️", "💔", "💯", "✅", "❌", "⭕", "🔴", "🟠", "🟡", "🟢"});
        emojiCategories.put("Flags", new String[] {"🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏴‍☠️", "🇺🇸", "🇬🇧", "🇯🇵"});
        
        // Display emoji by category
        for (Map.Entry<String, String[]> entry : emojiCategories.entrySet()) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(AttributedStyle.DEFAULT.bold())
               .append(entry.getKey())
               .append(": ")
               .style(AttributedStyle.DEFAULT);
            
            for (String emoji : entry.getValue()) {
                asb.append(emoji + " ");
            }
            
            asb.toAttributedString().println(terminal);
        }
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Try typing some emoji:");
        terminal.writer().flush();
        
        // Read a line with emoji support
        String line = reader.readLine("emoji> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Bidirectional Text

JLine can handle bidirectional text (like Arabic and Hebrew), but there are some limitations:

```java title="BidirectionalTextExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class BidirectionalTextExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display bidirectional text examples
        terminal.writer().println("Bidirectional Text Support in JLine:");
        terminal.writer().println();
        
        // highlight-start
        // Examples of bidirectional text
        terminal.writer().println("Hebrew: שלום עולם (Hello World)");
        terminal.writer().println("Arabic: مرحبا بالعالم (Hello World)");
        terminal.writer().println("Mixed LTR and RTL: Hello שלום");
        terminal.writer().println("Mixed RTL and LTR: שלום Hello");
        terminal.writer().println("Complex mixed: Hello, שלום! مرحبا - World");
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Note: JLine displays bidirectional text, but editing may not");
        terminal.writer().println("behave as expected due to terminal limitations.");
        terminal.writer().println();
        terminal.writer().println("Try typing some bidirectional text:");
        terminal.writer().flush();
        
        // Read a line with bidirectional text support
        String line = reader.readLine("bidi> ");
        terminal.writer().println("You entered: " + line);
        
        terminal.close();
    }
}
```

## Character Width Calculation

JLine provides utilities for calculating the display width of characters:

```java title="CharacterWidthExample.java" showLineNumbers
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.WCWidth;

import java.io.IOException;

public class CharacterWidthExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display character width examples
        terminal.writer().println("Character Width Calculation in JLine:");
        terminal.writer().println();
        
        // highlight-start
        // Examples of characters with different widths
        String[] examples = {
            "A",            // Latin letter (width 1)
            "Á",            // Latin letter with accent (width 1)
            "あ",           // Hiragana (width 2)
            "漢",           // Kanji (width 2)
            "👍",           // Emoji (width 2)
            "👨‍👩‍👧‍👦",     // Complex emoji with ZWJ (width 2)
            "\u0000",       // Null character (width 0)
            "\u200B",       // Zero-width space (width 0)
            "\t",           // Tab (special handling)
            "\n"            // Newline (special handling)
        };
        
        // Display width information for each example
        for (String example : examples) {
            terminal.writer().print("Character: ");
            if (example.equals("\u0000")) {
                terminal.writer().print("\\u0000 (null)");
            } else if (example.equals("\u200B")) {
                terminal.writer().print("\\u200B (zero-width space)");
            } else if (example.equals("\t")) {
                terminal.writer().print("\\t (tab)");
            } else if (example.equals("\n")) {
                terminal.writer().print("\\n (newline)");
            } else {
                terminal.writer().print(example);
            }
            
            terminal.writer().print(", Code points: ");
            example.codePoints().forEach(cp -> terminal.writer().print("U+" + Integer.toHexString(cp).toUpperCase() + " "));
            
            terminal.writer().print(", Width: ");
            if (example.length() == 1) {
                terminal.writer().println(WCWidth.wcwidth(example.charAt(0)));
            } else {
                terminal.writer().print("(String) ");
                int width = 0;
                for (int i = 0; i < example.length(); i++) {
                    width += WCWidth.wcwidth(example.charAt(i));
                }
                terminal.writer().println(width);
            }
        }
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Enter some text to calculate its display width:");
        terminal.writer().flush();
        
        // Read a line and calculate its display width
        String line = reader.readLine("width> ");
        
        // Calculate the display width
        int displayWidth = 0;
        for (int i = 0; i < line.length(); i++) {
            displayWidth += WCWidth.wcwidth(line.charAt(i));
        }
        
        terminal.writer().println("You entered: " + line);
        terminal.writer().println("Display width: " + displayWidth + " columns");
        
        terminal.close();
    }
}
```

## Unicode Normalization

When working with Unicode, it's important to understand normalization forms:

```java title="UnicodeNormalizationExample.java"
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

public class UnicodeNormalizationExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        
        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        
        // Display Unicode normalization examples
        terminal.writer().println("Unicode Normalization Examples:");
        terminal.writer().println();
        
        // highlight-start
        // Example of composed and decomposed characters
        String composed = "é"; // U+00E9: LATIN SMALL LETTER E WITH ACUTE
        String decomposed = "e\u0301"; // U+0065 U+0301: LATIN SMALL LETTER E + COMBINING ACUTE ACCENT
        
        terminal.writer().println("Visually identical characters can have different representations:");
        terminal.writer().println("  Composed (é): U+00E9");
        terminal.writer().println("  Decomposed (é): U+0065 U+0301");
        terminal.writer().println();
        
        terminal.writer().println("Comparison without normalization:");
        terminal.writer().println("  composed.equals(decomposed): " + composed.equals(decomposed));
        terminal.writer().println("  composed.length(): " + composed.length());
        terminal.writer().println("  decomposed.length(): " + decomposed.length());
        terminal.writer().println();
        
        // Normalize to different forms
        String nfc = Normalizer.normalize(decomposed, Form.NFC); // Canonical composition
        String nfd = Normalizer.normalize(composed, Form.NFD);   // Canonical decomposition
        
        terminal.writer().println("After normalization:");
        terminal.writer().println("  NFC (decomposed): " + nfc);
        terminal.writer().println("  NFD (composed): " + nfd);
        terminal.writer().println("  nfc.equals(composed): " + nfc.equals(composed));
        terminal.writer().println("  nfd.equals(decomposed): " + nfd.equals(decomposed));
        // highlight-end
        
        terminal.writer().println();
        terminal.writer().println("Enter text with accented characters to normalize:");
        terminal.writer().flush();
        
        // Read a line and show normalization
        String line = reader.readLine("normalize> ");
        
        terminal.writer().println("You entered: " + line);
        terminal.writer().println("NFC form: " + Normalizer.normalize(line, Form.NFC));
        terminal.writer().println("NFD form: " + Normalizer.normalize(line, Form.NFD));
        
        terminal.close();
    }
}
```

## Terminal Encoding

Terminal encoding is important for proper Unicode display:

```java title="TerminalEncodingExample.java"
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.Charset;

public class TerminalEncodingExample {
    public static void main(String[] args) throws IOException {
        // highlight-start
        // Create a terminal with explicit encoding
        Terminal terminal = TerminalBuilder.builder()
                .encoding(Charset.forName("UTF-8"))
                .build();
        // highlight-end
        
        // Display encoding information
        terminal.writer().println("Terminal Encoding Information:");
        terminal.writer().println();
        terminal.writer().println("Default charset: " + Charset.defaultCharset().name());
        terminal.writer().println("Terminal encoding: " + terminal.encoding().name());
        terminal.writer().println("File.encoding property: " + System.getProperty("file.encoding"));
        terminal.writer().println();
        
        // Display Unicode examples to verify encoding
        terminal.writer().println("Unicode display test:");
        terminal.writer().println("  Latin: é è ê ë ñ ß");
        terminal.writer().println("  Greek: α β γ δ ε ζ");
        terminal.writer().println("  Cyrillic: а б в г д е");
        terminal.writer().println("  CJK: 你好 こんにちは 안녕하세요");
        terminal.writer().println("  Emoji: 😀 🚀 🌍");
        terminal.writer().println();
        
        terminal.writer().println("Press Enter to exit...");
        terminal.writer().flush();
        terminal.reader().readLine();
        
        terminal.close();
    }
}
```

## Best Practices

When working with Unicode and wide characters in JLine, consider these best practices:

1. **Use UTF-8 Encoding**: Always use UTF-8 encoding for your terminal to ensure proper Unicode support.

2. **Calculate Display Width Correctly**: Use `WCWidth.wcwidth()` to calculate the display width of characters.

3. **Handle Combining Characters**: Be aware of combining characters and use normalization when comparing strings.

4. **Test with Various Scripts**: Test your application with various scripts, including Latin, CJK, Arabic, and Hebrew.

5. **Consider Terminal Limitations**: Some terminals may have limited Unicode support or display issues with certain characters.

6. **Be Careful with Bidirectional Text**: Editing bidirectional text can be challenging due to terminal limitations.

7. **Use Proper Line Breaking**: Consider proper line breaking for different scripts, especially for CJK characters.

8. **Handle Zero-Width Characters**: Be aware of zero-width characters that can affect display but not width calculations.

9. **Test Emoji Rendering**: Test emoji rendering in different terminals, as support can vary.

10. **Consider Font Support**: Ensure the terminal font supports the Unicode characters you're using.
