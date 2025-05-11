import React, { useEffect } from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';

export default function JavadocPage(): JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const jlineVersion = siteConfig.customFields.jlineVersion as string;

  // Redirect to the Maven Central Javadoc
  const javadocUrl = `https://javadoc.io/doc/org.jline/jline/${jlineVersion}/index.html`;

  useEffect(() => {
    // Use window.location for a proper external redirect
    window.location.href = javadocUrl;
  }, [javadocUrl]);

  return (
    <Layout
      title="Javadoc"
      description="JLine API Documentation">
      <div style={{ padding: '2rem', textAlign: 'center' }}>
        <p>Redirecting to Javadoc...</p>
        <p>
          <a href={javadocUrl}>Click here if you are not redirected automatically</a>
        </p>
      </div>
    </Layout>
  );
}
