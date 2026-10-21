package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.commands.StudentTarget;
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
     * field is examined before the command is refused, so that a user who mistyped the target
     * and a field is told about both at once. The shape is settled first, because an unclosed
     * quote, an unknown option, or a single-valued option given twice leaves it unclear which
     * field a value belongs to.
     *
     * <p>The target is read by {@link ParserUtil#parseRequiredStudentTarget}: a position in the
     * displayed list, or an NUS Student ID that opens with a letter. Unlike {@code delete}, an
     * ID opening with a digit cannot be a target, because {@code --id} names the field to set
     * rather than the student to edit.
     *
     * @throws ParseException if the user input does not conform to the expected format
     */
    public EditCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = FlagTokenizer.tokenizeWithIndex(
                args, FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_TAG,
                FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP);
        arguments.verifyNoDuplicateFlagsFor(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP);
        if (arguments.getValue(FLAG_FOLLOW_UP).isPresent() && arguments.getValue(FLAG_NO_FOLLOW_UP).isPresent()) {
            throw new ParseException(EditCommand.MESSAGE_CONFLICTING_FOLLOW_UP);
        }

        ParseProblems problems = new ParseProblems();

        StudentTarget target = problems.collect(() -> ParserUtil.parseRequiredStudentTarget(
                arguments.getPreamble(), EditCommand.FIELD_INDEX, EditCommand.MESSAGE_USAGE));
        if (!hasAnyField(arguments)) {
            problems.add(EditCommand.MESSAGE_NOT_EDITED);
        }
        EditStudentDescriptor descriptor = describeEdit(problems, arguments);

        problems.throwIfAny();

        return new EditCommand(target, descriptor);
    }

    /**
     * Returns true if the command named a field to change. A field whose value is rejected still
     * counts as named, so that a user who mistyped one is told what is wrong with it rather than
     * that they named no field.
     */
    private static boolean hasAnyField(FlagArgumentMap arguments) {
        return Stream.of(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_TAG,
                FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP)
                .anyMatch(flag -> !arguments.getAllValues(flag).isEmpty());
    }

    /**
     * Returns the fields the command asks to change, recording why for each value rejected.
     */
    private static EditStudentDescriptor describeEdit(ParseProblems problems, FlagArgumentMap arguments) {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();

        descriptor.setName(problems.collectIfPresent(arguments.getValue(FLAG_NAME), ParserUtil::parseName));
        descriptor.setPhone(problems.collectIfPresent(arguments.getValue(FLAG_PHONE), ParserUtil::parsePhone));
        arguments.getValue(FLAG_EMAIL).ifPresent(value -> parseEmailForEdit(problems, value, descriptor));
        arguments.getValue(FLAG_NUS_STUDENT_ID)
                .ifPresent(value -> parseNusStudentIdForEdit(problems, value, descriptor));
        parseTagsForEdit(problems, arguments.getAllValues(FLAG_TAG)).ifPresent(descriptor::setTags);
        if (arguments.getValue(FLAG_FOLLOW_UP).isPresent()) {
            descriptor.setFlagged(true);
        } else if (arguments.getValue(FLAG_NO_FOLLOW_UP).isPresent()) {
            descriptor.setFlagged(false);
        }

        return descriptor;
    }

    /**
     * Describes an explicit email clear or a validated replacement, recording why a value is rejected.
     */
    private static void parseEmailForEdit(ParseProblems problems, String value, EditStudentDescriptor descriptor) {
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            descriptor.clearEmail();
            return;
        }
        try {
            descriptor.setEmail(ParserUtil.parseEmail(trimmedValue));
        } catch (ParseException e) {
            problems.add(e.getMessage());
        }
    }

    /**
     * Describes an explicit NUS Student ID clear or a validated replacement, recording why a value is rejected.
     */
    private static void parseNusStudentIdForEdit(ParseProblems problems, String value,
            EditStudentDescriptor descriptor) {
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            descriptor.clearNusStudentId();
            return;
        }
        try {
            descriptor.setNusStudentId(ParserUtil.parseNusStudentId(trimmedValue));
        } catch (ParseException e) {
            problems.add(e.getMessage());
        }
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
