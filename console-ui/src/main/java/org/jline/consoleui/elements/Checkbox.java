/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements;

import java.util.List;

import org.jline.consoleui.elements.items.CheckboxItemIF;

public class Checkbox extends AbstractPromptableElement {

    private final int pageSize;
    private final PageSizeType pageSizeType;
    private final List<CheckboxItemIF> checkboxItemList;

    public Checkbox(
            String message,
            String name,
            int pageSize,
            PageSizeType pageSizeType,
            List<CheckboxItemIF> checkboxItemList) {
        super(message, name);
        if (pageSizeType == PageSizeType.RELATIVE && (pageSize < 1 || pageSize > 100))
            throw new IllegalArgumentException("for relative page size, the valid values are from 1 to 100");

        this.pageSizeType = pageSizeType;
        this.pageSize = pageSize;
        this.checkboxItemList = checkboxItemList;
    }

    public String getMessage() {
        return message;
    }

    public List<CheckboxItemIF> getCheckboxItemList() {
        return checkboxItemList;
    }

    public int getPageSize() {
        return pageSize;
    }

    public PageSizeType getPageSizeType() {
        return pageSizeType;
    }
}
