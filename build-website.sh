#!/bin/bash

set -e

# Change to website directory
cd website

# Install dependencies
echo "Installing dependencies..."
npm install

# Build the website (this will extract snippets automatically)
echo "Building website..."
npm run build

echo "Website built successfully in website/build directory"
echo "To preview the website, run: cd website && npm run serve"
