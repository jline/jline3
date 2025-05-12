#!/usr/bin/env node

/**
 * This script replaces version placeholders in the built documentation files
 * with the actual version from docusaurus.config.ts.
 * 
 * Usage: node replace-version.js [--source] [--revert]
 * 
 * Options:
 *   --source  Replace placeholders in source files (default: false)
 *   --revert  Revert changes in source files (only works with --source)
 */

const fs = require('fs');
const path = require('path');
const replace = require('replace-in-file');

// Parse command line arguments
const args = process.argv.slice(2);
const sourceMode = args.includes('--source');
const revertMode = args.includes('--revert');

// Only allow revert in source mode
if (revertMode && !sourceMode) {
  console.error('Error: --revert can only be used with --source');
  process.exit(1);
}

// Get the JLine version from docusaurus.config.ts
function getJLineVersion() {
  const configPath = path.join(process.cwd(), 'docusaurus.config.ts');
  const configContent = fs.readFileSync(configPath, 'utf8');
  
  // Extract the version using regex
  const versionMatch = configContent.match(/const\s+jlineVersion\s*=\s*['"]([^'"]+)['"]/);
  
  if (!versionMatch) {
    console.error('Error: Could not find jlineVersion in docusaurus.config.ts');
    process.exit(1);
  }
  
  return versionMatch[1];
}

// Define the placeholder
const VERSION_PLACEHOLDER = '%%JLINE_VERSION%%';

// Get the JLine version
const jlineVersion = getJLineVersion();
console.log(`JLine version: ${jlineVersion}`);

// Define the files to process
const sourceFiles = [
  'docs/**/*.md',
  'docs/**/*.mdx',
  'src/pages/**/*.md',
  'src/pages/**/*.mdx',
  'blog/**/*.md',
  'blog/**/*.mdx',
];

// Define the target directory for build mode
const targetDir = path.join(process.cwd(), 'target/build');

// Define the files to process in build mode
const buildFiles = [
  `${targetDir}/**/*.html`,
];

// Determine which files to process
const filesToProcess = sourceMode ? sourceFiles : buildFiles;

// Define the replacement options
const options = {
  files: filesToProcess,
  from: sourceMode && revertMode 
    ? new RegExp(jlineVersion, 'g') 
    : new RegExp(VERSION_PLACEHOLDER, 'g'),
  to: sourceMode && revertMode 
    ? VERSION_PLACEHOLDER 
    : jlineVersion,
  countMatches: true,
};

// Perform the replacement
try {
  const results = replace.sync(options);
  
  // Count total replacements
  let totalReplacements = 0;
  let filesChanged = 0;
  
  results.forEach(result => {
    if (result.hasChanged) {
      filesChanged++;
      totalReplacements += result.numReplacements;
      console.log(`Modified: ${result.file} (${result.numReplacements} replacements)`);
    }
  });
  
  console.log(`\nSummary: ${totalReplacements} replacements in ${filesChanged} files`);
  
  if (sourceMode) {
    if (revertMode) {
      console.log('\nReverted version placeholders in source files.');
    } else {
      console.log('\nReplaced version placeholders in source files.');
      console.log('Note: Run with --source --revert to revert these changes.');
    }
  } else {
    console.log('\nReplaced version placeholders in build files.');
  }
} catch (error) {
  console.error('Error occurred:', error);
  process.exit(1);
}
