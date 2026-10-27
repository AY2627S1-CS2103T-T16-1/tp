package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NO_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_TARGET_ID;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.EditCommand.EditStudentDescriptor;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.tag.Tag;

/**
 * Parses input arguments and creates a new EditCommand object
 */
public class EditCommandParser implements Parser<EditCommand> {

    public static final String MESSAGE_BLANK_TAG_WITH_OTHER_TAGS =
            "A blank tag clears all tags only when it is given on its own.";

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
     * displayed list, or an NUS Student ID that opens with a letter. The separate
     * {@code --target-id} option accepts any valid ID while {@code --id} sets the ID field.
     *
     * @throws ParseException if the user input does not conform to the expected format.
     */
    public EditCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = FlagTokenizer.tokenizeWithIndex(
                args, FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID, FLAG_TAG,
                FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP, FLAG_TARGET_ID);
        arguments.verifyNoDuplicateFlagsFor(FLAG_NAME, FLAG_PHONE, FLAG_EMAIL, FLAG_NUS_STUDENT_ID,
                FLAG_FOLLOW_UP, FLAG_NO_FOLLOW_UP, FLAG_TARGET_ID);
        Optional<Boolean> followUp = ParserUtil.parseFollowUpSelector(arguments);

        ParseProblems problems = new ParseProblems();

        StudentTarget target = problems.collect(() -> parseTarget(arguments));
        if (!hasAnyField(arguments)) {
            problems.add(EditCommand.MESSAGE_NOT_EDITED);
        }
        EditStudentDescriptor descriptor = describeEdit(problems, arguments, followUp);

        problems.throwIfAny();

        return new EditCommand(target, descriptor);
    }

    /**
     * Reads exactly one target without confusing ID selection with an ID field edit.
     */
    private static StudentTarget parseTarget(FlagArgumentMap arguments) throws ParseException {
        if (arguments.getValue(FLAG_TARGET_ID).isPresent()) {
            if (!arguments.getPreamble().isBlank()) {
                throw new ParseException(EditCommand.MESSAGE_MULTIPLE_TARGETS);
            }
            return StudentTarget.byNusStudentId(ParserUtil.parseNusStudentId(
                    arguments.getValue(FLAG_TARGET_ID).orElseThrow()));
        }
        return ParserUtil.parseRequiredStudentTarget(arguments.getPreamble(),
                EditCommand.FIELD_TARGET, EditCommand.FIELD_INDEX);
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
    private static EditStudentDescriptor describeEdit(ParseProblems problems, FlagArgumentMap arguments,
            Optional<Boolean> followUp) {
        EditStudentDescriptor descriptor = new EditStudentDescriptor();

        descriptor.setName(problems.collectIfPresent(arguments.getValue(FLAG_NAME), ParserUtil::parseName));
        arguments.getValue(FLAG_PHONE).ifPresent(value -> parseClearableField(problems, value,
                ParserUtil::parsePhone, descriptor::clearPhone, descriptor::setPhone));
        arguments.getValue(FLAG_EMAIL).ifPresent(value -> parseClearableField(problems, value,
                ParserUtil::parseEmail, descriptor::clearEmail, descriptor::setEmail));
        arguments.getValue(FLAG_NUS_STUDENT_ID)
                .ifPresent(value -> parseClearableField(problems, value, ParserUtil::parseNusStudentId,
                        descriptor::clearNusStudentId, descriptor::setNusStudentId));
        parseTagsForEdit(problems, arguments.getAllValues(FLAG_TAG)).ifPresent(descriptor::setTags);
        followUp.ifPresent(descriptor::setFlagged);

        return descriptor;
    }

    /**
     * Describes an explicit clear or a validated replacement, collecting any rejected value.
     */
    private static <T> void parseClearableField(ParseProblems problems, String value,
            ParseProblems.ValueParser<T> parser, Runnable clear, Consumer<T> set) {
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            clear.run();
            return;
        }
        T parsedValue = problems.collect(() -> parser.parse(trimmedValue));
        if (parsedValue != null) {
            set.accept(parsedValue);
        }
    }

    /**
     * Parses the tags the command supplied into the set that replaces the student's own, or
     * returns nothing when the command leaves the tags alone. A single value that is empty
     * after field normalization, such as {@code -t " "}, asks for every tag to be removed.
     */
    private static Optional<Set<Tag>> parseTagsForEdit(ParseProblems problems, List<String> tags) {
        if (tags.isEmpty()) {
            return Optional.empty();
        }
        List<String> normalizedTags = tags.stream().map(StringUtil::normalizeFieldValue).toList();
        if (normalizedTags.size() == 1 && normalizedTags.getFirst().isEmpty()) {
            return Optional.of(Set.of());
        }
        List<String> nonBlankTags = normalizedTags.stream().filter(tag -> !tag.isEmpty()).toList();
        if (nonBlankTags.size() != normalizedTags.size()) {
            problems.add(MESSAGE_BLANK_TAG_WITH_OTHER_TAGS);
        }
        return Optional.of(ParserUtil.parseTags(nonBlankTags, problems));
    }

}
