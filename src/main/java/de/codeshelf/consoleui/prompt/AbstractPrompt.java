package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public abstract class AbstractPrompt {
  protected int renderHeight;
  protected ResourceBundle resourceBundle;
  ReaderIF reader;

  protected void renderMessagePromptAndResult(String message, String resultValue) {

    System.out.println(ansi().cursorUp(renderHeight - 1).a(renderMessagePrompt(message)).fg(Ansi.Color.CYAN).a(" " + resultValue).eraseScreen(Ansi.Erase.FORWARD).reset());
  }

  protected String renderMessagePrompt(String message) {
    return (ansi().fg(Ansi.Color.GREEN).a("? ").fgBright(Ansi.Color.WHITE).a(message)).fg(Ansi.Color.DEFAULT).toString();
  }

  public AbstractPrompt() throws IOException {
    resourceBundle = ResourceBundle.getBundle("consoleui_messages");
    this.reader = new ConsoleReaderImpl();
  }

  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }
}
