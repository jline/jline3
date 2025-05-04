#!/usr/bin/env node

/**
 * This script generates a temporary Docusaurus configuration file
 * that uses the processed docs directory.
 * 
 * Usage: node generate-config.js <input-config> <output-config> <processed-docs-dir>
 * 
 * Example: node generate-config.js ./docusaurus.config.ts ./docusaurus.config.build.ts ./processed-docs
 */

const fs = require('fs');
const path = require('path');

// Check arguments
if (process.argv.length < 5) {
  console.error('Usage: node generate-config.js <input-config> <output-config> <processed-docs-dir>');
  process.exit(1);
}

const inputConfigPath = process.argv[2];
const outputConfigPath = process.argv[3];
const processedDocsDir = process.argv[4];

// Read the input configuration
let configContent = fs.readFileSync(inputConfigPath, 'utf8');

// Replace the docs path with the processed docs path
configContent = configContent.replace(
  /path:\s*['"]\.\/docs['"]/,
  `path: './${processedDocsDir}'`
);

// Write the output configuration
fs.writeFileSync(outputConfigPath, configContent);

console.log(`Generated temporary configuration at ${outputConfigPath}`);
