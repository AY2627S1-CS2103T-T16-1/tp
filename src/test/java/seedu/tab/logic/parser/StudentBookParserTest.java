package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.Command;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.EditStudentDescriptorBuilder;
import seedu.tab.testutil.StudentBuilder;
import seedu.tab.testutil.StudentUtil;

public class StudentBookParserTest {

    /** One example of each command word, in the alphabetical order of {@code COMMAND_WORDS}. */
    private static final List<String> EXAMPLE_COMMANDS = List.of(
            "add John Doe -p 91234567",
            "clear",
            "delete 1",
            "edit 1 -p 91234567",
            "exit",
            "find -n John",
            "help",
            "list",
            "tag 1 -a Lab");

    private final StudentBookParser parser = new StudentBookParser();

    @Test
    public void parseCommand_add() throws Exception {
        Student student = new StudentBuilder().withNusStudentId("A01234567X").build();
        AddCommand command = (AddCommand) parser.parseCommand(StudentUtil.getAddCommand(student));
        assertEquals(new AddCommand(student), command);

        Student flaggedStudent = new StudentBuilder().withName("Flagged Student").withFlag(true).build();
        AddCommand flaggedCommand = (AddCommand) parser.parseCommand(StudentUtil.getAddCommand(flaggedStudent));
        assertEquals(new AddCommand(flaggedStudent), flaggedCommand);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
    }

    @Test
    public void parseCommand_noArgumentCommandsWithInput_throwsParseException() {
        for (String command : List.of("clear", "list", "help", "exit")) {
            for (String input : List.of("oops", "3", "\"oops\"", "\"\"", "-p", "\"unfinished", "O’Neil")) {
                assertThrows(ParseException.class,
                        String.format(Messages.MESSAGE_UNEXPECTED_ARGUMENTS, command, input), () ->
                        parser.parseCommand(command + " " + input));
            }
        }
    }

    @Test
    public void parseCommand_noArgumentCommandsWithWhitespace_success() throws Exception {
        for (String command : List.of("clear", "list", "help", "exit")) {
            Class<?> expectedType = parser.parseCommand(command).getClass();
            for (int code = Character.MIN_VALUE; code <= Character.MAX_VALUE; code++) {
                char separator = (char) code;
                if (StringUtil.isWhitespace(separator)) {
                    assertEquals(expectedType, parser.parseCommand(separator + command + separator).getClass());
                }
            }
        }
    }

    @Test
    public void parseCommand_longUnexpectedInput_shortensFeedback() {
        String input = "x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH + 1);
        String expected = String.format(Messages.MESSAGE_UNEXPECTED_ARGUMENTS,
                "clear", "x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH) + "…");
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("clear " + input));
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_STUDENT.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT), command);

        DeleteCommand whitespaceCommand = (DeleteCommand) parser.parseCommand(
                "  " + DeleteCommand.COMMAND_WORD + "   " + INDEX_FIRST_STUDENT.getOneBased() + "  ");
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT), whitespaceCommand);

        // a target opening with a letter reaches the parser as an NUS Student ID
        DeleteCommand idCommand = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " a01234567x");
        assertEquals(new DeleteCommand(StudentTarget.byNusStudentId(new NusStudentId("A01234567X"))),
                idCommand);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Student student = new StudentBuilder().withNusStudentId("A01234567X").build();
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(student).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_STUDENT.getOneBased() + " " + StudentUtil.getEditStudentDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_STUDENT, descriptor), command);

        EditStudentDescriptor clearNusStudentIdDescriptor = new EditStudentDescriptorBuilder()
                .withoutNusStudentId().build();
        EditCommand clearNusStudentIdCommand = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_STUDENT.getOneBased() + " "
                + StudentUtil.getEditStudentDescriptorDetails(clearNusStudentIdDescriptor));
        assertEquals(new EditCommand(INDEX_FIRST_STUDENT, clearNusStudentIdDescriptor), clearNusStudentIdCommand);

        for (boolean isFlagged : new boolean[] {true, false}) {
            EditStudentDescriptor flagDescriptor = new EditStudentDescriptorBuilder().withFlag(isFlagged).build();
            EditCommand flagCommand = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                    + INDEX_FIRST_STUDENT.getOneBased() + " "
                    + StudentUtil.getEditStudentDescriptorDetails(flagDescriptor));
            assertEquals(new EditCommand(INDEX_FIRST_STUDENT, flagDescriptor), flagCommand);
        }
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand("  " + ExitCommand.COMMAND_WORD + "  ") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        String arguments = keywords.stream().map(keyword -> "-n " + keyword).collect(Collectors.joining(" "));
        FindCommand command = (FindCommand) parser.parseCommand(FindCommand.COMMAND_WORD + " " + arguments);
        assertEquals(new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, keywords))), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
    }

    @Test
    public void parseCommand_everySeparator_givesTheSameCommandAsASpace() {
        // only add and edit read their arguments with the tokenizer, so testing those alone
        // hides the rest: the others look for an ordinary space and used to keep the separator
        // as their first character, turning "delete 1" into an invalid index and "find -n John"
        // into a search for a keyword nobody typed
        String[] commands = {"delete 1", "edit 1 -p 91234567", "find -n John", "add John Doe -p 91234567"};

        for (String command : commands) {
            int space = command.indexOf(' ');
            String word = command.substring(0, space);
            String rest = command.substring(space + 1);

            // a command the parser refuses even with an ordinary space would compare null with
            // null for every separator, so the loop below would pass while testing nothing
            Command baseline = parseOrNull(word + ' ' + rest);
            assertNotNull(baseline, command + " is refused with an ordinary space");

            for (int code = Character.MIN_VALUE; code <= Character.MAX_VALUE; code++) {
                char separator = (char) code;
                if (!StringUtil.isWhitespace(separator)) {
                    continue;
                }
                assertEquals(baseline, parseOrNull(word + separator + rest),
                        command + " differed on U+" + Integer.toHexString(code));
            }
        }
    }

    /** Returns the command {@code input} parses to, or null when it is refused. */
    private Command parseOrNull(String input) {
        try {
            return parser.parseCommand(input);
        } catch (ParseException e) {
            return null;
        }
    }

    @Test
    public void parseCommand_everyTrailingSeparator_givesTheSameCommandAsNoSeparator() {
        // a command pasted from a web page keeps the invisible separator the page ended it
        // with; in delete that separator used to travel into the index itself, where it was
        // reported as an index that is not a positive integer
        String[] commands = {"delete 1", "edit 1 -p 91234567", "find -n John", "add John Doe -p 91234567"};

        for (String command : commands) {
            Command baseline = parseOrNull(command);
            assertNotNull(baseline, command + " is refused without a trailing separator");

            for (int code = Character.MIN_VALUE; code <= Character.MAX_VALUE; code++) {
                char separator = (char) code;
                if (!StringUtil.isWhitespace(separator)) {
                    continue;
                }
                assertEquals(baseline, parseOrNull(command + separator),
                        command + " differed on a trailing U+" + Integer.toHexString(code));
            }
        }
    }

    @Test
    public void parseCommand_leadingSeparators_areIgnored() throws Exception {
        assertTrue(parser.parseCommand("\u00A0\u3000 " + ListCommand.COMMAND_WORD) instanceof ListCommand);
    }

    @Test
    public void parseCommand_onlySeparators_throwsParseException() {
        String expected = String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE);
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("\u00A0 \t\n"));
    }

    @Test
    public void parseCommand_invisibleFormatCharacters_areStrippedBeforeParsing() throws Exception {
        // a direction mark or a zero-width space pasted before the command word or the index
        // leaves a word that looks right but matches no command, and an index rejected as not
        // a positive integer; both are hidden from the user by ordinary text editing
        assertTrue(parser.parseCommand("\u200Blist") instanceof ListCommand);
        assertTrue(parser.parseCommand("\uFEFFlist") instanceof ListCommand);
        assertTrue(parser.parseCommand("\u200E" + DeleteCommand.COMMAND_WORD + " \u200E1")
                instanceof DeleteCommand);
    }

    @Test
    public void parseCommand_everyFormatCharacterAfterTheWordOrIndex_isStripped() {
        // the strip runs at the one place every command enters, so no format character in
        // either position may survive to hide the command word or the index, and a strip
        // that reached only part of the line would let one through
        Command baseline = parseOrNull(DeleteCommand.COMMAND_WORD + " 1");
        assertNotNull(baseline, "delete 1 is refused with an ordinary space");

        for (int code = Character.MIN_CODE_POINT; code <= Character.MAX_CODE_POINT; code++) {
            if (Character.getType(code) != Character.FORMAT || code == '\u200C' || code == '\u200D') {
                continue;
            }
            String character = new String(Character.toChars(code));
            String label = "U+" + Integer.toHexString(code);
            assertEquals(baseline, parseOrNull(DeleteCommand.COMMAND_WORD + character + " 1"),
                    "a format character after the command word differed on " + label);
            assertEquals(baseline, parseOrNull(DeleteCommand.COMMAND_WORD + " 1" + character),
                    "a format character after the index differed on " + label);
        }
    }

    @Test
    public void parseCommand_optionValueHoldingAFormatCharacter_isReadWithoutIt() throws Exception {
        // a mark pasted between an option and its value would otherwise land in the field
        // itself, where the stored value looks right to the user but fails validation (#308
        // reports this for email); both fields that were left unnormalized are pinned here
        Student student = new StudentBuilder().withName("Jane Tan").withPhone("90001111")
                .withEmail("jane@example.com").withNusStudentId("A01234567X").build();
        AddCommand command = (AddCommand) parser.parseCommand(
                "add Jane Tan -p 90001111 -e \u200Ejane@example.com -i \u200EA01234567X");
        assertEquals(new AddCommand(student), command);
    }

    @Test
    public void parseCommand_joinerFormatCharacters_areKept() {
        // Sinhala and Arabic need the joiners to shape a word, so a command word holding one
        // is a different word rather than the same word with a character removed
        assertThrows(ParseException.class, () -> parser.parseCommand("\u200Dlist"));
    }

    @Test
    public void parseCommand_argumentsHoldingALineBreak_areStillRead() throws Exception {
        // a command copied out of a document may arrive wrapped across lines
        assertTrue(parser.parseCommand(AddCommand.COMMAND_WORD + " John Doe\n-p 91234567") instanceof AddCommand);
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class,
                "Unknown command \"unknownCommand\". Enter help to see every command.", ()
                    -> parser.parseCommand("unknownCommand"));
    }

    @Test
    public void parseCommand_missingLetter_suggestsTheIntendedCommand() {
        assertThrows(ParseException.class, "Unknown command \"delte\". Did you mean delete?", ()
            -> parser.parseCommand("delte 3"));
        assertThrows(ParseException.class, "Unknown command \"fnd\". Did you mean find?", ()
            -> parser.parseCommand("fnd James"));
    }

    @Test
    public void parseCommand_extraLetter_suggestsTheIntendedCommand() {
        assertThrows(ParseException.class, "Unknown command \"liist\". Did you mean list?", ()
            -> parser.parseCommand("liist"));
        assertThrows(ParseException.class, "Unknown command \"addd\". Did you mean add?", ()
            -> parser.parseCommand("addd John Doe -p 91234567"));
    }

    @Test
    public void parseCommand_swappedLetters_suggestsTheIntendedCommand() {
        assertThrows(ParseException.class, "Unknown command \"lsit\". Did you mean list?", ()
            -> parser.parseCommand("lsit"));
        assertThrows(ParseException.class, "Unknown command \"hepl\". Did you mean help?", ()
            -> parser.parseCommand("hepl"));
    }

    @Test
    public void parseCommand_wrongLetter_suggestsTheIntendedCommand() {
        assertThrows(ParseException.class, "Unknown command \"clexr\". Did you mean clear?", ()
            -> parser.parseCommand("clexr"));
        assertThrows(ParseException.class, "Unknown command \"edut\". Did you mean edit?", ()
            -> parser.parseCommand("edut 1 -p 91234567"));
    }

    @Test
    public void parseCommand_mistypedInAnotherCase_namesTheWordAsTyped() {
        assertThrows(ParseException.class, "Unknown command \"Delte\". Did you mean delete?", ()
            -> parser.parseCommand("Delte 3"));
        assertThrows(ParseException.class, "Unknown command \"LSIT\". Did you mean list?", ()
            -> parser.parseCommand("LSIT"));
    }

    @Test
    public void parseCommand_commandWordInAnyCase_reachesTheSameCommand() throws Exception {
        // Caps Lock left on, or Shift held a moment longer than meant, spells a command word
        // TAB knows, and no two of them differ only in case
        assertEquals(CommandWords.COMMAND_WORDS,
                EXAMPLE_COMMANDS.stream().map(StudentBookParserTest::commandWordOf).toList(),
                "the examples below no longer cover every command word");

        for (String command : EXAMPLE_COMMANDS) {
            Class<?> expectedType = parser.parseCommand(command).getClass();
            for (String variant : List.of(withCapitalisedWord(command), withUpperCasedWord(command))) {
                assertEquals(expectedType, parser.parseCommand(variant).getClass(), variant);
            }
        }
    }

    @Test
    public void parseCommand_commandWordInAnotherCase_leavesTheArgumentsAsTyped() throws Exception {
        FindCommand find = (FindCommand) parser.parseCommand("FIND -n Alex");
        assertEquals(new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("Alex")))), find);
        assertNotEquals(new FindCommand(new DetailsContainsKeywordsPredicate(Map.of(Field.NAME, List.of("alex")))),
                find);

        Student student = new StudentBuilder().withName("John Doe").withNusStudentId("A01234567X").build();
        String typed = StudentUtil.getAddCommand(student);
        AddCommand command = (AddCommand) parser.parseCommand(
                "Add" + typed.substring(AddCommand.COMMAND_WORD.length()));
        assertEquals(new AddCommand(student), command);
    }

    @Test
    public void parseCommand_noArgumentCommandInAnotherCase_namesTheCommandItRefused() {
        String expected = String.format(Messages.MESSAGE_UNEXPECTED_ARGUMENTS, "list", "oops");
        assertThrows(ParseException.class, expected, () -> parser.parseCommand("LIST oops"));
    }

    /** Returns the command word {@code command} opens with. */
    private static String commandWordOf(String command) {
        return command.substring(0, endOfCommandWord(command));
    }

    /** Returns {@code command} with its command word capitalised. */
    private static String withCapitalisedWord(String command) {
        return Character.toUpperCase(command.charAt(0)) + command.substring(1);
    }

    /** Returns {@code command} with its command word in capitals. */
    private static String withUpperCasedWord(String command) {
        int wordEnd = endOfCommandWord(command);
        return command.substring(0, wordEnd).toUpperCase(Locale.ROOT) + command.substring(wordEnd);
    }

    private static int endOfCommandWord(String command) {
        int space = command.indexOf(' ');
        return space == -1 ? command.length() : space;
    }

    @Test
    public void parseCommand_equallyNearCommands_namesEveryOneOfThem() {
        // "eit" is one letter short of both edit and exit, and naming one of the two would
        // read as knowledge TAB does not have
        assertThrows(ParseException.class, "Unknown command \"eit\". Did you mean edit or exit?", ()
            -> parser.parseCommand("eit"));
        assertThrows(ParseException.class, "Unknown command \"e\". Did you mean edit or exit?", ()
            -> parser.parseCommand("e"));
    }

    @Test
    public void parseCommand_partTypedCommandWord_suggestsWhatItOpens() {
        assertThrows(ParseException.class, "Unknown command \"del\". Did you mean delete?", ()
            -> parser.parseCommand("del 3"));
        assertThrows(ParseException.class, "Unknown command \"cle\". Did you mean clear?", ()
            -> parser.parseCommand("cle"));
    }

    @Test
    public void parseCommand_mistypedFirstLetter_suggestsTheWordWhenNothingElseDiffers() {
        assertThrows(ParseException.class, "Unknown command \"felete\". Did you mean delete?", ()
            -> parser.parseCommand("felete 3"));
        assertThrows(ParseException.class, "Unknown command \"edlete\". Did you mean delete?", ()
            -> parser.parseCommand("edlete 3"));
    }

    @Test
    public void parseCommand_wordOpeningUnlikeEveryNearCommand_pointsToHelp() {
        // "dlt" is two edits from "edit" and nothing else is within reach, which says more
        // about the arithmetic than about what the User meant
        assertThrows(ParseException.class, Messages.getErrorMessageForUnknownCommand("dlt"), ()
            -> parser.parseCommand("dlt 3"));
    }

    @Test
    public void parseCommand_wordFarFromEveryCommand_pointsToHelp() {
        for (String input : List.of("qwerty", "remove 3", "?", "日本語", "удалить")) {
            String word = input.split(" ")[0];
            assertThrows(ParseException.class, Messages.getErrorMessageForUnknownCommand(word), ()
                -> parser.parseCommand(input));
        }
    }

    @Test
    public void parseCommand_pastedWordTooLongToRead_isShortenedInTheFeedback() {
        String typed = "x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH + 1);
        String expected = String.format(Messages.MESSAGE_UNKNOWN_COMMAND,
                "x".repeat(Messages.MAX_QUOTED_VALUE_LENGTH) + "…");
        assertThrows(ParseException.class, expected, () -> parser.parseCommand(typed + " and more"));
    }

    @Test
    public void parseCommand_everyKnownCommandWord_isDispatched() {
        // a word CommandWords offers as a suggestion has to be one the parser then accepts
        for (String commandWord : CommandWords.COMMAND_WORDS) {
            String message = messageFromParsing(commandWord);
            assertNotEquals(Messages.getErrorMessageForUnknownCommand(commandWord), message);
            assertNotEquals(Messages.getErrorMessageForMistypedCommand(commandWord, List.of(commandWord)), message);
        }
    }

    /** Returns why {@code input} is refused, or null when it is accepted. */
    private String messageFromParsing(String input) {
        try {
            parser.parseCommand(input);
            return null;
        } catch (ParseException e) {
            return e.getMessage();
        }
    }
}
