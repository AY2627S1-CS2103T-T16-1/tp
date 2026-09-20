package seedu.tab.logic.parser;

/**
 * Contains Command Line Interface (CLI) syntax definitions common to multiple commands
 */
public class CliSyntax {

    /* Prefix definitions */
    public static final Prefix PREFIX_NAME = new Prefix("n/", "NAME");
    public static final Prefix PREFIX_PHONE = new Prefix("p/", "PHONE");
    public static final Prefix PREFIX_EMAIL = new Prefix("e/", "EMAIL");
    public static final Prefix PREFIX_ADDRESS = new Prefix("a/", "ADDRESS");
    public static final Prefix PREFIX_TAG = new Prefix("t/", "TAG");

}
