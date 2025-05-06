---
sidebar_position: 6
---

# Style Module

import CodeSnippet from '@site/src/components/CodeSnippet';

The JLine Style module provides a powerful and flexible way to add colors and formatting to your command-line applications. It includes support for style expressions, style resolvers, and stylers.

## Basic Styling

The Style module allows you to add colors and formatting to text:

<CodeSnippet name="BasicStylingExample" />

## Style Expressions

Style expressions provide a concise way to define styles:

<CodeSnippet name="StyleExpressionExample" />

## Style Resolver

The StyleResolver resolves style expressions to AttributedStyle objects:

<CodeSnippet name="StyleResolverExample" />

## Styler

The Styler combines style expressions and text styling:

<CodeSnippet name="StylerExample" />

## Best Practices

When using the JLine Style module, consider these best practices:

1. **Consistent Styling**: Maintain a consistent style throughout your application.

2. **Named Styles**: Define named styles for common elements like errors, warnings, and information messages.

3. **Color Accessibility**: Consider color blindness and other accessibility issues when choosing colors.

4. **Terminal Capabilities**: Check terminal capabilities before using advanced styling features.

5. **Style Reuse**: Reuse styles to maintain consistency and reduce code duplication.

6. **Style Documentation**: Document your application's style conventions.

7. **Style Customization**: Allow users to customize styles to match their preferences.

8. **Style Fallbacks**: Provide fallback styles for terminals that don't support certain features.

9. **Style Testing**: Test your styles in different terminal environments.

10. **Style Performance**: Be mindful of performance when applying styles to large amounts of text.
