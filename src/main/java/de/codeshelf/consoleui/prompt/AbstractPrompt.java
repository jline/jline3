package de.codeshelf.consoleui.prompt;

import org.fusesource.jansi.Ansi;

import javax.swing.*;

import java.util.ResourceBundle;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public class AbstractPrompt {
  protected int renderHeight;
  protected ResourceBundle resourceBundle;

  protected void renderMessagePromptAndResult(String message, String resultValue) {

    System.out.println(ansi().cursorUp(renderHeight - 1).a(renderMessagePrompt(message)).fg(Ansi.Color.CYAN).a(" " + resultValue).eraseScreen(Ansi.Erase.FORWARD).reset());
  }

  protected String renderMessagePrompt(String message) {
    return (ansi().fg(Ansi.Color.GREEN).a("? ").fgBright(Ansi.Color.WHITE).a(message)).fg(Ansi.Color.DEFAULT).toString();
  }

  public AbstractPrompt() {
    resourceBundle= ResourceBundle.getBundle("consoleui");
  }
}
