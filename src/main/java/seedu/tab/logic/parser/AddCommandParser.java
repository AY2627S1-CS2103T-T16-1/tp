package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.Optional;
import java.util.Set;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.logic.parser.exceptions.UnknownFlagException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
import seedu.tab.model.student.NusStudentId;
import seedu.tab.model.student.Phone;
import seedu.tab.model.student.Student;
import seedu.tab.model.tag.Tag;

/**
 * Parses input arguments and creates a new AddCommand object
 */
public class AddCommandParser implements Parser<AddCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the AddCommand
     * and returns an AddCommand object for execution.
     *
     * Once the shape of the command is settled, every field is examined before the command is
     * refused, so that a user who mistyped two of them is told about both at once. The shape is
     * settled first: an unclosed quote, an unknown flag, or a single-valued flag given twice
     * leaves it unclear which field a value belongs to, so any of them is refused on its own.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public AddCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = tokenize(args);
        arguments.verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_FOLLOW_UP);

        ParseProblems problems = new ParseProblems();

        if (arguments.getPreamble().isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(AddCommand.FIELD_NAME));
            problems.add(AddCommand.MESSAGE_NAME_GUIDANCE);
        }

        Name name = parseName(problems, arguments.getPreamble());
        Optional<Phone> phone = Optional.ofNullable(
                problems.collectIfPresent(arguments.getValue(FLAG_PHONE), ParserUtil::parsePhone));
        Optional<Email> email = Optional.ofNullable(
                problems.collectIfPresent(arguments.getValue(FLAG_EMAIL), ParserUtil::parseEmail));
        Optional<NusStudentId> nusStudentId = Optional.ofNullable(
                problems.collectIfPresent(arguments.getValue(FLAG_NUS_STUDENT_ID), ParserUtil::parseNusStudentId));
        Set<Tag> tags = ParserUtil.parseTags(arguments.getAllValues(FLAG_TAG), problems);

        problems.throwIfAny();

        boolean isFlagged = arguments.getValue(FLAG_FOLLOW_UP).isPresent();
        Student student = new Student(name, phone, email, nusStudentId, tags, isFlagged);

        return new AddCommand(student);
    }

    /** Adds placement guidance when an edit-style name option is refused. */
    private static FlagArgumentMap tokenize(String args) throws ParseException {
        try {
            return FlagTokenizer.tokenize(
                    args, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_TAG, FLAG_FOLLOW_UP);
        } catch (UnknownFlagException e) {
            if (FLAG_NAME.getMarkers().contains(e.getMarker())) {
                throw new ParseException(e.getMessage() + "\n" + AddCommand.MESSAGE_NAME_GUIDANCE, e);
            }
            throw e;
        }
    }

    /**
     * Parses the name given before the first flag, recording why if it is rejected. A name that
     * was left out yields null without a second complaint, since it is reported as missing.
     */
    private static Name parseName(ParseProblems problems, String name) {
        if (name.isEmpty()) {
            return null;
        }
        return problems.collect(() -> ParserUtil.parseName(name));
    }

}
