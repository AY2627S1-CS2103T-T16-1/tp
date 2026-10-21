package seedu.tab.logic.parser;

import static seedu.tab.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.tab.logic.parser.CliFlags.FLAG_EMAIL;
import static seedu.tab.logic.parser.CliFlags.FLAG_NAME;
import static seedu.tab.logic.parser.CliFlags.FLAG_NUS_STUDENT_ID;
import static seedu.tab.logic.parser.CliFlags.FLAG_PHONE;
import static seedu.tab.logic.parser.CliFlags.FLAG_TAG;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import seedu.tab.commons.util.StringUtil;
import seedu.tab.logic.commands.FindCommand;
import seedu.tab.logic.parser.CommandTokenizer.Token;
import seedu.tab.logic.parser.exceptions.ParseException;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate;
import seedu.tab.model.student.DetailsContainsKeywordsPredicate.Field;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class FindCommandParser implements Parser<FindCommand> {

    public static final String MESSAGE_EMPTY_KEYWORD =
            "A search term is empty. Remove the empty quotes or put a value between them.";

    public static final String MESSAGE_UNQUALIFIED_TERM =
            "Every search term needs a field option. For example: find -n Jason -t Lab -n Tan. "
            + "Put a multi-word term in double quotes, for example: find -n \"Jason Tan\".";

    private static final Map<Flag, Field> SEARCH_FIELDS = Map.of(
            FLAG_NAME, Field.NAME, FLAG_NUS_STUDENT_ID, Field.NUS_STUDENT_ID,
            FLAG_PHONE, Field.PHONE, FLAG_EMAIL, Field.EMAIL, FLAG_TAG, Field.TAG);

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

        FlagArgumentMap arguments = FlagTokenizer.tokenize(args, SEARCH_FIELDS.keySet().toArray(Flag[]::new));
        if (!arguments.getPreamble().isEmpty() || tokens.getFirst().beginsQuoted()) {
            throw new ParseException(MESSAGE_UNQUALIFIED_TERM);
        }
        Map<Field, List<String>> keywordsByField = new EnumMap<>(Field.class);
        for (Map.Entry<Flag, Field> entry : SEARCH_FIELDS.entrySet()) {
            List<String> values = arguments.getAllValues(entry.getKey());
            if (!values.isEmpty()) {
                keywordsByField.put(entry.getValue(), normalizeKeywords(values));
            }
        }
        return new FindCommand(new DetailsContainsKeywordsPredicate(keywordsByField));
    }

    /** Normalizes search values like stored details, rejecting empty terms. */
    private static List<String> normalizeKeywords(List<String> values) throws ParseException {
        List<String> keywords = new ArrayList<>();
        for (String value : values) {
            String keyword = StringUtil.normalizeFieldValue(value);
            if (keyword.isEmpty()) {
                throw new ParseException(MESSAGE_EMPTY_KEYWORD);
            }
            keywords.add(keyword);
        }
        return keywords;
    }

}
