package de.codeshelf.consoleui.prompt.reader;

import jline.console.ConsoleReader;
import jline.console.Operation;
import jline.console.completer.Completer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: Andreas Wegmann
 * Date: 02.01.16
 */
public class ConsoleReaderImpl implements ReaderIF {
  ConsoleReader console;

  private Set<SpecialKey> allowedSpecialKeys;
  private Set<Character> allowedPrintableKeys;

  public ConsoleReaderImpl() throws IOException {
    allowedPrintableKeys = new HashSet<Character>();
    allowedSpecialKeys = new HashSet<SpecialKey>();

    console = new ConsoleReader();
  }

  public void setAllowedSpecialKeys(Set<SpecialKey> allowedSpecialKeys) {
    this.allowedSpecialKeys.clear();
    this.allowedSpecialKeys.addAll(allowedSpecialKeys);
  }

  public void setAllowedPrintableKeys(Set<Character> allowedPrintableKeys) {
    this.allowedPrintableKeys.clear();
    this.allowedPrintableKeys.addAll(allowedPrintableKeys);
  }

  public void addAllowedPrintableKey(Character character) {
    this.allowedPrintableKeys.add(character);
  }

  public void addAllowedSpecialKey(SpecialKey specialKey) {
    this.allowedSpecialKeys.add(specialKey);
  }

  public ReaderInput read() {
    Object op;
    while (true) {
      try {
        op = console.readBinding(console.getKeys());
        if (op instanceof Operation) {
          Operation operation = (Operation) op;
          if (operation == Operation.NEXT_HISTORY && this.allowedSpecialKeys.contains(SpecialKey.DOWN))
            return new ReaderInput(SpecialKey.DOWN);
          if (operation == Operation.PREVIOUS_HISTORY && this.allowedSpecialKeys.contains(SpecialKey.UP))
            return new ReaderInput(SpecialKey.UP);
          if (operation == Operation.ACCEPT_LINE && this.allowedSpecialKeys.contains(SpecialKey.ENTER))
            return new ReaderInput(SpecialKey.ENTER);
          if (operation == Operation.BACKWARD_CHAR && this.allowedSpecialKeys.contains(SpecialKey.BACKSPACE))
            return new ReaderInput(SpecialKey.BACKSPACE);

          if (operation == Operation.SELF_INSERT) {
            String lastBinding = console.getLastBinding();
            Character c = lastBinding.charAt(0);
            if (allowedPrintableKeys.contains(c)) {
              return new ReaderInput(SpecialKey.PRINTABLE_KEY, c);
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Wrapper around JLine 2 library.
   *
   * @param completer
   * @param prompt
   * @return
   */
  public ReaderInput readLine(List<Completer> completer, String prompt, String value) throws IOException {
    if (completer != null) {
      for (Completer c : completer) {
        console.addCompleter(c);
      }
    }
    String readLine = console.readLine(prompt, null, value);

    return new ReaderInput(SpecialKey.PRINTABLE_KEY, readLine);
  }
}
