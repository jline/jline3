/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

/**
 * Result of an expandable choice. ExpandableChoiceResult contains a String with the
 * IDs of the selected item.
 * <p>
 * User: Andreas Wegmann<p>
 * Date: 03.02.16
 */
public class ExpandableChoiceResult implements PromptResultItemIF {
    String selectedId;

    /**
     * Default constructor.
     *
     * @param selectedId the selected id
     */
    public ExpandableChoiceResult(String selectedId) {
        this.selectedId = selectedId;
    }

    /**
     * Returns the selected id.
     *
     * @return selected id.
     */
    public String getSelectedId() {
        return selectedId;
    }

    public String getResult() {
        return selectedId;
    }

    @Override
    public String toString() {
        return "ExpandableChoiceResult{" + "selectedId='" + selectedId + '\'' + '}';
    }
}
