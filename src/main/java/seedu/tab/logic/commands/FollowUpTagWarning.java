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
            + "To flag the student, use %2$s.";

    private static final Pattern PUNCTUATION_AND_WHITESPACE = Pattern.compile("[\\p{P}\\p{Z}\\s]");
    private static final String COLLAPSED_MARKER = collapse(FollowUpFlag.FOLLOW_UP_NAME);

    private FollowUpTagWarning() {
    }

    /**
     * Returns {@code successMessage} with a note for every tag that reads like the marker,
     * naming the follow-up option as the way to flag the student. This suits a command that
     * takes the option itself, so that the user has only to add it to what they typed.
     */
    static String appendTo(String successMessage, Collection<Tag> tags) {
        return appendTo(successMessage, tags, FLAG_FOLLOW_UP.getLabel());
    }

    /**
     * Returns {@code successMessage} with a note for every tag that reads like the marker,
     * naming {@code flagCommand} as the way to flag the student. A command that takes no
     * follow-up option of its own names the whole command that does, so that the remedy
     * offered can be typed as it stands.
     */
    static String appendTo(String successMessage, Collection<Tag> tags, String flagCommand) {
        List<String> warnings = tags.stream()
                .map(Tag::getTagName)
                .filter(name -> collapse(name).equalsIgnoreCase(COLLAPSED_MARKER))
                .sorted()
                .map(name -> String.format(MESSAGE_NOTE, Messages.shorten(name), flagCommand))
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
