package seedu.tab.logic.parser;

/**
 * Contains the Command Line Interface (CLI) flags that mark the fields of a command.
 */
public class CliFlags {

    /* Flag definitions */
    public static final Flag FLAG_PHONE = new Flag("-p", "PHONE");
    public static final Flag FLAG_EMAIL = new Flag("-e", "EMAIL");
    public static final Flag FLAG_TAG = new Flag("-t", "TAG");
    public static final Flag FLAG_FOLLOW_UP = new Flag("-f");

    private CliFlags() {} // this class only holds flags
}
