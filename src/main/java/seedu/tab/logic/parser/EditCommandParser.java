package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.tag.Tag;

/**
 * Parses input arguments and creates a new EditCommand object
 */
public class EditCommandParser implements Parser<EditCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the EditCommand
     * and returns an EditCommand object for execution.
     *
     * The command is read the way {@code add} reads its own: once the shape is settled, every
     * field is examined before the command is refused, so that a user who mistyped the index
     * and a field is told about both at once. The shape is settled first, because an unclosed
     * quote, an unknown option, or a single-valued option given twice leaves it unclear which
     * field a value belongs to.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public EditCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = FlagTokenizer.tokenize(
                args, FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_TAG, FLAG_FOLLOW_UP);
        arguments.verifyNoDuplicateFlagsFor(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_FOLLOW_UP);

        ParseProblems problems = new ParseProblems();

        Index index = parseIndex(problems, arguments.getPreamble());
        if (!hasAnyField(arguments)) {
            problems.add(EditCommand.MESSAGE_NOT_EDITED);
        }
        EditStudentDescriptor descriptor = describeEdit(problems, arguments);

        problems.throwIfAny();

        return new EditCommand(index, descriptor);
    }

    /**
     * Returns true if the command named a field to change. A field whose value is rejected still
     * counts as named, so that a user who mistyped one is told what is wrong with it rather than
     * that they named no field.
     */
    private static boolean hasAnyField(FlagArgumentMap arguments) {
        return Stream.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_TAG, FLAG_FOLLOW_UP)
                .anyMatch(flag -> !arguments.getAllValues(flag).isEmpty());
    }

    /**
     * Parses the index given before the first option, recording why if it is rejected. An index
     * that was left out is reported as a missing field rather than as a malformed one.
     */
    private static Index parseIndex(ParseProblems problems, String index) {
        if (index.isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(EditCommand.FIELD_INDEX));
            return null;
        }
        return problems.collect(() -> ParserUtil.parseIndex(index));
    }

    /**
     * Returns the fields the command asks to change, recording why for each value rejected.
     */
    private static EditStudentDescriptor describeEdit(ParseProblems problems, FlagArgumentMap arguments) {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();

        descriptor.setName(problems.collectIfPresent(arguments.getValue(FLAG_NAME), ParserUtil::parseName));
        descriptor.setPhone(problems.collectIfPresent(arguments.getValue(FLAG_PHONE), ParserUtil::parsePhone));
        descriptor.setEmail(problems.collectIfPresent(arguments.getValue(FLAG_EMAIL), ParserUtil::parseEmail));
        parseTagsForEdit(problems, arguments.getAllValues(FLAG_TAG)).ifPresent(descriptor::setTags);
        descriptor.setFlagToggled(arguments.getValue(FLAG_FOLLOW_UP).isPresent());

        return descriptor;
    }

    /**
     * Parses the tags the command supplied into the set that replaces the student's own, or
     * returns nothing when the command leaves the tags alone. A single empty value, which is
     * how {@code -t ""} is spelled, asks for every tag to be removed.
     */
    private static Optional<Set<Tag>> parseTagsForEdit(ParseProblems problems, List<String> tags) {
        if (tags.isEmpty()) {
            return Optional.empty();
        }
        if (tags.size() == 1 && tags.get(0).isEmpty()) {
            return Optional.of(Set.of());
        }
        return Optional.of(problems.collectEach(tags, ParserUtil::parseTag));
    }

}
