package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_REMOVE_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_TARGET_ID;

import java.util.Optional;

import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.EditCommand;
import seedu.tab.logic.commands.StudentTarget;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.commands.TagCommand.Action;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.tag.Tag;

/**
 * Parses a student target and exactly one tag for a TagCommand.
 */
public class TagCommandParser implements Parser<TagCommand> {

    @Override
    public TagCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = tokenize(args);
        arguments.verifyNoDuplicateFlagsFor(FLAG_ADD_TAG, FLAG_REMOVE_TAG, FLAG_NUS_STUDENT_ID);
        if (arguments.getValue(FLAG_ADD_TAG).isPresent() && arguments.getValue(FLAG_REMOVE_TAG).isPresent()) {
            throw new ParseException(TagCommand.MESSAGE_CONFLICTING_ACTIONS);
        }
        CliFlag actionFlag = arguments.getValue(FLAG_REMOVE_TAG).isPresent() ? FLAG_REMOVE_TAG : FLAG_ADD_TAG;
        ParseProblems problems = new ParseProblems();
        StudentTarget target = problems.collect(() -> parseTarget(arguments));
        if (arguments.getValue(actionFlag).isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(
                    FLAG_ADD_TAG.getLabel() + " or " + FLAG_REMOVE_TAG.getLabel())
                    + "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE);
        }
        // removing it stays a tag, so that TagCommand can explain the marker to the user
        ParseProblems.ValueParser<Tag> readTag = actionFlag.equals(FLAG_REMOVE_TAG)
                ? ParserUtil::parseTag
                : value -> ParserUtil.parseTagToAdd(value, followUpCommandFor(arguments, target));
        Tag tag = problems.collectIfPresent(arguments.getValue(actionFlag), readTag).orElse(null);
        problems.throwIfAny();
        return new TagCommand(target, tag, actionFlag.equals(FLAG_REMOVE_TAG) ? Action.REMOVE : Action.ADD);
    }

    /**
     * Returns what the user types to flag the student this command names. The command is an
     * {@code edit} one, because {@code tag} takes no follow-up option and advice naming one
     * would fail as soon as it was followed.
     *
     * <p>The target is spelled as the user spelled it, so the remedy reaches the student they
     * meant. An ID given to the ID option moves to the option {@code edit} selects a target
     * with, since the plain option sets the ID field there. A target the command could not read
     * names no student, so the remedy falls back to naming the index field.
     *
     * @param arguments The options and preamble the user supplied.
     * @param target The student this command names, or null if the target was refused.
     */
    private static String followUpCommandFor(FlagArgumentMap arguments, StudentTarget target) {
        Optional<String> idTarget = arguments.getValue(FLAG_NUS_STUDENT_ID);
        String editTarget;
        if (target == null) {
            editTarget = TagCommand.FIELD_INDEX;
        } else if (idTarget.isPresent()) {
            editTarget = FLAG_TARGET_ID + " " + idTarget.orElseThrow().trim();
        } else {
            editTarget = arguments.getPreamble().trim();
        }
        return EditCommand.COMMAND_WORD + " " + editTarget + " " + FLAG_FOLLOW_UP;
    }

    /** Reads one target, retaining index diagnostics and using the shared parser for IDs. */
    private static StudentTarget parseTarget(FlagArgumentMap arguments) throws ParseException {
        String preamble = arguments.getPreamble().trim();
        if (arguments.getValue(FLAG_NUS_STUDENT_ID).isPresent()) {
            if (!preamble.isBlank()) {
                throw new ParseException(TagCommand.MESSAGE_MULTIPLE_TARGETS);
            }
            return StudentTarget.byNusStudentId(ParserUtil.parseNusStudentId(
                    arguments.getValue(FLAG_NUS_STUDENT_ID).orElseThrow()));
        }
        if (preamble.isEmpty()) {
            throw new ParseException(Messages.getErrorMessageForMissingFields(TagCommand.FIELD_TARGET));
        }
        if (ParserUtil.isIdTarget(preamble)) {
            return ParserUtil.parseRequiredStudentTarget(preamble, TagCommand.FIELD_TARGET,
                    TagCommand.FIELD_INDEX);
        }
        return ParserUtil.parsePreambleTarget(preamble, TagCommand.ID_TARGET_COMMAND,
                value -> StudentTarget.byIndex(
                        ParserUtil.parseRequiredIndex(value, TagCommand.FIELD_INDEX), value));
    }

    /** Adds command-specific guidance when the arguments cannot be read as options. */
    private static FlagArgumentMap tokenize(String args) throws ParseException {
        try {
            return FlagTokenizer.tokenizeWithIndex(args, FLAG_ADD_TAG, FLAG_REMOVE_TAG, FLAG_NUS_STUDENT_ID);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage() + "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE, e);
        }
    }
}
