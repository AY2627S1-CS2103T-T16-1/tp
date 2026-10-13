package seedu.tab.logic.parser;

import java.util.List;

/**
 * Contains the Command Line Interface (CLI) flags that mark the fields of a command.
 */
public class CliFlags {

    /* Flag definitions */
    public static final Flag FLAG_NAME = new Flag("-n", List.of("--name"), "NAME");
    public static final Flag FLAG_PHONE = new Flag("-p", List.of("--phone"), "PHONE");
    public static final Flag FLAG_EMAIL = new Flag("-e", List.of("--email"), "EMAIL");
    public static final Flag FLAG_TAG = new Flag("-t", List.of("--tag"), "TAG");
    public static final Flag FLAG_FOLLOW_UP = new Flag("--follow-up", List.of("-f"));

    private CliFlags() {} // this class only holds flags
}
