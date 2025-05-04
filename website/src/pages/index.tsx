import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import Heading from '@theme/Heading';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className="hero__title">
          {siteConfig.title}
        </Heading>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/intro">
            Get Started with JLine
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - Advanced Console Input for Java`}
      description="JLine is a Java library that brings advanced console input handling capabilities to your applications.">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
        <div className="container margin-vert--xl">
          <div className="row">
            <div className="col col--8 col--offset-2">
              <div className="text--center margin-bottom--lg">
                <Heading as="h2">See It in Action</Heading>
                <p>Watch JLine's capabilities in this interactive demo:</p>
              </div>
              <div className="text--center">
                <a href="https://asciinema.org/a/683979" target="_blank" rel="noopener noreferrer">
                  <img src="https://asciinema.org/a/683979.svg" alt="JLine gogo demo" width="100%" />
                </a>
              </div>
            </div>
          </div>
        </div>
      </main>
    </Layout>
  );
}
