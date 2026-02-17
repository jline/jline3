/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.layout;

import java.util.Objects;

/**
 * Immutable insets (padding/margin) in character cells.
 */
public final class Insets {

    public static final Insets NONE = new Insets(0, 0, 0, 0);

    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    public Insets(int top, int right, int bottom, int left) {
        this.top = Math.max(0, top);
        this.right = Math.max(0, right);
        this.bottom = Math.max(0, bottom);
        this.left = Math.max(0, left);
    }

    public static Insets of(int all) {
        return new Insets(all, all, all, all);
    }

    public static Insets of(int vertical, int horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    public int top() {
        return top;
    }

    public int right() {
        return right;
    }

    public int bottom() {
        return bottom;
    }

    public int left() {
        return left;
    }

    public int horizontal() {
        return left + right;
    }

    public int vertical() {
        return top + bottom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Insets)) return false;
        Insets insets = (Insets) o;
        return top == insets.top && right == insets.right && bottom == insets.bottom && left == insets.left;
    }

    @Override
    public int hashCode() {
        return Objects.hash(top, right, bottom, left);
    }

    @Override
    public String toString() {
        return "Insets[" + top + "," + right + "," + bottom + "," + left + "]";
    }
}
