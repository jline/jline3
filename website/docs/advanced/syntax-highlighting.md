---
sidebar_position: 1
---

# Syntax Highlighting

JLine provides powerful syntax highlighting capabilities that can enhance the user experience of your command-line application.

## Basic Highlighting

To add syntax highlighting to your `LineReader`, you need to implement the `Highlighter` interface:

```java
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

// Create a simple highlighter
Highlighter highlighter = (reader, buffer, candidates) -> {
    // Create a highlighted version of the buffer
    AttributedString highlighted = new AttributedStringBuilder()
            .append(buffer.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .toAttributedString();
    
    // Add the highlighted buffer to the candidates list
    candidates.add(highlighted);
    
    // Return the highlighted buffer
    return highlighted;
};

// Create a line reader with the highlighter
Terminal terminal = TerminalBuilder.builder().build();
LineReader reader = LineReaderBuilder.builder()
        .terminal(terminal)
        .highlighter(highlighter)
        .build();

// Now when the user types, the input will be highlighted in blue
String line = reader.readLine("prompt> ");
```

## Syntax-Aware Highlighting

For more sophisticated highlighting, you can create a highlighter that understands your command syntax:

```java
public class CommandHighlighter implements Highlighter {
    private static final AttributedStyle COMMAND_STYLE = AttributedStyle.BOLD.foreground(AttributedStyle.RED);
    private static final AttributedStyle OPTION_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
    private static final AttributedStyle ARG_STYLE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    
    private final Set<String> commands = Set.of("help", "list", "add", "remove", "exit");
    private final Set<String> options = Set.of("-v", "--verbose", "-h", "--help", "-f", "--force");
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        
        // Simple parsing for demonstration
        String[] words = buffer.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            
            if (i > 0) {
                builder.append(" ");
            }
            
            if (i == 0 && commands.contains(word)) {
                // First word is a command
                builder.append(word, COMMAND_STYLE);
            } else if (options.contains(word)) {
                // Word is an option
                builder.append(word, OPTION_STYLE);
            } else {
                // Word is an argument
                builder.append(word, ARG_STYLE);
            }
        }
        
        return builder.toAttributedString();
    }
    
    @Override
    public void setErrorPattern(Pattern pattern) {
        // Not used in this example
    }
    
    @Override
    public void setErrorIndex(int errorIndex) {
        // Not used in this example
    }
}
```

## Highlighting with Regular Expressions

You can use regular expressions for more flexible highlighting:

```java
public class RegexHighlighter implements Highlighter {
    private final List<Pair<Pattern, AttributedStyle>> patterns = new ArrayList<>();
    
    public RegexHighlighter() {
        // Add patterns with corresponding styles
        patterns.add(new Pair<>(Pattern.compile("\\b(help|exit|list|add|remove)\\b"), 
                               AttributedStyle.BOLD.foreground(AttributedStyle.RED)));
        patterns.add(new Pair<>(Pattern.compile("\\b(\\d+)\\b"), 
                               AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)));
        patterns.add(new Pair<>(Pattern.compile("\\b(true|false)\\b"), 
                               AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)));
        patterns.add(new Pair<>(Pattern.compile("\"([^\"]*)\""), 
                               AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA)));
    }
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedString result = new AttributedString(buffer);
        
        for (Pair<Pattern, AttributedStyle> pattern : patterns) {
            Matcher matcher = pattern.getLeft().matcher(buffer);
            while (matcher.find()) {
                result = result.styleMatches(matcher, pattern.getRight());
            }
        }
        
        return result;
    }
    
    @Override
    public void setErrorPattern(Pattern pattern) {
        // Not used in this example
    }
    
    @Override
    public void setErrorIndex(int errorIndex) {
        // Not used in this example
    }
}
```

## Error Highlighting

JLine can highlight syntax errors:

```java
public class ErrorHighlighter implements Highlighter {
    private Pattern errorPattern;
    private int errorIndex = -1;
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.append(buffer);
        
        // Highlight error if present
        if (errorIndex >= 0 && errorIndex < buffer.length()) {
            builder.styleAt(errorIndex, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        }
        
        // Highlight pattern matches
        if (errorPattern != null) {
            Matcher matcher = errorPattern.matcher(buffer);
            while (matcher.find()) {
                builder.styleMatches(matcher, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            }
        }
        
        return builder.toAttributedString();
    }
    
    @Override
    public void setErrorPattern(Pattern pattern) {
        this.errorPattern = pattern;
    }
    
    @Override
    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }
}
```

## Advanced Highlighting Techniques

### Incremental Highlighting

For better performance with long input:

```java
public class IncrementalHighlighter implements Highlighter {
    private AttributedString lastHighlighted;
    private String lastBuffer = "";
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        // If the buffer hasn't changed, return the cached result
        if (buffer.equals(lastBuffer) && lastHighlighted != null) {
            return lastHighlighted;
        }
        
        // Perform highlighting
        AttributedStringBuilder builder = new AttributedStringBuilder();
        // ... highlighting logic ...
        
        // Cache the result
        lastBuffer = buffer;
        lastHighlighted = builder.toAttributedString();
        
        return lastHighlighted;
    }
    
    // Other methods...
}
```

### Context-Aware Highlighting

Create highlighters that are aware of the current context:

```java
public class ContextAwareHighlighter implements Highlighter {
    private final Map<String, Highlighter> contextHighlighters = new HashMap<>();
    
    public ContextAwareHighlighter() {
        contextHighlighters.put("sql", new SqlHighlighter());
        contextHighlighters.put("java", new JavaHighlighter());
        contextHighlighters.put("default", new DefaultHighlighter());
    }
    
    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        // Get current context from reader variables
        String context = (String) reader.getVariable("SYNTAX_CONTEXT");
        if (context == null) {
            context = "default";
        }
        
        // Use the appropriate highlighter for this context
        Highlighter contextHighlighter = contextHighlighters.getOrDefault(context, 
                                                                        contextHighlighters.get("default"));
        return contextHighlighter.highlight(reader, buffer);
    }
    
    // Other methods...
}
```

## Best Practices

- Keep highlighting logic simple and efficient
- Use caching for complex highlighting patterns
- Consider the context when highlighting
- Use consistent colors for similar elements
- Test highlighting with various input scenarios
- Provide a way to disable highlighting for users who prefer plain text
- Consider accessibility when choosing colors
