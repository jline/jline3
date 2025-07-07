#!/bin/bash

# Script to fix all builder classes by removing console-ui dependencies

cd /Users/gnodet/work/git/jline3-bis

# Fix Java 8 compatibility - replace List.of() with Collections.emptyList()
find prompt/src -name "*.java" -exec sed -i '' 's/List\.of()/Collections.emptyList()/g' {} \;

# Add Collections import where needed
find prompt/src -name "*.java" -exec grep -l "Collections.emptyList" {} \; | while read file; do
    if ! grep -q "import java.util.Collections" "$file"; then
        sed -i '' '/import java.util.List;/a\
import java.util.Collections;' "$file"
    fi
done

echo "Fixed Java 8 compatibility issues"
