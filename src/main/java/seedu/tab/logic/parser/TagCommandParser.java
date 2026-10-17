package seedu.tab.logic.parser;

import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import seedu.tab.commons.core.index.Index;
import seedu.tab.logic.Messages;
import seedu.tab.logic.commands.TagCommand;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.tag.Tag;

/**
 * Parses a displayed student index and exactly one tag for a TagCommand.
 */
public class TagCommandParser implements Parser<TagCommand> {

    @Override
    public TagCommand parse(String args) throws ParseException {
        FlagArgumentMap arguments = FlagTokenizer.tokenize(args, FLAG_TAG);
        arguments.verifyNoDuplicateFlagsFor(FLAG_TAG);
        ParseProblems problems = new ParseProblems();
        if (arguments.getPreamble().isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(TagCommand.FIELD_INDEX));
        }
        if (arguments.getValue(FLAG_TAG).isEmpty()) {
            problems.add(Messages.getErrorMessageForMissingFields(FLAG_TAG.getLabel()));
        }
        Index index = arguments.getPreamble().isEmpty() ? null
                : problems.collect(() -> ParserUtil.parseIndex(arguments.getPreamble()));
        Tag tag = problems.collectIfPresent(arguments.getValue(FLAG_TAG), ParserUtil::parseTag);
        problems.throwIfAny();
        return new TagCommand(index, tag);
    }
}
