#!/usr/bin/env node

/**
 * This script processes documentation files and replaces snippet placeholders
 * with the actual snippets, creating new processed files without modifying the originals.
 *
 * Usage: node process-docs.js <docs-dir> <snippets-dir> <output-dir>
 *
 * Example: node process-docs.js ./docs ./snippets ./processed-docs
 */

const fs = require('fs');
const path = require('path');

// Check arguments
if (process.argv.length < 5) {
  console.error('Usage: node process-docs.js <docs-dir> <snippets-dir> <output-dir>');
  process.exit(1);
}

const docsDir = process.argv[2];
const snippetsDir = process.argv[3];
const outputDir = process.argv[4];

// Create output directory if it doesn't exist
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// Find all markdown files in the docs directory
function findMarkdownFiles(dir, fileList = [], baseDir = dir) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      // Create the corresponding directory in the output
      const relativePath = path.relative(baseDir, filePath);
      const outputPath = path.join(outputDir, relativePath);
      if (!fs.existsSync(outputPath)) {
        fs.mkdirSync(outputPath, { recursive: true });
      }

      findMarkdownFiles(filePath, fileList, baseDir);
    } else if (file.endsWith('.md')) {
      fileList.push({
        inputPath: filePath,
        relativePath: path.relative(baseDir, filePath)
      });
    }
  });

  return fileList;
}

// Process a markdown file
function processMarkdownFile(fileInfo) {
  const { inputPath, relativePath } = fileInfo;
  const outputPath = path.join(outputDir, relativePath);

  let content = fs.readFileSync(inputPath, 'utf8');
  let modified = false;

  // Find all snippet components
  const placeholderRegex = /<CodeSnippet\s+name=["']([a-zA-Z0-9-_]+)["']\s*\/>/g;
  let match;
  let lastIndex = 0;
  let processedContent = '';

  while ((match = placeholderRegex.exec(content)) !== null) {
    // Add the content before the placeholder
    processedContent += content.substring(lastIndex, match.index);
    lastIndex = match.index + match[0].length;

    const snippetName = match[1];
    const snippetPath = path.join(snippetsDir, `${snippetName}.java`);

    if (fs.existsSync(snippetPath)) {
      const snippetContent = fs.readFileSync(snippetPath, 'utf8');
      processedContent += snippetContent;
      modified = true;
      console.log(`Replaced snippet ${snippetName} in ${outputPath}`);
    } else {
      // If snippet not found, keep the placeholder
      processedContent += match[0];
      console.warn(`Warning: Snippet ${snippetName} not found for ${inputPath}`);
    }
  }

  // Add any remaining content after the last placeholder
  processedContent += content.substring(lastIndex);

  // Write the processed content to the output file
  fs.writeFileSync(outputPath, modified ? processedContent : content);

  return modified;
}

// Copy non-markdown files
function copyNonMarkdownFiles(dir, baseDir = dir) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      const relativePath = path.relative(baseDir, filePath);
      const outputPath = path.join(outputDir, relativePath);
      if (!fs.existsSync(outputPath)) {
        fs.mkdirSync(outputPath, { recursive: true });
      }

      copyNonMarkdownFiles(filePath, baseDir);
    } else if (!file.endsWith('.md')) {
      const relativePath = path.relative(baseDir, filePath);
      const outputPath = path.join(outputDir, relativePath);
      fs.copyFileSync(filePath, outputPath);
    }
  });
}

// Main function
function main() {
  const markdownFiles = findMarkdownFiles(docsDir);
  let modifiedCount = 0;

  markdownFiles.forEach(fileInfo => {
    if (processMarkdownFile(fileInfo)) {
      modifiedCount++;
    }
  });

  // Copy non-markdown files
  copyNonMarkdownFiles(docsDir);

  console.log(`Processed ${markdownFiles.length} markdown files, modified ${modifiedCount}`);
}

main();
