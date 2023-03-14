# [JLine3](https://github.com/jline/jline3)
<!-- git log --pretty=format:'* [`%h`](https://github.com/jline/jline3/commit/%H) %s' -->

## [JLine 3.23.0][3_23_0]
[3_23_0]: https://repo1.maven.org/maven2/org/jline/jline/3.23.0/

* [`e82b526a`](https://github.com/jline/jline3/commit/e82b526acadaa037459fb7651588910c98c28873) Reformat
* [`8951fc85`](https://github.com/jline/jline3/commit/8951fc8558846104b24fbb0f10549a51cfba1697) Add spotless
* [`0eddc136`](https://github.com/jline/jline3/commit/0eddc136e955aa8284efe20b7ef7bc2ecaf71041) Fix javadoc
* [`cc021a55`](https://github.com/jline/jline3/commit/cc021a55abd172bf81fe8f0dd51329341c21c293) Fix signal processing on windows, fixes #822
* [`6fa8b785`](https://github.com/jline/jline3/commit/6fa8b7859015b7755247a6a5fbea59c6dfaad5ea) Clean up console mode4
* [`c571b146`](https://github.com/jline/jline3/commit/c571b14604b11067448392688e35293c4e1f9e88) Refine color support in various environments, fixes #814 (#829)
* [`f3fa7036`](https://github.com/jline/jline3/commit/f3fa70368f3c8873d31a02c09df3cbf4513da97f) Verify ioctl return value in jansi
* [`362b233e`](https://github.com/jline/jline3/commit/362b233e6aefc7bf6e19606fc3c3829825331b2e) The JNA ioctl call is broken on Mac/aarch64
* [`ca176daf`](https://github.com/jline/jline3/commit/ca176daf5d5f7c24ecb8e5641d5ceaf69c34c6e4) This is a demo, so allow remote connections for debugging VM
* [`117782b0`](https://github.com/jline/jline3/commit/117782b017b23abcd71c6a615414e59ebcea1838) Fix calling commands in gogo under jdk 17
* [`6c5a2759`](https://github.com/jline/jline3/commit/6c5a2759775b1dbf68e79dd3ce32692c1f447090) Add missing ConEmu capabilities, fixes #800
* [`63618cc6`](https://github.com/jline/jline3/commit/63618cc6c02010a56f317c514cbcb96a47ca5977) Fix AltGr+Shift characters, fixes #747
* [`4dbe26bc`](https://github.com/jline/jline3/commit/4dbe26bcb22ae119921189c7f3a405fdeb620288) Decode alternative charset in/out sequences, fixes #805
* [`64fc2f20`](https://github.com/jline/jline3/commit/64fc2f208c675ba31109987ac5af532c7910b0d5) Add a test for #i805
* [`caa2d14a`](https://github.com/jline/jline3/commit/caa2d14a513467ba015dfdd94f22768cbb9c6d8b) Fix restoration of Terminal at close, fixes #819
* [`0b97167f`](https://github.com/jline/jline3/commit/0b97167f33a2f0060f3fcb613cc849d262d35ed5) Allow custom sort of providers, and prefer jansi by default
* [`6fcf987d`](https://github.com/jline/jline3/commit/6fcf987d13107213811aaf2298a4933b2d74725e) Add some javadoc on Terminal getAttributes/setAttributes
* [`b0b4f709`](https://github.com/jline/jline3/commit/b0b4f7097faabc643fae8de0be107a8abf704dae) Use Attributes copy constructor
* [`3e872d86`](https://github.com/jline/jline3/commit/3e872d861c83f3865a4b0fe89709c69a7d62cc97) fix javadocs of classes ConsoleEngine, JrtJavaBasePackages and Widgets
* [`c6a476e4`](https://github.com/jline/jline3/commit/c6a476e46f88ce9f80608cc3ad29204524223401) SyntaxHighlighter and Repl demo: use UTF-8 character encoding instead of JVM default
* [`90e9e868`](https://github.com/jline/jline3/commit/90e9e8688553db728a0efe0b676b9fa0e3b1071a) builtins Commands: fix keymap help and remove redundant toString() calls
* [`2c55e39b`](https://github.com/jline/jline3/commit/2c55e39b0380a1b6ce4696bb6068c0091568d336) Use a native library to create FileDescriptors if reflection does not allow access (fixes #575)
* [`e893fb48`](https://github.com/jline/jline3/commit/e893fb48308aa584f75a853984e722e2d745d2a9) Optimized Styles.isStylePattern() to avoid StackOverflowError (#817)
* [`8cb3793a`](https://github.com/jline/jline3/commit/8cb3793add3b2c46d86adcfd608ba7ea3de992de) Fix missing graalvm information for the exec provider (fixes #820)

## [JLine 3.22.0][3_22_0]
[3_22_0]: https://repo1.maven.org/maven2/org/jline/jline/3.22.0/

* [`dd697ee0`](https://github.com/jline/jline3/commit/dd697ee0f7dbb3ed7198aeedbbd1983503f60785) Remove compilation warnings (#816)
* [`e00a8370`](https://github.com/jline/jline3/commit/e00a837049f0038da6801c03d43e0f36f7195d60) Appveyor fix (#815)
* [`87f7e576`](https://github.com/jline/jline3/commit/87f7e57614f58c7a25afbf87a3124d69a176e080) Upgrade all plugins to latest version (#740)
* [`d6e84da4`](https://github.com/jline/jline3/commit/d6e84da4b7a55985fbf8b83b155da3811b0631d8) Support for out or err stream for the terminal (fixes #787) (#788)
* [`c2a0c9e8`](https://github.com/jline/jline3/commit/c2a0c9e84938a86806f33dda41724b5441253f0a) Avoid redundant Map.containsKey call (#781)
* [`77f1cea0`](https://github.com/jline/jline3/commit/77f1cea0d3438520000077575e2630247bc0ef84) Make readBuffered blocking and add more readBuffered methods, fixes #757 (#782)
* [`4f57697f`](https://github.com/jline/jline3/commit/4f57697f4e04ac65f94be24a5dce7c42a2009d01) Replace AtomicBoolean with volatile boolean field. (#796)
* [`6e94df5f`](https://github.com/jline/jline3/commit/6e94df5fb36f18e1e1b2e9f408ec385795e80469) Upgrade Groovy to 4.0.7
* [`b82a347c`](https://github.com/jline/jline3/commit/b82a347c551d9646785a0f1f144588449293dc96) Scroll forward of the cli cause tailtips to collide with text, fixes #738
* [`fcc8ce61`](https://github.com/jline/jline3/commit/fcc8ce6134e9ef7dba5b546184c2331f5d328e88) Bump ivy from 2.5.0 to 2.5.1
* [`751a7d7c`](https://github.com/jline/jline3/commit/751a7d7c0acb8a11e30cb9b74c49e3e66d2b7a96) prnt command: add option --multiColumns
* [`ade7806f`](https://github.com/jline/jline3/commit/ade7806fb0017d0ac2e6b1a9151e2a13897def5e) Less: eliminate a couple of source.lines() calls
* [`239d6e04`](https://github.com/jline/jline3/commit/239d6e04b2494f9874065c95d02a1670319aa0b2) less pager does not work well when using InputStreamSource, fixes #812
* [`181279c0`](https://github.com/jline/jline3/commit/181279c09642bc4f7639eb1197c8b3de80291aca)  Standard escape character (\) doesn't seem to be handled correctly, fixes #798
* [`1579fc04`](https://github.com/jline/jline3/commit/1579fc044d2912769bd0689491df240e28e8875d) command less: manage object arguments, fixes #811
* [`9243e6db`](https://github.com/jline/jline3/commit/9243e6db3cddceb923ede803946267a1e54f90fa) add CONSOLE_OPTIONS boolean variable redirectColor to switch on/off redirect colors
* [`9711716b`](https://github.com/jline/jline3/commit/9711716bdf8232569abd36d8c932616fb51e6e82)  Less method run(Source... sources) throws UnsupportedOperationException, fixes #810
* [`2c4a1923`](https://github.com/jline/jline3/commit/2c4a192397c8eeec5166dfecd6c91d3eb131c804) Upgrade maven-enforcer-plugin to 3.1.0
* [`58dccf72`](https://github.com/jline/jline3/commit/58dccf727ddf35046d17693249ecf32aedf09429) Upgrade Graal to 22.1.0
* [`396a39e1`](https://github.com/jline/jline3/commit/396a39e130ecf857918a42bde27a373e46c79320) Upgrade Groovy to 4.0.3
* [`4dac9b0c`](https://github.com/jline/jline3/commit/4dac9b0ce78a0ac37f580e708267d95553a999eb) Infinite loop in TerminalLine constructor, fixes #751
* [`8b89ff5f`](https://github.com/jline/jline3/commit/8b89ff5f305e9865214f6ff3b5545723a311fcd3) Console example fix NPE
* [`9706eadf`](https://github.com/jline/jline3/commit/9706eadf152981ca9f5dfaac87aff2166e88e080) Builtins command registry: Add null check for  configpath parameter
* [`1cf2cda9`](https://github.com/jline/jline3/commit/1cf2cda9ab1b5bf233a58770c763defe8d034cc4) Upgrade Groovy to 4.0.2
* [`ed6cef30`](https://github.com/jline/jline3/commit/ed6cef30551cc165dae91ffc74b0aa01944fb23e) Add a simple example of printAbove
* [`d75b4f18`](https://github.com/jline/jline3/commit/d75b4f18d3af10e4fafd225737d6fca494a1b975) Groovy REPL: add tab-completion for groovy array methods
* [`0959a40b`](https://github.com/jline/jline3/commit/0959a40b0212d6f0a4f5040ac4911caf162cc9d5) Upgrade Groovy to 4.0.1
* [`c8fcdda6`](https://github.com/jline/jline3/commit/c8fcdda668999857b8dac1822e20016b70167bf8) Simplify String operations (#776)
* [`fe1a6ff4`](https://github.com/jline/jline3/commit/fe1a6ff49920e7510ee8bab91f557a17f3c1e1b4) Docs typo: `Columns separated` -> `Colon separated` (#775)
* [`f9b267f6`](https://github.com/jline/jline3/commit/f9b267f6eb465ed811ab88c2855b4e16ddfe8227) Option to disable Undo in LineReader
* [`0bb26e99`](https://github.com/jline/jline3/commit/0bb26e999f40f12d7564839df83f84235bf1a74b) SystemHighlighter: command line parsed twice
* [`d3aa7dc6`](https://github.com/jline/jline3/commit/d3aa7dc66f9228d1963745c0c21ac26629a12e1b) nanorc parser: the reader is not closed if exception is thrown
* [`f988d35f`](https://github.com/jline/jline3/commit/f988d35f4f8787461dbb68e0df1a804866b92b82) args.nanorc: improve options highlighting
* [`4b743231`](https://github.com/jline/jline3/commit/4b7432316ab3b5f8b2068f7a79b6530679be6a50) SystemHighlighter: fix subcommand highlighting
* [`ac26d10c`](https://github.com/jline/jline3/commit/ac26d10c163b960968e58bda95a3de179e01c12a) lsStyle resolver defined as static field, fixes #764
* [`bfcc415b`](https://github.com/jline/jline3/commit/bfcc415b5b2dd77e99d62eadf11b6df27ef14592)  Candidates customized order lost when tabbing through candidates, fixes #763
* [`cea9632b`](https://github.com/jline/jline3/commit/cea9632b6a1f1a06c27dcbff77488249f260283e) LineReader Candidate: tests for sorting and potential int overflow fix (#762)
* [`aa11f6ee`](https://github.com/jline/jline3/commit/aa11f6eeb405eb88c2d569afaf3844a03c473a04) REPL demo: add nanorctheme.template (#761)
* [`233a6cce`](https://github.com/jline/jline3/commit/233a6cceb7b3ff8037fc8d9797009c43693e053c) gron.nanorc: improve *_COLORS variable value highlight
* [`c70d3272`](https://github.com/jline/jline3/commit/c70d327233c2f18c22fc44d02d31abeec050f45e) REPL demo refactoring: add nanorc directory
* [`43127122`](https://github.com/jline/jline3/commit/431271228215fb9adf6dc0c8017d4e559766d065) REPL demo: add light.nanorctheme and rename default theme to dark
* [`f72694f6`](https://github.com/jline/jline3/commit/f72694f60e7eaa8c99b154914187c4d60437f22f) DefaultPrinter: add @SuppressWarnings("serial") in highlighters cache map
* [`51c0399d`](https://github.com/jline/jline3/commit/51c0399d9f5036e8816176acdbc6d51c47b44170) SystemHighlighter refresh() refresh also external syntaxHighlighters
* [`652d7a19`](https://github.com/jline/jline3/commit/652d7a19350afece86df1cd02668900303b59fab) GroovyEngine & DefaultPrinter: add syntaxHighlighter cache
* [`e9cbb19f`](https://github.com/jline/jline3/commit/e9cbb19fa9c4d70aaad0d12f2fabb1743d07f3bb) builtins.Styles: make public method style(name, defStyle)
* [`1aae0ae8`](https://github.com/jline/jline3/commit/1aae0ae8f7676af90c9c245c5424d9971965a705) highlighter command: add option --switch=theme
* [`7ae2b8b9`](https://github.com/jline/jline3/commit/7ae2b8b9436e98ed1f0b57755acb473b8260d5fb) ConsoleEngineImpl: fix potential NPE
* [`771f2217`](https://github.com/jline/jline3/commit/771f2217e58fcb93525c71d5511944223941a9e2)  Mandatory import of sun.misc, fixes #758
* [`afb067fe`](https://github.com/jline/jline3/commit/afb067fea64a4a932eab7b8e74d32fead1b83d3e)  Tab candidate menu list exceeds from visible terminal display, fixes #759
* [`08d27d71`](https://github.com/jline/jline3/commit/08d27d71816dad36ebf8b5a9ec82e02e17a42f47) SyntaxHighlighter: added a few string constants
* [`4ce93d9f`](https://github.com/jline/jline3/commit/4ce93d9face3784cac47cb06c89b3819d2a20ab6) align less config parsing with nanorc theme system
* [`844ce0bb`](https://github.com/jline/jline3/commit/844ce0bb39753c707f33b99d02b89668c7cc0166)  nano editor fails to highlight block comments, fixes #753
* [`f759ea68`](https://github.com/jline/jline3/commit/f759ea68224b5a6b5692d9c1d15859c2ce0994dc) SyntaxHighlighter: add parser to tokenize string literals and comments, fixes #746
* [`bcc7f307`](https://github.com/jline/jline3/commit/bcc7f30792df9be1ecfc8c19090979cd900a509f) Refactoring: moved SyntaxHighlighter implementation in its own file
* [`5cef3bab`](https://github.com/jline/jline3/commit/5cef3babf3a9db4b61b7dad770eb5916793e8a5b) Nano SystemHighlighter: add theme system (#752)
* [`40109530`](https://github.com/jline/jline3/commit/401095301e53c8b837ec6ba24e9d9ac198e4693c) Groovy REPL: highlight shell commands
* [`35379cf8`](https://github.com/jline/jline3/commit/35379cf864ee50995255f3680e79ebd0414d4d43) SystemHighlighter: add specificHighlighter to manage subcommand syntax highlighting
* [`ec66af98`](https://github.com/jline/jline3/commit/ec66af989ac1e94131f56f0013939eaad463d77c) Groovy REPL: highlight triple-quoted strings
* [`abf8d943`](https://github.com/jline/jline3/commit/abf8d94381ffe7c538ff588472525941f694377d) Groovy REPL: fix '# comment line' highlight (#750)
* [`95f1e91c`](https://github.com/jline/jline3/commit/95f1e91c938de2bcc12942d4b3690e22be25520d) SystemHighlighter: fix commandIndex calculation
* [`2cbc73ca`](https://github.com/jline/jline3/commit/2cbc73cad3b667c95bb3ee8d31429a28e58f87a5)  Groovy REPL: methods print and println appear twice in tab completion candidate list, fixes #749
* [`d98f1648`](https://github.com/jline/jline3/commit/d98f164860ece38ee619d04dadff043c4f47cd48)  3.20.0 Regression - Groovy REPL highlight syntax errors while typing is broken, fixes #748
  #745
* [`789ac75b`](https://github.com/jline/jline3/commit/789ac75b0e94b83c5298088c6205c16c45a3e630)  Groovy REPL: prnt command parameter completion does not work well, fixes #744
* [`9fb62296`](https://github.com/jline/jline3/commit/9fb622965fb4a1883fc20fa898f7a677776f28c2)  Nano SyntaxHighlighter fails to highlight the line after the block comment, fixes #743
* [`4c4031d4`](https://github.com/jline/jline3/commit/4c4031d4133ae53e270e0d57a19ff0e170c1e1a1) Groovy REPL: highlight comments in command line
* [`045b3c8f`](https://github.com/jline/jline3/commit/045b3c8f3a5d00fbb7cefac926c9a58eae6c2320)  Nano SyntaxHighlighter fails to highlight strings with line delimiters, fixes #742
* [`fc22be31`](https://github.com/jline/jline3/commit/fc22be3139f176dde607cc9c3dd3a6bf86769ebd)  Error in custom command line highlighter let readLine() method in an unusable state, fixes #741
* [`bd2c2188`](https://github.com/jline/jline3/commit/bd2c2188b0c35168a140e5667c9e12304148cbc8) Groovy REPL: configure parser comment delimiters
* [`b8c26ce8`](https://github.com/jline/jline3/commit/b8c26ce881a641a5e25f96fbe58cb06a9a900b73) appveyor: fix download URL of maven
* [`1315fc0b`](https://github.com/jline/jline3/commit/1315fc0bde9325baff8bc4035dbf29184b0b79f7) [JLINE-730] Support for comments in DefaultParser (#731)
* [`f89e28ad`](https://github.com/jline/jline3/commit/f89e28adbbd0871d456e07df3a51b6be080c2507) Fix last line not displayed when scrolling using Display, fixes #737 (#739)
* [`997496e6`](https://github.com/jline/jline3/commit/997496e6a6338ca5d82c7dec26f32cf089dd2838) Fix Parser javadoc of ParseContext.SPLIT_LINE, fixes #733
* [`ae78e09c`](https://github.com/jline/jline3/commit/ae78e09c8bb5375ccee9bb31f0238c074fc5b83f) Parser interface: remove redundant variable initializer and field modifiers

## [JLine 3.21.0][3_21_0]
[3_21_0]: https://repo1.maven.org/maven2/org/jline/jline/3.21.0/

* [`3654a2a0`](https://github.com/jline/jline3/commit/3654a2a0ca276edbfb675e427d43c8782e9e7a6d) Fix JNA CLibrary constants on non x86 platforms, fixes #687 (#727)
* [`057d00e7`](https://github.com/jline/jline3/commit/057d00e70e9e49dcf08f371a6ba74d6fe321d5ec) Upgrade gogo libraries (#725)
* [`738635f7`](https://github.com/jline/jline3/commit/738635f71e3836f7bcf8f7a0670cfef31814a276) Fix line endings in jline-gogo.bat (#726)
* [`fd2589f7`](https://github.com/jline/jline3/commit/fd2589f7818d2b0ba1d88fc9011c657f92824ff8) Disable JNA for Mac/M1 platform (#721)
* [`427d05e1`](https://github.com/jline/jline3/commit/427d05e1ec91e1833133c6d7ca6677bd7dfef9d8) Upgrade to JNA 5.9.0 (#722)
* [`d774f6f5`](https://github.com/jline/jline3/commit/d774f6f522851fc5aab2dc02f5c87208ed533bcf) Fix telnet bind address, fixes #611 (#723)
* [`eac455ea`](https://github.com/jline/jline3/commit/eac455eab03f662b8e7adbb15a1594b0063e01bc) Improve PumpReader surrogate char handling (#720)
* [`e2313dfe`](https://github.com/jline/jline3/commit/e2313dfe614c5f69bfc436ba568debec2b6cde98) Fix problem with PumpReader waiting forever, fixes #659 (#719)
* [`c6ae2c12`](https://github.com/jline/jline3/commit/c6ae2c1295c81893456a38ae69509693a690b50d) Rename History.read parameter to be more clear (#718)
* [`c12a7bde`](https://github.com/jline/jline3/commit/c12a7bde7a082433c9390f4d525642314557f8f2) Upgrade to jansi 2.4.0 (#717)
* [`9908d12e`](https://github.com/jline/jline3/commit/9908d12e278b38e5d2ea9002386254a210ec8922) Upgrade sshd to 2.7.0 (#715)
* [`deb7469d`](https://github.com/jline/jline3/commit/deb7469dc8d0028afcd647169963db31b8ca3927) Fix PumpReader support for supplementary code points, fixes #658 (#716)
* [`0a35dc7f`](https://github.com/jline/jline3/commit/0a35dc7f2326346ad19a5d3c5eb377fae3552e41) Inherit appName from the terminal, fixes #631 (#714)
* [`8843bbe9`](https://github.com/jline/jline3/commit/8843bbe9014ed605ceecaf3ef6c6030078bbf3c3) Allow easy custom candidate sorting (#678)
* [`b9ca72d3`](https://github.com/jline/jline3/commit/b9ca72d338ea92f9062a252a154a181c16acbc37) Fix emoji character width, fixes #683 (#713)
* [`bef9396d`](https://github.com/jline/jline3/commit/bef9396dd2190242b1ed321ac885cdaa2c0192cf) Telnet ConnectionManager should clean closed connections, fixes #709 (#712)
* [`5eaf5194`](https://github.com/jline/jline3/commit/5eaf51941b652f8d61dc45d33c6658a3a5c20da4) Force to use FreeBSD stty on Mac (#706)
* [`397e7288`](https://github.com/jline/jline3/commit/397e72880bbbb82e08f5660e509633e69f5a7bd6) Stack is a thread-safe synchronized collection. (#696)
* [`f8c6bb78`](https://github.com/jline/jline3/commit/f8c6bb785e50589e799ce72b99fcba3518ae419b) Change nested classes to static nested classes where possible
* [`a3a56888`](https://github.com/jline/jline3/commit/a3a568882f48ce6fc94c92c82f8f551e2c93329f) Typo in CompletionMatcher's javadoc, fixes #711
* [`e2795498`](https://github.com/jline/jline3/commit/e27954987c0317a5d08b8b4210194186827a4929) fix build script missing complain if no command is given
* [`a3e97824`](https://github.com/jline/jline3/commit/a3e978247c4ee2eab349651bc39b80e49e1b553d) Enable jdk17 for testing
* [`d3de534c`](https://github.com/jline/jline3/commit/d3de534c645a20aa85aa32e8b0608abe17fe6875) REPL-console: fix the parameter expansion of exit command
* [`f3c967f4`](https://github.com/jline/jline3/commit/f3c967f463c28c318e912b5dd5ede3b1b7260938) Remove unnecessary t.getCause() call
* [`ba065314`](https://github.com/jline/jline3/commit/ba06531420fb41713b41df912f168da186da3b65) Use try-with-resources where possible
* [`56c2a07e`](https://github.com/jline/jline3/commit/56c2a07e1906111eb358ac3386198b7af941ad38) Remove superfluous use of boxing
* [`3ae43a15`](https://github.com/jline/jline3/commit/3ae43a15c36cbd807124fb76611acd6590b5eb15) [#681] Extend jansi 2.x import package range
* [`a01fe264`](https://github.com/jline/jline3/commit/a01fe264d43ecdf7ab969d4a431cec3f8f840986) 3.19.0 Regression - Escape sequences are printed when using Git Bash in Windows Terminal, fixes #693
* [`0670361a`](https://github.com/jline/jline3/commit/0670361acdf36dd36ea86188d7553605757c63e3) DefaultPrinter: fix table column selection when column name contains '.'-char(s)
* [`31229b05`](https://github.com/jline/jline3/commit/31229b058a1aab6c2260da8fe92a477a92826c96) Added a few tests for CompletionMatcher
* [`74c97a23`](https://github.com/jline/jline3/commit/74c97a23a3baceaf9efff598516d3936470009cb) [JLINE-699] Make candidates for completion unique
* [`9ca636f3`](https://github.com/jline/jline3/commit/9ca636f3b5b8b4aaebdc9c675049336f2fc74ff4) Fix possible ArrayStoreException in stream (#701)
* [`b14c437c`](https://github.com/jline/jline3/commit/b14c437c307fad38bd0040ad8e2ebfe6579b7115) Purge depshield badge
* [`a1fcd9f5`](https://github.com/jline/jline3/commit/a1fcd9f5d08f359a3f2e152d4dca4edf02e1724d) prnt command: improve management of option --columns=<fields>
* [`97391909`](https://github.com/jline/jline3/commit/9739190986d6ea38ed1c12f2af09c564049e9306) Auto suggestion error when command contains '|' character(s), fixes #679
* [`9abe8b7e`](https://github.com/jline/jline3/commit/9abe8b7ef0eb77c77d26e5accd68993e967d0815) Nano editor does not work well on Windows terminals, fixes #676
* [`574dd56e`](https://github.com/jline/jline3/commit/574dd56e906d25b06992c19773539ea916e11fc5) JLine bundle jline-3.20.0.jar has a wrong Automatic-Module-Name, fixes #675
* [`f55f2e29`](https://github.com/jline/jline3/commit/f55f2e293a2810e178c647e89c0f962f0f4f239e) Groovy REPL: enum tab completion fails for imported class, fixes #674
* [`03b6a55f`](https://github.com/jline/jline3/commit/03b6a55f7205e2eb02a0da0e3ab8cf0d583b2482) Windows CMD: adding terminal status overwrites previous command lines, fixes #673
* [`db29d290`](https://github.com/jline/jline3/commit/db29d290867c4baee98d5e5f9ce11762852a34a1) ttop command display is messed on Windows, fixes #672

## [JLine 3.20.0][3_20_0]
[3_20_0]: https://repo1.maven.org/maven2/org/jline/jline/3.20.0/

* [`8ed2b9a6`](https://github.com/jline/jline3/commit/8ed2b9a667b6058ebb0e6970032496025f5a663e) Command 'slurp --format=TEXT <file>' reads file lines to ArrayList
* [`8041ffcd`](https://github.com/jline/jline3/commit/8041ffcd7e4cb7158329876fdc2ad8b7dc92c790) Groovy REPL: fix source file package tab completion in Windows
* [`049d24a3`](https://github.com/jline/jline3/commit/049d24a314ab0f832868389b58f9023d9e7fe4aa) ArgDesc: add validity check for argument name, #665
* [`5f2497dd`](https://github.com/jline/jline3/commit/5f2497dd059c19fa928e9cd728f72ab870e8e2ce) SystemHighlighter: highlight command options file values
* [`53cd0c03`](https://github.com/jline/jline3/commit/53cd0c031fa6d3f047795a3b7e5540abf4f47052) Fix option value parsing in Groovy classloader command
* [`278fac94`](https://github.com/jline/jline3/commit/278fac94e66e74bef2d34e73e0b5ebd5598eeca6) Upgrade Groovy to 3.0.8
* [`8e9fc369`](https://github.com/jline/jline3/commit/8e9fc3697ae2a7695d867eb3c47199779285b087) Upgrade JNA to 5.8.0
* [`fac777f5`](https://github.com/jline/jline3/commit/fac777f5645f860b3379eda6d41b6e4736779b6b) Upgrade Jansi to 2.3.2
* [`39ac1bbe`](https://github.com/jline/jline3/commit/39ac1bbe041500ff9f16e2a05b1090228df414b1) Revert "JrtJavaBasePackages: before class search check that package exists"
* [`e9563d97`](https://github.com/jline/jline3/commit/e9563d97eb26bb91f09e892f17b82bd438b26181) PrintAboveWriter fix the last new line test
* [`7c697ebf`](https://github.com/jline/jline3/commit/7c697ebf1f5f8b79ad81dd4d7b0b6ffbc37908e1) add a `PrintAboveWriter` which buffers input until a newline is reached, and then forwards to LineReader's `printAbove()` method
* [`943abe0f`](https://github.com/jline/jline3/commit/943abe0f16114085dc6ceb139ca2969564fcd5dc) Fix PumpReader.read(CharBuffer) not being synchronized
* [`6d696428`](https://github.com/jline/jline3/commit/6d696428782d32f17bd8c3d4a12c42b1a28d1ca5) Add a color field on the TerminalBuilder to control the dumb terminal
* [`a3bf295e`](https://github.com/jline/jline3/commit/a3bf295e4f75c4121756c935309dda0165e732e4) Groovy REPL: option '--add JAR' to classloader command, step II
* [`adbf0242`](https://github.com/jline/jline3/commit/adbf02429a8a060cb820a11423fe924ba1299ca4) REPL demo: remove multiline comment syntax from groovy.nanorc. Start comment char sequence breaks commandline highlight.
* [`ead6de89`](https://github.com/jline/jline3/commit/ead6de895ae308023b74a045da830ddf833262dc) Groovy REPL: option '--add JAR' to classloader command
* [`5be0e7c3`](https://github.com/jline/jline3/commit/5be0e7c3012f209ecff6229ddecb865cdb6fc7f6) SystemRegistry: change trace(Exception) to trace(Throwable)
* [`1d9bb288`](https://github.com/jline/jline3/commit/1d9bb2884450aa25f204480689e75a5f480837e1) Groovy REPL: improve package completion performance, step II
* [`d22e9b36`](https://github.com/jline/jline3/commit/d22e9b36563b908b204a0dad3cdabcda2d4eba5c) Groovy REPL: fixed command 'inspect --info [object]'
* [`273ac1e9`](https://github.com/jline/jline3/commit/273ac1e910bdb76c65cea608adcb97ed3412e914) JrtJavaBasePackages: before class search check that package exists
* [`4da3433f`](https://github.com/jline/jline3/commit/4da3433f234fc62e748452ba72143606baa24629) Groovy REPL: add completion group Classes
* [`427fbadf`](https://github.com/jline/jline3/commit/427fbadf6a017a38ff2bd203d7d7f455478d0304) Groovy REPL: add option syntheticMethodsCompletion
* [`7df36e38`](https://github.com/jline/jline3/commit/7df36e38c5a61cb4738c3bc730d0a2c43f5736fc) Groovy REPL: add inner class completion
* [`8daf2917`](https://github.com/jline/jline3/commit/8daf29171bd4df32fb8adf261d79f6ee58f5d2ea) Groovy REPL: improve package completion performance
* [`8dc496fb`](https://github.com/jline/jline3/commit/8dc496fbb844856104029c3c6735343408387a6a) Groovy REPL: method tab completion fails complete obj. super class methods
* [`8f092569`](https://github.com/jline/jline3/commit/8f0925699f964cb23ef9ea68482b74f6b5d16a23) Groovy REPL: purge dynamically created Script classes from classloader cache & add groovy command classloader
* [`169f9cff`](https://github.com/jline/jline3/commit/169f9cff098e70fcd984fd29cec544b8d1cbfd07) DefaultPrinter: map print enable option for keys selection
* [`92a63e14`](https://github.com/jline/jline3/commit/92a63e1494d7e24a9f36cf639257b181080a535f) Groovy REPL: manage tab completions of dynamically loaded jars
* [`54c7862d`](https://github.com/jline/jline3/commit/54c7862d1ba2a060a26fdfd3b24d317ac0b680f3) groovy Utils.toMap() manage GroovyCastException
* [`58d233a9`](https://github.com/jline/jline3/commit/58d233a9ef960687866d7bcb69e167156386c931) DefaultPrinter: improve table columns selection
* [`513e6fd4`](https://github.com/jline/jline3/commit/513e6fd4ca3bb6821cb2fa24d56ca5092a56c3ec) Groovy REPL: add tab completions to classes that are created in REPL
* [`9d05f0ae`](https://github.com/jline/jline3/commit/9d05f0aec2b024f83063e5209b2830c0c1a93d03) Groovy REPL: add support for Groovy traits
* [`70e8dee8`](https://github.com/jline/jline3/commit/70e8dee805541c3136f4bff5a27ca15dca34b6b4) Groovy REPL: added regex test for import command & reviewed class load pattern
* [`706e0268`](https://github.com/jline/jline3/commit/706e026834d402999a517f39290c63de83136e57) Groovy REPL handle imports when load classes from sources, #664
* [`6567d4bf`](https://github.com/jline/jline3/commit/6567d4bf7876fdecea85af8a970e42749711f1ff) Groovy REPL: complete get meta methods also as identifiers
* [`6ffcadf4`](https://github.com/jline/jline3/commit/6ffcadf48a4e93a11891e2887cb19dc173f17853) Groovy REPL does not load classes from groovy sources, fixes #664
* [`eb1199ec`](https://github.com/jline/jline3/commit/eb1199ec577452047219b387cdb5ffacd6de2d72) Groovy REPL demo erroneously can complete groovy statements in shell command, fixes #663
* [`6b511041`](https://github.com/jline/jline3/commit/6b511041f456a27bb2cfc2708686e991566f70d4) Repl demo: fix shell command error message
* [`95784185`](https://github.com/jline/jline3/commit/95784185877ab8739ca1e0fce56bb1453a6faa4e) SystemHighlighter: fix Repl demo shell command highlight
* [`4e18cc6c`](https://github.com/jline/jline3/commit/4e18cc6c1365512fa6855eed0ed0ab17df353f43) SystemRegistryImpl: change some fields visibilities to simplify class extensions
* [`8e66a60f`](https://github.com/jline/jline3/commit/8e66a60f25d7ab0e61c9815e415f6b307c7fca31) Highlighter impl.: change some fields visibilities to simplify classes extensions
* [`3320df5f`](https://github.com/jline/jline3/commit/3320df5f74e2907cbd3b3ef8c4d347c9f24af235) SystemHighlighter: use LS_COLORS rules to highlight command file args
* [`91c205bf`](https://github.com/jline/jline3/commit/91c205bf93b2b1ee40ac20280df2775ec91faacb) Command prnt style option does not work, fixes #661
* [`d22d576c`](https://github.com/jline/jline3/commit/d22d576c428e74b65767c16d483b9002238c0d2b) Add ColonCommandCompletionTest, #654
* [`620b187d`](https://github.com/jline/jline3/commit/620b187d22ae8ba6bd783f39a841da9b05257f06) Unable to directly handle unknown commands in console, fixes #653
* [`289d339b`](https://github.com/jline/jline3/commit/289d339bb5821cb094fe2086bdc6f794084806b6) AbstractTerminal: if type null assign fallback terminal type value (ansi), #651
* [`774e890d`](https://github.com/jline/jline3/commit/774e890d589acd16aba51489359d7231415d434a) CamelCase matcher: improve completion performance, fixes #650
* [`18b350eb`](https://github.com/jline/jline3/commit/18b350ebe0a2ffee968cbf323c867092e06cb7db) CompletionMatcherImpl: adjust matcher functions execution order
* [`1edf3111`](https://github.com/jline/jline3/commit/1edf31114233002423b34f61c9577c406b6de6a3) Fix case insensitive typo matching
* [`e103debb`](https://github.com/jline/jline3/commit/e103debb0a486603d44780cf553ab8ff905027ce) PRNT_OPTIONS: add option valueStyleAll
* [`6ed371c9`](https://github.com/jline/jline3/commit/6ed371c912ca6203f6c7eb558fd215e56d1ba25b) SystemRegistryImpl: highlight customization improvement
* [`0d63afbe`](https://github.com/jline/jline3/commit/0d63afbe3086a9e03e2fabeb876d5f60ea1077c3) Make DefaultPrinter extendable so that it can be used without SystemRegistry, #638
* [`2aeff8eb`](https://github.com/jline/jline3/commit/2aeff8eba95f5dd0b7324625f44d7bfe744938e1) A few fixes in org.jline.example.Console
* [`18b4b4b0`](https://github.com/jline/jline3/commit/18b4b4b01324559d8f02ac4aabe96ccd71d784cb) Merge pull request #646 from retronym/topic/typo-matcher-disable
* [`b1e83bb5`](https://github.com/jline/jline3/commit/b1e83bb5d039cf0968bcb2542a3bee768b1f5337) Add option to disable typo completion matcher
* [`c232acdd`](https://github.com/jline/jline3/commit/c232acdde443a661dbd2a21fd1d638fe2d8053eb) TailTipWidgets throws IndexOutOfBoundsException when descriptionSize=0, fixes #645
* [`31da519a`](https://github.com/jline/jline3/commit/31da519a120eb5c5ccc242b6664a3823c3dfe97d) Fix wrong call with negative argument
* [`bcdd8ef9`](https://github.com/jline/jline3/commit/bcdd8ef9e70e2a290edd80850bdf7fcdbf8d5246) Make emacs(Backward|Forward)Word aliases to forwardWord/forwardWord, add tests, fixes #644
* [`859bc82a`](https://github.com/jline/jline3/commit/859bc82a0a48eae88a22c09db063533fa992afb6) Groovy REPL: string variable assignment tab complete with current value, step II
* [`913862f9`](https://github.com/jline/jline3/commit/913862f97766675f5a02cc00c90afb7d873ef2a4) After JLine has trimmed history history-search-backward widget does not work well, fixes #643
* [`91d2e331`](https://github.com/jline/jline3/commit/91d2e331e0f4164c800839f02758be1b41513670) JLine history file seems to grow without limit, fixes #642
* [`b0923666`](https://github.com/jline/jline3/commit/b0923666fa42b53d020c21019e899860e5b123f2) DefaultHistory: a few small improvements
* [`6c3cfb7f`](https://github.com/jline/jline3/commit/6c3cfb7f2ccc0b53dc5bd360fd0ca177253d8d4c) Command colors: add option --ansistyles
* [`cc874f78`](https://github.com/jline/jline3/commit/cc874f7805d881037516489752b2367b8dbd0284) Builtin Styles: make style(string) public & refactoring
* [`9a73dac9`](https://github.com/jline/jline3/commit/9a73dac91706a03a27f1f6f2f8b876aa1597b4ed) Groovy REPL: string variable assignment tab complete with current value
* [`74a41fbd`](https://github.com/jline/jline3/commit/74a41fbd406dbfa0c82b646a998edff637e31ebc) Use new 24bit support when parsing ANSI sequences
* [`68b68cfd`](https://github.com/jline/jline3/commit/68b68cfd587800cd73bf5074027ddcdc8a31c7e6) Merge branch 'hboutemy-reproducible'
* [`043a4329`](https://github.com/jline/jline3/commit/043a432981435474d3b5396c31a3be377ac4c8c0) Merge branch 'reproducible' of https://github.com/hboutemy/jline3 into hboutemy-reproducible

## [JLine 3.19.0][3_19_0]
[3_19_0]: https://repo1.maven.org/maven2/org/jline/jline/3.19.0/

* [`4ba4649e`](https://github.com/jline/jline3/commit/4ba4649ebd48ee9014b142659efeac367431ddba) CompletionMatcherImpl: add max search time for camelMatch()
* [`ac4cdc18`](https://github.com/jline/jline3/commit/ac4cdc18ff50d61579a4c53afdff41cf34bd7126) Fix non-synchronized access to handlers map
* [`cc094c7b`](https://github.com/jline/jline3/commit/cc094c7be3e574337dd79b18580f9ab6cba3aaee) Command colors: fix rounding errors in hue to r,g,b conversion
* [`c159895f`](https://github.com/jline/jline3/commit/c159895f977335faf029a676734d389613101c92) FilesCompleter: add constructor parameter namePattern
* [`a2e21b65`](https://github.com/jline/jline3/commit/a2e21b6577f3d89162081c8da52bcfdeebf0266d) FilesCompleter & DirectoriesCompleter: removed boolean constructor parameter forceSlash, see #476
* [`9a497186`](https://github.com/jline/jline3/commit/9a4971868e4bdd29a36e454de01f54d3cd6071e0) DefaultHistory.matchPatterns is broken, fixes #635
* [`5d4add13`](https://github.com/jline/jline3/commit/5d4add136ff28f7e206aef80328e2c535478adbb) Add Javadoc for a few LineReader's completion options and variables
* [`9e0916fc`](https://github.com/jline/jline3/commit/9e0916fc333577654cb1431ffb040c6ce194b939) CompletionMatcherImpl: skip camelMatch if word contains too many uppercase letters
* [`2c536817`](https://github.com/jline/jline3/commit/2c5368170259483214c1c0a2e7f9e05746ef513e) CompletionMatcherImpl: if completing word contains '=' start camel matching after it
* [`67667099`](https://github.com/jline/jline3/commit/6766709986b4f5a9184c6899b74c5f7949392782) Command colors -view: option values <color24bit> create a zoomed table
* [`d235e11c`](https://github.com/jline/jline3/commit/d235e11c9f87e9c5b082188d4d17f3c91388bde3) Command colors: improve HSL calculation
* [`2a95d381`](https://github.com/jline/jline3/commit/2a95d3811da04860857cced5fedef8c881be5523) DefaultPrinter: improve reporting of bad option values
* [`977550be`](https://github.com/jline/jline3/commit/977550be8e3bc9edea87e54ee738b6347beb87e6) DefaultPrinter: use method columnSubSequence() to truncate long lines and values
* [`59d7e699`](https://github.com/jline/jline3/commit/59d7e699598eb45f8edea6e7996853f0571da93e) Command colors: hue angle can be used as option --view value
* [`644ffbe5`](https://github.com/jline/jline3/commit/644ffbe5b43a5c1219c9178800df151378800eeb) Command colors: add option --view
* [`014296de`](https://github.com/jline/jline3/commit/014296dede6cd09fc9547976beb6160dc5cc1293) Command prnt: renamed option --delimiter to --border
* [`2d42d398`](https://github.com/jline/jline3/commit/2d42d3989f7edab4f76d90f2aeb04b0508008810) Command prnt: add option --rowHighlight
* [`0c231732`](https://github.com/jline/jline3/commit/0c231732bbb52836e116d1e368d6f37181e8dd1b) Command prnt: add option --delimiter and improve table header highlight
* [`b9dbc010`](https://github.com/jline/jline3/commit/b9dbc01036285c8287c6a1d194b806777d0ef78f) Allow to set only bg color name in *_COLORS variable
* [`5489815b`](https://github.com/jline/jline3/commit/5489815bbda4fcc84a5ae7739870ae35fdfe593e) Colors <color24bit> values can be used in nanorc and *_COLORS variables
* [`1162c8a0`](https://github.com/jline/jline3/commit/1162c8a032e8868de7be6c149eb92fe9bc8c7fb8) Fix AttributedStyle calculations for true colors, fixes #305
* [`0944af4e`](https://github.com/jline/jline3/commit/0944af4eb29c80d6563873850fee9d9709061c8f) Repl demo: add script to test true colors
* [`43662052`](https://github.com/jline/jline3/commit/43662052f4db067a4eeb4467d9d559c607cc9311) Ubuntu xterm supports the 24bit color sequences but JLine uses only 256 color palette, fixes #633
* [`ae2ff29d`](https://github.com/jline/jline3/commit/ae2ff29d134b2584548184593139a510b9044563) AttributedStyle: fixed method backgroundRgb()
* [`cadefd5e`](https://github.com/jline/jline3/commit/cadefd5ea68fbe06fe1c50f0306264bb4c3531f2) AttributedStyle.toAnsi(): fixed StringIndexOutOfBoundsException
* [`55589241`](https://github.com/jline/jline3/commit/555892419d87f235c92ac7e852a76bfb05f60d2b) StyleResolver: fixed default rgb style 'bg-rgb:default'
* [`c029c9e6`](https://github.com/jline/jline3/commit/c029c9e63d461aef2c94d9d2ef04e1a8d0fc42e2) Command colors: add option --rgb
* [`d3dc71f3`](https://github.com/jline/jline3/commit/d3dc71f3128cba6d93a4ae2851f5455400228f57) Repl demo: cleanup custom commands
* [`d68a3d83`](https://github.com/jline/jline3/commit/d68a3d835de68d449de3bdf8a0ad8ad42bb9d6b9) CompletionMatcher: do not allow errors when used in completion menu
* [`14fd4f9f`](https://github.com/jline/jline3/commit/14fd4f9f0ca885c71b40d81b959f4bd88d34e52d) StyleCompiler.getStyle(): do not throw exception if style reference not found
* [`f36c685b`](https://github.com/jline/jline3/commit/f36c685b5e8da770eb0ef191d4deae049a6abd9b) Command doc: improved exceptions
* [`79b9febf`](https://github.com/jline/jline3/commit/79b9febf51486f62c27c0d3c0518f0eb9fbc004b) Command colors: add option --lock to lock fore/background color
* [`a27bcd1b`](https://github.com/jline/jline3/commit/a27bcd1b4832ff82af41e870ee3e90fe51910e30) Named styles and colors can be used in variables LS_COLORS, HELP_COLORS and PRNT_COLORS
* [`e1c75ce3`](https://github.com/jline/jline3/commit/e1c75ce38dc841b52b350459f39b84ac21868a4b) NanorcParser: add nano color name extensions and use StyleResolver
* [`785c06ae`](https://github.com/jline/jline3/commit/785c06ae2f8346fdfd7a20608375c0163868f743) Groovy REPL: do not save HelpException to console variable
* [`82b7c38c`](https://github.com/jline/jline3/commit/82b7c38c26928234ea4d5a53d818567d15d98b56) Command colors: add option --find
* [`2ec403f7`](https://github.com/jline/jline3/commit/2ec403f745d31810e5036f4b9e9aa038bc5b5606) JLine3 should not allow building a system terminal if input *or* output is not a tty
* [`a6e31a18`](https://github.com/jline/jline3/commit/a6e31a18d8a443ee67f108d5ca7fff7dad1f6a41) Use CompletionMatcher also in menu
* [`89d4ec49`](https://github.com/jline/jline3/commit/89d4ec4999d19d17762a34dfe715e3c7281dff1e) Command colors: reformatting name table, step II
* [`12dc013e`](https://github.com/jline/jline3/commit/12dc013e4dc61e796916f142d34ff6bfc54a96a1) Groovy-REPL: support for camel-cased, acronym-style completion
* [`8422cfeb`](https://github.com/jline/jline3/commit/8422cfeb2bf1b98ab76db1715681846627af9658) Command colors: reformatting name table
* [`f163d405`](https://github.com/jline/jline3/commit/f163d405f4d401ffad020dc0f43baefb1b715cbb) Add CompletionMatcher in order to allow customize completion matchers
* [`28868afe`](https://github.com/jline/jline3/commit/28868afea4981b25815eab912254d6cbea686930) SystemRegistryImpl: reset captured command output in REPL-loop
* [`9e1a5305`](https://github.com/jline/jline3/commit/9e1a530566bc321699daf3fcdc1832aff666d625) Command colors: change angle brackets and add option -s to view 16-color table
* [`b53835aa`](https://github.com/jline/jline3/commit/b53835aa7ebcc082253c992f64cf9836fc288268) Redirecting input from file to app fails when using CONEMU with Jansi, fixes #298
* [`f0a4adf9`](https://github.com/jline/jline3/commit/f0a4adf94b001e630cc1ddab065cccc40a34ca8d) Option group-persist: keep also group names on menu-list
* [`58c0b38c`](https://github.com/jline/jline3/commit/58c0b38c9d556ac64cd2a144626dcf2764ed1890) Apply completion background styles to the entire completion table/list cell
* [`eb86dc76`](https://github.com/jline/jline3/commit/eb86dc76353c1c149369c78c0875f3b4c408ee0b) fix JLine variable name value of COMPLETION_STYLE_LIST_BACKGROUND
* [`6d87f7b4`](https://github.com/jline/jline3/commit/6d87f7b4fd16ba3f608f9a8faff93399524c666f) colors command: reformatting color table
* [`b602f924`](https://github.com/jline/jline3/commit/b602f9248465cce491a94fd788fa9f6b7b0e69e5) SystemRegistryImpl: fixed NPE
* [`b470ba1b`](https://github.com/jline/jline3/commit/b470ba1b81a473ab994c294a75ca8a39c9c862b3) ctrl-k, ctrl-u clean line however does nothing with tips from tailtipwidget, fixes #623
* [`34efd44f`](https://github.com/jline/jline3/commit/34efd44fdef7647fa4b237eae90633e864be1139) Builtins: add command colors to view 256-color table

## [JLine 3.18.0][3_18_0]
[3_18_0]: https://repo1.maven.org/maven2/org/jline/jline/3.18.0/

* [`e567eb70`](https://github.com/jline/jline3/commit/e567eb70e5247473cb3f3725a066384046e7ba21) Remove transitive dependency on the jline bundle
* [`6168d2b7`](https://github.com/jline/jline3/commit/6168d2b7167d28665a10fe836ae03facd7f4612e) Avoid javadoc warnings and do not generate javadoc for demos
* [`be78b98b`](https://github.com/jline/jline3/commit/be78b98b995281dada3743e9e280dd703195ec47) Upgrade to Jansi 2.1.0
* [`a6707274`](https://github.com/jline/jline3/commit/a67072745e3197b83317d9173406fcf9c7ea4dc7) Upgrade plugins to recente releases
* [`f4dd7a88`](https://github.com/jline/jline3/commit/f4dd7a886155c409adbb44d3714fc04cc4c7f342) Move javadoc generation into a separate profile (active by default)
* [`717fad86`](https://github.com/jline/jline3/commit/717fad869b01b2bb91ac9a3b91b00fd35a8bfb0f) Allow disabling the jline bundle module
* [`8ff1e1d3`](https://github.com/jline/jline3/commit/8ff1e1d35146a14b7c188c1c33d1020cb6ab0714) Support terminal palette, fixes #620
* [`e2b6f97e`](https://github.com/jline/jline3/commit/e2b6f97ed0ebceb3c909415baeba71c72f819c37) Support for 24-bit colours, fixes #619
* [`c20b1338`](https://github.com/jline/jline3/commit/c20b13380e3c1d90e43cdc0bbcb68493da910bca) Remove duplicate semicolon
* [`29d72f81`](https://github.com/jline/jline3/commit/29d72f817a6742c7a83e84812aadca300f57d9fb) Use style resolver to resolve completion styles, fixes #617
* [`b07a7cf1`](https://github.com/jline/jline3/commit/b07a7cf1240c37d597d59c3f91faeeeddb932cfc) Fix wrong indentation
* [`99e130d6`](https://github.com/jline/jline3/commit/99e130d6ff2d004d18cb8d89c2ca5151493a0e02) Fix some tput limitations
* [`b77a0a8a`](https://github.com/jline/jline3/commit/b77a0a8afea5a50a3cfa514e510ca214a0857900) Inline completion re-sorts while tabbing when using groups, fixes #618
* [`9d4a53b2`](https://github.com/jline/jline3/commit/9d4a53b2e0132334c3e2d77de5a964442bbe40ea) Bump Groovy to 3.0.7
* [`d185f726`](https://github.com/jline/jline3/commit/d185f7264f5233554d57681edce214e625583837) Add variable menu-list-max and sort candidates in menu-list
* [`538b7fa8`](https://github.com/jline/jline3/commit/538b7fa81bf73710ff4d596807386a7d8524ede4) Option group-persist: after double tab keep candidates grouped, fixes #613
* [`329768ca`](https://github.com/jline/jline3/commit/329768ca9e9ea72a42a35e5232055d124f52b06d) Graal demo: fix resolved demo target path
* [`ec1115df`](https://github.com/jline/jline3/commit/ec1115df688ca051fda2ca1a8e3e90d1d790978a) Failed to build JLine Graal demo: NoClassDefFoundError, fixes #615
* [`54218bc3`](https://github.com/jline/jline3/commit/54218bc330b7ba9a4e77cb6cd9968d16d4f713b4) Nanorc parser: align with GNU nano version 5
* [`82ca0f05`](https://github.com/jline/jline3/commit/82ca0f05501b29b99d66f27ebc8ef67e4f6bd9ec) Nanorc parser: replace Posix char class regexes with Java regexes
* [`172644f4`](https://github.com/jline/jline3/commit/172644f4281ff07951198129fdb62c85768b1e53)  Nano highlighter xml highlighting differs considerable from GNU nano highlight, fixes #614
* [`30860bc5`](https://github.com/jline/jline3/commit/30860bc5b947c53563b48ea5418de150f167acb8) nano/less ignore quietly PatternSyntaxException when using system installed nanorc files, fixes #609
* [`764a6a6a`](https://github.com/jline/jline3/commit/764a6a6ad46099c26630f1e60490f955fbc3c661) Less fails with PatternSyntaxException #609
* [`b1a17cbc`](https://github.com/jline/jline3/commit/b1a17cbc7d5f978c38d43d583e1e7bef05b8b670) SystemHighlighter highlight command aliases as commands
* [`0e5d510e`](https://github.com/jline/jline3/commit/0e5d510e3c1ebc38b8bd6b9027b9388c0c21792b) SystemHighlighter fixed NPE
* [`924d8ff3`](https://github.com/jline/jline3/commit/924d8ff346e1ff2107fe0a25bc598b31c14e097e) Builtins Commands.less(): add configPath parameter
* [`a2281234`](https://github.com/jline/jline3/commit/a2281234359bb9ce2ef5173b1953b102391ec6da) Secondary prompt: fix padding when primary prompt has line breaks
* [`a47ccc80`](https://github.com/jline/jline3/commit/a47ccc801e7f48e8d85b8653b8fead6b68b67daa) Jline completion has logic issues with terminal and prompt widths (StringIndexOutOfBoundsException), fixes #604
* [`1a767236`](https://github.com/jline/jline3/commit/1a76723605f85f48fd06f35d51df60b11e187e7f) Document how I/O error in LineReader.readLine is reported, fixes #608
* [`ca381eec`](https://github.com/jline/jline3/commit/ca381eec4885fab0523a19718db1a3e0ec8cbea9) Groovy REPL: highlight command and groovy language syntax
* [`bb5e85af`](https://github.com/jline/jline3/commit/bb5e85afd9009e9d3d917a19dcb0deab4b9b0922) Display command is incorrect when use here document, fixes #607
* [`0ba7e813`](https://github.com/jline/jline3/commit/0ba7e8137e0ff132f05c4c176daccd149e14ec66) edit-and-execute widget: set BRACKETED_PASTE_OFF before editing
* [`23034dbf`](https://github.com/jline/jline3/commit/23034dbfb50ea4da2322be652c4a07345f646f39) SystemRegistryImpl: fixed IndexOutOfBoundsException
* [`d66e7349`](https://github.com/jline/jline3/commit/d66e7349017098458e7961acfa314198c666ada2) DefaultParser: fixed default variable regex
* [`eb3e07c8`](https://github.com/jline/jline3/commit/eb3e07c88782179f3b1c185eb5b7a8efd2fc9f67) SystemRegistryImpl: method consoleEngine() is now public
* [`6580789c`](https://github.com/jline/jline3/commit/6580789cd5eca02317007afe522c8f41060bbc45) Keep argument sorting in large argument list when formatting candidates for terminal into multiple columns
* [`d92701d4`](https://github.com/jline/jline3/commit/d92701d4b41cee07cd2110d1e0b20ab2982021d6) Auto suggestion error when type "\\" character, fixes #602
* [`495b534a`](https://github.com/jline/jline3/commit/495b534afb1e0c7dbfb7780869a424c62d44fd7b) ConsoleEngineImpl: exclude pipe name aliases from command completion
* [`df991872`](https://github.com/jline/jline3/commit/df99187246f4021f4fe12a64f52cca9d55aeb809) Show auto-suggestions when the reader starts reading, fixes #598
* [`ea6dd89c`](https://github.com/jline/jline3/commit/ea6dd89cdc98abdfc77c262444fca91da3c1080d) Autosuggestion choices are not refreshed after tab, fixes #545
* [`2e6638cb`](https://github.com/jline/jline3/commit/2e6638cb6d4f27bc61bbb6ca6059e5df0384e2d0) Widgets: added widget name public constants
* [`a015a5df`](https://github.com/jline/jline3/commit/a015a5dfd66262ab463f24e2b25aec03c5fa2982) JLine option AUTO_MENU_LIST: bug fix for candidate list start position calculation
* [`a5686ab1`](https://github.com/jline/jline3/commit/a5686ab138225ee8214c68bda8c33fbaf2af5d26) JLine option AUTO_MENU_LIST: candidate list is wrongly positioned, fixes #600
* [`ea8d0d3b`](https://github.com/jline/jline3/commit/ea8d0d3b87a6dd74876fc67f909c3b769433294f) SystemRegistryImpl: refactoring command output redirection
* [`40c4d324`](https://github.com/jline/jline3/commit/40c4d3241fc242525cf7050ec0b977494620065d) Add completion candidate suffix test, #425
* [`404565b2`](https://github.com/jline/jline3/commit/404565b2ede2b9b83fab2c350b2806126a11e33f) Dumb terminal when piping input to console app disallows ANSI formatting, fixes #299
* [`fe1f2717`](https://github.com/jline/jline3/commit/fe1f27179461fdecc88f2cf01de812784bddfad9) Windows gitbash: JLine will create dumb terminal if JNA lib is in classpath, fixes #599
* [`1c9f16df`](https://github.com/jline/jline3/commit/1c9f16dfbe8e5e58df1740dd17ff5c5d0027ff34) JNA/Jansi isWindowsConsole() method return true only if both input and output streams are connected to a terminal
* [`542bfb64`](https://github.com/jline/jline3/commit/542bfb644c244d44c4bbc820c6b08ae897dad173) Ignore BRACKETED_PASTE if dumb terminal
* [`1d7fb07f`](https://github.com/jline/jline3/commit/1d7fb07f04d5380829329c9e7e0880643a2974bb) Windows CMD, redirecting input using JLine with Jansi fails, fixes #597
* [`3f399ace`](https://github.com/jline/jline3/commit/3f399ace9e35da4ec1d6f3610dbf18372700777e) readLine() ignores any text in the buffer when OEF is reached, fixes #298

## [JLine 3.17.1][3_17_1]
[3_17_1]: https://repo1.maven.org/maven2/org/jline/jline/3.17.1/

* [`437e7f43`](https://github.com/jline/jline3/commit/437e7f430623e883520f3cc16a7be9982c8ac79f) Upgrade to jansi 2.0.1
* [`15cf3895`](https://github.com/jline/jline3/commit/15cf3895e8782a32176bf62858907aab1a3e6ea2) Add native resource information
* [`9e5728c1`](https://github.com/jline/jline3/commit/9e5728c19600a67fc8d10ff48e49c0c00d9dcbfd) Fix unit test
* [`d609de12`](https://github.com/jline/jline3/commit/d609de124271b79e48cef3ce6a15b7606244bbcb) Fix console hangup on windows in combination with jansi after typing one char

## [JLine 3.17.0][3_17_0]
[3_17_0]: https://repo1.maven.org/maven2/org/jline/jline/3.17.0/

* [`4dc7d445`](https://github.com/jline/jline3/commit/4dc7d445981af68cccb528fae8fe1880c7a93c78) Upgrade to jansi 2.0
* [`04556a57`](https://github.com/jline/jline3/commit/04556a5757d06fe3ddee5a5989a5e6181d83a7bf) Remove unused import
* [`57aa5e1a`](https://github.com/jline/jline3/commit/57aa5e1a82daf42a7e094082de44eec506473595) Remove the getConsoleOutputCP on the AbstractWindowsTerminal
* [`299d0e91`](https://github.com/jline/jline3/commit/299d0e917e59342bdb3445c6ed6c63275256b011) Add select-option in Example, #592
* [`ea98b90f`](https://github.com/jline/jline3/commit/ea98b90f5346008c5233da08a1fadda572d55a3d) ConsoleEngineImpl doc command: check that the page exists before launching browser
* [`5d4d46b2`](https://github.com/jline/jline3/commit/5d4d46b2d5760bf1b6f85e07248e9478e9ac1e26) Tab completion: if auto-menu-list=true and candidates do not fit to display show candidates in table view instead
* [`af9196e1`](https://github.com/jline/jline3/commit/af9196e153929f9abb9b4c6cae64269d772fb88a) SystemRegistryImpl: improve command and pipe alias compilation
* [`ad90e038`](https://github.com/jline/jline3/commit/ad90e038c4af3b3563e167d9f35d6d9c799dee32) Jline silently ignore streams(in, out) when using a system Terminal, fixes #576
* [`7fdedc36`](https://github.com/jline/jline3/commit/7fdedc36976fc8b90465c05d69b8bf4af1b64776) SystemRegistryImpl: code clean up
* [`91a9af16`](https://github.com/jline/jline3/commit/91a9af1693d339315a4a5d794eaaf2bd5099c526) Using SystemRegistry without ConsoleEngine the unknown commands are quietly accepted, fixes #585
* [`43178f5e`](https://github.com/jline/jline3/commit/43178f5e8f00d51d2c15c08b0f45b9aee83a134a) SystemRegistry: use terminal to flush and close streams in output redirection
* [`12dbfa48`](https://github.com/jline/jline3/commit/12dbfa481e39ae1241c20003ff5a92e4385221dd) improve toString for completer
* [`64af4390`](https://github.com/jline/jline3/commit/64af439095813430ae5fedd18e1c130ce919bf69) Autosuggestion history: escape also '+' char, fixes #584
* [`9ec26880`](https://github.com/jline/jline3/commit/9ec268809c076294b9304da8bf0060e6bf51da38) Update README.md
* [`8053dc13`](https://github.com/jline/jline3/commit/8053dc13c0955d3e19c55f4c1a99bd222fe34506) Add option to do list view of autocomplete suggestions, #582
* [`0d861ddb`](https://github.com/jline/jline3/commit/0d861ddb9081a924b20f699a07535319551f6962) Autosuggestion choices of packages names and directories are refreshed too early, fixes #545
* [`740985bc`](https://github.com/jline/jline3/commit/740985bc1d60608b303b293f2227bf04bb6cc150) Groovy REPL: add widgets and key mappings to toggle Groovy completion options
* [`10aa3905`](https://github.com/jline/jline3/commit/10aa390532c524581d20efe22ccbc24a4ec835db) Groovy REPL: reviewed and fixed all methods completion
* [`5a4b6319`](https://github.com/jline/jline3/commit/5a4b631997fa861cc5023b4dafc1648e792be3a7) Groovy command grab: fix ArrayIndexOutOfBoundsException
* [`e6407044`](https://github.com/jline/jline3/commit/e6407044f00147c9fd26a8600810506661f4c108) Fixed maven-javadoc-plugin configurations
* [`25d3adc9`](https://github.com/jline/jline3/commit/25d3adc95e2b5f85a9fd70a25182e859d5cdeb7c) Bump Groovy to 3.06
* [`280d75c9`](https://github.com/jline/jline3/commit/280d75c9b5d8a3b5d2ca8b76d10288abcaa863a4) Groovy REPL: chained metaMethods completion with inline Closure parameters
* [`546c8ff7`](https://github.com/jline/jline3/commit/546c8ff725675b876d2bb4314688e82613a9fe9b) Fix for typo in javadoc
* [`f8cec7b7`](https://github.com/jline/jline3/commit/f8cec7b738e4f14341d94852f209b73033cc90d3) Groovy REPL: improve closure vars type detection, add better test for immutable variables in ObjectCloner and fix ClassCastException in Utils.groovy
* [`6ece81d9`](https://github.com/jline/jline3/commit/6ece81d9abdc2119bf08d5dcc64aca8afb7d4d11) nano & less: code clean up
* [`efd9c70b`](https://github.com/jline/jline3/commit/efd9c70b0132b2237d2b30838310aafb28a6972c) REPL pipe alias can now be defined using also other aliases
* [`36d3bc75`](https://github.com/jline/jline3/commit/36d3bc7510b0402d4194e94de9b4a70307b7d3cb) Groovy identifiers completion a couple of improvements
* [`ce3c9308`](https://github.com/jline/jline3/commit/ce3c93081f359e9dc207f63f6d3baff9a9cc400d) Groovy REPL: add option to complete groovy metaMethods
* [`22faa8b2`](https://github.com/jline/jline3/commit/22faa8b27cfe3ead484c19bd687d3e6340fef802) Navigating grouped candidates using arrow keys is broken, fixes #580
* [`d3860450`](https://github.com/jline/jline3/commit/d3860450112f723b79dcc4656159ead8e44e4c98) Groovy REPL: add widget to toggle candidate grouping and fix a bug in identifiers completion
* [`cda92e36`](https://github.com/jline/jline3/commit/cda92e3627c8c98986dcd54b0e4d5d34b7a5c249) Groovy REPL: add option to complete groovy identifiers
* [`ccf838c0`](https://github.com/jline/jline3/commit/ccf838c0e39194a093a989eb06bc1ac105bdd949) Groovy REPL: add option to complete private and protected classes, #577
* [`d15889fe`](https://github.com/jline/jline3/commit/d15889fe5b4ab5b1542821837e26237d61bce811) Groovy REPL: add options to complete private and protected fields, methods and constructors
* [`1b6a3322`](https://github.com/jline/jline3/commit/1b6a33228109b9923136a1da69bb4cb45b6ee673) Command prompt and line buffer are overlaid with completion candidates, fixes #574
* [`f364c00a`](https://github.com/jline/jline3/commit/f364c00a406254bb3fe683972e802ea8bb0b9869) Autosuggestion history: fixed search command regex pattern, #570
* [`f6c12465`](https://github.com/jline/jline3/commit/f6c1246591354ada24c2b4e03eb4446a2cb105c3) Completers and Options: polish up regex patterns
* [`8f52fb3b`](https://github.com/jline/jline3/commit/8f52fb3ba3b0f818af9598e9508a5b9825702440) JLine script: redirecting command output to temporary variable will
* [`2d82d47a`](https://github.com/jline/jline3/commit/2d82d47ac4e91c18375dced41304475d88ee6c00) Auto suggestion error when type "\_" character, fixes #570
* [`4d8aa743`](https://github.com/jline/jline3/commit/4d8aa74342d3436a4ef14afc9cc4dda92a1cc19a) Groovy REPL: improved constructor statement completion
* [`ff28596e`](https://github.com/jline/jline3/commit/ff28596e2bc74e047489869820ea813921b757d1) GroovyEngine Inspector: override only Closure vars with function def
* [`0d4fc5df`](https://github.com/jline/jline3/commit/0d4fc5dfa384b555f66012d82446dbcef450205d) Groovy REPL: no method completion nor descriptions if known command
* [`8b1b76ca`](https://github.com/jline/jline3/commit/8b1b76caac316a77a16ed836ad34f05bb26c5046) Groovy REPL completion: improved statement variables type detection
* [`6551251c`](https://github.com/jline/jline3/commit/6551251c1f987f4f74b303e4685dd5916f18346b) Groovy REPL: package tab completion failed:
* [`2894fa7b`](https://github.com/jline/jline3/commit/2894fa7ba8eb6e6f5332a5b83026f7cec105ac7a) Groovy REPL: improved var tab completion, restrictedCompletion=true
* [`db15475b`](https://github.com/jline/jline3/commit/db15475bb245860e95009dc252ced113b2fe4cca) Groovy REPL: constructor tab completion failed: NoClassDefFoundError, fixes #568
* [`6158378e`](https://github.com/jline/jline3/commit/6158378e4b4682600a079f057470d5c0f0cd5fc1) Groovy REPL: improved completion of function & closure parameters
* [`a30080a8`](https://github.com/jline/jline3/commit/a30080a87fed8137e7bc00ba627c5e7ffd68ce28) DefaultParser: ArrayIndexOutOfBoundsException, fixes #567
* [`bb8b6fe0`](https://github.com/jline/jline3/commit/bb8b6fe0792948f9d4f07071e40239a90dbdc78e) Groovy REPL: added Groovy options noSyntaxCheck and restrictedCompletion
* [`786dcd81`](https://github.com/jline/jline3/commit/786dcd811cfac24d01a68adadf248c0ffd6514bc) jline-console: code cleanup
* [`1484c4dd`](https://github.com/jline/jline3/commit/1484c4dda60e08efd24825861bf3d5ffbd559609) Groovy REPL method description: improved detection of method's Class + a few other minor fixes
* [`9c7acb9f`](https://github.com/jline/jline3/commit/9c7acb9f65c4d1984d543deb20be35c117305a87) Groovy REPL for statement: improved looping variable type detection
* [`80d4d3fa`](https://github.com/jline/jline3/commit/80d4d3fa7930c0eacbfbb4cf8003fb04eb92096b) widget package: minor fixes & code reformatting
* [`6ee50323`](https://github.com/jline/jline3/commit/6ee5032305fa4e61b9eed434f113e6496a506096) jrt classes: manage tab-completions & minor fixes and improvements
* [`19fedee9`](https://github.com/jline/jline3/commit/19fedee93392f064a1194c7ce6de1f2319e66f09) jline-groovy: minor fixes & code reformatting
* [`36f8ecdc`](https://github.com/jline/jline3/commit/36f8ecdcdbe37981e04d3658fad719af1f4fcefa) JLine app with and without console variables, fixes #565
* [`2b5e1ae2`](https://github.com/jline/jline3/commit/2b5e1ae2a9181772eea911c3975e18845aef1571) JLine bundle javadoc is not been generated, fixes #564
* [`949969e4`](https://github.com/jline/jline3/commit/949969e4cd167fafce2b0e7b7aae6a375ce4a0f2) Solaris automounter: createDirectories throws FileSystemException, fixes #559
* [`e7eb5e06`](https://github.com/jline/jline3/commit/e7eb5e0626b8dd3fb8483b4ff6303a826f585d41) Merge pull request #562 from morris821028/master
* [`992587a5`](https://github.com/jline/jline3/commit/992587a565bed73c696b982f236bde6b18af7320) Example: fix hung on exit with 'status' option
* [`fdc2fb53`](https://github.com/jline/jline3/commit/fdc2fb53f9dc618bfccc3b20ae447cabce3a809f) Add support for embedded applications

## [JLine 3.16.0][3_16_0]
[3_16_0]: https://repo1.maven.org/maven2/org/jline/jline/3.16.0/

* [`f867197e`](https://github.com/jline/jline3/commit/f867197effd901fa7e87c789b06eee1f185952fa) Add a unit test for #552
* [`0af26c08`](https://github.com/jline/jline3/commit/0af26c08ea622c04a97b69b935726eef9e7f08e9) NonBlockingPumpReader.read() does not block, fixes #552
* [`a0137601`](https://github.com/jline/jline3/commit/a01376015c3277b8e86e3a1b1ded18fb2d788ee6) GroovyEngine: renamed GROOVY_OPTION groovyColors to GROOVY_COLORS
* [`25824ac0`](https://github.com/jline/jline3/commit/25824ac0f450660ac847edd40a2bfa8f14465115) Autosuggestion choices not refreshed after tab #545, fixed regression caused by commit https://github.com/jline/jline3/commit/feb769018aab222614e4576aa63cc746b45224cc
* [`722f2ef7`](https://github.com/jline/jline3/commit/722f2ef79a1c6000518b262a5d18326d30cba677) GroovyEngine Inspector: manage for-each statement
* [`ef81ae72`](https://github.com/jline/jline3/commit/ef81ae721dc4a3ba947598ee331af7fee6dfa7f6) GroovyEngine: checkSyntax() ignore NullPointerException
* [`80423654`](https://github.com/jline/jline3/commit/804236549a36e46a5d262feafab26a4c9805bdbb) GroovyEngine: tab-completion manage lambda expression peculiarities
* [`ee3f01ba`](https://github.com/jline/jline3/commit/ee3f01baeb37248b42ea2fbc288a6dd8a23f5614) GroovyEngine: add try-catch & small refactoring
* [`0175f494`](https://github.com/jline/jline3/commit/0175f494827170ef8f9e8cd2a30f0376a3d222a5) Update README.md
* [`b6ffb521`](https://github.com/jline/jline3/commit/b6ffb521bdba6dc31361c709b46fa8a6c4ffe0c5) GroovyEngine: added methods markCache() and purgeCache() in Cloner API
* [`3b0d5d52`](https://github.com/jline/jline3/commit/3b0d5d52fd2c31fb92ad8aaa2f11a22aceab890a) rxvt-terminal: NullPointerException with `infocmp` warning, fixes #550
* [`d2ac4ef4`](https://github.com/jline/jline3/commit/d2ac4ef422409bbd4b37a41d10e6b66fb54bb926) GroovyEngine: fixed chained method tab completion
* [`99e5efe6`](https://github.com/jline/jline3/commit/99e5efe60957094960e95df8a5522902502519f1) Improved PipelineCompleter
* [`c0f0e79b`](https://github.com/jline/jline3/commit/c0f0e79b06f0b692064542caac51cfee0e5b58b0) TailTipWidgets: fixed status bar message compilation
* [`8e979e10`](https://github.com/jline/jline3/commit/8e979e10fdcf509480c39ab7c677a515d9fdc7a3) Groovy: customizable colors
* [`458ec405`](https://github.com/jline/jline3/commit/458ec4053284cbd35928ee0f6f9a839e8d94e2f7) GroovyEngine: added syntax error highlighting
* [`e30759c8`](https://github.com/jline/jline3/commit/e30759c86d949d51d433e7022ac4fe929e2da264) GroovyEngine: tab completion requires min. one char to show candidates
* [`aafbf365`](https://github.com/jline/jline3/commit/aafbf365cb2a75f190cb985de2b973da368436aa) GroovyEngine: improve inner class tab completions
* [`f1817734`](https://github.com/jline/jline3/commit/f1817734d5b65f788995921a8d0107b7f499e563) GroovyEngine: display method descriptions with short type names
* [`dbf4118b`](https://github.com/jline/jline3/commit/dbf4118bf04cf6def23eebc32ba9944e06f22219) Auto suggestion error when type Asterisk character (*), fixes #548
* [`3b797306`](https://github.com/jline/jline3/commit/3b79730668e573222abd83d54f97886089e1c8f0) GroovyEnine tab-completion: Inspector create closures from function def
* [`92b94dac`](https://github.com/jline/jline3/commit/92b94dacf34414f997d5c2e1e31b1ed7db78f034) GroovyEngine: tab-completion improvements and fixes
* [`915e942b`](https://github.com/jline/jline3/commit/915e942b99c5fcb2f1ac10b619a9c02db112c46a) TailTipWidgets: simplified if statement
* [`96defb80`](https://github.com/jline/jline3/commit/96defb80d9e5573d0a033ba1f7b541ba1e11fce5) TailTipWidgets: set LineReader variable errors=0
* [`fb196ca9`](https://github.com/jline/jline3/commit/fb196ca9e87434172765bff06b9a8ede265504e4) GroovyEngine: display method descriptions on status bar
* [`272648ec`](https://github.com/jline/jline3/commit/272648eccea0c7f391b4431b9be22be44cb7cb24) SystemRegistry help customizations, fixes #547
* [`991aae58`](https://github.com/jline/jline3/commit/991aae58c487db248b51287d9fe7acabc9bee1d0) GroovyEngine: tab-completion of object methods inside code block
* [`66fcd737`](https://github.com/jline/jline3/commit/66fcd7370e0332cdce046023b7717d6769a85b46) Builtins title is shown in command help without commands, fixes #546
* [`6c81a5b1`](https://github.com/jline/jline3/commit/6c81a5b1e74b087e8745074dc4ff1958dadae2b5) Display: ArithmeticException: / by zero, fixes #526
* [`ae8ff734`](https://github.com/jline/jline3/commit/ae8ff734cc033a0bc01a0d178a87f2a47cd1ae26) GroovyEngine: tab-completion for method parameters
* [`56b1f297`](https://github.com/jline/jline3/commit/56b1f297e34cfd9b18fc5f682f9f9451581a9e01) LineReaderImpl: added trace
* [`7d619144`](https://github.com/jline/jline3/commit/7d619144a1cffe644976d478fa37270f07fa540d) GroovyEngine: tab-completion for chained methods
* [`88cf67da`](https://github.com/jline/jline3/commit/88cf67da6511bdc9184d56e7fa9895feda546e6b) GroovyEngine: more tab-completions variables, methods and constructors
* [`53d00b85`](https://github.com/jline/jline3/commit/53d00b85db6c063ad22a444f0117605017d6649e) command grab: added --verbose option
* [`8618e471`](https://github.com/jline/jline3/commit/8618e471c99edc52a0421e743153c860a28dd8c1) JlineCommandRegistry: added traces
* [`5ae969ed`](https://github.com/jline/jline3/commit/5ae969edd75a7d86ee1673714d936b76bf5c546e) DefaultPrinter: added two methods to make prnt command options easily
* [`feb76901`](https://github.com/jline/jline3/commit/feb769018aab222614e4576aa63cc746b45224cc) Autosuggestion choices are not refreshed after tab, fixes #545
* [`04aa1932`](https://github.com/jline/jline3/commit/04aa19320a058c4b3d69133d24f46b6d7cd44acb) GroovyEngine: added a few tab-completions (import, def, class, ...)
* [`fe632071`](https://github.com/jline/jline3/commit/fe632071442875017b623adc690cf0f11e9b8306) GroovyCommand: improved command tab-completions
* [`c6a09bd9`](https://github.com/jline/jline3/commit/c6a09bd9e1f3154c145384b2d90d4bcf87257197) Improved OptionCompleter long options value completion
* [`de558860`](https://github.com/jline/jline3/commit/de55886075b0941c923d4fec3ac7d06fe549086d) CommandRegistry interface: removed method commandDescription(command),
* [`add29245`](https://github.com/jline/jline3/commit/add292459554f7a6863fb2c3cea4d302674ff169) prnt command: fix options tab-completion, regression caused by #540
* [`497f10c6`](https://github.com/jline/jline3/commit/497f10c6b8d62f6c720b5eea88a3e371f448345b) Merge pull request #543 from mattirn/commandRegistry-improvment
* [`f13b695a`](https://github.com/jline/jline3/commit/f13b695ac90e661388203973de7d2c3ccb5eaaa0) CommandRegistry interface: removed execute() method
* [`04eaadf4`](https://github.com/jline/jline3/commit/04eaadf4b532a9926d89476ebcf0f70964a86346) Update README.md
* [`64d127e4`](https://github.com/jline/jline3/commit/64d127e4232e0aff4bc7c3a91cb81ab2ac0264d7) Update README.md
* [`78b4c9f9`](https://github.com/jline/jline3/commit/78b4c9f98af825b5bdd63deb0970afa81294db08) Merge pull request #540 from mattirn/console-package
* [`aa84d181`](https://github.com/jline/jline3/commit/aa84d181f5b6ba00a42b01f45ba6106917542935) DefaultPrinter can now be used also without ScriptEngine
* [`f8b7615b`](https://github.com/jline/jline3/commit/f8b7615b15e6dad56480b039a9e5556567b768d0) prnt command: moved implementation to DefaultPrinter
* [`fcac9694`](https://github.com/jline/jline3/commit/fcac969406439b8c0d32bf01fbb30d1510a6f055) Console example: fixed widgets enabling/disabling, step II
* [`4c24ce18`](https://github.com/jline/jline3/commit/4c24ce18e2f718a82e0d9677db2bb38ae7b84706) Split Widgets class to org.jline.widget package
* [`b3428b42`](https://github.com/jline/jline3/commit/b3428b42e69925b0540ba6e2a6930848d6b517cb) ConsoleEngine: added setPrinter() method
* [`1aa0bda2`](https://github.com/jline/jline3/commit/1aa0bda27f7db310899482a8d4540b9a9abc5fec) Console example: fixed widgets enabling/disabling
* [`d31b6784`](https://github.com/jline/jline3/commit/d31b678470504f1af215b7b04a219fee2760526c) Added simplified example, fixes #537
* [`16115a55`](https://github.com/jline/jline3/commit/16115a559e8b566ab51a5610204c52effbaa2e55) Added jline-console module
* [`10c3f209`](https://github.com/jline/jline3/commit/10c3f209446af58a847a9cf46199902820db48cb) Also test with jdk 14

## [JLine 3.15.0][3_15_0]
[3_15_0]: https://repo1.maven.org/maven2/org/jline/jline/3.15.0/

* [`1ccf81cf`](https://github.com/jline/jline3/commit/1ccf81cf962b6f61faebd7d140c52227bdcb7bcd) Merge pull request #536 from Marcono1234/AttributedStringBuilder-append-null
* [`a1551e7b`](https://github.com/jline/jline3/commit/a1551e7b13b8f332b4e0606f4bbc76422d1cba0d) Fix AttributedStringBuilder.append not handling null correctly
* [`6924e3e9`](https://github.com/jline/jline3/commit/6924e3e95d6dc9681121820462c35976678c084c) Removed key sequence ^\ from nano help, #441
* [`67b2ba18`](https://github.com/jline/jline3/commit/67b2ba18710cc7422e7398b718f37a02ab7ec0ca) Fixed some javadoc typos
* [`56dbf56c`](https://github.com/jline/jline3/commit/56dbf56cf4847e716e1da328e2459f63d556e32e) PipelineCompleter: improved options completion
* [`5cd04e7c`](https://github.com/jline/jline3/commit/5cd04e7c7173349abab38aa5ca6c5079f7f334bc) Console printer: improved the management of boolean options
* [`482b9c2e`](https://github.com/jline/jline3/commit/482b9c2e397af25846d912864f1942c99a9d6ce7) Merge branch 'master' of https://github.com/jline/jline3.git
* [`0eb2410c`](https://github.com/jline/jline3/commit/0eb2410c3d7f614edc513ebe2225d7390a00c1a3) Nano SyntaxHighlighter: build highlighter from given nanorc url
* [`724e3772`](https://github.com/jline/jline3/commit/724e37725ae4bd4c2c5283f36ec58d6f6df13677) Update README.md
* [`f6717970`](https://github.com/jline/jline3/commit/f67179703ffcf7fb5c5e458041113782479f04c2) Bump groovy to 3.0.4
* [`80b6f585`](https://github.com/jline/jline3/commit/80b6f585c9e91637e83693f8376bbd7badc82c92) jline demo: removed code duplication
* [`2ed99410`](https://github.com/jline/jline3/commit/2ed994106734c3151bebabb7f8698b32b74c568e) prnt command: added shortNames option
* [`ab29ae24`](https://github.com/jline/jline3/commit/ab29ae246e95ec009a5b0c12bff287a6243cf817) ConsoleEngineImpl: fixed method isHighlighted()
* [`21cfe927`](https://github.com/jline/jline3/commit/21cfe9279de52560849a438ac576fe974954aac4) ConsoleEngineImpl: fixed printing of empty Map
* [`013251f3`](https://github.com/jline/jline3/commit/013251f39c5383898ea9bfad6b426c3270f4d15b) repl demo: improved object highlighting
* [`3a4abf67`](https://github.com/jline/jline3/commit/3a4abf6796542868cf7842cb600cd9abc10a8771) repl demo: fixed init script
* [`7eb01785`](https://github.com/jline/jline3/commit/7eb01785ab715e4e15ed3d4f7f1d363e7323e4fe) Add support for rxvt terminals, fixes #533
* [`f8aa6e45`](https://github.com/jline/jline3/commit/f8aa6e45d3865beb00411aa6658eee5aa144b245) Cut down verbosity of debug logging for unsupported signals, fixes 455
* [`a6176bc6`](https://github.com/jline/jline3/commit/a6176bc6d3493b8dac285a03c08a2932067ab0ca) Add Automatic Module Name, fixes #530
* [`80265b47`](https://github.com/jline/jline3/commit/80265b4795c0b77ca3d0471ce03bcb965b9fb8be) Fixed javadoc warnings
* [`3c83e59c`](https://github.com/jline/jline3/commit/3c83e59ce585a9f27fcd6c8eba5aad3aa1867f2e) Update README.md
* [`48663a77`](https://github.com/jline/jline3/commit/48663a775427fdf9c5ca72647c7e801f761b17c9) Merge pull request #531 from mattirn/graalvm
* [`875efcbd`](https://github.com/jline/jline3/commit/875efcbd0b2e404c89d8915539192a4d2111fb21) Appveyor: Bump maven to 3.3.9
* [`1248c23c`](https://github.com/jline/jline3/commit/1248c23c81e5ca81fc0118a9d51dea7300dd6c0d) jline-groovy: improved object to string conversion
* [`00a46ad8`](https://github.com/jline/jline3/commit/00a46ad80f2e467d5376ae93d04bea1578544364) Repl demo: added jline & groovy docs key bindings to F1 and F2
* [`67409e60`](https://github.com/jline/jline3/commit/67409e60e6201e44730290de91210ba45a6b6201) prnt command: fixed list printing indention
* [`b695c9b8`](https://github.com/jline/jline3/commit/b695c9b8bdb30ba4b82c5be049c8a36a12c76bb6) SystemRegistryImpl: fixed NPE
* [`4b28a0a4`](https://github.com/jline/jline3/commit/4b28a0a4e7599e26011eda558a0bb3c9ad21dfbb) GraalVM: added graal command in build.config
* [`1a9ee1ba`](https://github.com/jline/jline3/commit/1a9ee1baa8f90570855f0cf87f0b066acdae7dcf) SystemRegistryImpl: added method addCompleter()
* [`81e38b94`](https://github.com/jline/jline3/commit/81e38b948f8fce71b9f572b9b7a4414c8d6d63d9) slurp command can now deserialize also string variable context
* [`b0c272ea`](https://github.com/jline/jline3/commit/b0c272eaf71e1a2c9684b7f483383f22a1ffe0d3) Merge branch 'master' of https://github.com/jline/jline3.git
* [`7333eeb9`](https://github.com/jline/jline3/commit/7333eeb9d02362bfb581dba0c12b313837803027) FileNameCompleter: fixed Windows file name highlighting
* [`40900ef2`](https://github.com/jline/jline3/commit/40900ef27d9eecd461f04efdc2d3449f1776ed7b) GraalVM: added maven profile native-image
* [`6643730f`](https://github.com/jline/jline3/commit/6643730fec60bd0866228652d4c8a99f5371f2dd) GraalVM: added catch Error in ttop and removed it from graal demo
* [`0de635be`](https://github.com/jline/jline3/commit/0de635bedfccd025cc8634ebe890c4be1b4961e8) Support for cygwin recent versions, fix for #520
* [`c5eca10d`](https://github.com/jline/jline3/commit/c5eca10d5f98788b795e33d82ec5817da1fd0f7a) GraalVM: fixed java.util.logging problem
* [`39069251`](https://github.com/jline/jline3/commit/3906925164d08416daad564150b15937db4e46e1) GraalVM Support #381
* [`496492e9`](https://github.com/jline/jline3/commit/496492e98ee210e5b94ed2be2730788ee24b63d9) Fixed repl demo shell help, step II
* [`9613ec3b`](https://github.com/jline/jline3/commit/9613ec3bbc794696c1b5be3d16f1720207669d07) Widgets: executeWidget() restore old binding after runMacro()
* [`bf7f1ad0`](https://github.com/jline/jline3/commit/bf7f1ad040d45936b1cc77452403a1e3e598c925) Fixed repl demo shell help & improved console command completers
* [`8ff80c04`](https://github.com/jline/jline3/commit/8ff80c040b93aea9e5abc7bdbc408ac5aec839cf) TailTipWidget: highlight command main description if not highlighted
* [`4341151f`](https://github.com/jline/jline3/commit/4341151f334615b28eeb939e26521a51526da633) JlineCommandRegistry: improved command completer compilation
* [`bd35c22d`](https://github.com/jline/jline3/commit/bd35c22d07ee5ef000e01fabc633f3861ffa9836) Nano SyntaxHighlighter support also color codes 0-255
* [`ba709739`](https://github.com/jline/jline3/commit/ba7097398f09778d8df8c305b0d8d1ce371cedb7) print map: check on maxrows & SystemRegistryImpl.invoke() fixed NPE
* [`b7ae0ead`](https://github.com/jline/jline3/commit/b7ae0ead1059f9203e19296d5582285234a4ec80) Customizable colors for ls, help and prnt commands, fixes #525
* [`5cf8f030`](https://github.com/jline/jline3/commit/5cf8f030ead139114f9dd4f6ac23121d09b1ce6a) prnt command: use StyleResolver to highlight output
* [`d24883cd`](https://github.com/jline/jline3/commit/d24883cd999e0b2349beaee623b8436e13e2181f) prnt command: added valueStyle option
* [`f1529980`](https://github.com/jline/jline3/commit/f1529980f8b2071959a1f0a21289dad831240d84) Merge pull request #527 from mattirn/groovy-commands
* [`7f263440`](https://github.com/jline/jline3/commit/7f2634408f66fd7fe3802508fdb7a83f61bf9772) small improvements and bug fixes
* [`b024288b`](https://github.com/jline/jline3/commit/b024288bdf324ef6c2df065d311083b3b38f01d0) ConsoleEngineImpl: added doc command
* [`9cb2708a`](https://github.com/jline/jline3/commit/9cb2708a6e428cc6bc7f46f6ff5911604fdafa61) Repl demo: removed now obsolete SubCommands class example
* [`8be9242b`](https://github.com/jline/jline3/commit/8be9242b77210cf226c9dd4cea3e2e557716fa85) GroovyCommand: added grap command
* [`51b8bcf6`](https://github.com/jline/jline3/commit/51b8bcf67e5fa837861cb0f65d8bde7cdbdd38ed) Added javadocs in Printer and small fixes
* [`43748c7a`](https://github.com/jline/jline3/commit/43748c7ad60f45b03a4b29ea40deb9e396d54680) GroovyCommand: added command console & inspect command option --gui
* [`c1661890`](https://github.com/jline/jline3/commit/c166189033bfd1c9f597830e68078e139e4d6798) prnt command: improved Map collection value printing
* [`070d32c5`](https://github.com/jline/jline3/commit/070d32c58774d1953db9f51fca351d55b867d6ec) REPL console: added Printer interface and a groovy command inspect
* [`4179fbd9`](https://github.com/jline/jline3/commit/4179fbd9d1253b4a7c369dccf3dbe49b0705752c) Refactoring command registers, step III
* [`5f6a1e67`](https://github.com/jline/jline3/commit/5f6a1e671c64c24f60b2ff632bc230437cade149) Refactoring command registers, step II
* [`6dcb288d`](https://github.com/jline/jline3/commit/6dcb288d99b27500b219f36c092495ce84cce411) Refactoring command registers, step I
* [`a88409ff`](https://github.com/jline/jline3/commit/a88409ff589551036b782b274a8ac44b55b094f7) Refactor: moved CommandRegistry to org.jline.console package
* [`da71da6c`](https://github.com/jline/jline3/commit/da71da6c0283912ebdfbbd90ec2fd15de995fbcc) Refactor: added a new package org.jline.console
* [`d53681a6`](https://github.com/jline/jline3/commit/d53681a621d5dbfd5a8617cf135859c00f0399c4) prnt command: added options include and exclude
* [`0bd926dd`](https://github.com/jline/jline3/commit/0bd926dd3f86e476b85dbef71955a789e30b0315) prnt command: added options maxDepth and indention
* [`4694f748`](https://github.com/jline/jline3/commit/4694f748136dac425b9e2ed3ae930bcf988d51b0) Repl demo: added trace script
* [`e4c6f88c`](https://github.com/jline/jline3/commit/e4c6f88cf612d92ae2d61b701009c7bd2af8c8fb) SystemRegistryImpl: improved argument parsing
* [`bf35624c`](https://github.com/jline/jline3/commit/bf35624c4cbe5ae6d85e7c96f0ee9c281713b883) Repl demo: intercept Control-C
* [`d283a03d`](https://github.com/jline/jline3/commit/d283a03dee58b1fc729464f1d60b629b82f31851) TailTipWidgets: disabled command description cache as default +
* [`f67c0731`](https://github.com/jline/jline3/commit/f67c0731f3138c01c31950f358ff726d9491f776) CommandRegistry: added method commandDescription(List<String> args)
* [`90a67407`](https://github.com/jline/jline3/commit/90a67407d3b1ef5ba2160cda91a98114131539ea) Updated changelog
* [`381e8cb7`](https://github.com/jline/jline3/commit/381e8cb73f7df7e07faedde2647771c42dc79fcb) Move the plugin to the management section

## [JLine 3.14.1][3_14_1]
[3_14_1]: https://repo1.maven.org/maven2/org/jline/jline/3.14.1/

* [`81b6eade`](https://github.com/jline/jline3/commit/81b6eadeed147dcfe563ca7f18695064c1b5bab4) Fix signing
* [`df9f1f91`](https://github.com/jline/jline3/commit/df9f1f9133539c6715b407e5dc80a280d75fd395) Fix broken paste with remote connections, fixes #514
* [`88c28ae2`](https://github.com/jline/jline3/commit/88c28ae2a660b521552ffa4b5b508ac63446212c) REPL parameter expansion, do not add quote chars on numeric parameters
* [`5438565f`](https://github.com/jline/jline3/commit/5438565f582b127d013f93baecc9d754c0156587) ScriptEngine added methods: getSerializationFormats() and
* [`664eef8f`](https://github.com/jline/jline3/commit/664eef8fa4adc023fea9c787c4df1271a1892ab1) Refactoring Builtins.CommandInput
* [`3cac1ad7`](https://github.com/jline/jline3/commit/3cac1ad7cc68de1a67d4fd0fff7af38a4a026d52) Refactoring and improved java docs
* [`e381d1b2`](https://github.com/jline/jline3/commit/e381d1b24db3db63ec724f5e4cfe38704d9174a8) ConsoleEngineImpl: improved command completer and help
* [`db9f36e5`](https://github.com/jline/jline3/commit/db9f36e578b219a94a73b41691de797470118186) NanoTest: ignore nanorc files
* [`f0d7f238`](https://github.com/jline/jline3/commit/f0d7f23802c93eb721dd04402b4e48683c80ff85) prnt command: reviewed map similarity comparison and value highlight
* [`f6e3c083`](https://github.com/jline/jline3/commit/f6e3c083eca4c5ddccc144c57150706959211abe) pipeline completer: added console option maxValueNames
* [`0e55bb51`](https://github.com/jline/jline3/commit/0e55bb51bd00702a52123ba77e0d8e39319c5eaf) REPL console: added pipeline tab completer
* [`817c59a9`](https://github.com/jline/jline3/commit/817c59a9285ea45cb447384578d85e3690f0ff50) Bump groovy to 3.0.2
* [`2c46ae0a`](https://github.com/jline/jline3/commit/2c46ae0ada2013fe161f708596d6df47d10df4d8) Refactored repl demo and improved registered sub-command help
* [`8467b077`](https://github.com/jline/jline3/commit/8467b077310ac987c687772b1755067e85ffc951) prnt command: added more checks in table print decision
* [`38a909c0`](https://github.com/jline/jline3/commit/38a909c0ec1be56865944797a889caff21b98ba3) SystemRegistryImpl: improved help
* [`33f76291`](https://github.com/jline/jline3/commit/33f76291585396f47518002d9a3b03c03485fea0) prnt command: added options maxrows and maxColumnWidth
* [`c191801f`](https://github.com/jline/jline3/commit/c191801f2de1c9f7d5de219d4f0c9851c6c76ef7) Fix two regressions caused by pull request #518:
* [`d3336a04`](https://github.com/jline/jline3/commit/d3336a04718610b7eb7e2ba062bed029c27458e4) Merge pull request #518 from mattirn/subcommand
* [`f71d2c0d`](https://github.com/jline/jline3/commit/f71d2c0dd706b5b88ee75e841c9055622224d5e5) Update TerminalBuilder.java
* [`9696f11f`](https://github.com/jline/jline3/commit/9696f11fbb5b123445d6739451abbee4ced5b9ec) TailTip widget: improved sub-commands summary info description
* [`7318cf11`](https://github.com/jline/jline3/commit/7318cf1196c8ae6fac2ca43c6700952f0053e232) CommandRegistry: added registry command summary in default
* [`9ea33bed`](https://github.com/jline/jline3/commit/9ea33bedb114105235747bffbf5ca64124231499) Improved subcommands help
* [`b00a9a03`](https://github.com/jline/jline3/commit/b00a9a03a7fa36fe6c83780450570d4a374c4cfe) subcommands: added support for object parameters
* [`5a249551`](https://github.com/jline/jline3/commit/5a2495511f3a4601443e66c80473daa09ef44b7e) Command autosuggestion: support subcommands
* [`c2d2087d`](https://github.com/jline/jline3/commit/c2d2087dd0054cf50f14c7e04b56a118057067ef) SystemRegistry: register and manage subcommands execution and completion
* [`c4632055`](https://github.com/jline/jline3/commit/c4632055305c7b9cffd35f0f22383184b81e7fdf) Builtins.CommandInput: added field command
* [`9b7842ba`](https://github.com/jline/jline3/commit/9b7842ba964fa458a2261c863b63f9d9f2590035) Merge pull request #517 from mattirn/prnt-customize
* [`1feab624`](https://github.com/jline/jline3/commit/1feab6241026212d624df53b767e896588a35e78) prnt command: improved heterogeneous object list printing
* [`c41e2df5`](https://github.com/jline/jline3/commit/c41e2df51726e19253de99ea500871ad1d61692b) prnt command: improved Iterator and Iterable object printing
* [`af82e2cf`](https://github.com/jline/jline3/commit/af82e2cfc59bc0e5314792ecf4419f83d2fbebfc) Refactoring object printing
* [`9430ba01`](https://github.com/jline/jline3/commit/9430ba01ea07871d9a9131839e986966e4e4b50e) prnt command: improved map printing
* [`45234086`](https://github.com/jline/jline3/commit/4523408679914ccb4dc177279adc192f723e057c) prnt command: added option toString and custom highlight map values
* [`07b2df9f`](https://github.com/jline/jline3/commit/07b2df9fb322c2babdef6b8545aa5176cbb7ee8e) Expand parameter in file path, fixes #516
* [`f30e34b2`](https://github.com/jline/jline3/commit/f30e34b2994d24a2e6cf7bc1b4b6e521914c4e90) prnt command customization
* [`02c7f67c`](https://github.com/jline/jline3/commit/02c7f67ce13acc567c4fc673c65203e23834eabf) ScriptEngine: added method to execute closure
* [`bc331f89`](https://github.com/jline/jline3/commit/bc331f8958524374e9eb4c6b115fe3ddabe953ec) prnt command: added configuration options columnsIn and columnsOut
* [`eedaff33`](https://github.com/jline/jline3/commit/eedaff33b61a327599bd970962cf9599971c2c2a) prnt command: added options oneRowTable, structsOnTable and columns
* [`6e61b976`](https://github.com/jline/jline3/commit/6e61b976b8d115fea25b20f82f8a48ea96e48cf9) Parser: fixed getCommand() method, step II
* [`71e35644`](https://github.com/jline/jline3/commit/71e35644a6799a79bd033539bdc08cea84ffa0a3) REPL console: parameter ${@} expansion and two new pipes in demo
* [`89123999`](https://github.com/jline/jline3/commit/891239993a26f4dc87f4bb4f723da9957975d6a3) REPL demo: added command to execute shell commands
* [`c9e16309`](https://github.com/jline/jline3/commit/c9e16309696f7b0f054516a98ca5e6cb1cda60e4) Parser: fixed getCommand() method
* [`61693df3`](https://github.com/jline/jline3/commit/61693df3cf26a97782cdbcebb50570a5eb1cb89a) Refactoring object printing
* [`b8d7936b`](https://github.com/jline/jline3/commit/b8d7936b9826fa8bf6ad65b247d7720206b578ca) REPL console: throw Exception if redirecting console script output
* [`6fbdb250`](https://github.com/jline/jline3/commit/6fbdb250b390214441381be7cf1c25c7a6f13e48) REPL console: allow the use of console scripts in pipe line
* [`01e0c542`](https://github.com/jline/jline3/commit/01e0c542326cf4a2239820cda2da6dd7d2952b72) REPL console: improved object printing
* [`089f9898`](https://github.com/jline/jline3/commit/089f9898538f16324532f960721bd3b0e94d6f59) REPL console: redirect output to null device (command > null)
* [`e839b9c5`](https://github.com/jline/jline3/commit/e839b9c56fa2ffaf1b9c6f6b5cc22ecd553836b4) Fix NPE when use SystemRegistry without ConsoleEngine, fixes #515

## [JLine 3.14.0][3_14_0]
[3_14_0]: https://repo1.maven.org/maven2/org/jline/jline/3.14.0/

* [`c3317bc8`](https://github.com/jline/jline3/commit/c3317bc8b72a78dbf55d58ee79160ef8649e3f07) Track the system terminal, fixes #508
* [`1362e89f`](https://github.com/jline/jline3/commit/1362e89fe4550410708907d8519104a36fb29d51) Add a github workflow
* [`459733e2`](https://github.com/jline/jline3/commit/459733e2c648bf358aca38c7ac2c861268ac54c6) ScriptEngine: removed convert() method
* [`c1415305`](https://github.com/jline/jline3/commit/c1415305c9325528c5b3b94cd19ff9d5f2c8f9e0) ConsoleEngineImpl: refactoring & minor fixes and improvements
* [`6b8d8bcd`](https://github.com/jline/jline3/commit/6b8d8bcd95c2a9ce8e79a11112586a510798d510) ScriptEngine refactoring: removed highlight() and format() methods. Moved methods
* [`dc1885cf`](https://github.com/jline/jline3/commit/dc1885cf159d458b72b0741b11e0114b6bfb71ea) SystemRegistryImpl: refactoring...
* [`60a831cc`](https://github.com/jline/jline3/commit/60a831cc4d2737c90a008836602bfde7c043c844) GroovyEngine: reviewed class statement evaluation
* [`d73cfeaa`](https://github.com/jline/jline3/commit/d73cfeaa573fd4a6401d78ceeae29c4573530ac5) alias command: fixed alias replacement
* [`2a433640`](https://github.com/jline/jline3/commit/2a433640bde7a585e4ecb95c192dc734aff57da9) Refactoring: removed static println method from SystemRegistry
* [`23c29aa8`](https://github.com/jline/jline3/commit/23c29aa844d378d2924ae0b64e3ac7288c16a383) Added javadocs
* [`d0276266`](https://github.com/jline/jline3/commit/d0276266098eac900c3b298ea9686884d356980c) Console script use exit command to return value and minor improvements
* [`01811210`](https://github.com/jline/jline3/commit/0181121072a30cc6e2e7a000c46693021f8acb04) REPL console: improved exit status evaluation & pipe line compilation
* [`9109f983`](https://github.com/jline/jline3/commit/9109f9839bee8c4ac9709b6a1ce0f297830a3db3) REPL console: calculate exit status also for groovy statements and
* [`88e451bb`](https://github.com/jline/jline3/commit/88e451bb62d02dbf58f0da6834cb532070dac15e) REPL demo: added an other example script and widget
* [`2b7bb5b5`](https://github.com/jline/jline3/commit/2b7bb5b5ade5ec3cd6e4b9fa5057cae2c6a8ab28) Bump groovy to 3.0.0
* [`bd23b4fb`](https://github.com/jline/jline3/commit/bd23b4fb7f30ea7c06ddbc1e2e59ba9697fb79fa) REPL console: command exit status evaluation when redirecting to file
* [`9b0c9874`](https://github.com/jline/jline3/commit/9b0c9874644e0814136cdd565aba194d10ea0d99) Parser: added SPLIT_LINE context
* [`8b28847d`](https://github.com/jline/jline3/commit/8b28847d890b8a908ecccc2f33ada7b2bcede79f) REPL console: improved command exit status evaluation
* [`811c2f08`](https://github.com/jline/jline3/commit/811c2f08db0d0ba9d08524e01d8cd53a498074e0) REPL console: improved pipe line parsing
* [`6f357e9a`](https://github.com/jline/jline3/commit/6f357e9af3437ab5ba55c8783b8d28d7327c3136) ConsoleEngine: added pipe operators && and || and test-widget in demo
* [`8a5513e3`](https://github.com/jline/jline3/commit/8a5513e306d28d5e803fe16260d4d46fdcd9852a) ConsoleEngine: renamed named pipe operator to | and various improvemen
ts
* [`bb952c51`](https://github.com/jline/jline3/commit/bb952c517f4f730e84fdcedf62e91b8defdd03c4) Refactoring: Changed Parser static methods to non static
* [`1354c230`](https://github.com/jline/jline3/commit/1354c230c71adf10d2a6006dd388534e88a881bd) Merge pull request #511 from mattirn/fun-pipes
* [`96c2801a`](https://github.com/jline/jline3/commit/96c2801a2fcac84c77c98579ea5d3f5b5886b59c) Implemented pipe line alias
* [`95ff1327`](https://github.com/jline/jline3/commit/95ff132739db030d4cdf88763ddadc72b65adfcc) Implemented named pipe operator '|* <name>'
* [`8c03cdf3`](https://github.com/jline/jline3/commit/8c03cdf3c7488a536d73b956be29488643402253) Compile command-line with custom pipes
* [`ef3d00cd`](https://github.com/jline/jline3/commit/ef3d00cde42b3d5f0ab597c02da30ef4c2e8cd53) Added pipe command
* [`b38c2ab7`](https://github.com/jline/jline3/commit/b38c2ab7d46ced78aa709dfa0b3d2db99d366009) Implemented pipe operator |; that flips around the command and argumen
t:
* [`b471f94a`](https://github.com/jline/jline3/commit/b471f94ac142fb33c6bde315063dfb27c7aa2911) Merge pull request #507 from mattirn/output-redirection
* [`9bf0efea`](https://github.com/jline/jline3/commit/9bf0efea8ed303e3e07b7773f3f83eb2e629ed19) Groovy-REPL: A few small improvements
* [`a78b2346`](https://github.com/jline/jline3/commit/a78b2346e8ecbbdccb5ed2f7b866ed9312874277) Command output redirection to file, step II
* [`abdd4322`](https://github.com/jline/jline3/commit/abdd432258ca6233e28604ff39847677e859b3d3) Command output redirection to file
* [`cea2ea9a`](https://github.com/jline/jline3/commit/cea2ea9a73ecfc1b5db5ba8e877379513fad16aa) Improved javadoc comments
* [`31b3ea14`](https://github.com/jline/jline3/commit/31b3ea14c6ee82874d0f2fa48dab982fc476a396) Command output redirection to variable
* [`58c38580`](https://github.com/jline/jline3/commit/58c385801d6beabab8c6bb504736122ee18feb6f) Fixed filename option value tab completion, fixes #504
* [`1fed8189`](https://github.com/jline/jline3/commit/1fed8189df8ac60f6de6c0e1b63dd5cb97566776) ConsoleEngineImpl: Added a couple of helpers in widget execution, #503
* [`ab7f094d`](https://github.com/jline/jline3/commit/ab7f094da5edd2038af6657caca966797d40799a) ConsoleEngine: Add support for widget execution, fixes #503
* [`1bcf06f1`](https://github.com/jline/jline3/commit/1bcf06f1611e4e94f41c7da2d85a06219c95d025) Small improvements...
* [`29077a28`](https://github.com/jline/jline3/commit/29077a2817076203d5608bf2169d5678303d54b4) ConsoleEngineImpl: fixed StringIndexOutOfBoundsException
* [`33dad4a4`](https://github.com/jline/jline3/commit/33dad4a4f6b5182d6939938794f6eab30252390d) Improved script file detection and printing
* [`09f14e5d`](https://github.com/jline/jline3/commit/09f14e5da0c1905a692a2130298f1b401e5315ea) OptionCompleter: added a test, script and improved comment
* [`4ae7e843`](https://github.com/jline/jline3/commit/4ae7e8430f38ea24bdc274ddd6ad926db7c7a0c8) Updated README
* [`ee43eeb2`](https://github.com/jline/jline3/commit/ee43eeb2645ca019fc58e187fd3cb8f569581857) Merge pull request #499 from mattirn/jline-script
* [`f445ed84`](https://github.com/jline/jline3/commit/f445ed8402a4117e78ddf489f8e0da0bdbeb0fe3) GroovyEngine: added support for function and and class implementation
* [`104ba603`](https://github.com/jline/jline3/commit/104ba603dcbc65991f81fd1405cbd93786b40f53) Added comment in builtins pom and removed jline-groovy from jline pom
* [`7c3a8e3a`](https://github.com/jline/jline3/commit/7c3a8e3a6c9a4e8e100940c5a1bc3eef59100cdc) ConsoleEngineImpl: added alias and unalias commands
* [`84cf18e4`](https://github.com/jline/jline3/commit/84cf18e428619f8da07096a2e6d87019dd8ecf85) SystemRegistryImpl: tab completion for local commands & improved help
* [`5320ef38`](https://github.com/jline/jline3/commit/5320ef3866408130796cffad6646ddbceb17b6ac) Variables and scripts tab completion
* [`4b040eb6`](https://github.com/jline/jline3/commit/4b040eb681633175aec6579a23481a81da695584) Refactoring...
* [`a67db91a`](https://github.com/jline/jline3/commit/a67db91afa34f42ee9f3e4a378593ba3961ceb09) REPL demo: initialization & added example scripts
* [`fe80245a`](https://github.com/jline/jline3/commit/fe80245a9cacc7ce71899b294d2cba928cf40a40) Improved parameter expansion
* [`4e93fdbc`](https://github.com/jline/jline3/commit/4e93fdbc73a55ce45cb32bfd0fb34e86f2549913) Example: removed scriptEngine, consoleEngine & systemRegistry
* [`d1a11344`](https://github.com/jline/jline3/commit/d1a11344b3f3dd16e63fced88e8ab9c2a3f9a259) Refactoring...
* [`c77a3766`](https://github.com/jline/jline3/commit/c77a37667869fb33530261a4e63a389c9a3f2e55) Added Groovy REPL in demo
* [`ce13d2a8`](https://github.com/jline/jline3/commit/ce13d2a84ad77bfddffddae85f6e259b57dd4755) SystemRegistry: improved help
* [`e753abe2`](https://github.com/jline/jline3/commit/e753abe20cc3c355f5d3645d019d8b6fcd28ef88) ConsoleEngineImpl: implemented command completers
* [`82a9eb0a`](https://github.com/jline/jline3/commit/82a9eb0aff36e522a383d5b24c1b36db3d4c32cf) 'prnt <exception>' prints now stack trace
* [`1e289b63`](https://github.com/jline/jline3/commit/1e289b630e0f0a32e055122436558ea93da8e76a) added slurp command + removed obsolete code
* [`4276a8b1`](https://github.com/jline/jline3/commit/4276a8b1d5a9f2fa0875fb6d7d6830467579df5f) Script execution: added builtin options -? and -v.
* [`9546e896`](https://github.com/jline/jline3/commit/9546e896cee646526a0c2892ddfa3be1cc1b55df) Added comments
* [`a526e18d`](https://github.com/jline/jline3/commit/a526e18d62051376f90f56fcc0b42e3ebda11be8) SystemRegistry: added method initialize()
* [`af2d980a`](https://github.com/jline/jline3/commit/af2d980a12634b0dfb926a88ca2664537d191ab2) Console command 'del *' does not delete SYSTEM_VARS
* [`c279d610`](https://github.com/jline/jline3/commit/c279d61083f23eef5f240af82da1aa96338e17af) ConsoleEngine: implemented invoke() method
* [`5863651c`](https://github.com/jline/jline3/commit/5863651c6f911490f12d07a3d3da166032795143) Manage console variable PATH
* [`b1e1ff93`](https://github.com/jline/jline3/commit/b1e1ff936afb24190a294bcf7079b7209f4562eb) Improved parameter expansion
* [`82932326`](https://github.com/jline/jline3/commit/82932326b92e6d37aee04be260a2cbf40a5db648) ConsoleEngine: implemented prnt and echo commands
* [`8065b9ac`](https://github.com/jline/jline3/commit/8065b9acb7a7746b65d106ea58bce37724d2118d) GroovyEngine: added SuppressWarnings annotations
* [`7be7b2b0`](https://github.com/jline/jline3/commit/7be7b2b02dc3b23e3a5d5ee0eebe107c660dd676) Execute command inside code block
* [`5dbec17a`](https://github.com/jline/jline3/commit/5dbec17aec73d6ba0d772f0796141ebecdf7141d) Improved repl printing, step II
* [`0b7b8e35`](https://github.com/jline/jline3/commit/0b7b8e353721701e9760b3c2f2673d5e9b2c0461) Fixed compilation
* [`e2616451`](https://github.com/jline/jline3/commit/e26164515989853ce3038acb6541f7cb83218f9e) Merge branch 'master' into jline-script
* [`634ebb79`](https://github.com/jline/jline3/commit/634ebb7978daa4576ef6d77e2eb51a1d07e237f8) Improved repl printing
* [`47c0373d`](https://github.com/jline/jline3/commit/47c0373db4d7c058468791fcdc3801d31f5fc362) Commandline expansion inside code block
* [`1cc42a46`](https://github.com/jline/jline3/commit/1cc42a46b969a4179cd3d044043b5544af15eea2) Parameter expansion & refactoring...
* [`57c9a619`](https://github.com/jline/jline3/commit/57c9a619fb02c806e3db497012cb3eda99d8908e) Added Master CommandRegistry
* [`55c16240`](https://github.com/jline/jline3/commit/55c1624051350ed1bd27285d1c6d2affcb8a6f91) Implemented simple script execution
* [`939c1d75`](https://github.com/jline/jline3/commit/939c1d750f5da32c99983f3cfa1269c165060157) refactoring...
* [`5e0eb5e2`](https://github.com/jline/jline3/commit/5e0eb5e269fb502ff5bb1d8609f8ddd771a0123e) jline-script

## [JLine 3.13.3][3_13_3]
[3_13_3]: https://repo1.maven.org/maven2/org/jline/jline/3.13.3/

* [`37ef9925`](https://github.com/jline/jline3/commit/37ef99255a95baa7c29371cace5fd8c09f93b513) ExternalTerminal should accept attributes (eg ECHO false) in constructor, 
and set them before starting pump thread, fixes #433
* [`2327d649`](https://github.com/jline/jline3/commit/2327d649ceb1bc8b4a4452b7821f1f0050503bc5) Fix #492: 3.13 prevents scrolling in ConsoleZ
* [`94a83fd1`](https://github.com/jline/jline3/commit/94a83fd1b9a196160a502fac587ef8193344a9f0) Fix wide characters displaying, fixes #431
* [`b4ee6d93`](https://github.com/jline/jline3/commit/b4ee6d93e6df970487c406c874772a1a96ace471) Completer sorting order, fixes #419
* [`78368e48`](https://github.com/jline/jline3/commit/78368e4891d8ef8b388386f493d0bbef22e23d22) SystemCompleter: added null check and test for variable name
* [`0e537cb8`](https://github.com/jline/jline3/commit/0e537cb8f67c5d95c0d7496f7f620666916ec96b) Display & Candidate: added null checks to robust code, fixes #490
* [`dfc070da`](https://github.com/jline/jline3/commit/dfc070dacc99c32e099dc8c2bd8826becfa4586a) OptionCompleter: improved short option value completion, #485
* [`b1ec62ce`](https://github.com/jline/jline3/commit/b1ec62ce4da26e551b4d4d56a232dc65a7f8ea89) Improve Builtins commands tab completion, fixes #488
* [`daba5586`](https://github.com/jline/jline3/commit/daba5586062a600221a49780435e928460763bda) Tab completion: require at least one character to complete command
* [`10a8d991`](https://github.com/jline/jline3/commit/10a8d9914ead2a524451d21836c1f028f48c77e0) OptionCompleter uses now Completer to complete option values, fixes #485
* [`2d596b8f`](https://github.com/jline/jline3/commit/2d596b8ffe4e889c4ef5f536fc8e1b79cd946ebd) Improved help highlight...
* [`064aa776`](https://github.com/jline/jline3/commit/064aa7767aef021c9c5d026267b327f683a31ea8) OptionCompleter: complete values of short option, #485
* [`04ec0722`](https://github.com/jline/jline3/commit/04ec072241c454136c521ceff2e5e3e78bab2cf4) Improved Example by adding a command registry
* [`10840f8e`](https://github.com/jline/jline3/commit/10840f8e84b557bab38ca752b422c286849df01e) Improved nano and ttop commands when using on Windows terminal, fixes
* [`238f0202`](https://github.com/jline/jline3/commit/238f020247f283ac1fdfe2863554697a2046727e) Fixed ansi clr_eos sequences on windows, #481
* [`d0f52f09`](https://github.com/jline/jline3/commit/d0f52f091d86114a5d98eb120f92dce1533e8a0f) nano & less: translate '~' in file argument to 'user.home', fixes #483
* [`536d841e`](https://github.com/jline/jline3/commit/536d841e637a16bcff9ae252222dcb4a48a93f98) TailTipWidgets improvement: show option description in status bar when
* [`43c89282`](https://github.com/jline/jline3/commit/43c892823e08b712b4fa8415324c7414bd89286e) Small improvement in builtins Example
* [`e85af0ea`](https://github.com/jline/jline3/commit/e85af0ea91458146870feaa59f3dbad8134e0c9f) CommandRegistry: added two static helper methods, #480
* [`c36f8784`](https://github.com/jline/jline3/commit/c36f8784a0b12f8715767e7b6a17121b7d68bb2e) TailTipWidgets: auto suggests now also option values
* [`347b1c26`](https://github.com/jline/jline3/commit/347b1c26baf340edbc6322ed64312418790edfb0) TailTipWidgets: fix IndexOutOfBoundsException exception, fixes #482
* [`94b6fcf9`](https://github.com/jline/jline3/commit/94b6fcf901fbd5b09e339bea8128784af59dd974) Added CommandRegistry interface, fixes #480
* [`abb6bb80`](https://github.com/jline/jline3/commit/abb6bb802c08ee0283e5c774059b05a2a0539f36) Clear Status: use clr_eol if terminal does not support clr_eos, fixes #481

## [JLine 3.13.2][3_13_2]
[3_13_2]: https://repo1.maven.org/maven2/org/jline/jline/3.13.2/

* [`fea903cc`](https://github.com/jline/jline3/commit/fea903cc9e78da64d66422f07db1b7890cf18b89) Improve performances when pasting huge strings, fixes #479
* [`7fce4d39`](https://github.com/jline/jline3/commit/7fce4d39ba997457221cfc262871bab3866c3310) Add a maximum buffer length above which advanced features are disabled, #4
77
* [`69197dd9`](https://github.com/jline/jline3/commit/69197dd9de729cee665c4402d17079582518f03e) Builtins: reviewed completers
* [`6a0cb704`](https://github.com/jline/jline3/commit/6a0cb704595e61b3c67e19782220a1fb5740144c) Add LineReader option USE_FORWARD_SLASH, fixes #476
* [`8737ca25`](https://github.com/jline/jline3/commit/8737ca25bf1a9417eae5bb5ae1ffabfcc69695f2) OptionCompleter fails if command has more than one arg, fixed.
* [`ed0d946c`](https://github.com/jline/jline3/commit/ed0d946cf0330a476bd33febbba3efa99e008435) Merge branch 'command-assignment'
* [`4c73a52c`](https://github.com/jline/jline3/commit/4c73a52c659807efd3e35e2f60a3ec59cd66642d) Fail to scroll candidate list, fixes #475
* [`0b99819a`](https://github.com/jline/jline3/commit/0b99819a529eea01d2b34f6facfb3615f4e74f10) Example: use Parser.getCommand(line) everywhere
* [`733b3e16`](https://github.com/jline/jline3/commit/733b3e16fc99420a169ecb803bf8c999b07c4a41) Builtins: do not create a new NullCompleters use NullCompleter.INSTANCE
* [`cb6593ad`](https://github.com/jline/jline3/commit/cb6593ad152d1a3ed49e7c6a24d54b55c22db630) Merge branch 'option-completer'
* [`e02d5112`](https://github.com/jline/jline3/commit/e02d5112a9d291759152842632e9713fa3a87882) Example: fixed NPE
* [`9db02fe0`](https://github.com/jline/jline3/commit/9db02fe03bd3478a1bebbbdb1d188be9e0bf43fe) Merge branch 'tailtip-highlight'
* [`55a36a84`](https://github.com/jline/jline3/commit/55a36a845762478e2906205557e6fc57392045f6) Merge pull request #471 from mattirn/builtins
* [`96258c04`](https://github.com/jline/jline3/commit/96258c04d2adc01ac2749f499d11e6abbb2851ae) Execute builtins: we must get command=Parser.getCommand(line)
* [`e4efe36c`](https://github.com/jline/jline3/commit/e4efe36ce25722e93b7f3740ea6de657af8d4419) Parser: added static method getCommand()
* [`21d177a2`](https://github.com/jline/jline3/commit/21d177a226f86eb66beaf12532f7d3e0c47b3927) OptionCompleter: added Function parameter in constructor
* [`93702902`](https://github.com/jline/jline3/commit/93702902f6814ea40c12ad61c75e1cb458bb38ce) Added OptionCompleter class
* [`4fab621b`](https://github.com/jline/jline3/commit/4fab621b2dbcba0aa33ffb69e17b8935c8693540) TailTipWidgets: highlight command descriptions
* [`06eb15f8`](https://github.com/jline/jline3/commit/06eb15f8ef3e3421712942ebb080e10f787dafe0) Improve builtin commands integration with JLine app
* [`2e5820eb`](https://github.com/jline/jline3/commit/2e5820eb9f95cf18c8d008b5d059f1f49c6f1ab6) Merge pull request #456 from mattirn/tailtip-v2
* [`40834819`](https://github.com/jline/jline3/commit/40834819166c31d1b99c540530dd8a69a5e62eda) TailTipWidgets: added boolean field command in CmdDesc class
* [`1bb85c2f`](https://github.com/jline/jline3/commit/1bb85c2fe4973464f6427cdc1df122eb72bd74dc) tailtip-toggle: call widget redraw-line to refresh cursor position, fixes #468
* [`129c8755`](https://github.com/jline/jline3/commit/129c8755dd888c6aff35c2797056e5fd92b8454e) widget: list also builtin widgets, fixes #467
* [`c2a46f1a`](https://github.com/jline/jline3/commit/c2a46f1a16f1e44b8c500998c52b501ad711a3e8) Add setvar command: set lineReader variable value, fixes #466
* [`440d57ec`](https://github.com/jline/jline3/commit/440d57ec26ef9b865212b2990e9f9501924b4b5f) StringsCompleter: changed stringsSupplier to Collection, #464
* [`3a5d1324`](https://github.com/jline/jline3/commit/3a5d13249e8d5df1d71ea898055fb3650ab6fc4d) Merge branch 'master' into tailtip-v2
* [`a8906604`](https://github.com/jline/jline3/commit/a89066045f5320bca30971b25b7951c30c80f63c) Extend StringsCompleter to accept a lambda for the strings, fixes #464
* [`812ad2c5`](https://github.com/jline/jline3/commit/812ad2c5e34a0491d066403fea01d9069ebd8283) LineReaderImpl: replaced some method calls with callWidget() in order to r
efresh TailTipWidgets description pane
* [`4e974782`](https://github.com/jline/jline3/commit/4e9747825bd49491acc733fb0f526cba51b63801) nano: in help switch off syntax highlight, fixes #463
* [`92be6215`](https://github.com/jline/jline3/commit/92be6215952b49e7c2a397c6844794f3e1f778c8) Merge branch 'master' into tailtip-v2
* [`454a286a`](https://github.com/jline/jline3/commit/454a286a7ac1c36dc31a57f68aeade0ab6f0fa18) LineReaderImpl: readLine() final redisplay() do not add autosuggestions, f
ixes #461
* [`2189b1fb`](https://github.com/jline/jline3/commit/2189b1fbf0850b39f2d79b25e2e845f690db3998) Widgets: args() method parse in ParseContext.COMPLETE, fixes #460
* [`b2bb7b5c`](https://github.com/jline/jline3/commit/b2bb7b5c6cb5088e68b81e273aa6e26df63dee92)  Auto indentation: fix cursor position calculation, fixes #459
* [`2fb98453`](https://github.com/jline/jline3/commit/2fb984538832c37d738be94395d54c20a9c6730b) LineReaderImpl: clear commandsBuffer only in case of Exception, fixes #458
* [`92c598b3`](https://github.com/jline/jline3/commit/92c598b375d58ea888ab69c8e2a0213b8d8b4d8f) Insert closing bracket only when we have two unclosed brackets, fixes #457
* [`36bfbb6e`](https://github.com/jline/jline3/commit/36bfbb6e8ed686de20e56cc2fc9c2339dd662d54) TailTipWidgets: added setTailTips() method and field valid in CmdDesc clas
s
* [`c926b8b6`](https://github.com/jline/jline3/commit/c926b8b645354060ab1accb428bea7bb73211870) Highlighter: added errorIndex
* [`9e2741cf`](https://github.com/jline/jline3/commit/9e2741cff3a64814f2dfc6e1c92c772e997cf75a) Check syntax errors on closing parenthesis
* [`dd8098b3`](https://github.com/jline/jline3/commit/dd8098b37beff17f7b63e6f77e0279bb0891acba) FileNameCompleter catch and ignore all exceptions, fixes #453
* [`8e47654f`](https://github.com/jline/jline3/commit/8e47654f7ae9888df90551622b1a7e9a6a419aed) Merge branch 'master' into tailtip-v2
* [`da89fc4c`](https://github.com/jline/jline3/commit/da89fc4c22429794fc662f6cef7195395c664337) Nano SyntaxHighlighter: extended style syntax with styles bold, faint, ita
lic, ...
* [`3911e26d`](https://github.com/jline/jline3/commit/3911e26d5bc8ad7e16e1ea09597a99d512f7c5a9) Syntax error highlight
* [`bbdfc0ed`](https://github.com/jline/jline3/commit/bbdfc0ed0da2b649ff250b149ce00092d1340a7e) TailTipWidgets: added support for method descriptions
* [`7a9396da`](https://github.com/jline/jline3/commit/7a9396da2fc1f2c63528928da1500056f38fecdc) Nano: make SyntaxHighlighter public

## [JLine 3.13.1][3_13_1]
[3_13_1]: https://repo1.maven.org/maven2/org/jline/jline/3.13.1/

* [`10751890`](https://github.com/jline/jline3/commit/10751890388d54ef7340bda851f80ec25475f180) Upgrade the javadoc maven plugin to avoid exception when building with new
er JDK
* [`59231795`](https://github.com/jline/jline3/commit/5923179584de53867fb574afb5a43d207e9297dc) Add missing dependencies to ensure correct build order in multithreaded bu
ild
* [`68234fc1`](https://github.com/jline/jline3/commit/68234fc11419439777fd45df76f1f25d5ecaf87f) Exception when parsing infocmp number "0", fixes #451
* [`6116228a`](https://github.com/jline/jline3/commit/6116228abcc01458abc7e190a53d463fe4dc45b1) DefaultHistory: check index range before getting record, fixes #450

## [JLine 3.13.0][3_13_0]
[3_13_0]: https://repo1.maven.org/maven2/org/jline/jline/3.13.0/

* [`1bb3f423`](https://github.com/jline/jline3/commit/1bb3f423d48c2b6feedd922650983db92c51a2e0) [FELIX-6191] [gogo][jline] The cd command should normalize the directory
* [`e95b2c12`](https://github.com/jline/jline3/commit/e95b2c12588ab2a2c3a98eb97bf4145f3409c4aa) edit-and-execute-command: ensure commandsBuffer is empty when done
* [`621850f4`](https://github.com/jline/jline3/commit/621850f4bcbc7713ee6574f05e3ab32ed3d4526e) TailTipWidgets: a few bug fixes and improvements
* [`21546f91`](https://github.com/jline/jline3/commit/21546f91d5fb4a66b80ab06faf0c33c29cc925ba) Status bug fix: status border is lost after window resize.
* [`eeff0fbd`](https://github.com/jline/jline3/commit/eeff0fbd01099c173e7bffe0d31d159a6a02d212) TailTipWidgets: improved option description
* [`226e89e5`](https://github.com/jline/jline3/commit/226e89e5eca34ff8cc1cc34b76552d97431b384b) Merge remote-tracking branch 'upstream/master'
* [`2ae5901b`](https://github.com/jline/jline3/commit/2ae5901b6452e306d2c8e514fa0ad03cd79314a6) TailTipWidget: added descriptions of command options
* [`e7d15fd3`](https://github.com/jline/jline3/commit/e7d15fd3ee2d59e298d93c6b6c4202f188a53e8c) Merge pull request #445 from snuyanzin/JLINE3_COLLECTION_OF_CANDIDATES
* [`bd23dacc`](https://github.com/jline/jline3/commit/bd23daccc4f37488d428344964033bd73c39fe72) Widgets: added javadocs
* [`211b1b56`](https://github.com/jline/jline3/commit/211b1b5612e3dec8cb14470e6a65c5fbe8e975b8) Add constructor with collection of Candidates
* [`389ec833`](https://github.com/jline/jline3/commit/389ec8331244e2f7bb4eb29e40e3b7ad3704e605) Widgets: Constructors throw IllegalStateException if widgets already exist
s
* [`38f06cd3`](https://github.com/jline/jline3/commit/38f06cd399b6ad57f8c43f4e6b1f1e0dc5e5937f) Allow contemporary use of tailTip and autopair widgets
* [`61ff968e`](https://github.com/jline/jline3/commit/61ff968ef396962f9eaf48a256413d5e0dafed6e) Widgets refactoring: use widget aliases in place of keys rebinding
* [`6d8d7322`](https://github.com/jline/jline3/commit/6d8d7322702125ad0ad63e712b8902d629594e2f) widget command: fix for alias (-a) and list (-l) options
* [`6535be3b`](https://github.com/jline/jline3/commit/6535be3b0d59f3e630fc73de440a775dcb689263) Widget refactoring and fixed formatting
* [`ca0278af`](https://github.com/jline/jline3/commit/ca0278af6f146fb249a9f546f55a254c9b4355df) Merge pull request #354 from mattirn/indentation
* [`3025f70d`](https://github.com/jline/jline3/commit/3025f70d953f801e19781472f5cfe0b7e3ab976f) Fixed merge
* [`d88c1c7a`](https://github.com/jline/jline3/commit/d88c1c7a862148ec2e40f9488f82b2c3e59298a5) Merge branch 'master' into indentation
* [`f669f8bd`](https://github.com/jline/jline3/commit/f669f8bd413c8d2c4c88561e8aafb19c6eebf836) DefaultParser: Replaced '){' with ') {' ...
* [`95b6a654`](https://github.com/jline/jline3/commit/95b6a654691f80fdb7f0c54ef78ddeb4584ddb86) Merge pull request #440 from mattirn/tmux-windows
* [`cdbb55eb`](https://github.com/jline/jline3/commit/cdbb55eb19ef6a533d5b5f2e382e20f828c14be8) tmux: Added a couple of windows commands
* [`2c581637`](https://github.com/jline/jline3/commit/2c581637446ad014a930a780976768b335ff0053) Status: added indication of truncated status lines and status border
* [`5c464bce`](https://github.com/jline/jline3/commit/5c464bce1f9566aaf325cb246712fab121e66635) TailTipWidgets: bug fix & small improvements
* [`3462231f`](https://github.com/jline/jline3/commit/3462231f74195108703fe77abdedb0b0c8e7e382) TailTipWidgets added argument descriptions, fixes #254
* [`55567b0b`](https://github.com/jline/jline3/commit/55567b0b6a3d0e661ef18da4fa8ff046b02a1d24) Added TailTipWidgets, fixes #394
* [`969fa1a4`](https://github.com/jline/jline3/commit/969fa1a434ba10b3ab7a6e701e97713c09619b3d) Autosuggestion using command completer data, #254
* [`4c48c039`](https://github.com/jline/jline3/commit/4c48c039c0559638cfa2ac8ba12d3aa237d8870e) Added custom autosuggestion widgets
* [`58d60156`](https://github.com/jline/jline3/commit/58d60156aa429f1a7b979966efde35676c4faf1d) AutopairWidgets: refactoring...
* [`54dfee6f`](https://github.com/jline/jline3/commit/54dfee6fe756313f48ee865fd2732917662bb018) nano: now manages tabs correctly also in wrapped lines
* [`4abce452`](https://github.com/jline/jline3/commit/4abce4520d887e6f174790b6b70abe5220775532) AutopairWidgets: now restores defaultBindings correctly
* [`20076f29`](https://github.com/jline/jline3/commit/20076f29a50035d5ab80841d28f83176e213130b) nano: fixed bug in 'search and replace'
* [`b996a9a1`](https://github.com/jline/jline3/commit/b996a9a10af4ed5c18462bd1aaa194c51e2835d4) Merge remote-tracking branch 'upstream/master'
* [`4abce452`](https://github.com/jline/jline3/commit/4abce4520d887e6f174790b6b70abe5220775532) AutopairWidgets: now restores defaultBindings correctly
* [`20076f29`](https://github.com/jline/jline3/commit/20076f29a50035d5ab80841d28f83176e213130b) nano: fixed bug in 'search and replace'
* [`b996a9a1`](https://github.com/jline/jline3/commit/b996a9a10af4ed5c18462bd1aaa194c51e2835d4) Merge remote-tracking branch 'upstream/master'
* [`e4839f89`](https://github.com/jline/jline3/commit/e4839f8934d4a2c7d966a0bc809dd5c66dbe7210) AutopairWidget: fixed typo & small improvements
* [`0660ae29`](https://github.com/jline/jline3/commit/0660ae29f3af2ca3b56cdeca1530072306988e4d) Merge pull request #442 from nixel2007/fix/npe-pump
* [`f151862b`](https://github.com/jline/jline3/commit/f151862b1be23da74d56246cca18f3d0e245eae5) Added autopair custom widgets.
* [`c31cca7e`](https://github.com/jline/jline3/commit/c31cca7e6b4b48518c6aee5a076b00b67c7be9ea) Fix NPE
* [`65f61c35`](https://github.com/jline/jline3/commit/65f61c357620352b271741dbde9f9da648432ea6) Merge branch 'master' into indentation
* [`9fb78220`](https://github.com/jline/jline3/commit/9fb7822024282037bba6844fbb23afbc62872cb5) added automatic insertion of closing bracket
* [`d9ec9e0f`](https://github.com/jline/jline3/commit/d9ec9e0f1de81ed21490ac5b04f833f84e75b10e) nano: fixed cursor position calculation
* [`2e09ce41`](https://github.com/jline/jline3/commit/2e09ce410984a9b5b187230ed02bb566c874caa6) nano: added options tabstospaces & autoindent
* [`ff75120f`](https://github.com/jline/jline3/commit/ff75120f3c66e922c43b5d4f9aeff40b4e3d6944) nano: refactoring...
* [`e7da7e5b`](https://github.com/jline/jline3/commit/e7da7e5b509746f7340e9f80c5bd2b57263d0452) nano: search and replace operation
* [`23af0afd`](https://github.com/jline/jline3/commit/23af0afd50f80c1669ad24b1f7de90cdaf371e17) nano: added tab-character management, fixed if statement
* [`82b514e0`](https://github.com/jline/jline3/commit/82b514e0eb9b420ba4addd352fb5712c2c1900a0) nano: added tab-character management
* [`d9ea992e`](https://github.com/jline/jline3/commit/d9ea992ebeff402367615d293edd527a01a7a842) nano & less: added key bindings and improved search log navigation
* [`f2f911c8`](https://github.com/jline/jline3/commit/f2f911c87b630147a3dda1eceedff2bddbbb6cd9) nano & less refactoring: added usage() method
* [`241332c1`](https://github.com/jline/jline3/commit/241332c1a9a11169ccdc1e94227a5aa7a27653c6) Added missing license header, fixes #439
* [`c9e3f7d5`](https://github.com/jline/jline3/commit/c9e3f7d52044c5a15d08ec5776d87c87db619a56) nano & less commands: added historylog option
* [`f22d7e79`](https://github.com/jline/jline3/commit/f22d7e79cf538f67af4ea58292ceaf5fda38fc43) tmux: NPE and ConcurrentModificationException, fixes #438
* [`718e9df9`](https://github.com/jline/jline3/commit/718e9df9ec902181f15ccd57f872078faa436fc5) Highlight demo help
* [`a479daf1`](https://github.com/jline/jline3/commit/a479daf1839f42255beccda938f939af308d982e) Adjusted help highlight patterns
* [`ff1f07a1`](https://github.com/jline/jline3/commit/ff1f07a1affe6858dfddb27edc8655be91b6400d) less command: implemented search operations with spanning files
* [`b79a4255`](https://github.com/jline/jline3/commit/b79a42551c96affb2a9d4a4db22f8c30b4097a72) nano command: implemented a few missing options
* [`10b405ba`](https://github.com/jline/jline3/commit/10b405ba0e9e8a094859ac033745edb5ec5cd33d) Reviewed: nano and less commands wildcard file parameter
* [`43b1e1de`](https://github.com/jline/jline3/commit/43b1e1dea003c0832f67182f725f0c25a48c50be) Merge pull request #437 from mattirn/config-path
* [`270b52f1`](https://github.com/jline/jline3/commit/270b52f17b89b7729f4288e48bc49c77acaa0a8a) Improve (nano & less) command configuration management
* [`4a67e7f8`](https://github.com/jline/jline3/commit/4a67e7f8584b31e954802a09cb7421ead07281e1) Fixed javadoc warnings
* [`fd8743fa`](https://github.com/jline/jline3/commit/fd8743fa920757404174580db4f473bbe4eee295) nano command: added commandline options
* [`0af30363`](https://github.com/jline/jline3/commit/0af3036323a79265f9425f793e287c88275f30a4) Enhanced less command with nanorc like config and syntax highlight
* [`f9879070`](https://github.com/jline/jline3/commit/f9879070de4894ddf217dc7a79c1271e73eb02c7) Nano config file: support 'set/unset <option>'
* [`470206b5`](https://github.com/jline/jline3/commit/470206b541bb7bb4e111e4f2cf1db48b7207864d) Nano: manage configuration file. Now support only 'include' entries.
* [`5888c782`](https://github.com/jline/jline3/commit/5888c782eb1401e916cf173505d889a66264e363) nano syntaxHighlighter: read highlight config from nanorc files
* [`f1e38205`](https://github.com/jline/jline3/commit/f1e38205daefe9a8ebd3b1f258fed307caa3cd05) less command: fix next/prev page operation, fixes #434
* [`f3bea600`](https://github.com/jline/jline3/commit/f3bea600540ebe369de1887609cd67ff6a156309) Merge pull request #427 from bjab/dumb-color-fix
* [`13708e2d`](https://github.com/jline/jline3/commit/13708e2d6baabe8a9b295b566bdb7f461ae550c8) Implemented edit-and-execute-command, fixes #430
* [`39b61e55`](https://github.com/jline/jline3/commit/39b61e558d3f44b2fc149e342679f66451293b80) Merge pull request #432 from mattirn/openjdk
* [`66d71a08`](https://github.com/jline/jline3/commit/66d71a089dbdb463b98dc1a1c996b4b4f3a5635b) travis: changed oraclejdk8 to openjdk8
* [`fd6b36d7`](https://github.com/jline/jline3/commit/fd6b36d7ec74e9a1055f91c5175ad3d7f40363e9) Merge pull request #421 from mattirn/history-command
* [`d87545d5`](https://github.com/jline/jline3/commit/d87545d5b4d4e25982838538fd81783bae2b106c) Merge branch 'master' of https://github.com/mattirn/jline3.git into histor
y-command
* [`2eb14ad8`](https://github.com/jline/jline3/commit/2eb14ad85365c0a43bb3f6687d0b263aae23b836) history command: implemented options -e and -s
* [`742b0cba`](https://github.com/jline/jline3/commit/742b0cbade1a631c7e659619142ab6c57118e15f) Fix for #328 Properly register dumb-color terminal caps file
* [`bd6bbb16`](https://github.com/jline/jline3/commit/bd6bbb16f7e57181d1f60a0f54a5c80265b06e61) nano syntaxHighlighter: removed obsolate code
* [`61254920`](https://github.com/jline/jline3/commit/612549205d9796806e894f64c13492f7cdcc1704) nano: fixed xml highlight rule
* [`fc3579e5`](https://github.com/jline/jline3/commit/fc3579e51fdcb066ce89557569d38b87f557840a) nano command: added syntaxHighlighter (hardcoded xml & java)
* [`4e4969cb`](https://github.com/jline/jline3/commit/4e4969cb6eed603b3264fe51a62684663a582638) nano: insert/backspace operations ensure cursor in visible area, fixes #42
6
* [`9667327f`](https://github.com/jline/jline3/commit/9667327f11c7fb65e1845ea911eaa49a15928042) nano: refactoring...
* [`2a8493df`](https://github.com/jline/jline3/commit/2a8493df85448fceadb8f6ff32592f68cd7ace4d) nano command: navigation with arrow keys on unwrapped line
* [`d24e4e6d`](https://github.com/jline/jline3/commit/d24e4e6d11826524939a664b6eff500a28f4c23c) Merge remote-tracking branch 'upstream/master'
* [`82e8dcbd`](https://github.com/jline/jline3/commit/82e8dcbd154622ed82e4bfdc6dc4e68cb3de082b) nano command: re-enabled non wrapping mode, fixed #120
* [`0749251f`](https://github.com/jline/jline3/commit/0749251f4cd622869c3da0307268e898f60ed479) Merge pull request #424 from snuyanzin/patch-1
* [`3cb846ff`](https://github.com/jline/jline3/commit/3cb846ff8345846ba9158ecaca9cf7f5dcf05f29) Typo in LineReader.java's comment
* [`9511bfae`](https://github.com/jline/jline3/commit/9511bfae1ddf20ef4739fb2bebf1fe556362e772) nano command: added mark operations
* [`db6a91c1`](https://github.com/jline/jline3/commit/db6a91c12cd386663258fc9ffd9fa2602c9ca91d) nano: fixed cursor position calculation in nextSearch(), fixes #423
* [`a0305d6e`](https://github.com/jline/jline3/commit/a0305d6e94a974d5fbc9624846905fa3e945df85) nano command: added 'cut to end' operations
* [`53fa8586`](https://github.com/jline/jline3/commit/53fa85866b521a9ddac1089307fa79c2af2e290f) nano: next/prev page operation, keep cursor in upper right corner, fixes #
422
* [`32a44aaf`](https://github.com/jline/jline3/commit/32a44aafd1ca1a7b189740fea72480b6c17ff806) nano command: restored computeFooter() broke in previous commit
* [`490b623c`](https://github.com/jline/jline3/commit/490b623cc0bfd05e75b05ba909f2f37a1a9582d8) nano command: fixed endOfLine() method. Was failing with wrapped lines.
* [`127212d4`](https://github.com/jline/jline3/commit/127212d4ab7f101b915120bd454556deed1a32ae) nano command: implemented 'go to line' operation
* [`7e4263e8`](https://github.com/jline/jline3/commit/7e4263e8212e64eae6816157a62d10b252656e49) nano command: added copy, cut and uncut text operations
* [`5d12bcc0`](https://github.com/jline/jline3/commit/5d12bcc0692bb6241676f7e60b8a6d2ff0452580) FilesCompleter & DirectoryCompleter: force forward slash delimiter
* [`00891d43`](https://github.com/jline/jline3/commit/00891d4312d86d5eaeb1b24b003f5b888f4b52e2) less command: added repaint operations
* [`f7eaabc6`](https://github.com/jline/jline3/commit/f7eaabc63f999b2c1c1c48f1a7e5af0205117363) less command: added 'Examine a new file' operation
* [`93f63bc1`](https://github.com/jline/jline3/commit/93f63bc1d8dc386ec18ac157a5ef7a364752bfaf) Merge pull request #416 from mattirn/less-command
* [`141743e7`](https://github.com/jline/jline3/commit/141743e7091daa314ed2a5df8428687f5c530b00) Merge branch 'mattirn-wildcard-files'
* [`cd355cf4`](https://github.com/jline/jline3/commit/cd355cf49552e0a45b2e9271105042a3bf7b15b7) Merge branch 'wildcard-files' of https://github.com/mattirn/jline3 into ma
ttirn-wildcard-files
* [`032ef690`](https://github.com/jline/jline3/commit/032ef690cbca38827618f5f0b0e36da858c62e8c) Merge branch 'mattirn-history-command'
* [`aefb02d0`](https://github.com/jline/jline3/commit/aefb02d000bd6f4e26edb8062399071dadc1dbf2) Merge branch 'history-command' of https://github.com/mattirn/jline3 into m
attirn-history-command
* [`434c51cc`](https://github.com/jline/jline3/commit/434c51cc8cbcc417e6075aaf0dce5f1c3ad1365d) Nano: fix saved file path, fixes #420
* [`032445a3`](https://github.com/jline/jline3/commit/032445a3e351f5281ad2e3531dcbc14f6205247a) less command: added home and end key mappings & improved moveForward()
* [`6e12b27b`](https://github.com/jline/jline3/commit/6e12b27bbaa99aa7ad9c0d30623204fc58ee3739) less command: added key mappings for pattern line edit
* [`b33ea922`](https://github.com/jline/jline3/commit/b33ea9228809ab28ea7b694b48a35643e506c13b) less command: impl. file operations examine Nth file, delete and print cur
rent file name
* [`382062ba`](https://github.com/jline/jline3/commit/382062ba09379e409f14330152bf304f147ab0e7) less command: search '&pattern' display only matching lines
* [`e2dd9bf6`](https://github.com/jline/jline3/commit/e2dd9bf6c64be7c4c1b17a029471a981914260f2) less command: repeat search backward operation
* [`cff84a93`](https://github.com/jline/jline3/commit/cff84a933b0c4105fe8826169461ab720d6900ad) nano and less commands: accept wildcards in file parameter
* [`49e646d6`](https://github.com/jline/jline3/commit/49e646d6bd59d06cf4a36bf3e0b1d79b7aead23e) history method: add currentDir parameter and pattern match uses option Pat
tern.DOTALL
* [`a1f2c93f`](https://github.com/jline/jline3/commit/a1f2c93fa194f7adeab0c39922b1452fa83e4d69) Extend FilesCompleter/DirectoriesCompleter to accept a lambda for the curr
ent dir, fix for #413

## [JLine 3.12.1][3_12_1]
[3_12_1]: https://repo1.maven.org/maven2/org/jline/jline/3.12.1/

* [`d5037af1`](https://github.com/jline/jline3/commit/d5037af1fb517887787e47c7662ecbfad16fd8c5) Merge pull request #412 from mattirn/issue-411
* [`2daaf11b`](https://github.com/jline/jline3/commit/2daaf11bf23316408a560be3d1128fcfe8081315) Fix dumb terminal prompt, fixes issue #411

## [JLine 3.12.0][3_12_0]
[3_12_0]: https://repo1.maven.org/maven2/org/jline/jline/3.12.0/

* [`67c14726`](https://github.com/jline/jline3/commit/67c1472671f6f77563dc607212fe1032ac05948d) Fix indentation
* [`2fc0d902`](https://github.com/jline/jline3/commit/2fc0d90292b2d039f1cb88438ddb14addfdb9b80) The status gets created unwillingly, also fixes #403
* [`be2dbb98`](https://github.com/jline/jline3/commit/be2dbb98949cef496f2e2d1b83f74cfda4fba9cc) Merge pull request #401 from Minecrell/printabove-ansi-reset
* [`0376b965`](https://github.com/jline/jline3/commit/0376b965ca6e2ed6a33a17314124bcaa069e1312) Merge pull request #407 from snuyanzin/JNA_UPGRADE
* [`0fd273ff`](https://github.com/jline/jline3/commit/0fd273ff3ba1a789071564fc14dfe16f37ee0cf3) Merge pull request #402 from snuyanzin/STRING_COMPLETER_ALLOW_CANDIDATES
* [`c9cd33a9`](https://github.com/jline/jline3/commit/c9cd33a9f91fea11eadc225fb855edbb96755077) Fix merge problem
* [`8fe8b4cb`](https://github.com/jline/jline3/commit/8fe8b4cb371ca335f30d2d44861f4d56bb6db67e) Merge remote-tracking branch 'mattirn/history-command'
* [`4e87ebc0`](https://github.com/jline/jline3/commit/4e87ebc0587010fe210edc126b1c280f0105dfe1) Merge remote-tracking branch 'mattirn/multiline-editing'
* [`b4f594ee`](https://github.com/jline/jline3/commit/b4f594ee7c11e0ce58937da84e8a7614d7be219e) Merge remote-tracking branch 'mattirn/less-command'
* [`2655e1d0`](https://github.com/jline/jline3/commit/2655e1d097f01a4f60cc8a1be0d7e7a9abcd3aee) Merge remote-tracking branch 'mattirn/nano-command'
* [`8bd354dc`](https://github.com/jline/jline3/commit/8bd354dc943a85fd05f95443bbd8343ee9e21c47) Merge remote-tracking branch 'mattirn/demo-watch'
* [`fe076844`](https://github.com/jline/jline3/commit/fe076844e748608b35934ca10ec9f699d8cc694d) Merge remote-tracking branch 'mattirn/less-help'
* [`c3f81e2e`](https://github.com/jline/jline3/commit/c3f81e2e09d9b4e55a53da94fba2cef3eab16879) Command less: do not print line numbers after EOF has been reached
* [`2ba4c783`](https://github.com/jline/jline3/commit/2ba4c783689e572bc767b1c6340c2795832f133e) Command less: handle number param in 'next/prev file' and 'go to line'
* [`b34b0290`](https://github.com/jline/jline3/commit/b34b0290a7c5237504629162b0dba168eac6f4a5) Command less: added help
* [`864f2063`](https://github.com/jline/jline3/commit/864f2063255b6f1bedce1605ae4ff76d5f22b55c) Support octal in infocmp capabilities, fixes #408
* [`740a395a`](https://github.com/jline/jline3/commit/740a395a65de28c8efd41fe6243e452d340b0f58) retry travis
* [`9d24f655`](https://github.com/jline/jline3/commit/9d24f655b78d52a871d109f1676ff788e372ed3b) Upgrade jna to 5.3.1
* [`fa53f654`](https://github.com/jline/jline3/commit/fa53f6546adb7f08db0d07e91d64920a7567a918) Command watch: option --interval added missing argument
* [`a9fb9489`](https://github.com/jline/jline3/commit/a9fb9489417f5e23b749ef863bbffb60a2937f78) Allow specify Candidates with group, keys and etc for StringCompleters
* [`44f208b3`](https://github.com/jline/jline3/commit/44f208b37b4a52567951b8e414cd663c52f3cfed) Fix issue #399: Nano editor fails to display long lines
* [`7b3aa200`](https://github.com/jline/jline3/commit/7b3aa200b61f006338a5e1a20d5bdc0332b25c07) Avoid inserting duplicate line break in printAbove if line ends with ANSI 
reset
* [`31d7d9d1`](https://github.com/jline/jline3/commit/31d7d9d19fd450c8354cad7fec4a38780f1e009b) Bug fix: nano breaks if status enabled
* [`9ef1cc25`](https://github.com/jline/jline3/commit/9ef1cc255a03ea58d6cb3705914d7dd4f5ee8c2a) Merge pull request #395 from snuyanzin/CORRECT_MSG
* [`f1dce105`](https://github.com/jline/jline3/commit/f1dce105110836716f63ea019f9a693f687fe986) Bug fix: less fails to view first line(s) if status enabled + reviewed tab option implementation
* [`a4acf40d`](https://github.com/jline/jline3/commit/a4acf40d1fcebdb15dc731ddd806a9ddd3fe43f3) Command less: implemented tabs option
* [`8d1c9036`](https://github.com/jline/jline3/commit/8d1c903684e539b71a4f030427b19284658a3359) Remove duplicate 'to see' from warning message
* [`062d25ca`](https://github.com/jline/jline3/commit/062d25ca0c2989df72bc1c1787495c713fea9a48) Fix issue-311: Multiline editing breaks if input has more lines than terminal
* [`ea8b360e`](https://github.com/jline/jline3/commit/ea8b360e2b67d9109be992b065c37a62c4f1464c) history command: pattern argument improvements
* [`6d2855ea`](https://github.com/jline/jline3/commit/6d2855eaa2019012cf9f5b90bc463a301baf08af) history: command arguments [first] and [last] may be specified as strings
* [`db708373`](https://github.com/jline/jline3/commit/db7083737ec12461ce3163c4ea3dccd17e54f9ba) Merge pull request #377 from snuyanzin/JLINE3_376
* [`4d941b59`](https://github.com/jline/jline3/commit/4d941b597d9df77597ec571ab89d2c6891089474) [JLINE3-376] Add a test indicating ArrayIndexOutOfBoundException, adaptrs fix based on review comments
* [`985d2cd8`](https://github.com/jline/jline3/commit/985d2cd80e85cf4ee8b8d90443aa235ef13b0be9) Merge pull request #374 from mattirn/example
* [`fd798524`](https://github.com/jline/jline3/commit/fd798524aad1be531b843990417c453622fa2daf) Merge branch 'mattirn-status-hardReset'
* [`be0be9e7`](https://github.com/jline/jline3/commit/be0be9e71d7a19bba47cc61ac8a936c56ce9e754) Remove unneeded indentation
* [`7e08b928`](https://github.com/jline/jline3/commit/7e08b928f6638d313ed073b135a59cb0d4c2ef2a) Merge branch 'status-hardReset' of https://github.com/mattirn/jline3 into mattirn-status-hardReset
* [`795f2eaf`](https://github.com/jline/jline3/commit/795f2eaf90509a753999e997208371270fdb2b44) Merge pull request #386 from mattirn/jline3_384
* [`fff801da`](https://github.com/jline/jline3/commit/fff801daffb70cffd5630c66d5496b0289ed5977) Merge branch 'tpoliaw-sessions'
* [`c37db139`](https://github.com/jline/jline3/commit/c37db13943f3f9071f1bbcb19909f08f67d10625) Merge branch 'sessions' of https://github.com/tpoliaw/jline3 into tpoliaw-sessions
* [`afe59b20`](https://github.com/jline/jline3/commit/afe59b207f5cad8d77e11a6fe763e27ddad24a47) Merge pull request #373 from hyee/master
* [`85e60cb3`](https://github.com/jline/jline3/commit/85e60cb3c7eec4f32177bf90e868e23d3b21fbc9) Merge branch 'mattirn-nano-restricted'
* [`cc6a2ace`](https://github.com/jline/jline3/commit/cc6a2ace792b3a2dd07578d559f9c05714bf4a4b) Add nano restricted mode to the demo
* [`fd943359`](https://github.com/jline/jline3/commit/fd943359b796b157eaf6dcb672b3a8611346513a) invoke status.hardReset() after resizing terminal window
* [`4683d8e6`](https://github.com/jline/jline3/commit/4683d8e626ccf95d62b9393820b3888491bd79fb) fix NPE: redisplay() before readLine()
* [`860c0d64`](https://github.com/jline/jline3/commit/860c0d64a874ab7e327b0a21cc434a9aaf232149) Make ShellParams and ExecuteParams aware of the session
* [`3424fa3f`](https://github.com/jline/jline3/commit/3424fa3fd81449fe3fc690ebaa51689bd570e431) nano: added arrow key mappings in search line editing
* [`44153742`](https://github.com/jline/jline3/commit/44153742eeb3c46cc77b761aa6e962d1c4f0618d) nano: support backup/prepend/append & added arrow key mappings in read/write file name editing
* [`b3990070`](https://github.com/jline/jline3/commit/b399007090bd92c572ac65383a039d120abe6cd9) Implemented nano option restricted mode
* [`6808d75b`](https://github.com/jline/jline3/commit/6808d75be1bc81dd164b5b707aff20845d4ac4fa) added missing commands in help
* [`6b046b86`](https://github.com/jline/jline3/commit/6b046b8613a5a545e38aeae27f96bf5f2bb217b5) clean up...
* [`1555e65f`](https://github.com/jline/jline3/commit/1555e65f3b3e84c3433ead8a4d248c6ce81f6e01) Example: added help and completed usage
* [`136f3c40`](https://github.com/jline/jline3/commit/136f3c401b12716685a7e167536bc4aa04898d7e) Fix completer issue(#303)

## [JLine 3.11.0][3_11_0]
[3_11_0]: https://repo1.maven.org/maven2/org/jline/jline/3.11.0/

* [`ded05b8`](https://github.com/jline/jline3/commit/ded05b86f83c0a9499f103216ff600cca701a2b4) Rewrite the built-in function help highlighter to avoid manipulating ansi sequences directly
* [`2a4646a`](https://github.com/jline/jline3/commit/2a4646a33ddf2837b700de37ee809c22762cc74d) Add a method to style matching strings with groups
* [`4c66dfd`](https://github.com/jline/jline3/commit/4c66dfd175027a96ed3cd2f8456236f679b6293a) Add print()/println() methods to AttributedCharSequence to print to the terminal
* [`fd6eb24`](https://github.com/jline/jline3/commit/fd6eb2426e546fe79b83679f19d027a8e7fe13df) Upgrade to jansi 1.18
* [`108cd80`](https://github.com/jline/jline3/commit/108cd80839283502dd5f650eb8627157974737ef) Upgrade to jansi 1.18
* [`f7f9604`](https://github.com/jline/jline3/commit/f7f960404a499006b7f05b0e7e950c6dacac29c5) Fix ssh support in demo
* [`1c850e1`](https://github.com/jline/jline3/commit/1c850e16eccb7944abeb744db363968ad29f4fcf) Fix compatibility with jansi < 1.17 on windows, fix #369
* [`4f100ba`](https://github.com/jline/jline3/commit/4f100baa1974d9783433ecc1737fdf2d08a91e0c) Merge branch 'help-printer' of https://github.com/mattirn/jline3
* [`1d664b1`](https://github.com/jline/jline3/commit/1d664b1331a50e78dab4cb987d1e0a555e34d1ce) Fix ssh support in demo
* [`5664137`](https://github.com/jline/jline3/commit/5664137ae9adb681b203279b7969a3aae5270ca7) added test if terminal support ansi
* [`7b37f7f`](https://github.com/jline/jline3/commit/7b37f7fdd51644f2e7c2eb27df0a0ccaf1f72811) multi-instanciable HelpPrinter with the JLine's StyleResolver
* [`cb027ad`](https://github.com/jline/jline3/commit/cb027adc7be960eb8695bddc2a5bf2983462e37f) Merge branch 'master' into help-printer
* [`05b89e4`](https://github.com/jline/jline3/commit/05b89e49e38ba89f9e04750ee106a5fd68535431) Provide an easier way to retrieve jansi version, fixes #366
* [`4b70fb5`](https://github.com/jline/jline3/commit/4b70fb5459f169c67e9fdda077353355fcab4bae) Merge pull request #358 from mattirn/accept
* [`c4b521c`](https://github.com/jline/jline3/commit/c4b521c0006288a92e9082ce423c1be134e145e7) Merge pull request #363 from mattirn/history
* [`2a5798c`](https://github.com/jline/jline3/commit/2a5798c8dcadd478206be9b1b48b5c3c88dbf5f3) Merge branch 'issue-364'
* [`b3e6ed2`](https://github.com/jline/jline3/commit/b3e6ed2e805214a390fe5a48d8dd9770ae949435) Small code cleanup, #364
* [`514c516`](https://github.com/jline/jline3/commit/514c51670e04569d40b3e7be43025c9581905f81) Add a simple test for #364
* [`e1b0fde`](https://github.com/jline/jline3/commit/e1b0fdec5269b1919cf0fd7922a789641af8750e) Fix #364: Command completion fails when value starts with escape character
* [`886867c`](https://github.com/jline/jline3/commit/886867ce6a33198e89db7ba449f375a6e1dece23) LineReader encounters exception if tabs used in dummy terminal, fixes #367
* [`6d8f049`](https://github.com/jline/jline3/commit/6d8f0491a579d3ab328eb70f439b88f48f9c1e1d) history command: implemented options -ARWI
* [`d08ba29`](https://github.com/jline/jline3/commit/d08ba29279021cd2888228513c4f3cce3a7b3962) history command: implemented some missing options
* [`81e4c07`](https://github.com/jline/jline3/commit/81e4c07e0664334e6dccb124f382206ee28b358d) Add missing file headers, fixes #362
* [`acb9342`](https://github.com/jline/jline3/commit/acb9342504edb19ee4344e802f018b85a3586163) fixed ttop highlighted help
* [`0b61777`](https://github.com/jline/jline3/commit/0b61777ef040595559e86f370ef115fe24cbf2c1) Highlight builtin commands help
* [`64e6c8d`](https://github.com/jline/jline3/commit/64e6c8d6c1848f55dc686639926d964d3a21b2ca) Widgets: accept-and-infer-next-history, accept-and-hold & accept-line-and-down-history

## [JLine 3.10.0][3_10_0]
[3_10_0]: https://repo1.maven.org/maven2/org/jline/jline/3.10.0/

* [`b65cfe2`](https://github.com/jline/jline3/commit/b65cfe2ff9f8971c2834862b11e7aa3d9add9996) Upgrade to latest gogo and sshd
* [`310c846`](https://github.com/jline/jline3/commit/310c8462a8dca3d86cf47f14888969c738ff07ec) Merge pull request #353 from mattirn/brackets
* [`9a2e06d`](https://github.com/jline/jline3/commit/9a2e06da300eea8639f00bb5b9d3c5dde1768239) DefaultParser: do not check brackets if inside quotes
* [`a6dd868`](https://github.com/jline/jline3/commit/a6dd8683fd9a1ffa465bf574667b799b7fbb82e3) Fix unit tests for conemu
* [`8bb13a8`](https://github.com/jline/jline3/commit/8bb13a89fad80e51726a29e4b1f8a0724fed78b2) Fix conemu problems with less / clearscreen
* [`8b054e4`](https://github.com/jline/jline3/commit/8b054e425e070c941318111ef38cf3cb10f481b7) Fix less on conemu, fixes #344
* [`3b6a67b`](https://github.com/jline/jline3/commit/3b6a67b0a31cd29494a1f94dc029ea3bde7e7799) Provide both terminal window / buffer size on windows, fixes #303, reverts #136
* [`2d4e5e1`](https://github.com/jline/jline3/commit/2d4e5e1a2bc8af8fcdce512ccf192ef770a225a3) Remove groovy dependency, fix #351
* [`cfa7d0c`](https://github.com/jline/jline3/commit/cfa7d0cf6227df2aa5a893ca61b26affcd7beac9) Merge branch 'mattirn-brackets'
* [`2d1e01e`](https://github.com/jline/jline3/commit/2d1e01ed68728f37046d4aa1c98fa92777323934) Merge branch 'master' of https://github.com/mattirn/jline3 into mattirn-brackets
* [`f230c87`](https://github.com/jline/jline3/commit/f230c87921ba1fad5c449625387ae6efaf573293) Fix formatting
* [`faf0ef6`](https://github.com/jline/jline3/commit/faf0ef62b33c8611df8eb72c1f0dd47e1caabac6) Merge branch 'brackets' of https://github.com/mattirn/jline3 into mattirn-brackets
* [`e1d6bc1`](https://github.com/jline/jline3/commit/e1d6bc15cc49238156ae12f51484cef5f662832f) Merge pull request #347 from mkrueger92/master
* [`a8ce4e8`](https://github.com/jline/jline3/commit/a8ce4e85b850c061ecb53278b4eafdb1572e7ff5) KILL_WHOLE_LINE doesn't work for last line in buffer, fixes #339
* [`b807f97`](https://github.com/jline/jline3/commit/b807f97ad4d848a77b21585b434e2be93eee002c) The result of Commands.keymap with '-L' option contains unexpected string, fixes #340
* [`1e6e9a6`](https://github.com/jline/jline3/commit/1e6e9a61d1b86cbcb2352bcd79b929a27aae38c1) AttributedCharSequence.columnSubSequence does not handle UTF-16 surrogate pairs, fixes #314
* [`8de0b68`](https://github.com/jline/jline3/commit/8de0b682cd2fad99bbc0c1d63a40f60785e27ca9) Merge pull request #348 from Charliocat/master
* [`804959e`](https://github.com/jline/jline3/commit/804959e2d14d36dedf55d9834b1db48bb78899db) changed identation on try with resources
* [`efa961d`](https://github.com/jline/jline3/commit/efa961d910c313e0c7f3d3032b93dd77165cb4d1) change to try with resources
* [`3fa0adf`](https://github.com/jline/jline3/commit/3fa0adf98bebbb72dd2e7fec39d091a0644b43f2) Clear internal history in case of currupted histroy file
* [`31a3219`](https://github.com/jline/jline3/commit/31a321933ba7d2b246231af8b1ae455a5736d4a3) use StringBuilder in place of String concatenation
* [`ce92ad2`](https://github.com/jline/jline3/commit/ce92ad2f8defef9b7d035cd408e05d8e868badc8) Missing closing brackets: changed EOFError() 'missing' parameter value
* [`dd4e507`](https://github.com/jline/jline3/commit/dd4e507408132505304647ba0c5b4752d83c3783) DefaultParser: manage eofOnUnclosedBracket
* [`b8dd35d`](https://github.com/jline/jline3/commit/b8dd35d35692286de0edbc35db40e0c0d5bd4378) fixed middle quoted word escape rules
* [`4244c6a`](https://github.com/jline/jline3/commit/4244c6a03e9696e0e32f9573d085ed94283ad7f8) replaced tabs with spaces
* [`d5e2dbc`](https://github.com/jline/jline3/commit/d5e2dbce2785f880c21a91ef9191cff6c7731603) DefaultParser: parameter middle quoting
* [`f2e7070`](https://github.com/jline/jline3/commit/f2e7070f22ae1ca57fe9d1412cf5de728d0e6a14) Merge branch 'master' of https://github.com/jline/jline3.git
* [`7783c03`](https://github.com/jline/jline3/commit/7783c03e0372c0b3c20f44dfc092adb8433b1b74) Fix quote parsing and escaping, fixes #331
* [`dfedc72`](https://github.com/jline/jline3/commit/dfedc72d2ed06915feac7f50959be58aae40f53a) Nano search does not work, fixes #336
* [`a24636d`](https://github.com/jline/jline3/commit/a24636dc5de83baa6b65049e8215fb372433b3b1) Fix BSD license http url
* [`02542d1`](https://github.com/jline/jline3/commit/02542d1809a0f3245cd10cea868340b0b459485e) Fix link to BSD license
* [`03d35f7`](https://github.com/jline/jline3/commit/03d35f75f190c597165d1f06797a44d58d181dc4) Fix link to license
* [`438b2ea`](https://github.com/jline/jline3/commit/438b2eaafddb618a599974c79bc07af5d4c7f1c3) Default known terminal keys to beep
* [`f65e68b`](https://github.com/jline/jline3/commit/f65e68b68754ac97ebdf597a3acf8171fb216442) Merge pull request #333 from snuyanzin/JLINE_332
* [`ac87e85`](https://github.com/jline/jline3/commit/ac87e850be832b8fbb253e09bd9cd7455386c7f3) [JLINE3-332] Throw IllegalArgumentException in case there is no timestamp or timestamp is wrong in HISTORY_TIMESTAMPED file.
* [`5a781bb`](https://github.com/jline/jline3/commit/5a781bba450404512ea8e32b073ae6f40e77bdb2) Merge pull request #329 from snuyanzin/README_TYPO
* [`7b138d2`](https://github.com/jline/jline3/commit/7b138d259c0cfc20c5013ba54adc27b38fdff741) Cut down verbosity for unsupported signals, fixes #327
* [`fa5964f`](https://github.com/jline/jline3/commit/fa5964fec739a39a4ae642b9f1d8b66e64f7bef7) Add missing dumb-color capabilities, fixes #328
* [`12ad62c`](https://github.com/jline/jline3/commit/12ad62c7146ab4e00740e8dc16642505bd67e3f1) DefaultParser: enclosed candidate with quotes if it contains delimiter char (escapeChars=null)
* [`99b1698`](https://github.com/jline/jline3/commit/99b1698bb682b44c5fc5b184136480b0ce9f218f) Typo in README.md
* [`dc97839`](https://github.com/jline/jline3/commit/dc978395e1ee77da0103ef797a8750252a3f53fc) Merge branch 'mattirn-master'
* [`f85da18`](https://github.com/jline/jline3/commit/f85da186aa15b7ca6f07ada661455e30744f1192) Merge pull request #326 from snuyanzin/JLINE_325
* [`46784f7`](https://github.com/jline/jline3/commit/46784f70fd02f71f39c0bdc96574dccb9e9a0d2b) [JLINE3-325] Highlighting in history output
* [`8ae798f`](https://github.com/jline/jline3/commit/8ae798f09d6d409df79f8fbd963784bf068a9c1f) fixed NPE in DefaultParser escape() method
* [`09a0f04`](https://github.com/jline/jline3/commit/09a0f0432e66f8be641bbb251f7a8e21d5b95dba) Merge pull request #322 from Hypersonic/fix-dates-in-javadoc
* [`514f759`](https://github.com/jline/jline3/commit/514f759accc9572232a5ac3d56bd87afe2fb9594) Disable "Created At" dates in javadocs.
* [`8cedbef`](https://github.com/jline/jline3/commit/8cedbefb76a4d738812745fc0e061d48500c72a3) Merge pull request #319 from snuyanzin/MVNW_NO_BINARY
* [`d73b5be`](https://github.com/jline/jline3/commit/d73b5be840d3982e2a90b947a5c60fded6d2b6fb) Add extension to download maven instead of keeping binary in a repository, remove maven-wrapper.jar from the repository
* [`cd4c5f9`](https://github.com/jline/jline3/commit/cd4c5f9d1e6e3a35105a9c17e6755ee5e382d373) Merge pull request #313 from valencik/doc-fix
* [`1ba6424`](https://github.com/jline/jline3/commit/1ba6424516684230bf1311d13c73d1e376733f62) Fix typos in DefaultParser
* [`80aa625`](https://github.com/jline/jline3/commit/80aa625d793b93f0f55196cb82da3911795bd54f) Fix support for PROP_SUPPORT_PARSEDLINE, fixes #309
* [`68d6943`](https://github.com/jline/jline3/commit/68d69438041346f481db96ecc455dcfe5d6d76a3) Case-insensitive search returns no result for TreeCompleter when the root word is case-mismatched, fixes #308
* [`e95a7d9`](https://github.com/jline/jline3/commit/e95a7d991cd88d9c8d8c011359ca6025ff9c26d1) Fix typo in javadoc
* [`141442c`](https://github.com/jline/jline3/commit/141442c4d08fffe32a5724c5c2fbd2c941198411) Fix clear screen on ConEmu, fixes #301
* [`bb51b5c`](https://github.com/jline/jline3/commit/bb51b5c6da6f4977e89815e6458530454be564b2) Update README.md
* [`4b8a571`](https://github.com/jline/jline3/commit/4b8a571087e1a96dd92e6c038ea0a00d9aa09210) Less fails when a bad regex pattern is used, fixes #304
* [`d18b65c`](https://github.com/jline/jline3/commit/d18b65c11ace55760ce36006c2c7d4de0060590e) Add DepShield badge
* [`5a58d01`](https://github.com/jline/jline3/commit/5a58d01c960c0800f37a85fb4baf666ab693bfdb) Jline3 always removes backslash from readline, fixes #296
* [`50749f8`](https://github.com/jline/jline3/commit/50749f851435d89d8eaa8e97f616f1d69d66f60b) Update README.md
* [`d11b1bf`](https://github.com/jline/jline3/commit/d11b1bf10c31a66022a96c4a5596809a244c2853) Update README with a maven badge

## [JLine 3.9.0][3_9_0]
[3_9_0]: https://repo1.maven.org/maven2/org/jline/jline/3.9.0/

* [`d5fc7e8`](https://github.com/jline/jline3/commit/d5fc7e82b5b99a04b11cd2154884180cb2fe5d10) Provide an api to print output asynchronously above the prompt, fixes #292
* [`cd29a53`](https://github.com/jline/jline3/commit/cd29a53dbc78bd25087956473b207bdc9cf89d94) Add option to disable timestamps for history file
* [`b537a73`](https://github.com/jline/jline3/commit/b537a7385a94f9ccf71e19a7419c144adae37a58) Switch to 3.9.0-SNAPSHOT

## [JLine 3.8.2][3_8_2]
[3_8_2]: https://repo1.maven.org/maven2/org/jline/jline/3.8.2/

* [`62d6088`](https://github.com/jline/jline3/commit/62d6088010561caa305efc90d840ad4847466844) Fix ConEmu support for 256 colors, fixes #294

## [JLine 3.8.1][3_8_1]
[3_8_1]: https://repo1.maven.org/maven2/org/jline/jline/3.8.1/

* [`0fb40ab`](https://github.com/jline/jline3/commit/0fb40ab0b58015ad1e21af01d28513081c9421a1) [maven-release-plugin] prepare release jline-parent-3.8.1
* [`b5a643d`](https://github.com/jline/jline3/commit/b5a643de48f701a8088bb0e1b0cfaf4a52dde068) Autocomplete options are shown in random order, fixes #290
* [`a802712`](https://github.com/jline/jline3/commit/a802712d025fd8f065241098360dcc20dd796a98) Fix reflection problem

## [JLine 3.8.0][3_8_0]
[3_8_0]: https://repo1.maven.org/maven2/org/jline/jline/3.8.0/

* [`0beae63`](https://github.com/jline/jline3/commit/0beae63a6ac53cb04a58dc6880bc8381b498db55) Upgrade to gogo 1.1.0
* [`12b992f`](https://github.com/jline/jline3/commit/12b992f2c192b53d8897b99f6302c0d5dc406bf6) Improvement the ExternalTerminal to read multiple bytes in one go
* [`27de765`](https://github.com/jline/jline3/commit/27de76521d3ee2e7685b88b986cecbeff4f028b2) Provide a status bar, fixes #286
* [`51c4621`](https://github.com/jline/jline3/commit/51c46217ad367d8ad5e7cab86f5be1fa43b1ba0d) Add a AbstractTerminal#close() implementation
* [`cde6119`](https://github.com/jline/jline3/commit/cde61194b5ca19098e917c5613b74d7fe5985401) Add some javadoc for Terminal reader/writer/input/output
* [`f85e27b`](https://github.com/jline/jline3/commit/f85e27b2d3156927c6e8bdea0fe0a9a76d3e46a3) The clr_eos capability is not recognized, fixes #285
* [`7ff2bc8`](https://github.com/jline/jline3/commit/7ff2bc806a85e116421c62ebd4780e3bb8ad6f07) Restore ConEmu support
* [`9999987`](https://github.com/jline/jline3/commit/9999987bfcb1ea203b6feefca598d3a9cf063d2e) Correctly report the terminal size on windows, fixes #136
* [`65c3450`](https://github.com/jline/jline3/commit/65c34506ef45ec21c033fbd61650314e452e3b19) Upgrade to version 3.8.0-SNAPSHOT
* [`5f97cca`](https://github.com/jline/jline3/commit/5f97ccaefd31f607a4dfc7b1cb817b53601a9b30) Force decoding mouse position using UTF-8, fixes #284
* [`840d45e`](https://github.com/jline/jline3/commit/840d45eaf49cd48261a52ef62b30749c9953417a) Add a way to not persist some history entries, fixes #282
* [`d2cc0e3`](https://github.com/jline/jline3/commit/d2cc0e3bdea025283af11d98f5aa6fcc9504beea) Support advanced escape sequences on Windows 10, fixes #279
* [`44bafc2`](https://github.com/jline/jline3/commit/44bafc299eb01fd1276cee6fe98fc8f945a2d9aa) Provide a test demonstrating how to complete in the middle, fixes #274
* [`f0b6386`](https://github.com/jline/jline3/commit/f0b6386fb3daa27accf46283ecf873aaf6b51d0d) Provide a way to disable the warning when not implementing CompletingParsedLine, fixes #278
* [`340ebe2`](https://github.com/jline/jline3/commit/340ebe29b997e87c914eab736b0d03a78715f547) Fix dark gray problem on windows, fixes #277

## [JLine 3.7.1][3_7_1]
[3_7_1]: https://repo1.maven.org/maven2/org/jline/jline/3.7.1/

* [`24b5660`](https://github.com/jline/jline3/commit/24b5660669c235e02190363f8c4c827daa1943ff) Extract io exception checking in a method
* [`5bb4939`](https://github.com/jline/jline3/commit/5bb4939662029ffba7f03f34bca9d8d8ffbae95d) Fix build for jdk 10
* [`14c87f4`](https://github.com/jline/jline3/commit/14c87f479033b2b050054ff0ed58d82c1153d95e) Convert groovy source files to java
* [`2bf4058`](https://github.com/jline/jline3/commit/2bf4058a13491ab12e1c177a3125c095d310e812) Remove javadoc warnings
* [`e9f88f7`](https://github.com/jline/jline3/commit/e9f88f7591adf5b97bdb1f7a98a87da870729b88) Revert "IOException thrown by the terminal does not get reset on subsequent reads, fixes #270"
* [`c4c1d97`](https://github.com/jline/jline3/commit/c4c1d977a63918cef673a3244f1c8751c27fed33) Merge pull request #272 from vorburger/patch-1
* [`50f7718`](https://github.com/jline/jline3/commit/50f771812c3cdafc8f2406a6079355c152db7703) fix broken link to Apache Mina SSHD
* [`9302947`](https://github.com/jline/jline3/commit/9302947ef94d821edd7ab22b74a2c3861db19032) Merge pull request #271 from hflzh/master
* [`21f5d70`](https://github.com/jline/jline3/commit/21f5d70a731add9dcd7646321c1fd4c9f2eb37f1) Fix a typo which causes NullPointerException in PosixPtyTerminal#resume()
* [`557500c`](https://github.com/jline/jline3/commit/557500cb84ff88aa4cd171117a3133bbc9746fb7) Fix unwanted new line when using ERASE_LINE_ON_FINISH, fixes #181
* [`28e36be`](https://github.com/jline/jline3/commit/28e36be839cc06cfd086d658430024c353332f26) IOException thrown by the terminal does not get reset on subsequent reads, fixes #270
* [`eba1b43`](https://github.com/jline/jline3/commit/eba1b43bb202a32a621036da78c77a0fa4b1559c) Merge pull request #269 from cascala/patch-1
* [`85b08f4`](https://github.com/jline/jline3/commit/85b08f48614fcd28a262f0f1792053ccfd957809) Make sure all the stream is read before sending EOF, #267
* [`a67d60f`](https://github.com/jline/jline3/commit/a67d60f5de6f5b8dc6de86b52fa1c3babb466b57) Input stream supplied to TerminalBuilder.streams() is consumed when a terminal instance gets created, fixes #266
* [`f8894f3`](https://github.com/jline/jline3/commit/f8894f3e155a3607d9954748768756aaac0a6b49) IOExceptions thrown from streams is not propagated to LineReader#readLine(), fixed #267
* [`69471c7`](https://github.com/jline/jline3/commit/69471c79da999103974addf846b88f87600ad072) LineReader#readLine() should never return null, fixes #265
* [`c5f68dd`](https://github.com/jline/jline3/commit/c5f68dd236a982121cfcb9dfb0c398118e2c3123) Merge pull request #261 from ZeroErrors/master
* [`62132ff`](https://github.com/jline/jline3/commit/62132ff3bd592706ac6fa748f399377089cef74d) Fix reflection in TerminalBuilder getParentProcessCommand()
* [`6cc608c`](https://github.com/jline/jline3/commit/6cc608c0d8711c5c07332745abb2ae386beb7b6d) Command completion with quoted value fails: org.jline.reader.EOFError, fixes #257

## [JLine 3.7.0][3_7_0]
[3_7_0]: https://repo1.maven.org/maven2/org/jline/jline/3.7.0/

* [`e4d5fd6`](https://github.com/jline/jline3/commit/e4d5fd69b7421b184f8f4f79a6f8b46cc6326b69) Upgrade maven plugins
* [`c4cbea8`](https://github.com/jline/jline3/commit/c4cbea89715b84deef8fe8f8031165b3b13192e9) Add changelog
* [`b838d17`](https://github.com/jline/jline3/commit/b838d1754a009b180b473e85d36671a15be43186) Upgrade to jansi 1.17.1
* [`124114f`](https://github.com/jline/jline3/commit/124114f36034705e275847d69429c69f5e784d1b) Fix demo parser to support quotes and escape characters
* [`ed06ec3`](https://github.com/jline/jline3/commit/ed06ec37f39d1556cd1b471c8e0cd23faa4d0209) Bring back previous constructor with a deprecated notice, #245
* [`d7b1348`](https://github.com/jline/jline3/commit/d7b1348ceffe491597d7fb83bd39f83c1e3ffd8d) Remove all references to File.separator, #173
* [`a5cc30e`](https://github.com/jline/jline3/commit/a5cc30ef63df77835f698ad19422e6934d2c1577) Fix file separator in completer, #173
* [`eefd7a5`](https://github.com/jline/jline3/commit/eefd7a54edc9b00274d6598ef7d1c274fc8fba45) Improve support for completion with quotes, #245
* [`b84705a`](https://github.com/jline/jline3/commit/b84705a891f49039c9fb28eb9ddc0993c30cbddf) Make completion and parser work together, fixes #125 and fixes #245
* [`df01bed`](https://github.com/jline/jline3/commit/df01beda10002ba1b1d6e6ce9a57bbc083f05c87) Use the appName from the LineReader, #230
* [`9ccfe0b`](https://github.com/jline/jline3/commit/9ccfe0b121d2ea72a8524c84a9bda5abaa68d45a) Add support for inputrc parsing, fixes #230
* [`3d46f32`](https://github.com/jline/jline3/commit/3d46f32de193aefb081d4eb44a8e81a7ff6ad21a) Merge branch 'issue-235-ansi', fixes #235
* [`08933d5`](https://github.com/jline/jline3/commit/08933d5e1499011521299afa57fdb883ab72f409) Searching history with up/down keys doesn't respect case insensitivity option, fixes #252
* [`07c39ae`](https://github.com/jline/jline3/commit/07c39aeb91960944e62d259f14da0c9556989fcf) When the word in the buffer is followed by a space, completion does not consider the space correctly, fixes #251
* [`644fefe`](https://github.com/jline/jline3/commit/644fefecb0b50958401ec072a356b4b6dcf4ec2b) Case insensitive completion doesn't highlight results of different case, fixes #249
* [`dffdf84`](https://github.com/jline/jline3/commit/dffdf84724bfca4fd73416f7f6822d432647742a) Upgrade to latest maven
* [`26f7cf7`](https://github.com/jline/jline3/commit/26f7cf7e9760c434487cd133c159781dceaa4ee1) Improving readme for jansi / jna libraries, #234
* [`fbe61dd`](https://github.com/jline/jline3/commit/fbe61ddf95155af87e97f946749ab3f6bae349ef) Fixes #247
* [`4eedaae`](https://github.com/jline/jline3/commit/4eedaaec8400c254adabd36508e9a0e6e9311279) Improve readme, fixes #234
* [`29a1045`](https://github.com/jline/jline3/commit/29a10451d0149c5e5c2e323a41788b99a2a4db60) Rewrite i-search, fixes #242, #243, #244
* [`6970b6b`](https://github.com/jline/jline3/commit/6970b6b44395bded41f21f56501627f0f96be2b6) Improve Buffer copy
* [`e46f9f1`](https://github.com/jline/jline3/commit/e46f9f1006f4f01ac2dc83f60ab5844f685898de) Allow setting prompt once readline is running, fixes #248
* [`0c8bd46`](https://github.com/jline/jline3/commit/0c8bd466fcdc17223ba72511dfc773b38759cf57) Disable completion history expansion during completion, fixes #246
* [`d3068e1`](https://github.com/jline/jline3/commit/d3068e162dacc3e3e699020f7b488e620efa9502) Support for MSYS2 subsystem/shell, fixes #241
* [`ca43f6a`](https://github.com/jline/jline3/commit/ca43f6ac4869d500355352d8f0f3a9b9afc0f7e9) Make pause(boolean) public on Terminal, #231
* [`5b2578c`](https://github.com/jline/jline3/commit/5b2578c670061542e4f50a652ba2a2c617124c4b) Support for MSYS2 subsystem/shell, fixes #241
* [`adb1d94`](https://github.com/jline/jline3/commit/adb1d94643725b28686cd215e8f6a40e79c19d26) Support alternate charset for box-drawing operations
* [`0e135e5`](https://github.com/jline/jline3/commit/0e135e5e7de342296c318aa5b8b5c3caf5d0d87d) Make AttributedStyle's constructors and internal getters public
* [`eee2e70`](https://github.com/jline/jline3/commit/eee2e7081fa7922b246ddf6fbecaf86a35422f71) Refactor Curses#tput methods
* [`81c428c`](https://github.com/jline/jline3/commit/81c428cd86146d69bd3479f20ce16cd76b9415a5) Move system properties name to TerminalBuilder for easier reference

## [JLine 3.6.2][3_6_2], released 2018-03-15
[3_6_2]: https://repo1.maven.org/maven2/org/jline/jline/3.6.2/

* [`1be52a1`](https://github.com/jline/jline3/commit/1be52a176fb2db81282f345ddb6c62912823b456) Tidy up a bit, add real emacs support and remove requirement on java9
* [`88010aa`](https://github.com/jline/jline3/commit/88010aaaf2d7abe026a792472a2aea8c807643f7) Some bytes may be lost with NonBlockingReaderInputStream, fixes #238
* [`1b10052`](https://github.com/jline/jline3/commit/1b10052e11071e58cbebc1138a17395c66d7bdaf) experiment
* [`ae62ec8`](https://github.com/jline/jline3/commit/ae62ec80997b8f3544a7e11ab411610044fc437a) Terminal.input() does not work on Windows, fixes #237
* [`1ee156d`](https://github.com/jline/jline3/commit/1ee156d13dbf978da110ea4930b31fef426e64a9) Fix attributed string adding underline in ConEMU, #236
* [`cf0f501`](https://github.com/jline/jline3/commit/cf0f5019a09a6daf119e3792afe57b79886b32f2) Synchronize LineReaderImpl#redisplay to avoid concurrent threads updating the display, fixes #233

## [JLine 3.6.1][3_6_1], released 2018-02-15
[3_6_1]: https://repo1.maven.org/maven2/org/jline/jline/3.6.1/

* [`9fa5c89`](https://github.com/jline/jline3/commit/9fa5c899232fb2b9110891db0c44ab4cc4eb0bdb) Add a #pause(boolean) method to AbstractTerminal, #226
* [`784d8f3`](https://github.com/jline/jline3/commit/784d8f3a079e2aca58f6397506484c1f73d562cd) Avoid multiple pump threads being spawned, #226
* [`c21e220`](https://github.com/jline/jline3/commit/c21e2209641cf06fe5416c495c6a12a28d7411c9) Avoid possible infinite loop
* [`bc273be`](https://github.com/jline/jline3/commit/bc273be118ee88f183baa7c3ecfe73c1f4f38179) Fix mouse support not reporting button release
* [`5099a10`](https://github.com/jline/jline3/commit/5099a10c132e5abddf936eea5bc38fb5af44546c) Add a test for #255
* [`af61fb3`](https://github.com/jline/jline3/commit/af61fb3f30c6ec8f5c7fb4c15347c9b1b9f1831e) Colors.roundRgbColor is broken, fixes #225

## [JLine 3.6.0][3_6_0], released 2018-02-02
[3_6_0]: https://repo1.maven.org/maven2/org/jline/jline/3.6.0/

* [`cec09fe`](https://github.com/jline/jline3/commit/cec09fe4eca5f5011d7c4741e56aeb0586923f75) Upgrade to jansi 1.17
* [`2398d96`](https://github.com/jline/jline3/commit/2398d966bbcdf04ee2ca0414c4f01bb4e3d40247) Fix broken TreeCompleter and RegexCompleter, fixes #224
* [`caf355e`](https://github.com/jline/jline3/commit/caf355e10982ce1f32a004d0f8771d3ff7e8b5bf) Make sure <CR> is escaped in history, fixes #223
* [`4910a5a`](https://github.com/jline/jline3/commit/4910a5a51cf8dd553c8e3a3d78621f21ac96d715) Fix processing windows input events for window resizes, mouse and focus, fixes #220
* [`fde358d`](https://github.com/jline/jline3/commit/fde358d8690393ad86ee4e27ac7b6c1a7e90f61b) Fix ArrayIndexOutOfBoundsException when setting bright background color
* [`ae77c8f`](https://github.com/jline/jline3/commit/ae77c8f2a42461c8bc19f0d21b47f8cc01084365) Add support for raw ansi styling to the StyleResolver
* [`9d73f85`](https://github.com/jline/jline3/commit/9d73f85c0a5c901c26a7310cb3fca079a91fba40) The conemu activation should only be used when in conemu...
* [`7410619`](https://github.com/jline/jline3/commit/74106191eea166904d135e174cb8572a01393e9c) Make LineReaderImpl#getDisplayedBufferWithPrompts public, fixes #221
* [`c259d8d`](https://github.com/jline/jline3/commit/c259d8d258494dbdd61595548936fcfcf62ae7aa) Focus tracking support, fixes #222
* [`7008567`](https://github.com/jline/jline3/commit/7008567030fd5a9bd07c353e6a6b52ed31ec2c5c) Include jline-style in the uber-bundle
* [`7e1f85c`](https://github.com/jline/jline3/commit/7e1f85c8ff44635965bebe668737d7fd5cd4e1e3) Move the StyleResolver core parser into org.jline.utils
* [`eaea6f9`](https://github.com/jline/jline3/commit/eaea6f99bf758f29350011d21e4787723fead868) Move 256 color names inside a file
* [`18c1acc`](https://github.com/jline/jline3/commit/18c1acc8b9abbc3a830c48ebc172124401a58e5c) Deprecate the color related methods in AttributedCharSequence
* [`c353e2f`](https://github.com/jline/jline3/commit/c353e2fca47f9e5529e35c810eeaf914657510d4) Fix unit test
* [`07b0179`](https://github.com/jline/jline3/commit/07b0179dd31a7f887785c2048e65617f62d5c754) Fix bold support on ConEMU,  #219
* [`0bff330`](https://github.com/jline/jline3/commit/0bff330856bea54b9e0e5ff5a21778c688889ca0) Rework bold / underline attribute support on windows #219
* [`2973cec`](https://github.com/jline/jline3/commit/2973cec7a4352df6aeaea30b9736cfc6a1bfdc83) Make sure the bold attributed is outputed after the color in ansi sequences, #218, #219
* [`e8bc984`](https://github.com/jline/jline3/commit/e8bc984d733491b446f139007e37fc696f710a77) Fix problems with dumb terminals not being properly handled
* [`3ac6345`](https://github.com/jline/jline3/commit/3ac6345467fcd563cfa534eb5cb3fff057ec8c8a) Support for Windows/ConEMU, #209
* [`64629eb`](https://github.com/jline/jline3/commit/64629eb526181f71ac55dc78051d0732ee014ef3) Improve color rounding to 16 colors, fixes #217
* [`f23d15f`](https://github.com/jline/jline3/commit/f23d15f7359cab7d6ec0db0a6f5e371aff2dab0b) Disable blinking matching parenthesis if < 0, fixes #216
* [`1ce3880`](https://github.com/jline/jline3/commit/1ce388052b9e322cdeb271486c3dab4bfec412c7) Attempt to fix synchronization issue which cause streams to block forever, #214
* [`a3a115b`](https://github.com/jline/jline3/commit/a3a115b62ebb7c0828661159aa27772b5d9d2fc5) Improve ordering of candidates and groups, fixes #205 and fixes #210
* [`fc728fc`](https://github.com/jline/jline3/commit/fc728fcb1d50065e88f5970e46f5a2e6af844ff7) Make "others" and "original" groups name configurable, fixes #212
* [`6e72fc9`](https://github.com/jline/jline3/commit/6e72fc9ac3020b06cdc55a1cf1160cff3f0a9be3) Better formatting for completion groups, fixes #211
* [`0bb16a6`](https://github.com/jline/jline3/commit/0bb16a637e31ddeaaa67fe8c85bd361b701ba90b) Fix char peek on windows
* [`b15a992`](https://github.com/jline/jline3/commit/b15a9924b47a18a844bf1e7795ccc0774ae0457b) Move the non-blocking input stream implementation to the Pty, #140
* [`e6d5912`](https://github.com/jline/jline3/commit/e6d591288be97b87525b8e2f23d6391ee6751457) Revert #139 which should be handled by the underlying WindowsAnsiWriter, fixes #204
* [`eed23d2`](https://github.com/jline/jline3/commit/eed23d21c9fab17313de5c2ea69cfe42fa7d1faf) Move back default pause/resume implementations to AbstractTerminal, #140
* [`7df08fb`](https://github.com/jline/jline3/commit/7df08fb4f892e43f7698119d677bb57af89b3709) Fix int signal #140
* [`fc908f3`](https://github.com/jline/jline3/commit/fc908f33b9182c59095c583db365a9083f75ff46) Implement pause/resume on terminals, #140
* [`ac77a8a`](https://github.com/jline/jline3/commit/ac77a8ae863924399b5f6f63aca9f054aea65e61) Move the non blocking reads at the input stream level, #140
* [`a89f820`](https://github.com/jline/jline3/commit/a89f82088deef711260a8fd17d7adcca1afe3d45) Change default value for INSERT_TAB option
* [`b7af708`](https://github.com/jline/jline3/commit/b7af708bc08803af3c4f01c2345f4995dfdca21b) Fix javadoc a bit
* [`857076e`](https://github.com/jline/jline3/commit/857076e19f6f4d2a0fcd46dd827249c39a2b2b4b) Fix default grouping diplay, fixes #200
* [`ac6077e`](https://github.com/jline/jline3/commit/ac6077ef2904293a119f5626f3f2d23da153bd53) Switch to version 3.6.0-SNAPSHOT
* [`3acf3d2`](https://github.com/jline/jline3/commit/3acf3d20f7e33bd1bd91dab8b4dbae29ecb83ff2) On windows, absolute cursor positioning is relative to the full buffer, not the displayed window
* [`c03a3ba`](https://github.com/jline/jline3/commit/c03a3ba53ae2ef486f16a62291fe6ebf4a3d855e) Make the meaning of the IS_CYGWIN IS_MINGW flags more intuitive
* [`d8a810d`](https://github.com/jline/jline3/commit/d8a810d0fcef4d962d759260d990af5b84350469) Try to fix cygwin / git-win support
* [`2e31b52`](https://github.com/jline/jline3/commit/2e31b52c657035851729528690259a8fc6bb3aa1) Avoid duplicate INT signal on windows, fixes #199
* [`2fa2efc`](https://github.com/jline/jline3/commit/2fa2efc1fa930580da1ac00bb8263b1c84aca586) Support the DISABLE_COMPLETION variable, fixes #201
* [`fe2b8c0`](https://github.com/jline/jline3/commit/fe2b8c0d74cd37a11c5d5a30bf9daac33805b656) Fix case-insensitive completion, fixes #198

## [JLine 3.5.2][3_5_2], released 2017-12-19
[3_5_2]: https://repo1.maven.org/maven2/org/jline/jline/3.5.2/

* [`aeed42d`](https://github.com/jline/jline3/commit/aeed42dfdfcc18e720585be251e3d261642b1a76) Fix copyright year before release
* [`c74bc3b`](https://github.com/jline/jline3/commit/c74bc3bc82c0f316adc25763ab2d369ef341d44a) Make setters chainable, fixes #187
* [`152cf8f`](https://github.com/jline/jline3/commit/152cf8fba3c915437e247976d3c503f620abf254) Merge pull request #197 from PerBothner/master
* [`dffdd12`](https://github.com/jline/jline3/commit/dffdd12c1c739405e85382a59d6d0e6941427bad) Merge pull request #196 from facingBackwards/lineCount
* [`9b453cc`](https://github.com/jline/jline3/commit/9b453cc7bd323f41e8cfb020cbbf75d49f672980) Fix DiffHelper.diff logic for comparing strings of "hidden" character.
* [`ed016da`](https://github.com/jline/jline3/commit/ed016dac0eec3f209814653cb9dd7197781ab879) Recalculate number of columns after lines
* [`4b9e3e7`](https://github.com/jline/jline3/commit/4b9e3e73a2b5b9062f7e39e5a4c9ad2bb118fc1f) Merge pull request #195 from facingBackwards/navig
* [`c4b557e`](https://github.com/jline/jline3/commit/c4b557e029a4779042d8b469bf7473b888730ea4) Merge pull request #194 from facingBackwards/updown
* [`b49d954`](https://github.com/jline/jline3/commit/b49d95441a21ba62f00f4fe838145c23e41868ea) Prevent index errors moving through completion menu
* [`a5bbaf6`](https://github.com/jline/jline3/commit/a5bbaf61f8fd9b30f7c1bceaff330483d68c639e) Enable UP/DOWN movement through completions using arrow keys
* [`af91e80`](https://github.com/jline/jline3/commit/af91e8032810985f2d81d89599049c8f71014c27) Merge pull request #188 from mslinn/patch-1
* [`a836f61`](https://github.com/jline/jline3/commit/a836f6162d229e98baed7133c2e35efbc0aee9f7) Added URL for Javadoc, hosted on javadoc.io
* [`4ca790c`](https://github.com/jline/jline3/commit/4ca790c65365841b2d383744eaeaf834f3a39db6) Fix #185, update README with correct version
* [`27346d0`](https://github.com/jline/jline3/commit/27346d08d833d790f399331f44cb1104efabc0fd) Improve exception message when trying to load an old history file, fixes #180
* [`d2b81e3`](https://github.com/jline/jline3/commit/d2b81e34285e2256c087d45f676543dad7aa92ce) Improve exception message when unable to call a widget, fixes #183
* [`3fa869c`](https://github.com/jline/jline3/commit/3fa869c5c036622c135bd6700e0ae9933649dcf1) Ability to erase the line at the end of a readLine call, fixes #181
* [`7b3acf1`](https://github.com/jline/jline3/commit/7b3acf1289e438e51d2a0d03a9193c375c87c1ec) Fix windows console mode when closing the terminal, fixes #169

## [JLine 3.5.1][3_5_1], released 2017-09-22
[3_5_1]: https://repo1.maven.org/maven2/org/jline/jline/3.5.1/

* [`1f9e50c`](https://github.com/jline/jline3/commit/1f9e50c58e38f199366563f05b1ff59bcad20add) Remove unused code
* [`e39fb9a`](https://github.com/jline/jline3/commit/e39fb9afefc9a5a5a35552d00bb195b50065e9c9) Use StandardCharsets.UTF_8 whenever using the UTF-8 encoding
* [`587120f`](https://github.com/jline/jline3/commit/587120f2b50833d7b8e8fee46e7df2537a61a1ed) Merge pull request #176 from Minecrell/expose-terminal-encoding
* [`61aaf1d`](https://github.com/jline/jline3/commit/61aaf1df1c501f7b34ba28e82a672fdcd1ec5dda) Expose encoding used for Terminal input/output streams
* [`c4147db`](https://github.com/jline/jline3/commit/c4147db0a7926e180cb8e40c21c6cd7ac8675bd8) Merge pull request #175 from Minecrell/windows-avoid-input-encoding
* [`51c41b6`](https://github.com/jline/jline3/commit/51c41b61d9452ff486cf7f1009fd3c613403a0da) Fix issues in PumpReader discovered using the unit test
* [`bb59951`](https://github.com/jline/jline3/commit/bb599518ebc6e282b5460985688dfbf1046a6f12) Add unit test to ensure PumpReader works correctly
* [`938eeca`](https://github.com/jline/jline3/commit/938eecaf48dabb82f8a6e5f579b3e08b9beb76af) Windows: Avoid race condition when starting input pump thread
* [`ec009b5`](https://github.com/jline/jline3/commit/ec009b55e8ef770d73c6c70d58b11a5694d341a0) Windows: Avoid extra buffering when reading console input
* [`9fab73f`](https://github.com/jline/jline3/commit/9fab73f096c37343641fa8e70bbe0b895668c326) Windows: Avoid allocating new objects for each read/write with JNA
* [`4cb50e5`](https://github.com/jline/jline3/commit/4cb50e536efb9856ff8ea77bd680a0f972d46ddb) Support charset selection ansi sequence
* [`49f7e6e`](https://github.com/jline/jline3/commit/49f7e6e9ef857971126c4d6cdb0302cff552042d) Avoid encoding console input on Windows when using Terminal.reader()
* [`ef6b7c2`](https://github.com/jline/jline3/commit/ef6b7c2147f71b8c8fd7c1233d23a3106290dac6) Add a few comments to the AbstractWindowsTerminal class
* [`0fa03a9`](https://github.com/jline/jline3/commit/0fa03a94d0e9481a4e09b849c0c1831db5610d09) Fix possible encoding problems with wide chars if written separately
* [`e689b20`](https://github.com/jline/jline3/commit/e689b20f155272ecc794ae1bfb4582a971414592) Merge branch 'minecrell-WCW', fixes #186
* [`aaad984`](https://github.com/jline/jline3/commit/aaad9848e00f40b572454d1aeb7dbed8ce0d38f7) Add a unit test for #168
* [`2012f13`](https://github.com/jline/jline3/commit/2012f131028e576be4cc5332b125dcc5ac21e72d) Avoid buffering when using WriterOutputStream
* [`4ed7081`](https://github.com/jline/jline3/commit/4ed70812df869754cc83ac6207b92d104b46d122) Avoid possible NPE, #172
* [`11eb592`](https://github.com/jline/jline3/commit/11eb592999d68529932733db0d31aa234451b4ec) Partial revert of "Correctly close the NonBlockingReader to shutdown the reading thread", fixes #167
* [`babbc2c`](https://github.com/jline/jline3/commit/babbc2c2caef7dbe8070d4182f3b5810cb9a11e6) Merge pull request #171 from andrelfpinto/feature-appveyorbadge
* [`a21d95b`](https://github.com/jline/jline3/commit/a21d95b14c7dd5e9e51825caa7cf9b6a470d6c77) Fix owner
* [`eee0195`](https://github.com/jline/jline3/commit/eee0195a9eac2dcbba703831e0f9bfe86c1b8c9d) Add AppVeyor status badge
* [`4344091`](https://github.com/jline/jline3/commit/4344091d453030a15deee6eea8bece6a85888119) Extract common parts to AbstractWindowsConsoleWriter
* [`96c5e0f`](https://github.com/jline/jline3/commit/96c5e0f2c8abd0fd23f80ac7ebce4a00bd004ca0) Use WriteConsoleW to write to Windows console
* [`3fc333b`](https://github.com/jline/jline3/commit/3fc333b6cf111a4c2f73f54fbd7caf1c71712140) Fix Maven property to skip tests (#170)

## [JLine 3.5.0][3_5_0], released 2017-09-12
[3_5_0]: https://repo1.maven.org/maven2/org/jline/jline/3.5.0/

* [`6a8737f`](https://github.com/jline/jline3/commit/6a8737ff682bce6bf1a1d082940ecd3614c5ca9c) Upgrade demo to felix gogo runtime/jline 1.0.8
* [`df10800`](https://github.com/jline/jline3/commit/df10800f5fd779aefe9ed9ee72ee65fde37954d4) Add info about CI
* [`08a81d2`](https://github.com/jline/jline3/commit/08a81d292b5558ca8bcda0bca7d321cc5f68c949) Fix config #148
* [`433300b`](https://github.com/jline/jline3/commit/433300b3f10a52b15bf582a380c5b506f302ad2c) Investigate using appveyor, #148
* [`cb672b2`](https://github.com/jline/jline3/commit/cb672b283efd42223ab29511a1ad044ffc606871) Add codepage to the terminal builder, #164
* [`7d33254`](https://github.com/jline/jline3/commit/7d332545cbe41830561afd5a78da0c594fe088aa) Attempt to fix both #133 and #164 ...
* [`f320221`](https://github.com/jline/jline3/commit/f320221c8b5264cf6e10909a985f57f578b8f564) Do not modify the output codepage on windows, fixes #164
* [`60300ec`](https://github.com/jline/jline3/commit/60300ecb9e6bdb197d9a3f34c8c20ece983a4340) Do not use System.in directly
* [`9383ba1`](https://github.com/jline/jline3/commit/9383ba1d70b44944a1db26b843b12e187151c479) Switch to 3.5.0-SNAPSHOT
* [`3793dcd`](https://github.com/jline/jline3/commit/3793dcded0ef26c2646916822db6b29c2cfe8016) Introduce MaskingCallback to provide hooks to customize line output, fixes #163 Patch provided by John Poth, thx !
* [`15df62e`](https://github.com/jline/jline3/commit/15df62ef473fde75e81944c9b5dde54961c71cc6) Fix possible NPE
* [`b94e9b9`](https://github.com/jline/jline3/commit/b94e9b96740ebd735845d02e9ffe9d1a1504f661) Correctly close the NonBlockingReader to shutdown the reading thread

## [JLine 3.4.0][3_4_0], released 2017-08-03
[3_4_0]: https://repo1.maven.org/maven2/org/jline/jline/3.4.0/

* [`1561082`](https://github.com/jline/jline3/commit/156108202aa3a3bc4d100d5b25b2b81439c8b73f) AltGr characters are discarded, fixes #158
* [`7d766fb`](https://github.com/jline/jline3/commit/7d766fb420f24143b4482d4903b7fceb30142ff7) AltGr characters are discarded, fixes #158
* [`8e451a7`](https://github.com/jline/jline3/commit/8e451a7da384a8baeb26fbaade2208dd2a44b1c1) Add Manifest headers to allow automatic linking of source jar by Eclipse (#152)
* [`811d8f4`](https://github.com/jline/jline3/commit/811d8f427320d3cc8110e8752a818a1c6118d8a6) No history in terminal after auto-truncation of the history file, fixes #149
* [`29131f0`](https://github.com/jline/jline3/commit/29131f0197338870c2f43c74d32f582c1a04a701) Actually fix the "1B" parsing issue, #157
* [`b48b541`](https://github.com/jline/jline3/commit/b48b541fcb34e4bbd400c829e27b0bbafe5e4ffd) Fix problem with windows demo script
* [`a1d7850`](https://github.com/jline/jline3/commit/a1d785007b3225865fb129412271a378b53a882a) Exception on windows, fixes #157
* [`e2175b7`](https://github.com/jline/jline3/commit/e2175b7c9d19aecfca2e8bbd43e4312666fc9cfa) JNA or jansi based system terminals do not support the main output stream being redirected, fixes #156
* [`ddd7415`](https://github.com/jline/jline3/commit/ddd74155c987847c5977d53295ce80f242e71977) Fix possible NPE in LineDisciplineTerminal#processInputByte caused by non atomic call to EnumMap#getOrDefault, fixes #145
* [`53b2d52`](https://github.com/jline/jline3/commit/53b2d52694f60f5e6d2c9dddd16121166c63bea8) supports Ctrl/Shift in Windows keymap (#144)
* [`6f52587`](https://github.com/jline/jline3/commit/6f52587f5602d63dd5ca4d4ca8cd027ecdab081f) Merge pull request #146 from gnodet/disable-failing-test
* [`e3d87ad`](https://github.com/jline/jline3/commit/e3d87ade53af04b0f08ee7f8bf3a679f5ed141c2) Disable test failing on Travis CI
* [`c722074`](https://github.com/jline/jline3/commit/c72207409057a22642cad0d1af5444f79fbd69f8) Support bracketing paste, fixes #142
* [`2a7fa6e`](https://github.com/jline/jline3/commit/2a7fa6e8cd4ba7733bd9125e88a4f99e2d043e5d) Fix bold + intensity rendering on windows, fixes #139
* [`0eef133`](https://github.com/jline/jline3/commit/0eef133d3eb7d337ad29f6460f55c947d0daba15) Merge pull request #135 from Minecrell/remove-java-1-3-checks
* [`d5695a1`](https://github.com/jline/jline3/commit/d5695a128c277e6ac1229a5737b990546edcf1a8) Try to make the ExternalTerminalTest more robust
* [`bb87c2b`](https://github.com/jline/jline3/commit/bb87c2b1aefcc34dd17214eb46b353a7b5c7e873) Remove unneeded check for Java 1.3+ in ShutdownHooks
* [`6962db9`](https://github.com/jline/jline3/commit/6962db982d962d5d7f303bab332d019925cae038) Add missing header
* [`0cb0284`](https://github.com/jline/jline3/commit/0cb02841bc1c6ca58c494f9b7c4ff13aab8b11f9) Merge branch 'tabSize' of https://github.com/PeterHolloway/jline3 into PeterHolloway-tabSize Fixes #131
* [`9ca6187`](https://github.com/jline/jline3/commit/9ca61872648226025359562a7695bd4e6643e433) Restore AttributedStringBuilder compatibility
* [`4eb828f`](https://github.com/jline/jline3/commit/4eb828fd909574776debc65161b00b9093b35c70) Add StyleExpression test with referenced style from source
* [`1fdc15c`](https://github.com/jline/jline3/commit/1fdc15c136f7fc649173e0902c4339ebe72d5010) When adding spaces in place of tabs, calculate from start of line
* [`bf6e24c`](https://github.com/jline/jline3/commit/bf6e24c7abd8b6d9be0fe4d589e3e0c8fd0c75ce) Merge branch 'style', fixes #134
* [`b120987`](https://github.com/jline/jline3/commit/b120987fc0d8e6da62f4e138d8c65420427793fd) Merge branch 'issue-133', fixes #133
* [`de2f031`](https://github.com/jline/jline3/commit/de2f031ad3826b75d3ef8b2807561bd62b2b863d) Use the buffered output stream at the correct location, #133
* [`ae265f7`](https://github.com/jline/jline3/commit/ae265f7b6328315abe13e183385b3005a1860e0f) Fix things for #133
* [`97120a6`](https://github.com/jline/jline3/commit/97120a6eeb4dec5bb0ca0a37900b0f2d1223f5ff) Improve style parser to support escaping and recursive styling
* [`cc688bd`](https://github.com/jline/jline3/commit/cc688bdb270176344fd1b490cab3353d5e1cf364) Fix console output code page and add a BufferedWriter, #133
* [`d36ee25`](https://github.com/jline/jline3/commit/d36ee254344340976e2af04f38cdd8ca1153a2b3) Support for AttributedStringBuilder.append(AttributedCharSequence)
* [`e9d712f`](https://github.com/jline/jline3/commit/e9d712f7ab64475c0f7585e4faf6f5f8d5a39440) Add a default constructor to StyleExpression
* [`a46b004`](https://github.com/jline/jline3/commit/a46b0047f3c13c696cf48d2adc87dc4430c66cb1) Reformat with 4 spaces indentation
* [`8b2de8c`](https://github.com/jline/jline3/commit/8b2de8cf17297973c5b8edf1dea6064f834d00df) Encoding problems when using JNA Windows terminal implementation #133
* [`cc1c611`](https://github.com/jline/jline3/commit/cc1c6116e163b23d19e0e81728358752f15b670d) Merge branch 'master' into style
* [`412adbb`](https://github.com/jline/jline3/commit/412adbb6abb8600b31ff4ae8315d8030a4094536) Merge pull request #130 from jline/maven-3.5
* [`25fbc9b`](https://github.com/jline/jline3/commit/25fbc9b6ba89a3137e213a3e90b9e13fd838bd45) update to maven 3.5.0
* [`e4c79fa`](https://github.com/jline/jline3/commit/e4c79fa9a4db06c73878fab462d90e042b53d87f) simplify; not using very much from goodies-testsupport add comment about groovy-eclipse compiler options
* [`a3b2301`](https://github.com/jline/jline3/commit/a3b23014cd43905bcf03830746da7943fbf74b5e) Update @since
* [`54c2e55`](https://github.com/jline/jline3/commit/54c2e557516f50eff95aad7129c898ce29a3b6f2) Convert SLF4j to JUL ... :-(
* [`65476cd`](https://github.com/jline/jline3/commit/65476cdc04373bd3dc4442774759b056855ec63d) tidy
* [`862b4b8`](https://github.com/jline/jline3/commit/862b4b88f65968c39674a00e9404b3933e1af8b1) cleanup after IDEA refactor turds
* [`3c7ae05`](https://github.com/jline/jline3/commit/3c7ae0523df3f85237cfe07be0b308dafa7ad013) tidy
* [`58f78be`](https://github.com/jline/jline3/commit/58f78bedd3132d140569d4886fb448871ca3887c) Replace gossip-bootstrap with slf4j-api
* [`a70fe35`](https://github.com/jline/jline3/commit/a70fe355307b416beb5a8bec4a8d38283126db15) replace guava with java8 equivalents
* [`3f139c8`](https://github.com/jline/jline3/commit/3f139c870052ebcf439d8debcecef523d46aacea) Initial move of gshell-util's style support to jline-style module

## [JLine 3.3.1][3_3_1], released 2017-06-06
[3_3_1]: https://repo1.maven.org/maven2/org/jline/jline/3.3.1/

* [`5a31a1c`](https://github.com/jline/jline3/commit/5a31a1c55515b0441fbbcbcd319b918c70941ff0) `NumberFormatException` when parsing terminal capabilities, fixes #126
* [`599c1cc`](https://github.com/jline/jline3/commit/599c1cc2e9c0255583a369ee640aa6453efed794) Display#update() should flush the terminal
* [`fe928e4`](https://github.com/jline/jline3/commit/fe928e45ed7bc4fd4dd0fbb91276c5fadeb81ff4) Fix bold / faint rendering problems
* [`b50c103`](https://github.com/jline/jline3/commit/b50c10392c8721eeb92ab7cd0dd666c47fe4918e) Improve usability of attributed styles
* [`937e121`](https://github.com/jline/jline3/commit/937e1217476794e5258f9dfcd5bfd696dda9c889) adjust build script

## [JLine 3.3.0][3_3_0], released 2017-05-12
[3_3_1]: https://repo1.maven.org/maven2/org/jline/jline/3.3.0/

* [`63d9562`](https://github.com/jline/jline3/commit/63d95621fad8ffa340237f97af05dfd5d1ba4e5f) Update readme for 3.3.0 release
* [`d5da33b`](https://github.com/jline/jline3/commit/d5da33bc8ee11990e2b6c5e64eb42a2d611a84f1) make save/load/purge throw IOException instead of only logging so calling api can be made aware of failures
* [`871b1e4`](https://github.com/jline/jline3/commit/871b1e47cf52d6eba80402c3a0732a8c0675eaab) Log history file optional failures as WARN
* [`b80e29c`](https://github.com/jline/jline3/commit/b80e29c6c2052272d517f1f59709695af4129bfd) Add some tmux commands and completions
* [`7d88a3e`](https://github.com/jline/jline3/commit/7d88a3e3b5db89ef2b35bc24c2fbd269ae8d1ce8) Upgrade to gogo 1.0.6
* [`30970a1`](https://github.com/jline/jline3/commit/30970a121e35e30bbc865699b3b51226b9269137) Problems when TERM=ansi is used on an xterm terminal, fixes #123
* [`13d6722`](https://github.com/jline/jline3/commit/13d672228477b7660b03ed3a8e0b7f695174ae1e) Merge pull request #122 from jline/fix-off-by-one-history-display
* [`c8b3c0c`](https://github.com/jline/jline3/commit/c8b3c0cefe8297be034e32cca6c83f1bd710da95) history index display is off-by-one
* [`346cf06`](https://github.com/jline/jline3/commit/346cf062ab3017d06440e6ae7d5134db7d577858) During completion, accept-line should keep the suffix
* [`ee66c21`](https://github.com/jline/jline3/commit/ee66c215e77bc31c4a1575c3534dc6321b7ef891) add helper to run the demo
* [`0532ee3`](https://github.com/jline/jline3/commit/0532ee39c82ba94e0c5081bf5ee54d704c30087c) Switch to released version of jansi 1.16
* [`5677904`](https://github.com/jline/jline3/commit/5677904a79d8fa3e8035023396287d0c1d281750) Fix messe up display on windows when writing up to the last column of the window
* [`cb089d8`](https://github.com/jline/jline3/commit/cb089d8788878fe749b6258aa59d1f2434fc9dc2) Support ansi insert/delete lines sequences on windows
* [`365bd32`](https://github.com/jline/jline3/commit/365bd325beccd12de5067ce4ec4c6ec822426eee) Fix reverse color on windows + jna
* [`e49862a`](https://github.com/jline/jline3/commit/e49862acca4c64c9b310903da3257b823564a4e3) Use scrolling when displaying enough lines
* [`6f11851`](https://github.com/jline/jline3/commit/6f1185196f3fd799148baad299529d973333ea16) Fix possibly failing AttributedString#equals
* [`62f5389`](https://github.com/jline/jline3/commit/62f53890c58868f91a2ecbc868e5a46c8c718adc) Add sensible default attributes to the LineDisciplineTerminal
* [`4e801d8`](https://github.com/jline/jline3/commit/4e801d873b977e2d1d55680465734a8b1bb19abb) Add possibly missing newline in the full screen menu completion mode
* [`6d4122b`](https://github.com/jline/jline3/commit/6d4122b01443dd7e06d5952bcd0a0f846831989a) Add a few options to less: quitIfOneScreen, noKeypad, noInit
* [`1c04336`](https://github.com/jline/jline3/commit/1c0433690571c1cc8ceb0b31587ad0625ebf1805) Trap ^Z signal in the demo
* [`cc39441`](https://github.com/jline/jline3/commit/cc3944139be835761763a272d9b7381ca7313f77) Make sure to also catch exceptions during terminal initialization, not only during pty creation
* [`4795bac`](https://github.com/jline/jline3/commit/4795bac885f4b1e6ca01c55fd8e11f11cedf3e00) Add ssh to the windows demo
* [`e734d9b`](https://github.com/jline/jline3/commit/e734d9bc0e758c59d7c404af8586f16310f1848d) Fix openpty support for jna and jansi
* [`3f255ae`](https://github.com/jline/jline3/commit/3f255ae1758355ea55540ff94ae68fe98bfd552e) Jansi native support for external terminals
* [`d2cea60`](https://github.com/jline/jline3/commit/d2cea607f579665fc470bedcf136b46d9efeedee) Accept any authentication in the demo sshd server
* [`b329518`](https://github.com/jline/jline3/commit/b329518d410b7f42458561f8dd36fc6db8c78715) Add test for nano line overflow, #120
* [`43b443b`](https://github.com/jline/jline3/commit/43b443b2575fffc24bb103b48ef7e888a70de45e) Jline Nano unresponsive upon line overflow, fixes #120
* [`ab41b43`](https://github.com/jline/jline3/commit/ab41b435a104a059de9bfc29cd2fa7d1929826b7) sudo: false; for clarity this however should be the default already
* [`b19712a`](https://github.com/jline/jline3/commit/b19712a3a88d5e824f5cffe169746c86bf0342fb) Use the post message to prompt the user for confirmation when displaying lots of completion candidates to avoid display problems
* [`16055f7`](https://github.com/jline/jline3/commit/16055f700f18fe122344da64709d1b67aecbd5ea) Restore compatibility with jansi 1.12
* [`5ed8881`](https://github.com/jline/jline3/commit/5ed888110549a7d3180c5457773d421bb820880c) adjust .gitignore for build.rc
* [`c3b2823`](https://github.com/jline/jline3/commit/c3b2823181b38bffe72515c60305eb7c0e3a6999) Fix regression causing bad cursor position
* [`c446ccc`](https://github.com/jline/jline3/commit/c446ccc2b210f2ef55edf5cf266284e7ab60816f) Avoid stack trace in demo
* [`d463ec2`](https://github.com/jline/jline3/commit/d463ec2e35c7f561c527c0754f173c839b4bccb6) Add missing snapshot repository
* [`1243c6e`](https://github.com/jline/jline3/commit/1243c6e4dae43c5b669a9aea2552d40d31b0351a) Upgrade to gogo 1.0.5-SNAPSHOT
* [`e2f7d2e`](https://github.com/jline/jline3/commit/e2f7d2eb16d039c23d1f9b76d4997b71e3c1cb40) Simplify the demo
* [`e92c739`](https://github.com/jline/jline3/commit/e92c739864962feab9d44a7c154a8a0b3488ad4b) Leverage Clibrary.ttyname method
* [`5463fd6`](https://github.com/jline/jline3/commit/5463fd61292877fc423c68fa1a2e0aaaaed231cc) Add support for custom options when launching the demo
* [`32595a4`](https://github.com/jline/jline3/commit/32595a4a46810d540cce2faa7bcf4ce671b8bcad) Add debugs option
* [`d858e5f`](https://github.com/jline/jline3/commit/d858e5fe8bdb072ff22f6cca8d8f08678d8a1988) Add an InputStreamSource for reuse
* [`7dedd74`](https://github.com/jline/jline3/commit/7dedd74d3578b846dc9b752437597e5a84b94a53) Upgrade to jansi 1.16-SNAPSHOT
* [`474e6c1`](https://github.com/jline/jline3/commit/474e6c1a1ac41b33f9efb7a446daba5f844af88a) Add the ability to disable the ExecPty
* [`ef41f60`](https://github.com/jline/jline3/commit/ef41f60c7815c252eb22dbd050e9cd3fabb67869) update deploy plugin; was under the impression the sonatype oss parent setup the nexus-staging deploy integration; but its not.
* [`d7574c5`](https://github.com/jline/jline3/commit/d7574c5c236b554ce3bb32f744e8cac4c17ca33f) only build master branch by default; enable deploy-at-end
* [`c93c227`](https://github.com/jline/jline3/commit/c93c22751a4010fc0c556c8b900e17f9a278d9c2) Merge pull request #119 from jline/travis
* [`4da96cb`](https://github.com/jline/jline3/commit/4da96cb4687f27be083c83d0af59d057841bffba) add rebuild command
* [`17a2bef`](https://github.com/jline/jline3/commit/17a2bef04644678c7c06a167193ebb89f9a7a36f) avoid "unary operator expected" errors
* [`4fbd805`](https://github.com/jline/jline3/commit/4fbd8053338d5b8d977cb2e50089c8cce7b142c1) adjust readme
* [`42ceb00`](https://github.com/jline/jline3/commit/42ceb00cae05adf96647cfb3f4d7cd8d2d3391a3) for now only complain about license headers do not fail; until this can be normalized
* [`ec68964`](https://github.com/jline/jline3/commit/ec68964da9988f3790d2dca5ffe9666e3d3b1be5) Adjust pom for ci and add build badge
* [`7c8d7a2`](https://github.com/jline/jline3/commit/7c8d7a2da90424261de18526cc653cb280ab9dc0) adjust secure variables for jline/jline3 project
* [`177cf52`](https://github.com/jline/jline3/commit/177cf523d1d526ba997467a4c8d646bc7ca39a30) Add basic build scripts and configuration for travis setup
* [`4bda4a1`](https://github.com/jline/jline3/commit/4bda4a15ff9d9ea2ebc27795ea777016c6f1d44f) Make AnsiOutputStream#write methods synchronized, fixes #116
* [`7c9e5ba`](https://github.com/jline/jline3/commit/7c9e5ba9f0a1d1ed699b2c7b5a62fa42c7b5dee0) Use the default charset instead of looking up the charset each time.  It should only contain 8bit ascii chars anyway.
* [`8425e63`](https://github.com/jline/jline3/commit/8425e6337219b0defd4e458bd8c4f6daf2b3b494) Log helper not setting logger-name on LogRecord, fixes #117
* [`c0ce9c5`](https://github.com/jline/jline3/commit/c0ce9c52a6691109c421a0ab82ce9c55b0ad6af1) Fix rendering problems on windows, fixes #114
* [`aa22442`](https://github.com/jline/jline3/commit/aa22442bb45a9539fd3f0a875e22480dd8b9597f) Log exceptions caught during completion, fixes #115
* [`d52c65b`](https://github.com/jline/jline3/commit/d52c65ba895772ea6156b9c6c39df642a52ccb01) UnsatisfiedLinkError when using terminal-jansi on Linux, #112
* [`85fe02b`](https://github.com/jline/jline3/commit/85fe02b9a7e5b65380df13febfe891458885f774) Control chars are not set properly on a linux terminal, fixes #111
* [`3424a8a`](https://github.com/jline/jline3/commit/3424a8aab1d2e7df7f3dfb1dca59ada58b891e80) Fix control char values for freebsd and linux, #111
* [`70f1c96`](https://github.com/jline/jline3/commit/70f1c96a719eefa19261e6540c6b002bed5a9fcd) NPE when building LineReader without explicit terminal, fixes #110
* [`ec945e1`](https://github.com/jline/jline3/commit/ec945e11de0f7f910d450db959d45fa14c6fe712) Provide system properties to be able to control the TerminalBuilder, fixes #109
* [`cb941af`](https://github.com/jline/jline3/commit/cb941af977c946e02feb801a3f66f5efbd322313) Fix wrong test assertion
* [`7ef2abb`](https://github.com/jline/jline3/commit/7ef2abbac33285f83770287f5077a0ffa7ed1115) Fix wrong argument size causing ioctl calls to return "bad address" on linux 32bit, fixes #108
* [`02ed6bf`](https://github.com/jline/jline3/commit/02ed6bfcf76739d1ee281d17d099c019a6d0303a) non-daemon WindowsStreamPump prevents killing with Ctrl-C, fixes #107
* [`9deee96`](https://github.com/jline/jline3/commit/9deee963477480c214bf024ae03f581edb6a3370) Tmux resize-pane command implementation
* [`13527d5`](https://github.com/jline/jline3/commit/13527d507cccb0beabce0e95cdca19ef05558b06) Use the correct in/out/err streams for ttop/sshd/ssh
* [`42b6177`](https://github.com/jline/jline3/commit/42b61779547190f071fe9c6cb024bfd06eddb24f) Always flush if a single byte is written to the LineDisciplineTerminal
* [`b8d7c4a`](https://github.com/jline/jline3/commit/b8d7c4a6b2c38250bbca2c222c2cc98577fc2e01) Tmux improvements: keep layout while resizing, display-panes, clock-mode, better select-pane, split-window
* [`65faecc`](https://github.com/jline/jline3/commit/65faecc424cad35a65e6ec5426cb17b593938d2f) Improve screen terminal resizing
* [`2c13944`](https://github.com/jline/jline3/commit/2c13944f08edf065ab41967fc036be9c69ee18de) Fix conveying signals through SSH
* [`338d756`](https://github.com/jline/jline3/commit/338d7563af1603a6f6557d33775480d21fca2b5f) Fix encoding problems in tmux terminals
* [`c78f430`](https://github.com/jline/jline3/commit/c78f430e5a63db83b36b42a49e889095778576a4) Improve window borders rendering
* [`cd0a252`](https://github.com/jline/jline3/commit/cd0a25276c220c57e77aa18c8d1cd7753d15fc38) Fix code formatting
* [`7e11039`](https://github.com/jline/jline3/commit/7e11039edd4e450574f7f1a2a57528d0f3da191b) Fix tmux binding reading in case there are still some characters available
* [`f1bd29a`](https://github.com/jline/jline3/commit/f1bd29abc3d705b15db2f72eab78ad8e0f9c230f) BindingReader sometimes wait for a character even if a binding is available from the internal opBuffer, fixes #106
* [`1051778`](https://github.com/jline/jline3/commit/10517783ce733a33a54b643bda4bf1c0ecebf562) On some terminals when using a right prompt, the cursor is positioned one character on the right of its correct location, fixes #105
* [`477cb55`](https://github.com/jline/jline3/commit/477cb559c85e00d6b7f500c4d1115a8b16b99034) Less does not display tabs correctly, fixes #104
* [`50c14de`](https://github.com/jline/jline3/commit/50c14deeb95477676e64a2c436078f7fcf036004) Improve toString() for Attributes, Pty
* [`769426c`](https://github.com/jline/jline3/commit/769426c0fafc897dccc44148e445293aa6e44c2d) Correct support for 256 colors in tmux, fixes #103
* [`d933910`](https://github.com/jline/jline3/commit/d93391028b6e6f02c383b6a47d3963c1540e00ad) Fix failing test case, #101
* [`38060c7`](https://github.com/jline/jline3/commit/38060c71a183707edaf83b8fe24f601a85e1dba8) Upgrade to jansi 1.15
* [`baac2b0`](https://github.com/jline/jline3/commit/baac2b0b85b3dd660d9b9ea4247d799037ad03d6) Use ttyname() with jansi-native > 1.6
* [`33eb5d4`](https://github.com/jline/jline3/commit/33eb5d472b4f08945394490f46968b34e32694d2) Jansi native pty support, fixes #102
* [`be0e9c2`](https://github.com/jline/jline3/commit/be0e9c27fac360bd9b49789a2b92e1ba41398c64) Switch to 3.3.0-SNAPSHOT
* [`9ec6a45`](https://github.com/jline/jline3/commit/9ec6a45da1d9ec8a271d4b166a5922784d00f198) Fix possible StringIndexOutOfBoundsException in Buffer.substring, #101
* [`f6559d5`](https://github.com/jline/jline3/commit/f6559d579bfc69d229d710e6417d5de6bdce9c11) Update README.md

## [JLine 3.2.0][3_2.0], released 2017-03-13
[3_2_0]: https://repo1.maven.org/maven2/org/jline/jline/3.2.0/

* [`79e7a34`](https://github.com/jline/jline3/commit/79e7a34c36a5012829c30ed06feac733531a3404) Upgrade demo to Gogo 1.0.4
* [`51f34d2`](https://github.com/jline/jline3/commit/51f34d207d98a409c392c67d968976f29f28c294) Add an option to disable syntax highlighting, fixes #100
* [`a3c6d61`](https://github.com/jline/jline3/commit/a3c6d614203cd3dc5bb1e27fea7bbc6a8489b196) Support terminals with only one line, #92
* [`7eaa384`](https://github.com/jline/jline3/commit/7eaa384cf06555c1b8c60eed6ca4b0baead8c6af) Add some javadoc
* [`b393ef5`](https://github.com/jline/jline3/commit/b393ef5a8d70f085ad9a64713c4db4471713ebfa) Support for SSH / telnet commands, fixes #68
* [`3e402db`](https://github.com/jline/jline3/commit/3e402db2078fd1bedd58a735f1d9d27ec1ba1fa9) Fix cursor computation when using a mask
* [`b3eb67b`](https://github.com/jline/jline3/commit/b3eb67b0f45833c30b73dd5afd411398a4b4ef02) Fix regression caused by #93
* [`a829116`](https://github.com/jline/jline3/commit/a829116ed7afb1c15a50894ce65ffacb004fae45) Remove reference to LineReaderImpl from the example
* [`cd7ecf6`](https://github.com/jline/jline3/commit/cd7ecf6e1c6124ed1d3f259b36d3977feb0c5fd2) Merge pull request #96 from cdupuis/master
* [`12219fa`](https://github.com/jline/jline3/commit/12219fa5a5fd56dbe0731519032c862fc4e39388) Possible exceptions when using gnu stty, fixes #97
* [`43cde96`](https://github.com/jline/jline3/commit/43cde9680b81405d149f6a98d5520a828648f861) Suppress outputting of group names in list and menu
* [`b6b3136`](https://github.com/jline/jline3/commit/b6b3136c169b9dfe7765e4ed680399fa72e30cbd) The history-search-forward widget does not work, fixes #94
* [`273bf18`](https://github.com/jline/jline3/commit/273bf18f9a2f4b4a6d17f032cdbab451dc61a979) AttributedCharSequence.toAnsi() - fix problems with multiple colors, fixes #93
* [`2c74096`](https://github.com/jline/jline3/commit/2c74096936c293ec6717e0f7ade3246590e2ac73) Add mouse usage javadoc, examples, and fix usage of getCursorPosition when reading mouse events, #91
* [`104a5eb`](https://github.com/jline/jline3/commit/104a5eb754cde677cf16033422df697e2c6914ab) Add a regexp based aggregate completer, fixes #90
* [`ea204fb`](https://github.com/jline/jline3/commit/ea204fb7a2003e0aa3e9f8407811d0d881b2e5a8) Remove unused import
* [`bd37774`](https://github.com/jline/jline3/commit/bd37774533e6e11f43bb49750b1fce0265638072) Fix bad indentation
* [`e618606`](https://github.com/jline/jline3/commit/e618606b4d7c844b060d574e1ea416cd68ca48e9) Fix possible sublist exception
* [`2144e6a`](https://github.com/jline/jline3/commit/2144e6a00746b56101cc2e9ebe5b188e261b5a90) Support an empty value as a Candidate that only suggests but doesn't complete, fixes #89
* [`6f7a2e5`](https://github.com/jline/jline3/commit/6f7a2e5497d7daf393e020797712dacde46d7348) Pass correct cursor position to Parser#parse() inside the acceptLine method, fixes #84
* [`66ce215`](https://github.com/jline/jline3/commit/66ce2159768a2bd5b82b8fed49bf3ee2343c6ad6) Improve api separation, fixes #86
* [`12dd8cc`](https://github.com/jline/jline3/commit/12dd8cc6615677b7add692256386d0b85a2e4c69) Support for 'Y' (yank-whole-line command), fixes #85
* [`691e876`](https://github.com/jline/jline3/commit/691e876de812a3bdbacd430f4d6499123fd35ab1) Tests for #84
* [`f6f2c95`](https://github.com/jline/jline3/commit/f6f2c953a1dda5f4c97500b30d89ac5bcee8ade7) Add missing vi binding for ^X^F, ^X^K, ^X^N, fixes #83
* [`c9768fc`](https://github.com/jline/jline3/commit/c9768fc0da0443eb9e470d675c2c2fcd32fad8a9) Support for the vi 'P' (put before) command, fixes #82
* [`cddb9ac`](https://github.com/jline/jline3/commit/cddb9acfa85bb28cfcbad21bd2c8e4f7086bd8d2) Support for the vi 'J' (join lines) command, #81
* [`ab0fcdb`](https://github.com/jline/jline3/commit/ab0fcdb9cb9ceadce3300d9e1d0df0f4cb09503e) Support for vi 'o' (add new line) command, fixes #80
* [`a22adf2`](https://github.com/jline/jline3/commit/a22adf2eb64086f0b80f7652f7c3ed9bafd8b5c4) Provide a thread top builtin command, fixes #66
* [`a5f0f19`](https://github.com/jline/jline3/commit/a5f0f19dbdddd09da06810b39950a41f7ace91f6) Fix demo script broken with modular build (#62)
* [`e5dbf83`](https://github.com/jline/jline3/commit/e5dbf8328c58c0f8ca94ce038ac45d4639031ec6) Package jansi / jna extensions as OSGi fragments to cross the ServiceLoader classloader boundary
* [`aecbf19`](https://github.com/jline/jline3/commit/aecbf19e6d38799591575ae33db5a68ccde6fdd2) Modular build, fixes #62
* [`a51a5c4`](https://github.com/jline/jline3/commit/a51a5c4194397e967dc664b01bcc177994d8a0d3) Provide a new 'clear' widget to erase the current display, fixes #75
* [`b14eb5e`](https://github.com/jline/jline3/commit/b14eb5ecb498eb0d09191a6ca4511e19a953f736) Simultaneous input and output example, #75
* [`83db9a0`](https://github.com/jline/jline3/commit/83db9a09005e29e89735ceb18a89583a98e345a1) Upgrade demo to gogo 1.0.2
* [`435ce36`](https://github.com/jline/jline3/commit/435ce3652441d8280bcec49b39926d435205e16e) Add a "fresh-line" widget, fixes #73
* [`75251a5`](https://github.com/jline/jline3/commit/75251a542cd03f3e8d1562f16f6c7c7fa49f02df) Add a link to demos in the readme
* [`3fb3fa4`](https://github.com/jline/jline3/commit/3fb3fa4e1da0899d1803f82fb7b56a17ef498f01) Remove unused test resources, fixes #71
* [`72da3dc`](https://github.com/jline/jline3/commit/72da3dc4eab0e403ad19470759e542e53af539ec) Lower InfoCmp memory consumption, fixes #49
* [`0e0ed37`](https://github.com/jline/jline3/commit/0e0ed37b2688b1399f7a9f8b44285c678e328fb5) Support WINCH on JNA+Windows, fixes #67
* [`b856769`](https://github.com/jline/jline3/commit/b856769d042c301382973d0001fc8af03cad424e) Remove maven site related files, fixes #63
* [`c89ff39`](https://github.com/jline/jline3/commit/c89ff39991a779f9e048af715d03a2fcb29d8b2e) Remove deprecated methods, fixes #69
* [`6a26a97`](https://github.com/jline/jline3/commit/6a26a97e63f0f9c1a32833ce4708b8d5770dd3b8) Correct fix for JDK8/JDK9 incompatibility, fixes #64
* [`bc807c5`](https://github.com/jline/jline3/commit/bc807c582f48fec584b05d7209810395912f5197) Fix closing panes in tmux, fix 32 color support, fixes #65
* [`1123650`](https://github.com/jline/jline3/commit/1123650e65189bdc2960054c07c0d4c1f28488b3) Update README.md
* [`f4fcb97`](https://github.com/jline/jline3/commit/f4fcb97b0d6d278800d304f8b82932db0335814d) Change master to 3.2.x
* [`03c94c7`](https://github.com/jline/jline3/commit/03c94c7763b8c9fafdb6c787dd8a51773caaef3e) Fix test on windows
* [`65dd1e9`](https://github.com/jline/jline3/commit/65dd1e97bcf34274a90fa145c74ac0e75fc011d0) Improve signal handling, fixes #59
* [`7576a62`](https://github.com/jline/jline3/commit/7576a62a144c33d1a91a434caf234ace36aea18f) JDK9 compatibility problem
* [`0e30d9f`](https://github.com/jline/jline3/commit/0e30d9fc7a881a7349579ed2cd4b44a32a3c4e84) JDK 9 compatibility
* [`6262a4b`](https://github.com/jline/jline3/commit/6262a4bc0044957a17ea8cf88123b2fcf042aed2) Remove site informations from the pom
* [`ee0a721`](https://github.com/jline/jline3/commit/ee0a721fa29fd2549c7e035eaa0297dd348931f9) Remove jline version from the windows demo script
* [`456c131`](https://github.com/jline/jline3/commit/456c1317ad44558aaf4b9c767d38264741c17b36) Improve support for dumb terminals (see #42, FELIX-5388)
* [`d6ac0de`](https://github.com/jline/jline3/commit/d6ac0deadb86d15c7b511b68866793702a16a5dd) i-search crashes with default setup, fixes #58
* [`65d06a0`](https://github.com/jline/jline3/commit/65d06a0b1739d994f91a69e8c328b16c112a8796) Crash on SIGTSTP (ctrl-Z) with native signals, fixes #59
* [`27e231c`](https://github.com/jline/jline3/commit/27e231ca23944abcaeee2ae4191e685a69b3dc11) Update README.md

## [JLine 3.1.1][3_1_1], released 2016-12-15
[3_1_1]: https://repo1.maven.org/maven2/org/jline/jline/3.1.1/

* [`972a85a`](https://github.com/jline/jline3/commit/972a85a479f430aa5e86c00ad1267f1172f0f75f) Possible endless loop on windows when the event reading loop fails, fixes #51
* [`1970ed7`](https://github.com/jline/jline3/commit/1970ed77a85764c76b7335b6ef4dc36fb6f38f6c) Escaped new lines should not be copied into the resulting buffer, fixes #54
* [`a2919c5`](https://github.com/jline/jline3/commit/a2919c579931c8ee7f307f1306c6369ab1790fbb) Add a redraw-line widget, fixes #53
* [`11fa95b`](https://github.com/jline/jline3/commit/11fa95b790ea3d34b560b08641bbaf8a220b8483) JNA linux / freebsd flags are wrong, fixes #52
* [`85e44ed`](https://github.com/jline/jline3/commit/85e44edfd6d424b17fafca8769fabf7272b211f6) Support for mingw, fixes #50
* [`bc29571`](https://github.com/jline/jline3/commit/bc295713603577b0a6ed2624d78762a311bc0dfb) Remove line wrapping in nano, fixes #45
* [`12e5b85`](https://github.com/jline/jline3/commit/12e5b8571e595d017d504b7b162ce8685e03cf5c) JDK9 compatibility: removing explicit boxing, #48
* [`c768abb`](https://github.com/jline/jline3/commit/c768abb8c0964a01b680d92e2044ffd0e1cb4796) Update README.md

## [JLine 3.1.0][3_1_0], released 2016-11-21
[3_1_0]: https://repo1.maven.org/maven2/org/jline/jline/3.1.0/

* [`bd7e7c5`](https://github.com/jline/jline3/commit/bd7e7c56d80bf14d536e61ba301e8eeec69ffcb3) Upgrade various plugins to fix Windows build, fixes #3
* [`77d2528`](https://github.com/jline/jline3/commit/77d25288763d144cdaa80681d76f3b78a4062c8b) Use UTF-8 in all tests, #3
* [`094828f`](https://github.com/jline/jline3/commit/094828ff05cbb7004d83688c3da3c5ba7472252d) Support compact1 Java 8 profile, fixes #43
* [`c9c1a15`](https://github.com/jline/jline3/commit/c9c1a15e33232e36ac856be95d02d5002466c0ba) Dumb terminal related fix (related to #42)
* [`fecb812`](https://github.com/jline/jline3/commit/fecb812f4ab920b119f52a8edda1513aae7d4f50) Mouse is only supported with Jna on Windows
* [`75dab0a`](https://github.com/jline/jline3/commit/75dab0aec2f5b9b57a012e26f4cde91ea3b2e710) Do not suspend the demo in debug mode
* [`fd054cf`](https://github.com/jline/jline3/commit/fd054cfd8004e99347b0352ee2597403a06c42f3) Cursor position computation is wrong
* [`8e45bb2`](https://github.com/jline/jline3/commit/8e45bb280d52d23bdcee8bc9015e12c172149c9b) Fix cursor positioning when moving down with wrapped lines
* [`4524a42`](https://github.com/jline/jline3/commit/4524a4288670b15f93c0980b5262f0b95212054b) Switch to 3.1.0-SNAPSHOT
* [`e754177`](https://github.com/jline/jline3/commit/e754177c830657ab76cec16345b1d20b6d1bd285) Merge remote-tracking branch 'PerBothner/wrapping'
* [`9ad43df`](https://github.com/jline/jline3/commit/9ad43df7c4c06ba80be51c0999281cf52d589828) Merge branch '3.0.x'
* [`349e39a`](https://github.com/jline/jline3/commit/349e39a4fa0907e0fa7060197a9ebf1bc8cf0e23) Remove unused imports
* [`d863394`](https://github.com/jline/jline3/commit/d8633941bf27f08ce77e7544a19d01e8bf7c11de) Minor code cleanup
* [`dc467f4`](https://github.com/jline/jline3/commit/dc467f48b8e964435cd2f83b1b11f8e486052128) Make sure we have a character left on the end footer lines to avoid a scroll down on the very last character
* [`99939b3`](https://github.com/jline/jline3/commit/99939b3bbc94a606656380eb96744d96fe41ba1e) The dumb terminal should not report ansi sequences support, fixes #42
* [`ac55a63`](https://github.com/jline/jline3/commit/ac55a6371ff730f3b3fd0437cfdeb73d6d715dc9) Fix off-by-one problems with right prompts.
* [`f5ad557`](https://github.com/jline/jline3/commit/f5ad557e32e35ddb3305658a0bd3f3065eae9b88) Better of implementation for when DELAY_LINE_WRAP is unset.
* [`8bbf2c7`](https://github.com/jline/jline3/commit/8bbf2c78c01fa0f2e35002e0603f9fad1a97dd33) Fix right prompt support This is actually https://github.com/gnodet/jline3/commit/8e921519dbc5fc228ab8776effe616d19c2ca5f2
* [`8438a84`](https://github.com/jline/jline3/commit/8438a8400c2b673382c1eab5648e6b88f337396a) Correctly catch format exceptions when loading history #28
* [`429c51c`](https://github.com/jline/jline3/commit/429c51c046df95a7a3115d10c29ab903556aefd7) After saving, the file is still flagged as modified
* [`f808f57`](https://github.com/jline/jline3/commit/f808f57899f8601858fde677efcbeee0225c9116) Full redraw when resizing
* [`a335cee`](https://github.com/jline/jline3/commit/a335ceee9a2ebe70a57f537828f7159a9b325b22) Merge branch 'master' of https://github.com/jline/jline3 into wrapping
* [`7c47075`](https://github.com/jline/jline3/commit/7c470753d0699a08fdf3602a2d2365f3c5facf04) Fix unstable HistoryPersistenceTest
* [`fef74c3`](https://github.com/jline/jline3/commit/fef74c356db972c4cb8c507947d95de738b907a6) Cygwin pty is used when running with the default windows console, fixes #41
* [`99ded1b`](https://github.com/jline/jline3/commit/99ded1b1d8045fe27e9d6b39566cd3b27ac44e0b) Add a simple demo for gogo
* [`dbf7363`](https://github.com/jline/jline3/commit/dbf73639e16cd4db8c5d9af6b6534547cdf45d8b) Fix unstable HistoryPersistenceTest
* [`fe83731`](https://github.com/jline/jline3/commit/fe837316521543cd4fabaf2c02da54be6eab8cfb) Mouse support, fixes #38
* [`81c63ae`](https://github.com/jline/jline3/commit/81c63ae70f0103c84b005b5c5cf473a7b84f71e9) Cygwin pty is used when running with the default windows console, fixes #41
* [`80b798e`](https://github.com/jline/jline3/commit/80b798e26e29750f0b8ae936b7e0291842325bde) Move nano help files to the correct location
* [`9ea4722`](https://github.com/jline/jline3/commit/9ea47227e1eaa2f735f6d2d876c1dab3d781db32) Cursor reporting support, fixes #40
* [`a2578fc`](https://github.com/jline/jline3/commit/a2578fc49da7cf07e835f8d35023a1a2c7244c22) Update README.md
* [`8d259a9`](https://github.com/jline/jline3/commit/8d259a9c9782ec85e3567590843883b07ad37249) Fix some problems with wrapped lines and multiple logical lines.
* [`2af23c1`](https://github.com/jline/jline3/commit/2af23c183038783fb49c14fe059f286b35651b9f) Special handle of "hidden" character in DiffHelper. Specifically, a span of hidden characters has to be handled as a unit: Since its meaning is opaque, we have to emit all or none.
* [`d0e191f`](https://github.com/jline/jline3/commit/d0e191f4d1c3e218a572d67e611d22df9ceda08a) Add an example for multiline parsing, fixes #36
* [`ab1d420`](https://github.com/jline/jline3/commit/ab1d420e10365cabf779d7e500d18e419e9fe0b9) Add an example completer for #35 It would be nice to have a few more builtin Completers to help
* [`63ce9be`](https://github.com/jline/jline3/commit/63ce9beae0937816fcb473e19e5da9d368197525) Add a public way to identify dumb terminals.
* [`380584c`](https://github.com/jline/jline3/commit/380584c7a9b019a4b55e446209707f1dcd951cf3) Add a public way to identify dumb terminals.
* [`f4cf2a2`](https://github.com/jline/jline3/commit/f4cf2a2e5dc0416d7da7e11f94fd8e71010ee177) Remove unwanted newline at the end of error message
* [`ac603a2`](https://github.com/jline/jline3/commit/ac603a2e428205505347523aeb9cbbe8ed2f2762) Create a dumb terminal on windows too, fixes #32. Prints a warning when creating a dumb terminal unless dumb(true) has been explicitely called.
* [`216d28f`](https://github.com/jline/jline3/commit/216d28f7655fd6da7efe7eabebe515b309905f6b) Fix possible NPE, #32
* [`f44de2e`](https://github.com/jline/jline3/commit/f44de2e863ad33132d0f45609fb3f9f88c849228) Add a bit of javadoc
* [`c994055`](https://github.com/jline/jline3/commit/c994055e697c18a995e483b9854b41ac955c7327) Throw a more meaningful error when jna and jansi are not available on windows, #30
* [`066491e`](https://github.com/jline/jline3/commit/066491e915c8673d91b458cc9727f039633e13b1) TerminalBuilder now logs a warning for ignored attributes, fixes #29
* [`6285fdd`](https://github.com/jline/jline3/commit/6285fdde000aef3e1c1fa4a4f6791f718853e2ab) Minor logging improvements
* [`a408dd3`](https://github.com/jline/jline3/commit/a408dd3d3f0140c27bf09c255965313b6c480c2b) Update readme for 3.0.1
* [`e5a38bc`](https://github.com/jline/jline3/commit/e5a38bcbb8434ec180bee640cfd562abd48e6d29) TerminalBuilder ignores attributes, fixes #29
* [`b28794c`](https://github.com/jline/jline3/commit/b28794c40c920a383b86d180b83c4955604e1d3f) Merge pull request #33 from PerBothner/no-max_colors
* [`25d3a3d`](https://github.com/jline/jline3/commit/25d3a3d1349f4aeea42ec8a644ecef98bdcfb467) When lines wrap, write wrapped lines to terminals that support it: This enables property copy/paste (from terminal to editor, say) and fixes JLine issue "Support correct line wrapping in org.jline.utils.Display #25".
* [`82b7d28`](https://github.com/jline/jline3/commit/82b7d283ea65c20f0c591108d8fba1866bc3ff4b) Avoid NullPointerException when the max_colors capability is null. This happened when setting TERM=vt100.

## [JLine 3.0.1][3_0_1], released 2016-10-20
[3_0_1]: https://repo1.maven.org/maven2/org/jline/jline/3.0.1/

* [`892be0d`](https://github.com/jline/jline3/commit/892be0dccc31f726c973a17f1574f88c3d730a27) Remove changelog
* [`ecea215`](https://github.com/jline/jline3/commit/ecea215518dfb8ab45586ce88d0bae01cdb86892) Problems resetting the pty on linux+jna, #27
* [`7d1a3a8`](https://github.com/jline/jline3/commit/7d1a3a8b950c14aedc20a2df8e5e034940a1f5b2) Some colors do not work for background in 256 colors, fixes #26

## [JLine 3.0.0][3_0_0], released 2016-10-11
[3_0_0]: https://repo1.maven.org/maven2/org/jline/jline/3.0.0/

