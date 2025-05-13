import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import CodeBlock from '@theme/CodeBlock';
import { useEffect, useState } from 'react';
import ExecutionEnvironment from '@docusaurus/ExecutionEnvironment';

// This component loads and displays a code snippet
export default function CodeSnippet({ name }) {
  const [snippet, setSnippet] = useState(null);
  const [error, setError] = useState(null);
  const { siteConfig } = useDocusaurusContext();

  useEffect(() => {
    if (!ExecutionEnvironment.canUseDOM) {
      // Skip client-side fetching during SSR
      return;
    }

    // In browser, load the snippet from the snippets directory
    fetch(`/snippets/${name}.java`)
      .then(response => {
        if (!response.ok) {
          throw new Error(`Failed to load snippet: ${name}`);
        }
        return response.text();
      })
      .then(text => {
        // Extract the code from the markdown code block
        const match = text.match(/```java.*?\n([\s\S]*?)\n```/);
        if (match && match[1]) {
          // Clean up the snippet
          let code = match[1];
          // Remove any trailing comment markers
          code = code.replace(/\/\/[^\n]*$/g, '');
          // Trim any trailing whitespace
          code = code.trim();
          setSnippet(code);
        } else {
          setSnippet(text);
        }
      })
      .catch(err => {
        console.error(err);
        setError(`Failed to load snippet: ${name}`);
      });
  }, [name]);

  if (error) {
    // Display the error in the UI and also throw it to make debugging easier
    const errorMessage = `Failed to load snippet: ${name}`;
    return (
      <div className="code-snippet-error" style={{ color: 'red', padding: '1rem', border: '1px solid red', borderRadius: '4px', marginBottom: '1rem' }}>
        <h3>Error Loading Code Snippet</h3>
        <p>{errorMessage}</p>
        <p>This error should have been caught during build time. Please check your build process.</p>
      </div>
    );
  }

  // During SSR or while loading, show a placeholder
  if (!snippet) {
    return (
      <div className="code-snippet-loading">
        <p>Loading snippet: {name}...</p>
      </div>
    );
  }

  return (
    <CodeBlock language="java" title={`${name}.java`}>
      {snippet}
    </CodeBlock>
  );
}
