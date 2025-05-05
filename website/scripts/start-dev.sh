#!/bin/bash

set -e

# Extract code snippets from example classes directly to the static directory
echo "Extracting code snippets..."
mkdir -p ./static/snippets
node scripts/extract-snippets.js ../demo/src/main/java/org/jline/demo/examples ./static/snippets

# Start the development server
echo "Starting development server..."
npm start
