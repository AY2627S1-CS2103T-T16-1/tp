package seedu.tab.logic.parser;

import java.util.List;

/**
 * Contains the Command Line Interface (CLI) flags that mark the fields of a command.
 */
public class CliFlags {

    /* CliFlag definitions */
    public static final CliFlag FLAG_NAME = new CliFlag("-n", List.of("--name"), "NAME");
    public static final CliFlag FLAG_PHONE = new CliFlag("-p", List.of("--phone"), "PHONE");
    public static final CliFlag FLAG_EMAIL = new CliFlag("-e", List.of("--email"), "EMAIL");
    public static final CliFlag FLAG_NUS_STUDENT_ID = new CliFlag("--id", List.of("-i"), "NUS_STUDENT_ID");
    public static final CliFlag FLAG_TAG = new CliFlag("-t", List.of("--tag"), "TAG");
    public static final CliFlag FLAG_ADD_TAG = new CliFlag("-a", List.of("--add"), "TAG");
    public static final CliFlag FLAG_REMOVE_TAG = new CliFlag("-r", List.of("--remove"), "TAG");
    public static final CliFlag FLAG_FOLLOW_UP = new CliFlag("--follow-up", List.of("-f"));
    public static final CliFlag FLAG_NO_FOLLOW_UP = new CliFlag("--no-follow-up", List.of("-nf"));

    private CliFlags() {} // this class only holds flags
}
