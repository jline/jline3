---
sidebar_position: 3
---

# Version Management

This document explains how version numbers are managed in the JLine documentation.

## Version Placeholders

The JLine documentation uses a placeholder system to ensure that version numbers are consistent throughout the documentation. Instead of hardcoding version numbers, we use the placeholder `%%JLINE_VERSION%%` which is automatically replaced with the actual version during the build process.

### How It Works

1. The actual JLine version is defined in `docusaurus.config.ts`:

```javascript
// JLine version - update this when releasing a new version
const jlineVersion = '...';
```

2. During the build process, all instances of `%%JLINE_VERSION%%` in the documentation files are replaced with the actual version.

## Using Version Placeholders

When adding version numbers to the documentation, use the `%%JLINE_VERSION%%` placeholder instead of hardcoding the version:

```xml
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>%%JLINE_VERSION%%</version>
</dependency>
```

## Development vs. Production

The version replacement strategy works differently in development and production:

### Development Mode

In development mode (`npm run start-with-snippets`), the placeholders are replaced in the source files before starting the development server. This allows you to see the actual version numbers while editing the documentation.

When you're done, you can revert the changes to the source files using:

```bash
npm run revert-version-source
```

### Production Build

In production mode (`npm run build`), the placeholders are replaced in the built HTML files after the build process. This ensures that the source files remain unchanged while the published documentation shows the correct version numbers.

## Updating the Version

When releasing a new version of JLine, update the version in `docusaurus.config.ts`:

```javascript
// JLine version - update this when releasing a new version
const jlineVersion = '3.31.0'; // Updated version
```

The build process will automatically use the new version when replacing the placeholders.
