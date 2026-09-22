package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.tab.testutil.Assert.assertThrows;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.ClearCommand;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.commands.ExitCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.HelpCommand;
import seedu.tab.logic.commands.ListCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.Student;
import seedu.tab.testutil.EditStudentDescriptorBuilder;
import seedu.tab.testutil.StudentBuilder;
import seedu.tab.testutil.StudentUtil;

public class StudentBookParserTest {

    private final StudentBookParser parser = new StudentBookParser();

    @Test
    public void parseCommand_add() throws Exception {
        Student student = new StudentBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(StudentUtil.getAddCommand(student));
        assertEquals(new AddCommand(student), command);

        Student flaggedStudent = new StudentBuilder().withName("Flagged Student").withFlag(true).build();
        AddCommand flaggedCommand = (AddCommand) parser.parseCommand(StudentUtil.getAddCommand(flaggedStudent));
        assertEquals(new AddCommand(flaggedStudent), flaggedCommand);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        DeleteCommand command = (DeleteCommand) parser.parseCommand(
                DeleteCommand.COMMAND_WORD + " " + INDEX_FIRST_STUDENT.getOneBased());
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT), command);

        DeleteCommand whitespaceCommand = (DeleteCommand) parser.parseCommand(
                "  " + DeleteCommand.COMMAND_WORD + "   " + INDEX_FIRST_STUDENT.getOneBased() + "  ");
        assertEquals(new DeleteCommand(INDEX_FIRST_STUDENT), whitespaceCommand);
    }

    @Test
    public void parseCommand_edit() throws Exception {
        Student student = new StudentBuilder().build();
        EditStudentDescriptor descriptor = new EditStudentDescriptorBuilder(student).build();
        EditCommand command = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_STUDENT.getOneBased() + " " + StudentUtil.getEditStudentDescriptorDetails(descriptor));
        assertEquals(new EditCommand(INDEX_FIRST_STUDENT, descriptor), command);

        EditStudentDescriptor toggleDescriptor = new EditStudentDescriptorBuilder()
                .withFlagToggled(true).build();
        EditCommand toggleCommand = (EditCommand) parser.parseCommand(EditCommand.COMMAND_WORD + " "
                + INDEX_FIRST_STUDENT.getOneBased() + " "
                + StudentUtil.getEditStudentDescriptorDetails(toggleDescriptor));
        assertEquals(new EditCommand(INDEX_FIRST_STUDENT, toggleDescriptor), toggleCommand);
    }

    @Test
    public void parseCommand_exit() throws Exception {
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD) instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " 3") instanceof ExitCommand);
        assertTrue(parser.parseCommand("  " + ExitCommand.COMMAND_WORD + "  ") instanceof ExitCommand);
        assertTrue(parser.parseCommand(ExitCommand.COMMAND_WORD + " extra arguments") instanceof ExitCommand);
    }

    @Test
    public void parseCommand_find() throws Exception {
        List<String> keywords = List.of("foo", "bar", "baz");
        FindCommand command = (FindCommand) parser.parseCommand(
                FindCommand.COMMAND_WORD + " " + keywords.stream().collect(Collectors.joining(" ")));
        assertEquals(new FindCommand(new DetailsContainsKeywordsPredicate(keywords)), command);
    }

    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD + " 3") instanceof ListCommand);
    }

    @Test
    public void parseCommand_unrecognisedInput_throwsParseException() {
        assertThrows(ParseException.class, String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE), ()
            -> parser.parseCommand(""));
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
