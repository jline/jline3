package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.InputValue;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import jline.console.completer.Completer;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Implementation of the input choice prompt. The user will be asked for a string input value.
 * With support of completers an automatic expansion of strings and filenames can be configured.
 * Defining a mask character, a password like input is possible.
 * <p>
 * User: Andreas Wegmann<p>
 * Date: 06.01.16
 */
public class InputPrompt extends AbstractPrompt implements PromptIF<InputValue,InputResult> {

  private InputValue inputElement;
  private ReaderIF reader;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  public InputPrompt() throws IOException {
  }


  public InputResult prompt(InputValue inputElement) throws IOException {
    this.inputElement = inputElement;

    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }

    if (renderHeight == 0) {
      renderHeight = 2;
    } 

    String prompt = renderMessagePrompt(this.inputElement.getMessage()) + itemRenderer.renderOptionalDefaultValue(this.inputElement);
    //System.out.print(prompt + itemRenderer.renderValue(this.inputElement));
    //System.out.flush();
    List<Completer> completer = inputElement.getCompleter();
    Character mask = inputElement.getMask();
    ReaderIF.ReaderInput readerInput = reader.readLine(completer,prompt,inputElement.getValue(),mask);

    String lineInput = readerInput.getLineInput();

    if (lineInput == null || lineInput.trim().length() == 0) {
      lineInput = inputElement.getDefaultValue();
    }

    String result;
    if (mask == null) {
      result=lineInput;
    } else {
      result="";
      if (lineInput!=null) {
        for (int i=0; i<lineInput.length(); i++) {
          result+=mask;
        }
      }
    }

    renderMessagePromptAndResult(inputElement.getMessage(), result);

    return new InputResult(lineInput);
  }
}
