/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jline.curses.*;
import org.jline.curses.Curses.Location;

public class BorderPanel extends AbstractPanel {

    @Override
    public void addComponent(Component component, Constraint constraint) {
        if (!(constraint instanceof Location)) {
            throw new IllegalArgumentException("Constraint should be a Location: " + constraint);
        }
        if (components.containsValue(constraint)) {
            throw new IllegalArgumentException("Two components have the same location: " + constraint);
        }
        super.addComponent(component, constraint);
    }

    @Override
    protected Size doGetPreferredSize() {
        Map<Location, Integer> w = new EnumMap<>(Location.class);
        Map<Location, Integer> h = new EnumMap<>(Location.class);
        // Compute preferred heights and widths of components
        preferred(w, h);
        // Width
        int pw = max(
                w.get(Location.Top),
                w.get(Location.Left) + w.get(Location.Center) + w.get(Location.Right),
                w.get(Location.Bottom));
        // Height
        int ph = h.get(Location.Top)
                + max(h.get(Location.Left), h.get(Location.Center), h.get(Location.Right))
                + h.get(Location.Bottom);
        return new Size(pw, ph);
    }

    @Override
    protected void layout() {
        Size size = getSize();

        Map<Location, Integer> x = new EnumMap<>(Location.class);
        Map<Location, Integer> y = new EnumMap<>(Location.class);
        Map<Location, Integer> w = new EnumMap<>(Location.class);
        Map<Location, Integer> h = new EnumMap<>(Location.class);
        // Compute preferred heights and widths
        preferred(w, h);
        // Arrange
        fit(h, size.h(), Location.Center, Location.Top, Location.Bottom);
        fit(w, size.w(), Location.Center, Location.Left, Location.Right);
        w.put(Location.Top, size.w());
        w.put(Location.Bottom, size.w());
        h.put(Location.Left, h.get(Location.Center));
        h.put(Location.Right, h.get(Location.Center));
        pos(x, w, Location.Left, Location.Center, Location.Right);
        pos(y, h, Location.Top, Location.Center, Location.Bottom);
        x.put(Location.Top, 0);
        x.put(Location.Bottom, 0);
        y.put(Location.Left, y.get(Location.Center));
        y.put(Location.Right, y.get(Location.Center));
        // Assign
        for (Map.Entry<Component, Constraint> entry : components.entrySet()) {
            Component c = entry.getKey();
            Location l = (Location) entry.getValue();
            c.setPosition(new Position(x.get(l), y.get(l)));
            c.setSize(new Size(w.get(l), h.get(l)));
        }
    }

    private void pos(Map<Location, Integer> p, Map<Location, Integer> s, Location... locs) {
        int c = 0;
        for (Location loc : locs) {
            p.put(loc, c);
            c += s.getOrDefault(loc, 0);
        }
    }

    private void fit(Map<Location, Integer> h, int max, Location... locs) {
        int diff = Stream.of(locs)
                        .map(l -> h.getOrDefault(l, 0))
                        .mapToInt(Integer::intValue)
                        .sum()
                - max;
        if (diff < 0) {
            h.put(locs[0], h.get(locs[0]) - diff);
        } else {
            for (int idx = locs.length - 1; diff > 0; idx--) {
                int l = h.get(locs[idx]);
                int nb = Math.min(diff, l);
                h.put(locs[idx], l - nb);
                diff -= nb;
            }
        }
    }

    private void preferred(Map<Location, Integer> w, Map<Location, Integer> h) {
        for (Location l : Location.values()) {
            w.put(l, 0);
            h.put(l, 0);
        }
        for (Map.Entry<Component, Constraint> e : components.entrySet()) {
            Location l = (Location) e.getValue();
            Size s = e.getKey().getPreferredSize();
            w.put(l, s.w());
            h.put(l, s.h());
        }
    }

    private static int max(int i0, int i1, int i2) {
        return Math.max(i0, Math.max(i1, i2));
    }
}
