package seedu.tab.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import seedu.tab.logic.parser.exceptions.ParseException;

/**
 * Checks that the add commands printed in the User Guide are ones the parser accepts. A guide
 * that hands out a command the app refuses is a bug whichever of the two is wrong.
 */
public class UserGuideExamplesTest {

    private static final Path USER_GUIDE = Paths.get("docs", "UserGuide.md");

    /** An `add ...` inside backticks, on a bullet or in the command summary. */
    private static final Pattern ADD_EXAMPLE = Pattern.compile("`(add [^`]+)`");

    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void userGuide_everyAddExample_parses() throws IOException {
        List<String> examples = findAddExamples();

        // a guide that stops showing the command at all would otherwise pass silently
        assertEquals(true, examples.size() >= 5, "the User Guide shows no add examples");

        for (String example : examples) {
            String arguments = example.substring(AddCommand.COMMAND_WORD.length());
            if (arguments.contains("NAME") || arguments.contains("TAG")) {
                continue; // a format line, not a worked example
            }
            try {
                parser.parse(arguments);
            } catch (ParseException e) {
                throw new AssertionError("the User Guide shows `" + example
                        + "`, which the parser refuses: " + e.getMessage(), e);
            }
        }
    }

    private static List<String> findAddExamples() throws IOException {
        String guide = Files.readString(USER_GUIDE, StandardCharsets.UTF_8);
        Matcher matcher = ADD_EXAMPLE.matcher(guide);
        return matcher.results().map(result -> result.group(1).trim()).toList();
    }
}
