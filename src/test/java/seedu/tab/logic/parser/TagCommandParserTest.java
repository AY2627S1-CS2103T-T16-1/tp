package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.tab.logic.parser.CommandParserTestUtil.assertParseSuccess;
import static seedu.tab.testutil.TypicalIndexes.INDEX_FIRST_STUDENT;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.commands.TagCommand.Action;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.tag.Tag;

public class TagCommandParserTest {

    private final TagCommandParser parser = new TagCommandParser();

    @Test
    public void parse_oversizedIndex_explainsBound() {
        for (String index : List.of("2147483648", "999999999999999999999")) {
            for (String action : List.of("-a", "-r", "--remove")) {
                assertParseFailure(parser, index + " " + action + " T1",
                        String.format(ParserUtil.MESSAGE_INDEX_TOO_LARGE, index));
            }
        }
    }

    @Test
    public void parse_studentIdTargets_acceptsPlainAndExplicitForms() {
        for (String id : List.of("abc", "A01234567X", "a01234567x", "Z".repeat(32))) {
            StudentTarget target = StudentTarget.byNusStudentId(new NusStudentId(id));
            assertParseSuccess(parser, "\u00a0" + id + " -a \"Lab 3\"",
                    new TagCommand(target, new Tag("Lab 3"), Action.ADD));
            assertParseSuccess(parser, "\"  " + id + "  \" -a T1",
                    new TagCommand(target, new Tag("T1"), Action.ADD));
            assertParseSuccess(parser, id + " --remove=T1", new TagCommand(target, new Tag("T1"), Action.REMOVE));
        }
        for (String marker : List.of("--id", "-i")) {
            for (String id : List.of("A01234567X", "1abc", "1234", "0", "9".repeat(32))) {
                StudentTarget target = StudentTarget.byNusStudentId(new NusStudentId(id));
                assertParseSuccess(parser, marker + "=\"  " + id + "  \" --add=\"组 1\"",
                        new TagCommand(target, new Tag("组 1"), Action.ADD));
                assertParseSuccess(parser, "--remove T1 " + marker + " " + id,
                        new TagCommand(target, new Tag("T1"), Action.REMOVE));
            }
        }
    }

    @Test
    public void parse_invalidStudentId_reportsValueAndReason() {
        for (String id : List.of("", " ", "A-1", "王", "a".repeat(33))) {
            assertParseFailure(parser, "--id \"" + id + "\" -a T1",
                    Messages.getErrorMessageForInvalidValue(NusStudentId.FIELD_NAME, id.trim(),
                            NusStudentId.getFailureReason(id.trim())));
        }
        assertFailureWithGuidance("--id -a T1", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE,
                "--id NUS_STUDENT_ID"));
    }

    @Test
    public void parse_multipleStudentTargets_rejected() {
        assertParseFailure(parser, "1 --id A01234567X -a T1", TagCommand.MESSAGE_MULTIPLE_TARGETS);
        assertParseFailure(parser, "A01234567X -i A01234567X -r T1", TagCommand.MESSAGE_MULTIPLE_TARGETS);
        assertParseFailure(parser, "--id A01234567X -i B01234567X -a T1",
                Messages.getErrorMessageForDuplicateFlags("--id"));
    }

    @Test
    public void parse_validValues_success() {
        for (String marker : List.of("-a", "--add")) {
            for (String value : List.of("T1", "Lab 3", "组 1", "CS2103T (AY26/27)", "-Lab",
                    "-a", "--add", "x".repeat(1000))) {
                assertParseSuccess(parser, " 1 " + marker + " \"  " + value + "  \" ",
                        new TagCommand(INDEX_FIRST_STUDENT, new Tag(value)));
            }
        }
        assertParseSuccess(parser, "\u00a01\t--add \"Lab\u3000 3\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab 3")));
        assertParseSuccess(parser, "1 -a \"Lab \\\"A\\\"\\\\B\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab \"A\"\\B")));
    }

    @Test
    public void parse_missingFields_reportsEachField() {
        assertFailureWithGuidance("", "Missing required field(s): INDEX or NUS_STUDENT_ID\n"
                + "Missing required field(s): -a TAG or -r TAG");
        assertFailureWithGuidance("1", "Missing required field(s): -a TAG or -r TAG");
        assertParseFailure(parser, "-a T1", "Missing required field(s): INDEX or NUS_STUDENT_ID");
        assertParseFailure(parser, "\"\" -a T1", "Missing required field(s): INDEX or NUS_STUDENT_ID");
    }

    @Test
    public void parse_invalidIndex_reportsIndex() {
        for (String index : List.of("0", "1.5")) {
            assertParseFailure(parser, index + " -a T1", ParserUtil.MESSAGE_INVALID_INDEX);
        }
        assertParseFailure(parser, "\"-1\" -a T1", ParserUtil.MESSAGE_INVALID_INDEX);
        assertParseSuccess(parser, "2147483647 -a T1",
                new TagCommand(Index.fromOneBased(Integer.MAX_VALUE), new Tag("T1")));
    }

    @Test
    public void parse_invalidTag_reportsValueAndReason() {
        for (String value : List.of("", "   ", "\u00a0\u3000", "---", "🙂")) {
            assertParseFailure(parser, "1 -a \"" + value + "\"",
                    Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, value.trim(),
                            "it holds no letter or number"));
        }
        assertParseFailure(parser, "0 -a \"\"",
                ParserUtil.MESSAGE_INVALID_INDEX + "\nTag \"\" is not valid: it holds no letter or number");
    }

    @Test
    public void parse_mistypedOptions_reportsSpecificProblem() {
        for (String marker : List.of("-a", "--add")) {
            assertFailureWithGuidance("1 " + marker,
                    marker + " TAG needs a value after it. A value opening with a hyphen goes in double quotes.");
            for (String repeatedMarker : List.of("-a", "--add")) {
                assertParseFailure(parser, "1 " + marker + " T1 " + repeatedMarker + " T2",
                        Messages.getErrorMessageForDuplicateFlags(marker));
            }
        }
        assertFailureWithGuidance("1 -a -p 123", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-a TAG"));
        assertFailureWithGuidance("1 --adds T1", "There is no --adds option.");
        assertFailureWithGuidance("1 -a \"Lab 3", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
    }

    @Test
    public void parse_nonAddOptions_rejected() {
        for (String marker : List.of("-t", "--tag", "-A", "--ADD")) {
            assertFailureWithGuidance("1 " + marker + " T1",
                    String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker));
        }
    }

    @Test
    public void parse_negativeIndex_reportsIndexAndOptionWithGuidance() {
        for (String index : List.of("-1", "-0", "-1.5", "-2147483649", "-999999999999999999999")) {
            for (String marker : List.of("-a", "--add")) {
                assertFailureWithGuidance(index + " " + marker + " T1", ParserUtil.MESSAGE_INVALID_INDEX
                        + "\n" + String.format(Messages.MESSAGE_UNKNOWN_FLAG, index));
            }
        }
    }

    @Test
    public void parse_unflaggedTag_reportsUnexpectedValueAndActionGuidance() {
        for (String value : List.of("lab4", "Lab 3", "组 1")) {
            assertFailureWithGuidance("1 " + value, String.format(ParserUtil.MESSAGE_VALUE_AFTER_INDEX,
                    "INDEX", value) + "\nMissing required field(s): -a TAG or -r TAG");
        }
        assertParseFailure(parser, "1 extra -a T1",
                String.format(ParserUtil.MESSAGE_VALUE_AFTER_INDEX, "INDEX", "extra"));
    }

    @Test
    public void parse_tagActionOptionsInOtherCommands_rejected() {
        for (String marker : List.of("-a", "--add", "-r", "--remove")) {
            String expected = String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker);
            assertParseFailure(new AddCommandParser(), "Alice -p 91234567 " + marker + " T1", expected);
            assertParseFailure(new EditCommandParser(), "1 " + marker + " T1", expected);
        }
    }

    @Test
    public void parse_removeValidValues_success() {
        for (String marker : List.of("-r", "--remove")) {
            for (String value : List.of("T1", "Lab 3", "组 1", "CS2103T (AY26/27)", "-Lab",
                    "-r", "--remove", "x".repeat(1000))) {
                assertParseSuccess(parser, "\u00a01\t" + marker + " \"  " + value + "  \" ",
                        new TagCommand(INDEX_FIRST_STUDENT, new Tag(value), Action.REMOVE));
            }
        }
        assertParseSuccess(parser, "1 --remove \"Lab\u3000 3\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab 3"), Action.REMOVE));
        assertParseSuccess(parser, "1 -r \"Lab \\\"A\\\"\\\\B\"",
                new TagCommand(INDEX_FIRST_STUDENT, new Tag("Lab \"A\"\\B"), Action.REMOVE));
    }

    @Test
    public void parse_removeInvalidIndexOrTag_reportsSpecificProblems() {
        for (String marker : List.of("-r", "--remove")) {
            for (String index : List.of("0", "1.5")) {
                assertParseFailure(parser, index + " " + marker + " T1", ParserUtil.MESSAGE_INVALID_INDEX);
            }
            assertFailureWithGuidance("-1 " + marker + " T1", ParserUtil.MESSAGE_INVALID_INDEX
                    + "\n" + String.format(Messages.MESSAGE_UNKNOWN_FLAG, "-1"));
            assertParseSuccess(parser, "2147483647 " + marker + " T1",
                    new TagCommand(Index.fromOneBased(Integer.MAX_VALUE), new Tag("T1"), Action.REMOVE));
            assertParseFailure(parser, marker + " T1", "Missing required field(s): INDEX or NUS_STUDENT_ID");
            for (String value : List.of("", "   ", "\u00a0\u3000", "---", "🙂")) {
                assertParseFailure(parser, "1 " + marker + " \"" + value + "\"",
                        Messages.getErrorMessageForInvalidValue(Tag.FIELD_NAME, value.trim(),
                                "it holds no letter or number"));
            }
        }
        assertParseFailure(parser, "0 -r \"\"",
                ParserUtil.MESSAGE_INVALID_INDEX + "\nTag \"\" is not valid: it holds no letter or number");
    }

    @Test
    public void parse_removeMistypedOptions_reportsSpecificProblem() {
        for (String marker : List.of("-r", "--remove")) {
            assertFailureWithGuidance("1 " + marker, String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE,
                    marker + " TAG"));
            for (String repeatedMarker : List.of("-r", "--remove")) {
                assertParseFailure(parser, "1 " + marker + " T1 " + repeatedMarker + " T2",
                        Messages.getErrorMessageForDuplicateFlags(marker));
            }
        }
        assertFailureWithGuidance("1 -r -p 123", String.format(Messages.MESSAGE_FLAG_WITHOUT_VALUE, "-r TAG"));
        assertFailureWithGuidance("1 --removes T1", "There is no --removes option.");
        assertFailureWithGuidance("1 -r Lab 3", Messages.getErrorMessageForValueAfterFlags("3", true));
        assertFailureWithGuidance("1 -r \"Lab 3", CommandTokenizer.MESSAGE_UNCLOSED_QUOTE);
        for (String marker : List.of("-R", "--REMOVE")) {
            assertFailureWithGuidance("1 " + marker + " T1", String.format(Messages.MESSAGE_UNKNOWN_FLAG, marker));
        }
    }

    @Test
    public void parse_bothActions_rejectedInEitherOrder() {
        for (String add : List.of("-a", "--add")) {
            for (String remove : List.of("-r", "--remove")) {
                assertParseFailure(parser, "1 " + add + " T1 " + remove + " T2",
                        "Options -a/--add and -r/--remove cannot be used together.");
                assertParseFailure(parser, "1 " + remove + " T2 " + add + " T1",
                        "Options -a/--add and -r/--remove cannot be used together.");
            }
        }
    }

    @Test
    public void parse_twoWordTagAfterAnIndex_pointsToQuotesRatherThanTheFront() {
        // the index already stands before the options, so only the quotes advice can be followed
        assertFailureWithGuidance("1 -a Lab 3", Messages.getErrorMessageForValueAfterFlags("3", true));
        assertParseSuccess(parser, "1 -a \"Lab 3\"",
                new TagCommand(StudentTarget.byIndex(INDEX_FIRST_STUDENT, "1"), new Tag("Lab 3"), Action.ADD));
    }

    @Test
    public void parse_indexAfterTheOptions_pointsToTheFrontOfTheCommand() {
        assertFailureWithGuidance("-a Lab 1", Messages.getErrorMessageForValueAfterFlags("1", false));
        assertParseSuccess(parser, "1 -a Lab",
                new TagCommand(StudentTarget.byIndex(INDEX_FIRST_STUDENT, "1"), new Tag("Lab"), Action.ADD));
    }

    private void assertFailureWithGuidance(String input, String problem) {
        assertParseFailure(parser, input, problem + "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE);
    }
}
