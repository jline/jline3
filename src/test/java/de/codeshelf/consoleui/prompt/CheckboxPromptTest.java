package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.elements.items.impl.Separator;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Andreas Wegmann
 * Date: 07.12.15
 */
public class CheckboxPromptTest {
  @Test
  public void renderSimpleList() throws IOException {
    List<CheckboxItemIF> list= new ArrayList<>();

    list.add(new CheckboxItem("One"));
    list.add(new CheckboxItem(true,"Two"));
    CheckboxItem three = new CheckboxItem("Three");
    three.setDisabled("not available");
    list.add(three);
    list.add(new Separator("some extra items"));
    list.add(new CheckboxItem("Four"));
    list.add(new CheckboxItem(true,"Five"));

  }

}