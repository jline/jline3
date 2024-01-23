package de.codeshelf.consoleui.prompt;

/**
 * Result of a list choice. Holds the id of the selected item.
 * <p>
 * Created by Andreas Wegmann on 03.02.16.
 */
public class ListResult implements PromptResultItemIF {

  String selectedId;

  /**
   * Returns the ID of the selected item.
   *
   * @return id of selected item
   */
  public String getSelectedId() {
    return selectedId;
  }

  public String getResult() {
    return selectedId;
  }

  /**
   * Default constructor.
   *
   * @param selectedId id of selected item.
   */
  public ListResult(String selectedId) {
    this.selectedId = selectedId;
  }

  @Override
  public String toString() {
    return "ListResult{" +
            "selectedId='" + selectedId + '\'' +
            '}';
  }
}
