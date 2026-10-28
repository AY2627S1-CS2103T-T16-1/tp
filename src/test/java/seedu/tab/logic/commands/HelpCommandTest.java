package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.HelpCommand.SHOWING_HELP_MESSAGE;
import static seedu.tab.testutil.TypicalStudents.getTypicalStudentBook;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.exceptions.CommandException;
import seedu.tab.logic.parser.CliFlag;
import seedu.tab.logic.parser.CliFlags;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Student;

public class HelpCommandTest {

    private static final String OPTIONAL_FIELD_HEADING = "EDITING OPTIONAL FIELDS";

    /**
     * How to tell whether a student still holds the detail each {@code edit} option sets. A
     * student always has a name, which is the one detail the command cannot take away.
     */
    private static final Map<CliFlag, Predicate<Student>> DETAILS_BY_OPTION = Map.of(
            CliFlags.FLAG_NAME, student -> true,
            CliFlags.FLAG_PHONE, student -> student.getPhone().isPresent(),
            CliFlags.FLAG_EMAIL, student -> student.getEmail().isPresent(),
            CliFlags.FLAG_NUS_STUDENT_ID, student -> student.getNusStudentId().isPresent(),
            CliFlags.FLAG_TAG, student -> !student.getTags().isEmpty());

    private Model model = new ModelManager();
    private Model expectedModel = new ModelManager();

    @Test
    public void execute_help_success() {
        CommandResult expectedCommandResult = new CommandResult(SHOWING_HELP_MESSAGE, true, false);
        assertCommandSuccess(new HelpCommand(), model, expectedCommandResult, expectedModel);
    }

    @Test
    public void summary_supportedCommandsAndOptions_coversEverySpelling() throws Exception {
        StudentBookParser parser = new StudentBookParser();
        for (String example : List.of("add Alex -p 91234567", "edit 1 -n Alex", "tag 1 -a T1",
                "tag 1 -r T1", "tag 1 --remove T1", "find -n Alex", "list", "delete 1", "clear", "help", "exit")) {
            parser.parseCommand(example);
            assertTrue(SHOWING_HELP_MESSAGE.lines().map(String::stripLeading).anyMatch(line ->
                    line.startsWith(example.split(" ")[0] + " —")
                            || line.startsWith(example.split(" ")[0] + " INDEX —")));
        }
        for (CliFlag flag : List.of(CliFlags.FLAG_NAME, CliFlags.FLAG_PHONE, CliFlags.FLAG_EMAIL,
                CliFlags.FLAG_NUS_STUDENT_ID, CliFlags.FLAG_TARGET_ID, CliFlags.FLAG_TAG,
                CliFlags.FLAG_ADD_TAG, CliFlags.FLAG_REMOVE_TAG,
                CliFlags.FLAG_FOLLOW_UP, CliFlags.FLAG_NO_FOLLOW_UP)) {
            assertTrue(SHOWING_HELP_MESSAGE.contains(flag.getMarkerSummary()));
        }
        String example = SHOWING_HELP_MESSAGE.lines().map(String::stripLeading)
                .filter(line -> line.startsWith("Example: "))
                .findFirst().orElseThrow().substring("Example: ".length());
        parser.parseCommand(example);
    }

    @Test
    public void summary_deleteSyntax_parsesAsDeleteCommands() throws Exception {
        StudentBookParser parser = new StudentBookParser();
        List<String> deleteSyntax = SHOWING_HELP_MESSAGE.lines().map(String::stripLeading)
                .filter(line -> line.startsWith("delete INDEX") || line.startsWith("delete NUS_STUDENT_ID")
                        || line.startsWith("delete --id "))
                .toList();
        assertTrue(deleteSyntax.contains("delete INDEX"));
        assertTrue(deleteSyntax.contains("delete NUS_STUDENT_ID"));
        assertTrue(deleteSyntax.contains("delete --id NUS_STUDENT_ID"));
        for (String syntax : deleteSyntax) {
            assertTrue(parser.parseCommand(syntax.replace("INDEX", "1")
                    .replace("NUS_STUDENT_ID", "A0123456X")) instanceof DeleteCommand);
        }
    }

    @Test
    public void summary_tagActionSyntax_parsesAsTagCommands() throws Exception {
        StudentBookParser parser = new StudentBookParser();
        List<String> tagSyntax = SHOWING_HELP_MESSAGE.lines().map(String::stripLeading)
                .filter(line -> line.startsWith("tag INDEX ") || line.startsWith("tag NUS_STUDENT_ID ")
                        || line.startsWith("tag --id ")).toList();
        assertTrue(tagSyntax.contains("tag INDEX -a TAG"));
        assertTrue(tagSyntax.contains("tag INDEX -r TAG"));
        assertTrue(tagSyntax.contains("tag NUS_STUDENT_ID -a TAG"));
        assertTrue(tagSyntax.contains("tag NUS_STUDENT_ID -r TAG"));
        assertTrue(tagSyntax.contains("tag --id NUS_STUDENT_ID -a TAG"));
        assertTrue(tagSyntax.contains("tag --id NUS_STUDENT_ID -r TAG"));
        for (String syntax : tagSyntax) {
            assertTrue(parser.parseCommand(syntax.replace("INDEX", "1").replace("NUS_STUDENT_ID", "A01234567X")
                    .replace("TAG", "T1"))
                    instanceof TagCommand);
        }
    }

    @Test
    public void summary_optionalFieldBlock_namesEveryDetailEditCanClear() {
        for (Map.Entry<CliFlag, Predicate<Student>> option : DETAILS_BY_OPTION.entrySet()) {
            String clear = option.getKey() + " \"\"";
            assertEquals(clearsDetail(clear, option.getValue()), isListedAsClearing(clear),
                    OPTIONAL_FIELD_HEADING + " must name every option that empties a detail: " + clear);
        }
    }

    /** Returns true if {@code edit 1 CLEAR} leaves the first typical student without the detail. */
    private static boolean clearsDetail(String clear, Predicate<Student> hasDetail) {
        Model editedModel = new ModelManager(getTypicalStudentBook(), new UserPrefs());
        assertTrue(hasDetail.test(editedModel.getFilteredStudentList().get(0)),
                "the first typical student must hold the detail " + clear + " would empty");
        try {
            new StudentBookParser().parseCommand("edit 1 " + clear).execute(editedModel);
        } catch (ParseException | CommandException e) {
            return false;
        }
        return !hasDetail.test(editedModel.getFilteredStudentList().get(0));
    }

    /** Returns true if the optional-field block opens one of its lines with {@code clear}. */
    private static boolean isListedAsClearing(String clear) {
        return SHOWING_HELP_MESSAGE.lines()
                .dropWhile(line -> !line.equals(OPTIONAL_FIELD_HEADING))
                .skip(1)
                .takeWhile(line -> line.isBlank() || line.startsWith(" "))
                .map(String::stripLeading)
                .anyMatch(line -> line.startsWith(clear + " "));
    }
}
