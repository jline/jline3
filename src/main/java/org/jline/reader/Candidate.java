/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import static org.jline.utils.Preconditions.checkNotNull;

public class Candidate implements Comparable<Candidate> {

    private final String value;
    private final String displ;
    private final String group;
    private final String descr;
    private final String suffix;
    private final String key;
    private final boolean complete;

    public Candidate(String value) {
        this(value, value, null, null, null, null, true);
    }

    public Candidate(String value, String displ, String group, String descr, String suffix, String key, boolean complete) {
        checkNotNull(value);
        this.value = value;
        this.displ = displ;
        this.group = group;
        this.descr = descr;
        this.suffix = suffix;
        this.key = key;
        this.complete = complete;
    }

    public String value() {
        return value;
    }

    public String displ() {
        return displ;
    }

    public String group() {
        return group;
    }

    public String descr() {
        return descr;
    }

    public String suffix() {
        return suffix;
    }

    public String key() {
        return key;
    }

    public boolean complete() {
        return complete;
    }

    @Override
    public int compareTo(Candidate o) {
        return value.compareTo(o.value);
    }
}
