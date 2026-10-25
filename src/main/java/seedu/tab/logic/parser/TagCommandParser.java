package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_REMOVE_TAG;

import seedu.tab.logic.Messages;
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
        Flag actionFlag = arguments.getValue(FLAG_REMOVE_TAG).isPresent() ? FLAG_REMOVE_TAG : FLAG_ADD_TAG;
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
                : ParserUtil::parseTagToAdd;
        Tag tag = problems.collectIfPresent(arguments.getValue(actionFlag), readTag);
        problems.throwIfAny();
        return new TagCommand(target, tag, actionFlag.equals(FLAG_REMOVE_TAG) ? Action.REMOVE : Action.ADD);
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
        if (Character.isLetter(preamble.charAt(0))) {
            return ParserUtil.parseRequiredStudentTarget(preamble, TagCommand.FIELD_TARGET,
                    TagCommand.FIELD_INDEX, TagCommand.MESSAGE_USAGE);
        }
        return StudentTarget.byIndex(ParserUtil.parseRequiredIndex(preamble, TagCommand.FIELD_INDEX), preamble);
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
