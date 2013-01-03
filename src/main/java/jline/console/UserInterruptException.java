package jline.console;

/**
 * This exception is thrown by {@link ConsoleReader#readLine} when
 * user interrupt handling is enabled and the user types the
 * interrupt character (ctrl-C).
 */
public class UserInterruptException
    extends RuntimeException
{
}
