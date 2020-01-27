# [JLine3](https://github.com/jline/jline3)

<!-- git log --pretty=format:'* [`%h`](https://github.com/jline/jline3/commit/%H) %s' -->
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

