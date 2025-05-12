#!/usr/bin/env node

/**
 * This script extracts code snippets from Java source files and saves them
 * to a directory for use in the documentation.
 *
 * Usage: node extract-snippets.js <source-dir> <output-dir>
 *
 * Example: node extract-snippets.js ../demo/src/main/java/org/jline/demo/examples ./snippets
 */

const fs = require('fs');
const path = require('path');

// Helper function to ensure target directory exists
function ensureDirectoryExists(dirPath) {
  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }
  return dirPath;
}

// Check arguments
if (process.argv.length < 4) {
  console.error('Usage: node extract-snippets.js <source-dir> <output-dir>');
  process.exit(1);
}

const sourceDir = process.argv[2];
let outputDir = process.argv[3];

// If the output directory is a relative path like './target/static/snippets',
// handle it appropriately
if (outputDir.startsWith('./') || !path.isAbsolute(outputDir)) {
  // Get the base directory (website folder)
  const baseDir = process.cwd();

  // Check if the path already includes 'target'
  if (outputDir.includes('target')) {
    // Path already has 'target', use as is but make it absolute
    outputDir = path.resolve(baseDir, outputDir);
  } else if (outputDir.startsWith('./static')) {
    // For './static/...' paths, redirect to './target/static/...'
    const targetDir = path.join(baseDir, 'target');
    outputDir = path.join(targetDir, 'static', outputDir.substring(8)); // Remove './static/' prefix
  } else {
    // For other relative paths, put them in the target directory
    const targetDir = path.join(baseDir, 'target');
    outputDir = path.join(targetDir, outputDir);
  }
}

// Create output directory if it doesn't exist
ensureDirectoryExists(outputDir);

// Find all Java files in the source directory
function findJavaFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      findJavaFiles(filePath, fileList);
    } else if (file.endsWith('.java')) {
      fileList.push(filePath);
    }
  });

  return fileList;
}

// Extract snippets from a Java file
function extractSnippets(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const snippets = {};

  // Find all snippet blocks
  const snippetRegex = /SNIPPET_START:\s*([a-zA-Z0-9-_]+)([\s\S]*?)SNIPPET_END:\s*\1/g;
  let match;

  // Get the class name from the file path
  const className = path.basename(filePath, '.java');

  while ((match = snippetRegex.exec(content)) !== null) {
    // Use the class name as the snippet name if it matches
    const snippetName = match[1];
    let snippetContent = match[2];

    // Process highlight markers
    snippetContent = snippetContent.replace(/\/\/\s*HIGHLIGHT:\s*(.*)/g, '// highlight-next-line\n$1');
    snippetContent = snippetContent.replace(/\/\/\s*HIGHLIGHT_START:?\s*(.*)/g, '// highlight-start');
    snippetContent = snippetContent.replace(/\/\/\s*HIGHLIGHT_END/g, '// highlight-end');

    // Process error markers
    snippetContent = snippetContent.replace(/\/\/\s*ERROR:\s*(.*)/g, '// error-next-line\n$1');
    snippetContent = snippetContent.replace(/\/\/\s*ERROR_START:?\s*(.*)/g, '// error-start');
    snippetContent = snippetContent.replace(/\/\/\s*ERROR_END/g, '// error-end');

    // Remove trailing comment marker (// SNIPPET_END: xxx)
    snippetContent = snippetContent.replace(/\/\/[^\n]*$/g, '');

    // Normalize indentation
    snippetContent = normalizeIndentation(snippetContent);

    snippets[snippetName] = {
      content: snippetContent.trim(),
      className: className
    };
  }

  return snippets;
}

// Normalize indentation by removing the indentation of the first non-empty line from all lines
function normalizeIndentation(code) {
  // Split into lines
  const lines = code.split('\n');

  // Find the first non-empty line
  const firstNonEmptyLineIndex = lines.findIndex(line => line.trim().length > 0);

  if (firstNonEmptyLineIndex === -1) {
    // All lines are empty
    return code;
  }

  // Get the indentation of the first non-empty line
  const firstLineIndent = lines[firstNonEmptyLineIndex].match(/^\s*/)[0].length;

  // Remove the first line's indentation from all lines
  if (firstLineIndent > 0) {
    return lines
      .map(line => {
        // If the line has at least as much indentation as the first line, remove it
        // Otherwise, keep the line as is (this preserves empty lines and lines with less indentation)
        if (line.length >= firstLineIndent && line.substring(0, firstLineIndent).trim() === '') {
          return line.substring(firstLineIndent);
        }
        return line;
      })
      .join('\n');
  }

  return code;
}

// Save snippets to output directory
function saveSnippets(snippets, className) {
  Object.entries(snippets).forEach(([name, data]) => {
    const outputPath = path.join(outputDir, `${name}.java`);
    const content = `\`\`\`java title="${data.className}.java"\n${data.content}\n\`\`\``;

    fs.writeFileSync(outputPath, content);
    console.log(`Saved snippet ${name} to ${outputPath}`);
  });
}

// Main function
function main() {
  const javaFiles = findJavaFiles(sourceDir);

  javaFiles.forEach(file => {
    const className = path.basename(file, '.java');
    const snippets = extractSnippets(file);

    if (Object.keys(snippets).length > 0) {
      saveSnippets(snippets, className);
    }
  });

  console.log(`Processed ${javaFiles.length} Java files`);
}

main();
