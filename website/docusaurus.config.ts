import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import path from 'path';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

// JLine version - update this when releasing a new version
const jlineVersion = '3.30.0';

// Define build output directory - only used for production builds
// We'll set this via CLI for development server
const outputDir = path.join(process.cwd(), 'target/build');

// Check if we're in build mode (not dev server)
const isBuild = process.argv.includes('build');

const config: Config = {
  customFields: {
    jlineVersion: jlineVersion,
    // Store output directory in customFields for reference
    ...(isBuild && {outDir: outputDir}),
  },

  // Configure static directories
  staticDirectories: ['static', 'target/static'],
  title: 'JLine',
  tagline: 'Advanced Console Input for Java',
  favicon: 'img/favicon.ico',

  // Set the production url of your site here
  url: 'https://jline.org',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'jline', // Usually your GitHub org/user name.
  projectName: 'jline3', // Usually your repo name.

  onBrokenLinks: 'warn', // Changed from 'throw' to 'warn' to allow build to complete
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          path: './docs',
          sidebarPath: './sidebars.ts',
          // The path to the processed docs that will be used for the build
          // This will be populated by the build script
          routeBasePath: 'docs',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/jline/jline3/edit/master/website/',
        },
        blog: false, // blog section disabled
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    // Replace with your project's social card
    image: 'img/docusaurus-social-card.jpg',
    navbar: {
      title: 'JLine',
      logo: {
        alt: 'JLine Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          to: '/javadoc',
          label: 'Javadoc',
          position: 'left',
        },
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Documentation',
        },
        // blog link removed
        {
          href: 'https://github.com/jline/jline3',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/intro',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Stack Overflow',
              href: 'https://stackoverflow.com/questions/tagged/jline',
            },
            {
              label: 'GitHub Issues',
              href: 'https://github.com/jline/jline3/issues',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/jline/jline3',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} JLine. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'bash', 'diff', 'json'],
      defaultLanguage: 'java',
      magicComments: [
        // Remember to extend the default highlight class name as well!
        {
          className: 'theme-code-block-highlighted-line',
          line: 'highlight-next-line',
          block: {start: 'highlight-start', end: 'highlight-end'},
        },
        {
          className: 'code-block-error-line',
          line: 'error-next-line',
          block: {start: 'error-start', end: 'error-end'},
        },
      ],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
