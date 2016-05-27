package de.codeshelf.consoleui.prompt.reader;

import jline.console.ConsoleReader;
import jline.console.Operation;
import jline.console.completer.Completer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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

    StringBuilder sb = new StringBuilder();
    Stack<Character> pushBackChar = new Stack<Character>();
    try {
      while (true) {
        int c = pushBackChar.isEmpty() ? console.readCharacter() : pushBackChar.pop ();
        if (c == -1) {
            return null;
        }
        sb.appendCodePoint(c);

        op = console.getKeys().getBound( sb );
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
            String lastBinding = sb.toString();
            Character cc = lastBinding.charAt(0);
            if (allowedPrintableKeys.contains(cc)) {
              return new ReaderInput(SpecialKey.PRINTABLE_KEY, cc);
            } else {
              sb = new StringBuilder();
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
    	System.out.println("!!!!!!!!!!!!!! UPDTED");
    	console.shutdown();
    }
    return null;
  }

  /**
   * Wrapper around JLine 2 library.
   *
   * @param completer List of completes to use
   * @param prompt the text to display as prompt left side from the input
   * @param mask optional mask character (may be used for password entry)
   * @return a ReaderInput object with results
   */
  public ReaderInput readLine(List<Completer> completer, String prompt, String value, Character mask) throws IOException {
    if (completer != null) {
      for (Completer c : completer) {
        console.addCompleter(c);
      }
    }
    String readLine;
    if (mask == null) {
      readLine = console.readLine(prompt);
    } else {
      readLine = console.readLine(prompt, mask);
    }


    return new ReaderInput(SpecialKey.PRINTABLE_KEY, readLine);
  }
}
