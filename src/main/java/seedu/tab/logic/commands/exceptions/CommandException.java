package seedu.tab.logic.commands.exceptions;

/**
 * Represents an error which occurs during execution of a {@link seedu.tab.logic.commands.Command}.
 */
public class CommandException extends Exception {
    /**
     * Creates a command failure with an explanation for the user.
     */
    public CommandException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code CommandException} with the specified detail {@code message} and {@code cause}.
     */
    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
