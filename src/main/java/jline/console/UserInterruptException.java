package jline.console;

/**
 * This exception is thrown by {@link ConsoleReader#readLine} when
 * user interrupt handling is enabled and the user types the
 * interrupt character (ctrl-C). The partially entered line is
 * available via the {@link #getPartialLine()} method.
 */
public class UserInterruptException
    extends RuntimeException
{
    private final String partialLine;

    public UserInterruptException(String partialLine)
    {
        this.partialLine = partialLine;
    }

    /**
     * @return the partially entered line when ctrl-C was pressed
     */
    public String getPartialLine()
    {
        return partialLine;
    }
}
