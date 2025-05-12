# Website

This website is built using [Docusaurus](https://docusaurus.io/), a modern static website generator.

### Installation

```
$ npm install
```

### Local Development

To start the development server without code snippets:

```
$ npm start
```

To start the development server with code snippets extracted from the demo directory:

```
$ npm run start-with-snippets
```

To preview the website with code snippets (alternative method):

```
$ npm run preview
```

This command extracts code snippets from the demo directory, then starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Code Snippets

Code snippets are extracted from Java files in the `demo/src/main/java/org/jline/demo/examples` directory. The snippets are marked with special comments:

```java
// SNIPPET_START: SnippetName
// Your code here
// SNIPPET_END: SnippetName
```

To manually extract snippets:

```
$ npm run extract-snippets
```

### Build

```
$ npm run build
```

This command extracts code snippets, then generates static content into the `build` directory and can be served using any static contents hosting service.

### Deployment

Using SSH:

```
$ USE_SSH=true yarn deploy
```

Not using SSH:

```
$ GIT_USER=<Your GitHub username> yarn deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and push to the `gh-pages` branch.
