#!/bin/bash

set -e

# Change to website directory
cd website

# Install dependencies
echo "Installing dependencies..."
npm install

# Test the MDX plugin
echo "Testing MDX plugin..."
./scripts/test-plugin.sh

# Build the website (this will extract snippets automatically)
echo "Building website..."
npm run build

echo "Website built successfully in website/build directory"
echo "To preview the website, run: cd website && npm run serve"
