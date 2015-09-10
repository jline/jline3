package org.jline;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * Created by gnodet on 07/09/15.
 */
public class Candidate {

    private final String value;
    private final String group;
    private final String descr;

    public Candidate(String value) {
        this(value, null, null);
    }

    public Candidate(String value, String group, String descr) {
        checkNotNull(value);
        this.value = value;
        this.group = group;
        this.descr = descr;
    }

    public String value() {
        return value;
    }

    public String group() {
        return group;
    }

    public String descr() {
        return descr;
    }
}
