# JLine Documentation Examples

This package contains example classes that are used in the JLine documentation.
These examples are designed to be both runnable and extractable for documentation purposes.

## How to Use

### Adding a New Example

1. Create a new Java class in this package
2. Add the necessary imports and code
3. Mark the snippet sections with special comments:

```java
// SNIPPET_START: snippet-name
public class MyExample {
    // Your code here
    
    // HIGHLIGHT: This line will be highlighted in the docs
    
    // HIGHLIGHT_START: This block will be highlighted
    // Your code here
    // HIGHLIGHT_END
}
// SNIPPET_END: snippet-name
```

### Using Snippets in Documentation

In your Markdown documentation files, use the following syntax to include a snippet:

```markdown
<CodeSnippet name="snippet-name" />
```

The build process will automatically replace this placeholder with the actual code snippet.

## Build Process

The website build process includes the following steps:

1. Extract snippets from the example classes
2. Process documentation files to include the snippets
3. Build the website

This ensures that the code examples in the documentation are always up-to-date and compilable.

## Running the Examples

Most of the examples in this package are runnable. You can run them directly from your IDE or using the command line:

```bash
mvn exec:java -Dexec.mainClass="org.jline.demo.examples.PrintAboveExample"
```

Note that some examples may run indefinitely and require manual termination.
