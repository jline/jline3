#!/usr/bin/env node

/**
 * This script validates that all code snippets referenced in markdown files exist.
 * It should be run as part of the build process to catch missing snippets early.
 *
 * Usage: node validate-snippets.js <docs-dir> <snippets-dir>
 *
 * Example: node validate-snippets.js ./docs ./target/static/snippets
 */

const fs = require('fs');
const path = require('path');

// Helper function to ensure directory exists
function ensureDirectoryExists(dirPath) {
  if (!fs.existsSync(dirPath)) {
    console.error(`Directory does not exist: ${dirPath}`);
    process.exit(1);
  }
  return dirPath;
}

// Check arguments
if (process.argv.length < 4) {
  console.error('Usage: node validate-snippets.js <docs-dir> <snippets-dir>');
  process.exit(1);
}

const docsDir = process.argv[2];
const snippetsDir = process.argv[3];

// Ensure directories exist
ensureDirectoryExists(docsDir);
ensureDirectoryExists(snippetsDir);

// Find all markdown files in the docs directory
function findMarkdownFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      findMarkdownFiles(filePath, fileList);
    } else if (file.endsWith('.md') || file.endsWith('.mdx')) {
      fileList.push(filePath);
    }
  });

  return fileList;
}

// Extract CodeSnippet component references from markdown files
function extractSnippetReferences(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const regex = /<CodeSnippet\s+name=["']([^"']+)["']\s*\/>/g;
  const references = [];
  let match;

  // Skip validation for documentation examples
  if (filePath.includes('contributing/code-snippets.md') ||
      filePath.includes('examples/README.md')) {
    return [];
  }

  while ((match = regex.exec(content)) !== null) {
    references.push({
      name: match[1],
      file: filePath
    });
  }

  return references;
}

// Check if a snippet exists
function snippetExists(name) {
  const snippetPath = path.join(snippetsDir, `${name}.java`);
  return fs.existsSync(snippetPath);
}

// Main function
function main() {
  const markdownFiles = findMarkdownFiles(docsDir);
  console.log(`Found ${markdownFiles.length} markdown files`);

  let allReferences = [];
  markdownFiles.forEach(file => {
    const references = extractSnippetReferences(file);
    allReferences = allReferences.concat(references);
  });

  console.log(`Found ${allReferences.length} snippet references`);

  // Check if all referenced snippets exist
  const missingSnippets = allReferences.filter(ref => !snippetExists(ref.name));

  if (missingSnippets.length > 0) {
    console.error('\nERROR: The following snippets are referenced but do not exist:');
    missingSnippets.forEach(ref => {
      console.error(`- "${ref.name}" referenced in ${ref.file}`);
    });
    process.exit(1);
  } else {
    console.log('All referenced snippets exist!');
  }
}

main();
