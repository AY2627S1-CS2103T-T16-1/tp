package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_ADD_TAG;
import static seedu.tab.logic.parser.CliFlags.FLAG_REMOVE_TAG;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.commands.TagCommand.Action;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.tag.Tag;

/**
 * Parses a displayed student index and exactly one tag for a TagCommand.
 */
public class TagCommandParser implements Parser<TagCommand> {

    @Override
    public TagCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = tokenize(args);
        arguments.verifyNoDuplicateFlagsFor(FLAG_ADD_TAG, FLAG_REMOVE_TAG);
        if (arguments.getValue(FLAG_ADD_TAG).isPresent() && arguments.getValue(FLAG_REMOVE_TAG).isPresent()) {
            throw new ParseException(TagCommand.MESSAGE_CONFLICTING_ACTIONS);
        }
        Flag actionFlag = arguments.getValue(FLAG_REMOVE_TAG).isPresent() ? FLAG_REMOVE_TAG : FLAG_ADD_TAG;
        ParseProblems problems = new ParseProblems();
        Index index = problems.collect(() -> ParserUtil.parseRequiredIndex(
                arguments.getPreamble(), TagCommand.FIELD_INDEX));
        if (arguments.getValue(actionFlag).isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(
                    FLAG_ADD_TAG.getLabel() + " or " + FLAG_REMOVE_TAG.getLabel())
                    + "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE);
        }
        Tag tag = problems.collectIfPresent(arguments.getValue(actionFlag), ParserUtil::parseTag);
        problems.throwIfAny();
        return new TagCommand(index, tag, actionFlag.equals(FLAG_REMOVE_TAG) ? Action.REMOVE : Action.ADD);
    }

    /** Adds command-specific guidance when the arguments cannot be read as options. */
    private static FlagArgumentMap tokenize(String args) throws ParseException {
        try {
            return FlagTokenizer.tokenizeWithIndex(args, FLAG_ADD_TAG, FLAG_REMOVE_TAG);
        } catch (ParseException e) {
            throw new ParseException(e.getMessage() + "\n" + TagCommand.MESSAGE_ACTION_GUIDANCE, e);
        }
    }
}
