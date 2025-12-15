import java.io.IOException;
import java.util.Collections;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;

public class MinimalStatusBarBug {
   public MinimalStatusBarBug() {
   }

   public static void main(String[] var0) throws IOException {
      Terminal var1 = TerminalBuilder.builder().system(true).build();
      LineReader var2 = LineReaderBuilder.builder().terminal(var1).appName("MinimalTest").build();
      Status var3 = Status.getStatus(var1, true);
      var3.update(Collections.singletonList(new AttributedString("Status Bar - Resize the terminal vertically to see the bug")));
      System.out.println("=== Minimal JLine3 Status Bar Bug Reproduction ===");
      System.out.println();
      System.out.println("Bug reproduction steps:");
      System.out.println("1. You should see a status bar at the bottom");
      System.out.println("2. Resize the terminal vertically (make it taller or shorter)");
      System.out.println("3. BUG: The old status bar stays at its old position");
      System.out.println("4. Press Enter - the old status bar disappears");
      System.out.println();
      System.out.println("Type 'quit' to exit");
      System.out.println();

      while(true) {
         String var4 = var2.readLine("> ");
         if (var4 == null || "quit".equalsIgnoreCase(var4.trim())) {
            var3.update(Collections.emptyList());
            var1.close();
            return;
         }

         System.out.println("Input: " + var4);
      }
   }
}