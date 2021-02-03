package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public class Checkbox extends AbstractPromptableElement {

  private final int pageSize;
  private final PageSizeType pageSizeType;
  private List<CheckboxItemIF> checkboxItemList;

  public Checkbox(String message, String name, int pageSize, PageSizeType pageSizeType, List<CheckboxItemIF> checkboxItemList) {
    super(message,name);
    if (pageSizeType == PageSizeType.RELATIVE && (pageSize < 1 || pageSize >100))
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
