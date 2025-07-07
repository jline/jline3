#!/bin/bash

# Script to fix List.of() calls for Java 8 compatibility

cd /Users/gnodet/work/git/jline3-bis

# Replace List.of( with Arrays.asList(
find prompt/src -name "*.java" -exec sed -i '' 's/List\.of(/Arrays.asList(/g' {} \;

# Add Arrays import where needed
find prompt/src -name "*.java" -exec grep -l "Arrays.asList" {} \; | while read file; do
    if ! grep -q "import java.util.Arrays" "$file"; then
        sed -i '' '/import java.util.ArrayList;/a\
import java.util.Arrays;' "$file"
    fi
done

echo "Fixed List.of() calls for Java 8 compatibility"
