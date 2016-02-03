package de.codeshelf.consoleui.prompt;

/**
 * Created by Andreas Wegmann on 03.02.16.
 */
public class ListResult implements PromtResultItemIF {
  String selectedId;

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
