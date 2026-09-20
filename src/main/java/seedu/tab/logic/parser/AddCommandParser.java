package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.AddCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.Email;
import seedu.tab.model.student.Name;
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
     * @throws ParseException if the user input does not conform to the expected format
     */
    public AddCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = FlagTokenizer.tokenize(args, FLAG_PHONE, FLAG_EMAIL, FLAG_TAG);
        arguments.verifyNoDuplicateFlagsFor(FLAG_PHONE, FLAG_EMAIL);

        ParseProblems problems = new ParseProblems();

        String[] missingFields = findMissingFields(arguments);
        if (missingFields.length > 0) {
            problems.add(Messages.getErrorMessageForMissingFields(missingFields));
        }

        Name name = parseName(problems, arguments.getName());
        Phone phone = parseIfPresent(problems, arguments, FLAG_PHONE, ParserUtil::parsePhone);
        Email email = parseIfPresent(problems, arguments, FLAG_EMAIL, ParserUtil::parseEmail);
        Set<Tag> tagList = parseEachTag(problems, arguments.getAllValues(FLAG_TAG));

        problems.throwIfAny();

        Student student = new Student(name, phone, email, tagList);

        return new AddCommand(student);
    }

    /**
     * Returns the required fields the command left out, in the order they are written in the
     * format, so that a user who omitted several is told about all of them at once.
     */
    private static String[] findMissingFields(FlagArgumentMap arguments) {
        List<String> missing = new ArrayList<>();
        if (arguments.getName().isEmpty()) {
            missing.add(CliFlags.FIELD_NAME);
        }
        if (arguments.getValue(FLAG_PHONE).isEmpty()) {
            missing.add(FLAG_PHONE.getLabel());
        }
        return missing.toArray(String[]::new);
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

    /**
     * Parses every tag supplied, recording why for each one that is rejected, and returns the
     * ones that were accepted.
     */
    private static Set<Tag> parseEachTag(ParseProblems problems, Collection<String> tags) {
        Set<Tag> parsed = new HashSet<>();
        for (String tag : tags) {
            Tag parsedTag = problems.collect(() -> ParserUtil.parseTag(tag));
            if (parsedTag != null) {
                parsed.add(parsedTag);
            }
        }
        return parsed;
    }

    /**
     * Parses the value supplied for {@code flag}, recording why if it is rejected. A flag the
     * command left out yields null without a complaint, because an optional field is entitled
     * to be absent and a required one is reported separately.
     */
    private static <T> T parseIfPresent(ParseProblems problems, FlagArgumentMap arguments,
            Flag flag, ParseProblems.ValueParser<T> parser) {
        Optional<String> value = arguments.getValue(flag);
        if (value.isEmpty()) {
            return null;
        }
        return problems.collect(() -> parser.parse(value.get()));
    }

}
