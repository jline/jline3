#!/bin/bash

set -e

# Extract code snippets from example classes
echo "Extracting code snippets..."
cd website
node scripts/extract-snippets.js ../demo/src/main/java/org/jline/demo/examples ./snippets

# Copy snippets to the static directory so they can be loaded by the CodeSnippet component
echo "Copying snippets to static directory..."
mkdir -p ./static/snippets
cp ./snippets/* ./static/snippets/

# Build the website
echo "Building website..."
npm install
npm run build

echo "Website built successfully in website/build directory"
echo "To preview the website, run: cd website && npm run serve"
