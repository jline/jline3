import React, { useEffect } from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import { Redirect } from '@docusaurus/router';

export default function JavadocPage(): JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const jlineVersion = siteConfig.customFields.jlineVersion as string;
  
  // Redirect to the Maven Central Javadoc
  const javadocUrl = `https://javadoc.io/doc/org.jline/jline/${jlineVersion}/index.html`;
  
  return (
    <Layout
      title="Javadoc"
      description="JLine API Documentation">
      <Redirect to={javadocUrl} />
    </Layout>
  );
}
