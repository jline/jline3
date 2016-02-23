package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;

import java.io.IOException;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public class ConfirmPrompt extends AbstractPrompt implements PromptIF<ConfirmChoice,ConfirmResult> {

  private ReaderIF reader;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();
  private ConfirmChoice confirmChoice;
  char yes_key;
  char no_key;
  String yes_answer;
  String no_answer;
  ConfirmChoice.ConfirmationValue givenAnswer;

  public ConfirmPrompt() throws IOException {
    super();
    yes_key = resourceBundle.getString("confirmation_yes_key").trim().charAt(0);
    no_key = resourceBundle.getString("confirmation_no_key").trim().charAt(0);
    yes_answer = resourceBundle.getString("confirmation_yes_answer");
    no_answer = resourceBundle.getString("confirmation_no_answer");
  }


  public ConfirmResult prompt(ConfirmChoice confirmChoice) throws IOException {
    givenAnswer = null;
    this.confirmChoice = confirmChoice;

    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }

    if (renderHeight == 0) {
      renderHeight = 1;
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    this.reader.addAllowedPrintableKey(no_key);
    this.reader.addAllowedPrintableKey(yes_key);
    this.reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);
    this.reader.addAllowedSpecialKey(ReaderIF.SpecialKey.BACKSPACE);

    render();
    ReaderIF.ReaderInput readerInput = this.reader.read();
    while (true) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.ENTER) {
        if (givenAnswer != null) {
          break;
        } else if (confirmChoice.getDefaultConfirmation() != null) {
          givenAnswer = confirmChoice.getDefaultConfirmation();
          break;
        }
      }
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        if (readerInput.getPrintableKey().equals(yes_key)) {
          givenAnswer = ConfirmChoice.ConfirmationValue.YES;
        } else if (readerInput.getPrintableKey().equals(no_key)) {
          givenAnswer = ConfirmChoice.ConfirmationValue.NO;
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.BACKSPACE) {
        givenAnswer = null;
      }
      render();
      readerInput = this.reader.read();
    }
    String resultValue = calcResultValue();

    System.out.println();
    renderMessagePromptAndResult(confirmChoice.getMessage(), resultValue);

    return new ConfirmResult(givenAnswer);
  }

  private void render() {
    System.out.println("");
    System.out.println(ansi().eraseLine().cursorUp(2));
    System.out.print(renderMessagePrompt(this.confirmChoice.getMessage()) +
            itemRenderer.renderConfirmChoiceOptions(this.confirmChoice) + " " + ansi().reset().a(calcResultValue()+" ").eraseLine());
    System.out.flush();
    renderHeight = 1;
  }

  private String calcResultValue() {
    if (givenAnswer == ConfirmChoice.ConfirmationValue.YES) {
      return yes_answer;
    } else if (givenAnswer == ConfirmChoice.ConfirmationValue.NO) {
      return no_answer;
    }
    return "";
  }
}
