#!/bin/bash

set -e

# Extract code snippets from example classes
echo "Extracting code snippets..."
node scripts/extract-snippets.js ../demo/src/main/java/org/jline/demo/examples ./snippets

# Copy snippets to the static directory so they can be loaded by the CodeSnippet component
echo "Copying snippets to static directory..."
mkdir -p ./static/snippets
cp ./snippets/* ./static/snippets/

# Start the development server
echo "Starting development server..."
npm start

# Clean up (this will only run if the server is stopped with Ctrl+C)
echo "Cleaning up..."
rm -rf ./snippets
