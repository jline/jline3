---
sidebar_position: 2
---

# Code Snippets System

The JLine documentation uses a code snippet system that extracts code examples from actual working code in the project. This ensures that all code examples in the documentation are accurate, up-to-date, and compilable.

## How It Works

The snippet system works as follows:

1. Example classes in the `demo/src/main/java/org/jline/demo/examples` directory are marked with special comments
2. During the build process, snippets are extracted from these classes and saved as Markdown code blocks directly to the `static/snippets` directory
3. The `CodeSnippet` component loads the snippet file at runtime and displays it as a code block

## Using Snippets in Documentation

To include a code snippet in your documentation, use the following syntax:

```jsx
import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="ExampleClassName" />
```

Note: You only need to include the import statement once per file, at the top of the file.

## Creating New Snippets

To add a new snippet:

1. Create a new Java class in the `demo/src/main/java/org/jline/demo/examples` directory
2. Name the class appropriately (e.g., `MyFeatureExample.java`)
3. Mark the snippet section with `SNIPPET_START` and `SNIPPET_END` comments
4. Use the class name as the snippet name in the comments

Example:

```java
package org.jline.demo.examples;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating a feature.
 */
public class MyFeatureExample {
    // SNIPPET_START: MyFeatureExample
    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Your example code here

        String line = reader.readLine("prompt> ");
        System.out.println("You entered: " + line);
    }
    // SNIPPET_END: MyFeatureExample
}
```

## Highlighting Code

You can highlight specific parts of your code using special comments:

### Highlighting a Single Line

```java
// HIGHLIGHT: This line will be highlighted
reader.printAbove("This line will be highlighted");
```

### Highlighting a Block of Code

```java
// HIGHLIGHT_START: Create a PrintAboveWriter
// Create a PrintAboveWriter
PrintAboveWriter writer = new PrintAboveWriter(reader);
// HIGHLIGHT_END
```

## Error Highlighting

Similarly, you can mark code as erroneous:

### Marking a Single Line as Error

```java
// ERROR: This line contains an error
reader.printAbove(123); // Error: incompatible types
```

### Marking a Block as Error

```java
// ERROR_START: This block contains errors
String result = reader.readLine();
int value = result; // Error: incompatible types
// ERROR_END
```

## Build Process

The website build process includes the following steps:

1. Extract snippets from the example classes directly to the static directory
2. Build the website

To build the website with snippets:

```bash
./build-website.sh
```

To preview the website with snippets:

```bash
cd website
./scripts/preview-website.sh
```

## Best Practices

1. **Create Runnable Examples**: Make sure your example classes are runnable and demonstrate real use cases
2. **Keep Examples Focused**: Each example should demonstrate a single feature or concept
3. **Use Descriptive Names**: Name your classes and snippets descriptively
4. **Add Javadoc**: Include Javadoc comments to explain what the example demonstrates
5. **Use Highlighting**: Highlight the most important parts of the code
6. **Test Your Examples**: Ensure your examples compile and run correctly
