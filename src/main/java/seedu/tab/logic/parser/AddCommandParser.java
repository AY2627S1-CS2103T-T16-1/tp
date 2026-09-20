package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.tab.logic.parser.CliSyntax.PREFIX_TAG;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
     * settled first: a value before the first prefix, or a single-valued prefix given twice,
     * leaves it unclear which field a value belongs to, so either is refused on its own.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public AddCommand parse(String args) throws ParseException {
        ArgumentMultimap argMultimap =
                ArgumentTokenizer.tokenize(args, PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL, PREFIX_TAG);

        if (!argMultimap.getPreamble().isEmpty()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE));
        }

        argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_NAME, PREFIX_PHONE, PREFIX_EMAIL);

        ParseProblems problems = new ParseProblems();

        Prefix[] missingPrefixes = findMissingPrefixes(argMultimap,
                PREFIX_NAME, PREFIX_PHONE);
        if (missingPrefixes.length > 0) {
            problems.add(Messages.getErrorMessageForMissingPrefixes(missingPrefixes));
        }

        Name name = parseIfPresent(problems, argMultimap, PREFIX_NAME, ParserUtil::parseName);
        Phone phone = parseIfPresent(problems, argMultimap, PREFIX_PHONE, ParserUtil::parsePhone);
        Email email = parseIfPresent(problems, argMultimap, PREFIX_EMAIL, ParserUtil::parseEmail);
        Set<Tag> tagList = parseEachTag(problems, argMultimap.getAllValues(PREFIX_TAG));

        problems.throwIfAny();

        Student student = new Student(name, phone, email, tagList);

        return new AddCommand(student);
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
     * Parses the value supplied for {@code prefix}, recording why if it is rejected. A prefix
     * the command left out yields null without a complaint, because an optional field is
     * entitled to be absent and a required one is reported separately.
     */
    private static <T> T parseIfPresent(ParseProblems problems, ArgumentMultimap argMultimap,
            Prefix prefix, ParseProblems.ValueParser<T> parser) {
        Optional<String> value = argMultimap.getValue(prefix);
        if (value.isEmpty()) {
            return null;
        }
        return problems.collect(() -> parser.parse(value.get()));
    }

    /**
     * Returns the prefixes the command left out, in the order they are written in the format,
     * so that a user who omitted several fields is told about all of them at once.
     */
    private static Prefix[] findMissingPrefixes(ArgumentMultimap argumentMultimap, Prefix... prefixes) {
        return Stream.of(prefixes)
                .filter(prefix -> argumentMultimap.getValue(prefix).isEmpty())
                .toArray(Prefix[]::new);
    }

}
