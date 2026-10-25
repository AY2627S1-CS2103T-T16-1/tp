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
    public static final Flag FLAG_NUS_STUDENT_ID = new Flag("--id", List.of("-i"), "NUS_STUDENT_ID");
    public static final Flag FLAG_TARGET_ID = new Flag("--target-id", "NUS_STUDENT_ID");
    public static final Flag FLAG_TAG = new Flag("-t", List.of("--tag"), "TAG");
    public static final Flag FLAG_ADD_TAG = new Flag("-a", List.of("--add"), "TAG");
    public static final Flag FLAG_REMOVE_TAG = new Flag("-r", List.of("--remove"), "TAG");
    public static final Flag FLAG_FOLLOW_UP = new Flag("--follow-up", List.of("-f"));
    public static final Flag FLAG_NO_FOLLOW_UP = new Flag("--no-follow-up", List.of("-nf"));

    private CliFlags() {} // this class only holds flags
}
