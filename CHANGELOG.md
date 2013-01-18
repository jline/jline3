<!--

    Copyright (c) 2002-2012, the original author or authors.

    This software is distributable under the BSD license. See the terms of the
    BSD license in the documentation provided with this software.

    http://www.opensource.org/licenses/bsd-license.php

-->
## [Jline 2.9][2_9]
[2_9]: https://oss.sonatype.org/content/groups/public/jline/jline/2.9
                                                                     
* Ability to control terminal encoding

## [Jline 2.8][2_8]
[2_8]: https://oss.sonatype.org/content/groups/public/jline/jline/2.8
                                                                     
* Backward history searching
* Update to jansi 2.9
* Handle EOF / Ctrl-D on unsupported terminals
* Distinguish carriage return from newline
* Correcting Manifest to make jline work as a bundle in OSGi
* Handle TERM=dumb as an UnsupportedTerminal
* Add back PasswordReader

## [JLine 2.7][2_7]
[2_7]: https://oss.sonatype.org/content/groups/public/jline/jline/2.7

* Updated license headers to be consistent BSD version
* Added support for vi keymap. Most major vi features should work.
   * The following features are NOT yet available.
      * Undo/redo support is not yet available
      * Character search (CTRL-])
      * Yank via (CTRL-Y)
      * Quoted insert (CTRL-Y).
   * The "jline.esc.timeout" configuration option (in your $HOME/.jline.rc) controls the number of millisesconds that jline will wait after seeing an ESC key to see if another character arrives.
* The JVM shutdown hook that restores the terminal settings when the JVM exits (jline.shutdownhook) is now turned on by default.

