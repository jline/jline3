/**
 * Test script for the MDX version display plugin
 */
const plugin = require('./mdx-version-display');

// Create a mock tree with a code node
const mockTree = {
  type: 'root',
  children: [
    {
      type: 'code',
      lang: 'xml',
      value: '<dependency>\n    <groupId>org.jline</groupId>\n    <artifactId>jline</artifactId>\n    <version><VersionDisplay /></version>\n</dependency>'
    }
  ]
};

// Apply the plugin
const transformer = plugin();
transformer(mockTree);

// Check the result
console.log('Original value:');
console.log('<dependency>\n    <groupId>org.jline</groupId>\n    <artifactId>jline</artifactId>\n    <version><VersionDisplay /></version>\n</dependency>');
console.log('\nTransformed value:');
console.log(mockTree.children[0].value);

// Verify the transformation
const success = mockTree.children[0].value.includes('{<VersionDisplay />}');
console.log(`\nTransformation ${success ? 'successful' : 'failed'}`);
