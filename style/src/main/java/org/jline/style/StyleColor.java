/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

/**
 * Enumeration of named colors for use in styles.
 * <p>
 * This enum provides a comprehensive list of named colors that can be used in style
 * specifications. Each color has an associated ANSI color code and an RGB hex value
 * (shown in comments).
 * </p>
 * <p>
 * This class is deprecated in favor of {@link org.jline.utils.Colors#rgbColor(String)},
 * which provides a more flexible way to specify colors using RGB values or X11 color names.
 * </p>
 * <p>
 * Example usage (not recommended due to deprecation):
 * </p>
 * <pre>
 * // Get the ANSI color code for a named color
 * int code = StyleColor.red.code;  // 9
 * </pre>
 *
 * @since 3.4
 * @deprecated use {@link org.jline.utils.Colors#rgbColor(String)} instead
 */
@Deprecated
public enum StyleColor {
    black(0), // #000000
    maroon(1), // #800000
    green(2), // #008000
    olive(3), // #808000
    navy(4), // #000080
    purple(5), // #800080
    teal(6), // #008080
    silver(7), // #c0c0c0
    grey(8), // #808080
    red(9), // #ff0000
    lime(10), // #00ff00
    yellow(11), // #ffff00
    blue(12), // #0000ff
    fuchsia(13), // #ff00ff
    aqua(14), // #00ffff
    white(15), // #ffffff
    grey0(16), // #000000
    navyblue(17), // #00005f
    darkblue(18), // #000087
    blue3(19), // #0000af
    blue3a(20), // #0000d7
    blue1(21), // #0000ff
    darkgreen(22), // #005f00
    deepskyblue4(23), // #005f5f
    deepskyblue4a(24), // #005f87
    deepskyblue4b(25), // #005faf
    dodgerblue3(26), // #005fd7
    dodgerblue2(27), // #005fff
    green4(28), // #008700
    springgreen4(29), // #00875f
    turquoise4(30), // #008787
    deepskyblue3(31), // #0087af
    deepskyblue3a(32), // #0087d7
    dodgerblue1(33), // #0087ff
    green3(34), // #00af00
    springgreen3(35), // #00af5f
    darkcyan(36), // #00af87
    lightseagreen(37), // #00afaf
    deepskyblue2(38), // #00afd7
    deepskyblue1(39), // #00afff
    green3a(40), // #00d700
    springgreen3a(41), // #00d75f
    springgreen2(42), // #00d787
    cyan3(43), // #00d7af
    darkturquoise(44), // #00d7d7
    turquoise2(45), // #00d7ff
    green1(46), // #00ff00
    springgreen2a(47), // #00ff5f
    springgreen1(48), // #00ff87
    mediumspringgreen(49), // #00ffaf
    cyan2(50), // #00ffd7
    cyan1(51), // #00ffff
    darkred(52), // #5f0000
    deeppink4(53), // #5f005f
    purple4(54), // #5f0087
    purple4a(55), // #5f00af
    purple3(56), // #5f00d7
    blueviolet(57), // #5f00ff
    orange4(58), // #5f5f00
    grey37(59), // #5f5f5f
    mediumpurple4(60), // #5f5f87
    slateblue3(61), // #5f5faf
    slateblue3a(62), // #5f5fd7
    royalblue1(63), // #5f5fff
    chartreuse4(64), // #5f8700
    darkseagreen4(65), // #5f875f
    paleturquoise4(66), // #5f8787
    steelblue(67), // #5f87af
    steelblue3(68), // #5f87d7
    cornflowerblue(69), // #5f87ff
    chartreuse3(70), // #5faf00
    darkseagreen4a(71), // #5faf5f
    cadetblue(72), // #5faf87
    cadetbluea(73), // #5fafaf
    skyblue3(74), // #5fafd7
    steelblue1(75), // #5fafff
    chartreuse3a(76), // #5fd700
    palegreen3(77), // #5fd75f
    seagreen3(78), // #5fd787
    aquamarine3(79), // #5fd7af
    mediumturquoise(80), // #5fd7d7
    steelblue1a(81), // #5fd7ff
    chartreuse2(82), // #5fff00
    seagreen2(83), // #5fff5f
    seagreen1(84), // #5fff87
    seagreen1a(85), // #5fffaf
    aquamarine1(86), // #5fffd7
    darkslategray2(87), // #5fffff
    darkreda(88), // #870000
    deeppink4a(89), // #87005f
    darkmagenta(90), // #870087
    darkmagentaa(91), // #8700af
    darkviolet(92), // #8700d7
    purplea(93), // #8700ff
    orange4a(94), // #875f00
    lightpink4(95), // #875f5f
    plum4(96), // #875f87
    mediumpurple3(97), // #875faf
    mediumpurple3a(98), // #875fd7
    slateblue1(99), // #875fff
    yellow4(100), // #878700
    wheat4(101), // #87875f
    grey53(102), // #878787
    lightslategrey(103), // #8787af
    mediumpurple(104), // #8787d7
    lightslateblue(105), // #8787ff
    yellow4a(106), // #87af00
    darkolivegreen3(107), // #87af5f
    darkseagreen(108), // #87af87
    lightskyblue3(109), // #87afaf
    lightskyblue3a(110), // #87afd7
    skyblue2(111), // #87afff
    chartreuse2a(112), // #87d700
    darkolivegreen3a(113), // #87d75f
    palegreen3a(114), // #87d787
    darkseagreen3(115), // #87d7af
    darkslategray3(116), // #87d7d7
    skyblue1(117), // #87d7ff
    chartreuse1(118), // #87ff00
    lightgreen(119), // #87ff5f
    lightgreena(120), // #87ff87
    palegreen1(121), // #87ffaf
    aquamarine1a(122), // #87ffd7
    darkslategray1(123), // #87ffff
    red3(124), // #af0000
    deeppink4b(125), // #af005f
    mediumvioletred(126), // #af0087
    magenta3(127), // #af00af
    darkvioleta(128), // #af00d7
    purpleb(129), // #af00ff
    darkorange3(130), // #af5f00
    indianred(131), // #af5f5f
    hotpink3(132), // #af5f87
    mediumorchid3(133), // #af5faf
    mediumorchid(134), // #af5fd7
    mediumpurple2(135), // #af5fff
    darkgoldenrod(136), // #af8700
    lightsalmon3(137), // #af875f
    rosybrown(138), // #af8787
    grey63(139), // #af87af
    mediumpurple2a(140), // #af87d7
    mediumpurple1(141), // #af87ff
    gold3(142), // #afaf00
    darkkhaki(143), // #afaf5f
    navajowhite3(144), // #afaf87
    grey69(145), // #afafaf
    lightsteelblue3(146), // #afafd7
    lightsteelblue(147), // #afafff
    yellow3(148), // #afd700
    darkolivegreen3b(149), // #afd75f
    darkseagreen3a(150), // #afd787
    darkseagreen2(151), // #afd7af
    lightcyan3(152), // #afd7d7
    lightskyblue1(153), // #afd7ff
    greenyellow(154), // #afff00
    darkolivegreen2(155), // #afff5f
    palegreen1a(156), // #afff87
    darkseagreen2a(157), // #afffaf
    darkseagreen1(158), // #afffd7
    paleturquoise1(159), // #afffff
    red3a(160), // #d70000
    deeppink3(161), // #d7005f
    deeppink3a(162), // #d70087
    magenta3a(163), // #d700af
    magenta3b(164), // #d700d7
    magenta2(165), // #d700ff
    darkorange3a(166), // #d75f00
    indianreda(167), // #d75f5f
    hotpink3a(168), // #d75f87
    hotpink2(169), // #d75faf
    orchid(170), // #d75fd7
    mediumorchid1(171), // #d75fff
    orange3(172), // #d78700
    lightsalmon3a(173), // #d7875f
    lightpink3(174), // #d78787
    pink3(175), // #d787af
    plum3(176), // #d787d7
    violet(177), // #d787ff
    gold3a(178), // #d7af00
    lightgoldenrod3(179), // #d7af5f
    tan(180), // #d7af87
    mistyrose3(181), // #d7afaf
    thistle3(182), // #d7afd7
    plum2(183), // #d7afff
    yellow3a(184), // #d7d700
    khaki3(185), // #d7d75f
    lightgoldenrod2(186), // #d7d787
    lightyellow3(187), // #d7d7af
    grey84(188), // #d7d7d7
    lightsteelblue1(189), // #d7d7ff
    yellow2(190), // #d7ff00
    darkolivegreen1(191), // #d7ff5f
    darkolivegreen1a(192), // #d7ff87
    darkseagreen1a(193), // #d7ffaf
    honeydew2(194), // #d7ffd7
    lightcyan1(195), // #d7ffff
    red1(196), // #ff0000
    deeppink2(197), // #ff005f
    deeppink1(198), // #ff0087
    deeppink1a(199), // #ff00af
    magenta2a(200), // #ff00d7
    magenta1(201), // #ff00ff
    orangered1(202), // #ff5f00
    indianred1(203), // #ff5f5f
    indianred1a(204), // #ff5f87
    hotpink(205), // #ff5faf
    hotpinka(206), // #ff5fd7
    mediumorchid1a(207), // #ff5fff
    darkorange(208), // #ff8700
    salmon1(209), // #ff875f
    lightcoral(210), // #ff8787
    palevioletred1(211), // #ff87af
    orchid2(212), // #ff87d7
    orchid1(213), // #ff87ff
    orange1(214), // #ffaf00
    sandybrown(215), // #ffaf5f
    lightsalmon1(216), // #ffaf87
    lightpink1(217), // #ffafaf
    pink1(218), // #ffafd7
    plum1(219), // #ffafff
    gold1(220), // #ffd700
    lightgoldenrod2a(221), // #ffd75f
    lightgoldenrod2b(222), // #ffd787
    navajowhite1(223), // #ffd7af
    mistyrose1(224), // #ffd7d7
    thistle1(225), // #ffd7ff
    yellow1(226), // #ffff00
    lightgoldenrod1(227), // #ffff5f
    khaki1(228), // #ffff87
    wheat1(229), // #ffffaf
    cornsilk1(230), // #ffffd7
    grey100(231), // #ffffff
    grey3(232), // #080808
    grey7(233), // #121212
    grey11(234), // #1c1c1c
    grey15(235), // #262626
    grey19(236), // #303030
    grey23(237), // #3a3a3a
    grey27(238), // #444444
    grey30(239), // #4e4e4e
    grey35(240), // #585858
    grey39(241), // #626262
    grey42(242), // #6c6c6c
    grey46(243), // #767676
    grey50(244), // #808080
    grey54(245), // #8a8a8a
    grey58(246), // #949494
    grey62(247), // #9e9e9e
    grey66(248), // #a8a8a8
    grey70(249), // #b2b2b2
    grey74(250), // #bcbcbc
    grey78(251), // #c6c6c6
    grey82(252), // #d0d0d0
    grey85(253), // #dadada
    grey89(254), // #e4e4e4
    grey93(255); // #eeeeee

    /**
     * The ANSI color code for this color.
     * <p>
     * This code can be used in ANSI escape sequences to set the foreground or
     * background color in a terminal.
     * </p>
     */
    public final int code;

    /**
     * Constructs a StyleColor with the specified ANSI color code.
     *
     * @param code the ANSI color code
     */
    StyleColor(final int code) {
        this.code = code;
    }
}
