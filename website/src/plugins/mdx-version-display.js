/**
 * MDX plugin to properly handle VersionDisplay component in code blocks
 */
let visit;
try {
  // Try to load the new version of unist-util-visit
  visit = require('unist-util-visit').visit;
} catch (e) {
  // Fall back to the old version
  visit = require('unist-util-visit');
}

module.exports = function mdxVersionDisplayPlugin() {
  return (tree) => {
    visit(tree, 'code', (node) => {
      // Look for <VersionDisplay /> in code blocks
      if (node.value && node.value.includes('<VersionDisplay />')) {
        // Replace with a special marker that will be processed by the MDX renderer
        node.value = node.value.replace(
          /<VersionDisplay \/>/g,
          '{<VersionDisplay />}'
        );
      }
    });
  };
};
