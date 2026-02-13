<!--
Copyright (C) 2026 the original author(s).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# JLine Provider Loading Mechanism

This directory contains provider-specific resource files used for runtime loading of terminal providers.

## Directory Structure

```
META-INF/jline/providers/
├── exec    - ExecTerminalProvider
└── dumb    - DumbTerminalProvider
```

Additional providers are defined in other modules:
- `terminal-ffm/META-INF/jline/providers/ffm` - FfmTerminalProvider
- `terminal-jni/META-INF/jline/providers/jni` - JniTerminalProvider

## Purpose

These files enable loading terminal providers by name without instantiating all available providers.
This is critical because some providers (JNI, FFM) may fail to initialize due to missing native
libraries or platform-specific dependencies.

## Relationship to META-INF/services

JLine maintains two parallel service registration mechanisms:

1. **META-INF/services/org.jline.terminal.spi.TerminalProvider** (Standard Java SPI)
   - Used by jlink and JPMS module tools
   - Required for proper module dependency resolution
   - **NOT used at runtime by JLine**

2. **META-INF/jline/providers/{name}** (JLine-specific)
   - Used at runtime by `TerminalProvider.load(String name)`
   - Allows on-demand loading of specific providers
   - Avoids instantiation failures from unavailable providers

## File Format

Provider files follow standard Java SPI format:
- One fully qualified class name per line
- Comments start with `#` and extend to end of line
- Blank lines and whitespace are ignored

Example:
```
# JLine FFM Terminal Provider
org.jline.terminal.impl.ffm.FfmTerminalProvider
```

## More Information

See `TerminalProvider.load()` javadoc for detailed documentation.
