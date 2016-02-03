package de.codeshelf.consoleui.prompt;

/**
 * User: ${FULL_NAME}
 * Date: 03.02.16
 */
public class ExpandableChoiceResult implements PromtResultItemIF {
  String selectedId;

  public ExpandableChoiceResult(String selectedId) {
    this.selectedId = selectedId;
  }

  public String getSelectedId() {
    return selectedId;
  }

  @Override
  public String toString() {
    return "ExpandableChoiceResult{" +
            "selectedId='" + selectedId + '\'' +
            '}';
  }
}
