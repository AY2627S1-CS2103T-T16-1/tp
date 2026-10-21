package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.tab.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.tab.logic.commands.HelpCommand.SHOWING_HELP_MESSAGE;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.parser.CliFlags;
import seedu.tab.logic.parser.Flag;
import seedu.tab.logic.parser.StudentBookParser;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;

public class HelpCommandTest {
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
        for (Flag flag : List.of(CliFlags.FLAG_NAME, CliFlags.FLAG_PHONE, CliFlags.FLAG_EMAIL,
                CliFlags.FLAG_NUS_STUDENT_ID, CliFlags.FLAG_TAG, CliFlags.FLAG_ADD_TAG, CliFlags.FLAG_REMOVE_TAG,
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
                .filter(line -> line.startsWith("delete INDEX") || line.startsWith("delete --id "))
                .toList();
        assertTrue(deleteSyntax.contains("delete INDEX"));
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
                .filter(line -> line.startsWith("tag INDEX ")).toList();
        assertTrue(tagSyntax.contains("tag INDEX -a TAG"));
        assertTrue(tagSyntax.contains("tag INDEX -r TAG"));
        for (String syntax : tagSyntax) {
            assertTrue(parser.parseCommand(syntax.replace("INDEX", "1").replace("TAG", "T1"))
                    instanceof TagCommand);
        }
    }
}
