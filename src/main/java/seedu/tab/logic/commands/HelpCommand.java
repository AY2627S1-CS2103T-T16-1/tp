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
            "TAB COMMAND REFERENCE",
            "",
            "COMMANDS",
            "",
            "  " + AddCommand.COMMAND_WORD + " — Add a student",
            "    " + AddCommand.COMMAND_WORD + " " + AddCommand.FIELD_NAME + " " + FLAG_PHONE.getLabel(),
            "        [" + FLAG_EMAIL.getLabel() + "] [" + FLAG_NUS_STUDENT_ID.getLabel() + "]",
            "        [" + FLAG_TAG.getLabel() + "]... [" + FLAG_FOLLOW_UP.getLabel() + "]",
            "",
            "  " + EditCommand.COMMAND_WORD + " — Edit a student",
            "    " + EditCommand.COMMAND_WORD + " " + EditCommand.FIELD_INDEX
                    + " [" + FLAG_NAME.getLabel() + "] [" + FLAG_PHONE.getLabel() + "]",
            "         [" + FLAG_EMAIL.getLabel() + "] [" + FLAG_NUS_STUDENT_ID.getLabel() + "]",
            "         [" + FLAG_TAG.getLabel() + "]...",
            "         [" + FLAG_FOLLOW_UP.getLabel() + " | " + FLAG_NO_FOLLOW_UP.getLabel() + "]",
            "    Provide at least one option.",
            "",
            "  " + TagCommand.COMMAND_WORD + " — Add a tag, keeping existing tags",
            "    " + TagCommand.COMMAND_WORD + " " + TagCommand.FIELD_INDEX + " " + FLAG_ADD_TAG.getLabel(),
            "",
            "  " + FindCommand.COMMAND_WORD + " — Find students by any partial keyword",
            "    " + FindCommand.COMMAND_WORD + " KEYWORD [MORE_KEYWORDS]...",
            "    Matches ignore case.",
            "",
            "  " + ListCommand.COMMAND_WORD + " — List all students",
            "    " + ListCommand.COMMAND_WORD,
            "",
            "  " + DeleteCommand.COMMAND_WORD + " — Delete a displayed student",
            "    " + DeleteCommand.COMMAND_WORD + " INDEX",
            "",
            "  " + ClearCommand.COMMAND_WORD + " — Delete all students",
            "    " + ClearCommand.COMMAND_WORD,
            "    Asks for confirmation (see below).",
            "",
            "  " + COMMAND_WORD + " — Show this reference",
            "    " + COMMAND_WORD,
            "    Also available through F1 or the Help menu.",
            "",
            "  " + ExitCommand.COMMAND_WORD + " — Exit TAB",
            "    " + ExitCommand.COMMAND_WORD,
            "",
            "READING THE SYNTAX",
            "",
            "  [ ]       Optional argument",
            "  ...       Repeatable argument",
            "  INDEX     Positive number in the displayed student list",
            "  Wrapped syntax lines form one command.",
            "",
            "OPTION ALIASES",
            "",
            "  Either spelling is accepted:",
            Stream.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                    FLAG_TAG, FLAG_ADD_TAG, FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP)
                    .map(flag -> "    " + flag.getMarkerSummary()).collect(Collectors.joining("\n")),
            "",
            "INPUT RULES",
            "",
            "  Commands and option spellings are case-sensitive.",
            "  help, list, clear and exit take no arguments.",
            "",
            "  Quote option values that contain spaces or start with a hyphen.",
            "  Example: add Siti Nur-Aisyah -p \"+65 9123 4567\" -t \"Lab 3\"",
            "",
            "EDITING OPTIONAL FIELDS",
            "",
            "  -e \"\"      Clear email",
            "  --id \"\"    Clear NUS Student ID",
            "  -t \"\"      Clear all tags",
            "",
            "  Otherwise, edit -t replaces existing tags.",
            "  Follow-up options take no value and cannot be combined.",
            "",
            "CLEAR CONFIRMATION",
            "",
            "  y / yes       Confirm",
            "  n / no        Cancel",
            "  Enter         Cancel",
            "",
            "  Replies ignore case. Other replies keep confirmation pending.",
            "",
            "MORE HELP",
            "",
            "  Up/Down in the command box recalls previous commands.",
            "  Quit this pager with q or Escape to enter your next command.",
            "",
            "  User Guide:",
            "  " + USER_GUIDE_URL);

    @Override
    public CommandResult execute(Model model) {
        return new CommandResult(SHOWING_HELP_MESSAGE, true, false);
    }
}
