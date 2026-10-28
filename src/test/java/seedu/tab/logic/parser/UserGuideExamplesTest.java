package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.commands.DeleteCommand;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.Model;
import seedu.tab.model.ModelManager;
import seedu.tab.model.UserPrefs;
import seedu.tab.model.student.Student;
import seedu.tab.model.util.SampleDataUtil;
import seedu.tab.testutil.StudentBuilder;

/**
 * Checks that the commands printed in the User Guide are ones the parser accepts. A guide that
 * hands out a command the app refuses is a bug whichever of the two is wrong.
 */
public class UserGuideExamplesTest {

    private static final Path USER_GUIDE = Paths.get("docs", "UserGuide.md");

    /** The placeholders a format line holds, which mark it as something other than an example. */
    private static final List<String> PLACEHOLDERS =
            List.of("INDEX", "TARGET", "NAME", "PHONE_NUMBER", "EMAIL", "NUS_STUDENT_ID", "TAG", "KEYWORD",
                    "FIELD_OPTION");

    @Test
    public void userGuide_noArgumentCommands_parse() throws Exception {
        String guide = Files.readString(USER_GUIDE, StandardCharsets.UTF_8);
        for (String command : List.of("help", "list", "clear", "exit", "LIST")) {
            assertTrue(guide.contains("`" + command + "`"), "Missing documented command: " + command);
            new StudentBookParser().parseCommand(command);
        }
    }

    @Test
    public void userGuide_everyAddExample_parses() throws IOException {
        assertEveryExampleParses(AddCommand.COMMAND_WORD, new AddCommandParser());
    }

    @Test
    public void userGuide_everyDeleteExample_parses() throws IOException {
        assertEveryExampleParses(DeleteCommand.COMMAND_WORD, new DeleteCommandParser());
    }

    @Test
    public void userGuide_everyEditExample_parses() throws IOException {
        assertEveryExampleParses(EditCommand.COMMAND_WORD, new EditCommandParser());
    }

    @Test
    public void userGuide_everyFindExample_parses() throws IOException {
        assertEveryExampleParses(FindCommand.COMMAND_WORD, new FindCommandParser());
    }

    @Test
    public void userGuide_everyTagExample_parses() throws IOException {
        assertEveryExampleParses(TagCommand.COMMAND_WORD, new TagCommandParser());
    }

    @Test
    public void userGuide_numericIdExample_updatesTheStudentCreatedByItsSetup() throws Exception {
        Model model = new ModelManager(SampleDataUtil.getSampleStudentBook(), new UserPrefs());
        executeDocumentedExamples(model, "add Numeric Example --id 1234",
                "edit --target-id 1234 -p 91234567", "edit --target-id 1234 --id E7654321");

        Student expected = new StudentBuilder().withName("Numeric Example").withPhone("91234567")
                .withoutEmail().withNusStudentId("E7654321").build();
        assertEquals(expected, model.getStudentBook().getStudentList().getLast());
    }

    @Test
    public void userGuide_tagExamples_preserveExistingTags() throws Exception {
        Model model = new ModelManager(SampleDataUtil.getSampleStudentBook(), new UserPrefs());
        Student original = model.getFilteredStudentList().getFirst();
        executeDocumentedExamples(model, "tag 1 -a \"Lab 3\"");
        Student expected = new StudentBuilder(original).withTags("friends", "Lab 3").build();
        assertEquals(expected, model.getFilteredStudentList().getFirst());

        executeDocumentedExamples(model, "tag 1 -r \"lab 3\"");
        assertEquals(original, model.getFilteredStudentList().getFirst());
    }

    @Test
    public void userGuide_deleteExample_usesTheFilteredIndex() throws Exception {
        Model model = new ModelManager(SampleDataUtil.getSampleStudentBook(), new UserPrefs());
        executeDocumentedExamples(model, "find -n Charlotte");
        assertEquals(1, model.getFilteredStudentList().size());
        Student target = model.getFilteredStudentList().getFirst();

        executeDocumentedExamples(model, "delete 1");
        assertTrue(model.getFilteredStudentList().isEmpty());
        assertFalse(model.getStudentBook().getStudentList().contains(target));
    }

    /** Executes a worked scenario only when its commands are still present in the guide. */
    private static void executeDocumentedExamples(Model model, String... commands) throws Exception {
        String guide = Files.readString(USER_GUIDE, StandardCharsets.UTF_8);
        for (String command : commands) {
            assertTrue(guide.contains("`" + command + "`"), "Missing documented command: " + command);
            new StudentBookParser().parseCommand(command).execute(model);
        }
    }

    /**
     * Fails if the guide has no worked example of {@code commandWord}, or shows one
     * that {@code parser} refuses.
     */
    private static void assertEveryExampleParses(String commandWord, Parser<?> parser) throws IOException {
        List<String> examples = findExamples(commandWord);

        // a guide that stops showing the command at all would otherwise pass silently
        assertFalse(examples.isEmpty(), "the User Guide has no " + commandWord + " examples");

        for (String example : examples) {
            String arguments = example.substring(commandWord.length());
            try {
                parser.parse(arguments);
            } catch (ParseException e) {
                throw new AssertionError("the User Guide shows `" + example
                        + "`, which the parser refuses: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the worked examples of {@code commandWord} in the guide: every occurrence inside
     * backticks that is not a format line.
     */
    private static List<String> findExamples(String commandWord) throws IOException {
        String guide = Files.readString(USER_GUIDE, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("`(" + commandWord + " [^`]+)`").matcher(guide);
        return matcher.results()
                .map(result -> result.group(1).trim())
                .filter(example -> PLACEHOLDERS.stream().noneMatch(example::contains))
                .toList();
    }
}
