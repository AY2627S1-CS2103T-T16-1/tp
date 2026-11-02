package seedu.tab.logic.commands;

import static seedu.tab.logic.parser.CliFlags.FLAG_FOLLOW_UP;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import seedu.tab.logic.Messages;
import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

/** Formats advisory notes for tags that could be mistaken for the follow-up marker. */
final class FollowUpTagWarning {
    static final String MESSAGE_NOTE = "NOTE: \"%1$s\" looks like the follow-up marker. "
            + "To flag the student, use %2$s.";
    static final String MESSAGE_FIND_NOTE = "NOTE: The \"" + FollowUpFlag.FOLLOW_UP_NAME
            + "\" label marks the follow-up flag, which is not a tag. To find flagged students, use "
            + FindCommand.COMMAND_WORD + " " + FLAG_FOLLOW_UP.getLabel() + ".";

    private static final Pattern PUNCTUATION_AND_WHITESPACE = Pattern.compile("[\\p{P}\\p{Z}\\s]");
    private static final String COLLAPSED_MARKER = collapse(FollowUpFlag.FOLLOW_UP_NAME);
    private static final String COLLAPSED_MARKER_LOWER_CASE = COLLAPSED_MARKER.toLowerCase(Locale.ROOT);
    private static final String MARKER_KEYWORD = "follow";

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
     * Returns {@code foundMessage} with a note pointing to the follow-up option when a search that
     * found nobody has a tag term reading like part of the marker and naming its word "follow", so
     * that a search for the label is not mistaken for proof that nobody is flagged. A search that
     * found someone matched a real tag with every term, so it gets no note. A term such as "up" is
     * left alone, since it is far more likely to name a tag than the marker.
     */
    static String appendFindNoteTo(String foundMessage, boolean hasFoundAny, List<String> tagTerms) {
        boolean seeksMarker = tagTerms.stream().anyMatch(FollowUpTagWarning::isPartOfMarker);
        return hasFoundAny || !seeksMarker ? foundMessage : foundMessage + "\n\n" + MESSAGE_FIND_NOTE;
    }

    private static boolean isPartOfMarker(String term) {
        String collapsedTerm = collapse(term).toLowerCase(Locale.ROOT);
        return collapsedTerm.contains(MARKER_KEYWORD)
                && COLLAPSED_MARKER_LOWER_CASE.contains(collapsedTerm);
    }

    /**
     * Returns {@code name} without the punctuation and spacing that a near spelling of the
     * marker varies, so that the label a reader sees decides whether two labels are alike.
     */
    private static String collapse(String name) {
        return PUNCTUATION_AND_WHITESPACE.matcher(name).replaceAll("");
    }
}
