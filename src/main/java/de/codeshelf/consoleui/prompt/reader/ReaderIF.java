package de.codeshelf.consoleui.prompt.reader;

import jline.console.completer.Completer;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * User: Andreas Wegmann
 * Date: 02.01.16
 */
public interface ReaderIF {

  public enum SpecialKey {
	NONE,
    UP,
    DOWN,
    ENTER,
    BACKSPACE,
    PRINTABLE_KEY,  // not really a special key, but indicates an ordianry printable key
  }

  void setAllowedSpecialKeys(Set<SpecialKey> allowedSpecialKeys);

  void setAllowedPrintableKeys(Set<Character> allowedPrintableKeys);

  void addAllowedPrintableKey(Character character);

  void addAllowedSpecialKey(SpecialKey specialKey);

  ReaderInput read();

  ReaderInput readLine(List<Completer> completer, String promt, String value, Character mask) throws IOException;

  class ReaderInput {
    private SpecialKey specialKey;
    private Character printableKey;
    private String lineInput;

    public ReaderInput(SpecialKey specialKey) {
      this.specialKey = specialKey;
    }

    public ReaderInput(SpecialKey specialKey, Character printableKey) {
      this.specialKey = specialKey;
      this.printableKey = printableKey;
    }

    public ReaderInput(SpecialKey specialKey, String lineInput) {
      this.specialKey = specialKey;
      this.lineInput = lineInput;
    }

    public SpecialKey getSpecialKey() {
      return specialKey;
    }

    public Character getPrintableKey() {
      return printableKey;
    }

    public String getLineInput() {
      return lineInput;
    }
  }
}
