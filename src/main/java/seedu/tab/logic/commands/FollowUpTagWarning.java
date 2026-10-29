package seedu.tab.logic.commands;

import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import seedu.tab.logic.Messages;
import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

/** Formats advisory notes for tags that could be mistaken for the follow-up marker. */
final class FollowUpTagWarning {
    static final String MESSAGE_NOTE = "NOTE: \"%1$s\" looks like the follow-up marker. "
            + "To flag the student, use " + FLAG_FOLLOW_UP.getLabel() + ".";

    private static final Pattern PUNCTUATION_AND_WHITESPACE = Pattern.compile("[\\p{P}\\p{Z}\\s]");
    private static final String COLLAPSED_MARKER = collapse(FollowUpFlag.FOLLOW_UP_NAME);

    private FollowUpTagWarning() {
    }

    static String appendTo(String successMessage, Collection<Tag> tags) {
        List<String> warnings = tags.stream()
                .map(Tag::getTagName)
                .filter(name -> collapse(name).equalsIgnoreCase(COLLAPSED_MARKER))
                .sorted()
                .map(name -> String.format(MESSAGE_NOTE, Messages.shorten(name)))
                .toList();
        return warnings.isEmpty() ? successMessage : successMessage + "\n\n" + String.join("\n", warnings);
    }

    /**
     * Returns {@code name} without the punctuation and spacing that a near spelling of the
     * marker varies, so that the label a reader sees decides whether two labels are alike.
     */
    private static String collapse(String name) {
        return PUNCTUATION_AND_WHITESPACE.matcher(name).replaceAll("");
    }
}
