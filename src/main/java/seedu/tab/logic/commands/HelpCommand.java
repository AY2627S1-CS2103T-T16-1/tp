package seedu.tab.logic.commands;

import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.tab.model.Model;

/**
 * Formats a command summary for display inside the main window.
 */
public class HelpCommand extends Command {

    public static final String COMMAND_WORD = "help";
    public static final String USER_GUIDE_URL = "https://ay2627s1-cs2103t-t16-1.github.io/tp/UserGuide.html";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows the command summary in the main window.\n"
            + "Example: " + COMMAND_WORD;

    public static final String SHOWING_HELP_MESSAGE = String.join("\n",
            "Command summary — F6 to read/scroll; Escape to return to the command box.",
            "[ ] = optional; ... = repeatable. INDEX is a positive number in the displayed list.",
            "",
            AddCommand.COMMAND_WORD + " — Add a student",
            "  " + AddCommand.COMMAND_WORD + " " + AddCommand.FIELD_NAME + " " + FLAG_PHONE.getLabel()
                    + " [" + FLAG_EMAIL.getLabel() + "] [" + FLAG_NUS_STUDENT_ID.getLabel() + "]"
                    + " [" + FLAG_TAG.getLabel() + "]... [" + FLAG_FOLLOW_UP.getLabel() + "]",
            EditCommand.COMMAND_WORD + " — Edit a student (provide at least one option)",
            "  " + EditCommand.COMMAND_WORD + " " + EditCommand.FIELD_INDEX
                    + " [" + FLAG_NAME.getLabel() + "] [" + FLAG_PHONE.getLabel() + "]"
                    + " [" + FLAG_EMAIL.getLabel() + "] [" + FLAG_NUS_STUDENT_ID.getLabel() + "]"
                    + " [" + FLAG_TAG.getLabel() + "]..."
                    + " [" + FLAG_FOLLOW_UP.getLabel() + " | " + FLAG_NO_FOLLOW_UP.getLabel() + "]",
            TagCommand.COMMAND_WORD + " — Add a tag, keeping existing tags",
            "  " + TagCommand.COMMAND_WORD + " " + TagCommand.FIELD_INDEX + " " + FLAG_ADD_TAG.getLabel(),
            FindCommand.COMMAND_WORD + " — Find students by any partial keyword, ignoring case",
            "  " + FindCommand.COMMAND_WORD + " KEYWORD [MORE_KEYWORDS]...",
            ListCommand.COMMAND_WORD + " — List all students",
            DeleteCommand.COMMAND_WORD + " INDEX — Delete a displayed student",
            ClearCommand.COMMAND_WORD + " — Delete all students (asks for confirmation)",
            COMMAND_WORD + " — Show this summary (also F1 or Help menu)",
            ExitCommand.COMMAND_WORD + " — Exit TAB",
            "",
            "Options (either spelling): " + Stream.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL,
                    FLAG_NUS_STUDENT_ID, FLAG_TAG, FLAG_ADD_TAG, FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP)
                    .map(flag -> flag.getMarkerSummary()).collect(Collectors.joining(", ")),
            "Put option values containing spaces or starting with a hyphen in double quotes.",
            "Example: add Siti Nur-Aisyah -p \"+65 9123 4567\" -t \"Lab 3\"",
            "In edit, -e \"\" clears email; --id \"\" clears ID; -t \"\" clears all tags.",
            "Otherwise edit -t replaces tags. Follow-up options take no value and cannot be combined.",
            "Commands and option spellings are case-sensitive. help, list, clear and exit take no arguments.",
            "For clear confirmation, y/yes confirms; n/no or Enter cancels (case-insensitive).",
            "Other replies keep clear confirmation pending. Use Up/Down in the command box to recall commands.",
            "In the summary use arrows or Page Up/Down.",
            "User Guide: " + USER_GUIDE_URL);

    @Override
    public CommandResult execute(Model model) {
        return new CommandResult(SHOWING_HELP_MESSAGE, true, false);
    }
}
