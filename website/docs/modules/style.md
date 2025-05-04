---
sidebar_position: 3
---

# JLine Style

The `jline-style` module provides a powerful styling API for terminal output. It allows you to define and apply styles to text, create color schemes, and maintain consistent styling across your application.

## Maven Dependency

To use the style module, add the following dependency to your project:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline-style</artifactId>
    <version>3.29.0</version>
</dependency>
```

## Basic Styling

The style module provides a high-level API for styling text:

```java title="BasicStylingExample.java" showLineNumbers
import org.jline.style.StyleExpression;
import org.jline.style.StyleResolver;
import org.jline.style.Styler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import java.io.IOException;
import java.io.PrintWriter;

public class BasicStylingExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        PrintWriter writer = terminal.writer();

        // highlight-start
        // Create a styler
        Styler styler = Styler.defaultStyler();

        // Style text using style expressions
        AttributedString styledText = styler.style("This is @{bold,fg:red}red bold text@{} and this is @{italic,fg:blue}blue italic text@{}.");
        // highlight-end

        // Print the styled text
        styledText.println(terminal);
        writer.flush();
    }
}
```

## Style Expressions

Style expressions use a simple syntax to define text styles:

```
@{style-attributes}text@{}
```

Where `style-attributes` is a comma-separated list of style attributes:

```java title="StyleExpressionExample.java"
import org.jline.style.Styler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class StyleExpressionExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Styler styler = Styler.defaultStyler();

        // highlight-start
        // Basic foreground and background colors
        styler.style("@{fg:red}Red text@{}").println(terminal);
        styler.style("@{bg:blue}Blue background@{}").println(terminal);

        // Text attributes
        styler.style("@{bold}Bold text@{}").println(terminal);
        styler.style("@{italic}Italic text@{}").println(terminal);
        styler.style("@{underline}Underlined text@{}").println(terminal);
        styler.style("@{blink}Blinking text@{}").println(terminal);
        styler.style("@{inverse}Inverse text@{}").println(terminal);

        // Combining attributes
        styler.style("@{bold,fg:green,bg:black}Bold green text on black background@{}").println(terminal);
        // highlight-end

        // Named styles (defined in the styler)
        styler.style("@{error}Error message@{}").println(terminal);
        styler.style("@{warning}Warning message@{}").println(terminal);
        styler.style("@{info}Info message@{}").println(terminal);

        terminal.flush();
    }
}
```

## Style Resolver

The `StyleResolver` allows you to define named styles and resolve style expressions:

```java title="StyleResolverExample.java" showLineNumbers
import org.jline.style.StyleExpression;
import org.jline.style.StyleResolver;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StyleResolverExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Create a map of named styles
        Map<String, AttributedStyle> styles = new HashMap<>();
        styles.put("error", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
        styles.put("warning", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        styles.put("info", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
        styles.put("success", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

        // Create a style resolver with the named styles
        StyleResolver resolver = new StyleResolver(styles);
        // highlight-end

        // Resolve style expressions
        AttributedString errorMessage = resolver.resolve("@{error}Something went wrong!@{}");
        AttributedString warningMessage = resolver.resolve("@{warning}Be careful!@{}");
        AttributedString infoMessage = resolver.resolve("@{info}Just so you know...@{}");
        AttributedString successMessage = resolver.resolve("@{success}Operation completed successfully.@{}");

        // Print the styled messages
        errorMessage.println(terminal);
        warningMessage.println(terminal);
        infoMessage.println(terminal);
        successMessage.println(terminal);

        // Combine named styles with inline styles
        AttributedString combinedStyle = resolver.resolve(
                "@{error}Error:@{} @{bold,fg:white}Cannot open file @{italic}'example.txt'@{}");
        combinedStyle.println(terminal);

        terminal.flush();
    }
}
```

## Style Configuration

You can load style definitions from configuration files:

```java title="StyleConfigurationExample.java"
import org.jline.style.StyleExpression;
import org.jline.style.StyleResolver;
import org.jline.style.Styler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class StyleConfigurationExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // highlight-start
        // Define styles in a properties format
        String styleConfig =
                "error = fg:red,bold\n" +
                "warning = fg:yellow\n" +
                "info = fg:blue\n" +
                "success = fg:green\n" +
                "header = fg:cyan,underline\n" +
                "highlight = bg:yellow,fg:black";

        // Load the styles
        Properties props = new Properties();
        props.load(new StringReader(styleConfig));

        // Create a styler with the loaded styles
        Styler styler = Styler.create(props);
        // highlight-end

        // Use the configured styles
        styler.style("@{header}System Information@{}").println(terminal);
        styler.style("@{info}OS: @{highlight}Linux@{}").println(terminal);
        styler.style("@{info}User: @{highlight}admin@{}").println(terminal);
        styler.style("@{success}All systems operational@{}").println(terminal);

        terminal.flush();
    }
}
```

## Styling Tables and Structured Output

The style module works well with structured output like tables:

```java title="StyledTableExample.java" showLineNumbers
import org.jline.builtins.Tables;
import org.jline.builtins.Tables.Column;
import org.jline.builtins.Tables.ColumnType;
import org.jline.style.Styler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class StyledTableExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Define styles
        Properties styleProps = new Properties();
        styleProps.setProperty("header", "fg:cyan,bold");
        styleProps.setProperty("error", "fg:red");
        styleProps.setProperty("warning", "fg:yellow");
        styleProps.setProperty("ok", "fg:green");

        Styler styler = Styler.create(styleProps);

        // Define table columns
        List<Column> columns = Arrays.asList(
                new Column("Service", ColumnType.String),
                new Column("Status", ColumnType.String),
                new Column("Message", ColumnType.String)
        );

        // Create table data with styled content
        List<List<AttributedString>> data = new ArrayList<>();
        data.add(Arrays.asList(
                new AttributedString("Database"),
                styler.style("@{ok}Running@{}"),
                new AttributedString("Connected to MySQL 8.0")
        ));
        data.add(Arrays.asList(
                new AttributedString("Web Server"),
                styler.style("@{warning}Degraded@{}"),
                new AttributedString("High load detected")
        ));
        data.add(Arrays.asList(
                new AttributedString("Cache"),
                styler.style("@{error}Down@{}"),
                new AttributedString("Connection refused")
        ));

        // Build and display the table
        Tables.TableBuilder tableBuilder = new Tables.TableBuilder(columns);
        tableBuilder.addAllAttributedString(data);

        Tables.Table table = tableBuilder.build();
        AttributedString tableString = table.toAttributedString(
                terminal.getWidth(),
                true,  // display borders
                true   // display header
        );

        tableString.println(terminal);
        terminal.flush();
    }
}
```

## Styling Progress Indicators

You can use the style module to create styled progress indicators:

```java title="StyledProgressExample.java"
import org.jline.style.Styler;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;

import java.io.IOException;

public class StyledProgressExample {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();
        Styler styler = Styler.defaultStyler();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // highlight-start
        // Create a styled progress bar
        for (int i = 0; i <= 100; i++) {
            // Calculate progress bar width
            int width = 50;
            int completed = width * i / 100;

            // Build the progress bar
            AttributedStringBuilder builder = new AttributedStringBuilder();
            builder.append("\r");
            builder.append(styler.style("@{bold}Progress: @{fg:green}["));

            // Completed portion
            builder.append(styler.style("@{fg:green}" + "=".repeat(completed)));

            // Remaining portion
            builder.append(styler.style("@{fg:black,bg:white}" + " ".repeat(width - completed)));

            builder.append(styler.style("@{fg:green}]@{} "));
            builder.append(styler.style("@{bold}" + i + "%"));

            // Print the progress bar
            terminal.writer().print(builder.toAnsi(terminal));
            terminal.flush();

            // Simulate work
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // highlight-end

        terminal.writer().println();
        terminal.writer().println(styler.style("@{bold,fg:green}Complete!@{}"));
        terminal.flush();
    }
}
```

## Best Practices

When using the JLine style module, consider these best practices:

1. **Define a Consistent Style Palette**: Create a set of named styles for your application and use them consistently.

2. **Load Styles from Configuration**: Allow users to customize styles by loading style definitions from configuration files.

3. **Use Semantic Style Names**: Name styles based on their semantic meaning (e.g., "error", "warning", "info") rather than their appearance.

4. **Check Terminal Capabilities**: Some terminals may not support all styling features, so check capabilities before using advanced styles.

5. **Provide Fallbacks**: Define fallback styles for terminals with limited capabilities.

6. **Separate Style from Content**: Keep style definitions separate from content to make it easier to change the appearance without modifying the code.

7. **Use Style Expressions for Complex Styling**: Style expressions provide a concise way to apply multiple style attributes.

8. **Consider Accessibility**: Choose colors and styles that are accessible to users with visual impairments.
