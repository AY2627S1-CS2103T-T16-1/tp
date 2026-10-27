package seedu.tab.logic.commands;

import java.util.Collection;
import java.util.List;

import seedu.tab.logic.Messages;
import seedu.tab.model.tag.Tag;

/** Formats advisory notes for tags that could be mistaken for the follow-up marker. */
final class FollowUpTagWarning {
    private FollowUpTagWarning() {
    }

    static String appendTo(String successMessage, Collection<Tag> tags) {
        List<String> warnings = tags.stream()
                .map(Tag::getTagName)
                .filter(name -> name.replaceAll("[\\p{P}\\p{Z}\\s]", "").equalsIgnoreCase("needsfollowup"))
                .sorted()
                .map(name -> "NOTE: \"" + Messages.shorten(name)
                        + "\" looks like the follow-up marker. To flag the student, use --follow-up.")
                .toList();
        return warnings.isEmpty() ? successMessage : successMessage + "\n\n" + String.join("\n", warnings);
    }
}
