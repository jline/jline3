import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import CodeBlock from '@theme/CodeBlock';
import { useEffect, useState } from 'react';

// This component loads and displays a code snippet
export default function CodeSnippet({ name }) {
  const [snippet, setSnippet] = useState(null);
  const [error, setError] = useState(null);
  const { siteConfig } = useDocusaurusContext();

  useEffect(() => {
    // In development mode, try to load the snippet from the snippets directory
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
    return (
      <div className="code-snippet-error">
        <p>{error}</p>
      </div>
    );
  }

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
