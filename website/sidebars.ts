import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    'intro',
    'architecture',
    'terminal',
    'line-reader',
    'tab-completion',
    'history',
    'remote-terminals',
    'troubleshooting',
    {
      type: 'category',
      label: 'Examples',
      items: [
        'examples/print-above',
      ],
    },
    {
      type: 'category',
      label: 'Contributing',
      items: [
        'contributing/code-snippets',
      ],
    },
    {
      type: 'category',
      label: 'Advanced Features',
      items: [
        'advanced/syntax-highlighting',
        'advanced/interactive-features',
        'advanced/non-blocking-input',
        'advanced/terminal-attributes',
        'advanced/attributed-strings',
        'advanced/key-bindings',
        'advanced/widgets',
        'advanced/mouse-support',
        'advanced/terminal-size',
        'advanced/screen-clearing',
        'advanced/library-integration',
        'advanced/autosuggestions',
        'advanced/auto-indentation-pairing',
        'advanced/theme-system',
        'advanced/nano-less-customization',
      ],
    },
    {
      type: 'category',
      label: 'Modules',
      items: [
        'modules/overview',
        'modules/builtins',
        'modules/style',
        'modules/terminal-providers',
        'modules/repl-console',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      items: [
        'api/overview',
        // These will link to JavaDoc
        // 'api/terminal',
        // 'api/line-reader',
        // 'api/completer',
        // 'api/history'
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      items: [
        'reference/glossary',
        'reference/diagnostic',
      ],
    },
  ],
};

export default sidebars;
