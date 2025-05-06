/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.utils.InfoCmp.Capability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the InfoCmp class.
 */
public class InfoCmpTest {

    @Test
    public void testInfoCmp() {
        Set<Capability> bools = new HashSet<>();
        Map<Capability, Integer> ints = new HashMap<>();
        Map<Capability, String> strings = new HashMap<>();

        String infocmp = InfoCmp.getDefaultInfoCmp("ansi");
        InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
        assertEquals(4, bools.size());
        assertTrue(strings.containsKey(Capability.byName("acsc")));
    }

    @Test
    public void testGetNames() {
        String[] result = Capability.bit_image_entwining.getNames();
        assertArrayEquals(new String[] {"bit_image_entwining", "bitwin"}, result);
    }

    @Test
    public void testInfoCmpWithHexa() {
        Set<Capability> bools = new HashSet<>();
        Map<Capability, Integer> ints = new HashMap<>();
        Map<Capability, String> strings = new HashMap<>();
        String infocmp = "xterm-256color|xterm with 256 colors,\n" + "\tam, bce, ccc, km, mc5i, mir, msgr, npc, xenl,\n"
                + "\tcolors#0x100, cols#010, it#0, lines#24, pairs#0x7fff,\n"
                + "\tacsc=``aaffggiijjkkllmmnnooppqqrrssttuuvvwwxxyyzz{{||}}~~,\n"
                + "\tbel=^G, blink=\\E[5m, bold=\\E[1m, cbt=\\E[Z, civis=\\E[?25l\n";
        InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
        assertEquals(8, (int) ints.get(Capability.columns));
        assertEquals(0x100, (int) ints.get(Capability.max_colors));
        assertEquals(0x7fff, (int) ints.get(Capability.max_pairs));
    }

    @Test
    public void testClrEos() {
        Set<Capability> bools = new HashSet<>();
        Map<Capability, Integer> ints = new HashMap<>();
        Map<Capability, String> strings = new HashMap<>();
        String infocmp = InfoCmp.getDefaultInfoCmp("xterm");
        InfoCmp.parseInfoCmp(infocmp, bools, ints, strings);
        assertEquals("\\E[J", strings.get(Capability.clr_eos));
    }

    @Test
    public void testAllCapsFile() throws IOException {
        String packagePath = InfoCmp.class.getPackage().getName().replace(".", "/");
        ArrayList<URL> packageLocations =
                Collections.list(Thread.currentThread().getContextClassLoader().getResources(packagePath));
        List<String> allCaps = packageLocations.stream()
                .map(url -> {
                    try {
                        Path capsLocation = Paths.get(url.toURI());
                        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.caps");
                        List<String> s = Files.walk(capsLocation)
                                .filter(pathMatcher::matches)
                                .map(Path::getFileName)
                                .map(Path::toString)
                                .map(fileName -> fileName.split("\\.")[0])
                                .collect(Collectors.toList());
                        return s.stream();
                    } catch (URISyntaxException | IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Function.identity())
                .collect(Collectors.toList());

        allCaps.forEach((capsName) -> assertNotNull(
                String.format("%s.caps was not registered in InfoCmp class", capsName),
                InfoCmp.getLoadedInfoCmp(capsName)));
    }
}
