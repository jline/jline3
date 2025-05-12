# Documentation Examples

This directory contains documentation pages that use code snippets extracted from actual working code.

## How to Use Snippets

To include a code snippet in your documentation, use the following syntax:

```jsx
import CodeSnippet from '@site/src/components/CodeSnippet';

<CodeSnippet name="snippet-name" />
```

Note: You only need to include the import statement once per file, at the top of the file.

The build process will automatically replace this placeholder with the actual code snippet.

## Available Snippets

Snippets are extracted from the example classes in the `demo/src/main/java/org/jline/demo/examples` directory. Each snippet is marked with special comments in the source code:

```java
// SNIPPET_START: snippet-name
public class MyExample {
    // Your code here
}
// SNIPPET_END: snippet-name
```

## Adding New Snippets

To add a new snippet:

1. Create or modify a class in the `demo/src/main/java/org/jline/demo/examples` directory
2. Mark the snippet section with `SNIPPET_START` and `SNIPPET_END` comments
3. Use the snippet in your documentation with the `<CodeSnippet name="snippet-name" />` component

## How It Works

The snippet system works as follows:

1. Example classes in the `demo/src/main/java/org/jline/demo/examples` directory are marked with `SNIPPET_START` and `SNIPPET_END` comments
2. During the build process, snippets are extracted from these classes and saved as Markdown code blocks directly to the `static/snippets` directory
3. The `CodeSnippet` component loads the snippet file at runtime and displays it as a code block

This approach ensures that:
- Code examples in the documentation are always up-to-date with the actual code
- The original documentation files remain unchanged
- The build process is clean and repeatable

## Preview the Website

To preview the website with the processed documentation:

```bash
cd website
./scripts/preview-website.sh
```

This will start a development server with the processed documentation.
