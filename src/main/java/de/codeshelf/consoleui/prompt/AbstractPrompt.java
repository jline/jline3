package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Abstract base class for all prompt implementations.
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public abstract class AbstractPrompt {
  protected int renderHeight;
  protected ResourceBundle resourceBundle;

  // the reader where we get the user input from
  ReaderIF reader;

  /**
   * Generic method to render the message prompt and the users input after the prompt. This method is
   * used by all prompt implementations to display the question and result after the user has made
   * the input.
   *
   * @param message     message to render as colored prompt.
   * @param resultValue result value generated from the prompt implementation
   */
  protected void renderMessagePromptAndResult(String message, String resultValue) {
    System.out.println(ansi().cursorUp(renderHeight - 1).a(renderMessagePrompt(message)).fg(Ansi.Color.CYAN).a(" " + resultValue).eraseScreen(Ansi.Erase.FORWARD).reset());
  }

  /**
   * Generic method to render a message prompt. The message (displayed white) is prefixed by a
   * green question mark.
   *
   * @param message message to render as a colored prompt.
   * @return String with ANSI-Color printable prompt.
   */
  protected String renderMessagePrompt(String message) {
    return (ansi().fg(Ansi.Color.GREEN).a("? ").fgBright(Ansi.Color.WHITE).a(message)).fg(Ansi.Color.DEFAULT).toString();
  }

  /**
   * Default constructor. Initializes the resource bundle for localized messages.
   *
   * @throws IOException may be thrown from console reader
   */
  public AbstractPrompt() throws IOException {
    resourceBundle = ResourceBundle.getBundle("consoleui_messages");
    this.reader = new ConsoleReaderImpl();
  }

  /**
   * Setter for the reader implementation. Usually the prompt implementation uses the default
   * {@link ConsoleReaderImpl} initialized in the constructor.
   * This methods is mainly inteded for JUnit tests to inject a new reader for simulated uses input.
   *
   * @param reader reader implementation to use.
   */
  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }
}
