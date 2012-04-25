## [JLine 2.7][2_7], PENDING
[2_7]: https://oss.sonatype.org/content/groups/public/jline/jline/2.7

* Added support for vi keymap. Most major vi features should work.
   * The following features are NOT yet available.
      * Undo/redo support is not yet available
      * Character search (CTRL-])
      * Yank via (CTRL-Y)
      * Quoted insert (CTRL-Y).
   * The "jline.esc.timeout" configuration option (in your $HOME/.jlne.rc) controls the number of millisesconds that jline will wait after seeing an ESC key to see if another character arrives.
* The JVM shutdown hook that restores the terminal settings when the JVM exits (jline.shutdownhook) is now turned on by default.

