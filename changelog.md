# [JLine3](https://github.com/jline/jline3)

<!-- git log --pretty=format:'* [`%h`](https://github.com/jline/jline3/commit/%H) %s' -->

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

