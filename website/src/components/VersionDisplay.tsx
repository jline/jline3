import React from 'react';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';

export default function VersionDisplay(): JSX.Element {
  const { siteConfig } = useDocusaurusContext();
  const jlineVersion = siteConfig.customFields.jlineVersion as string;
  
  return (
    <span>{jlineVersion}</span>
  );
}
