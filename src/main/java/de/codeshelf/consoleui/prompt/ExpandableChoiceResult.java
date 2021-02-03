package de.codeshelf.consoleui.prompt;

/**
 * Result of a an expandable choice. ExpandableChoiceResult contains a String with the
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
    return "ExpandableChoiceResult{" +
            "selectedId='" + selectedId + '\'' +
            '}';
  }
}
