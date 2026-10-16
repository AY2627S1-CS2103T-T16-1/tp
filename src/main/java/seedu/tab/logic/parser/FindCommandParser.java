package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.ArrayList;
import java.util.List;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class FindCommandParser implements Parser<FindCommand> {

    public static final String MESSAGE_EMPTY_KEYWORD =
            "A search term is empty. Remove the empty quotes or put a value between them.";

    /**
     * Parses the given {@code String} of arguments in the context of the FindCommand
     * and returns a FindCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FindCommand parse(String args) throws ParseException {
        List<Token> tokens = CommandTokenizer.tokenize(args);
        if (tokens.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        List<String> keywords = new ArrayList<>();
        for (Token token : tokens) {
            String keyword = StringUtil.normalizeFieldValue(token.value());
            if (keyword.isEmpty()) {
                throw new ParseException(MESSAGE_EMPTY_KEYWORD);
            }
            keywords.add(keyword);
        }

        return new FindCommand(new DetailsContainsKeywordsPredicate(List.copyOf(keywords)));
    }

}
