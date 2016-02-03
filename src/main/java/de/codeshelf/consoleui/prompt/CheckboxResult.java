package de.codeshelf.consoleui.prompt;

import java.util.HashSet;

/**
 * User: ${FULL_NAME}
 * Date: 03.02.16
 */
public class CheckboxResult implements PromtResultItemIF {
  HashSet<String> selectedIds;

  public CheckboxResult(HashSet<String> selectedIds) {
    this.selectedIds = selectedIds;
  }

  public HashSet<String> getSelectedIds() {
    return selectedIds;
  }

  @Override
  public String toString() {
    return "CheckboxResult{" +
            "selectedIds=" + selectedIds +
            '}';
  }
}
