package seedu.tab.logic.parser;

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
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Checks that the commands printed in the User Guide are ones the parser accepts. A guide that
 * hands out a command the app refuses is a bug whichever of the two is wrong.
 */
public class UserGuideExamplesTest {

    private static final Path USER_GUIDE = Paths.get("docs", "UserGuide.md");

    /** The placeholders a format line holds, which mark it as something other than an example. */
    private static final List<String> PLACEHOLDERS =
            List.of("INDEX", "NAME", "PHONE_NUMBER", "EMAIL", "TAG", "KEYWORD");

    /** Enough that the guide has stopped showing a command if it drops below. */
    private static final int MINIMUM_EXAMPLES = 5;

    @Test
    public void userGuide_everyAddExample_parses() throws IOException {
        assertEveryExampleParses(AddCommand.COMMAND_WORD, new AddCommandParser());
    }

    @Test
    public void userGuide_everyEditExample_parses() throws IOException {
        assertEveryExampleParses(EditCommand.COMMAND_WORD, new EditCommandParser());
    }

    /**
     * Fails if the guide shows fewer than {@code MINIMUM_EXAMPLES} worked examples of
     * {@code commandWord}, or shows one that {@code parser} refuses.
     */
    private static void assertEveryExampleParses(String commandWord, Parser<?> parser) throws IOException {
        List<String> examples = findExamples(commandWord);

        // a guide that stops showing the command at all would otherwise pass silently
        assertTrue(examples.size() >= MINIMUM_EXAMPLES,
                "the User Guide shows too few " + commandWord + " examples");

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
     * backticks, on a bullet or in the command summary, that is not a format line.
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
