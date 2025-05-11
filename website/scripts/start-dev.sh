#!/bin/bash

set -e

# Test the MDX plugin
echo "Testing MDX plugin..."
./scripts/test-plugin.sh

# Extract code snippets from example classes directly to the static directory
echo "Extracting code snippets..."
mkdir -p ./static/snippets
node scripts/extract-snippets.js ../demo/src/main/java/org/jline/demo/examples ./static/snippets

# Start the development server
echo "Starting development server..."
npm start
