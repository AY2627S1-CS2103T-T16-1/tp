package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.tab.model.tag.FollowUpFlag;
import seedu.tab.model.tag.Tag;

public class FollowUpTagWarningTest {
    @Test
    public void appendTo_similarMarkerSpellings_appendsAdviceAndKeepsTagSpelling() {
        for (String name : List.of("Needs follow up", "Needs followup", "Needs follow-up.",
                "NEEDS FOLLOW UP", "needs-followup", "Needs\u00a0follow\u00a0up")) {
            Tag tag = new Tag(name);
            assertEquals("Saved.\n\nNOTE: \"" + tag.getTagName()
                    + "\" looks like the follow-up marker. To flag the student, use --follow-up.",
                    FollowUpTagWarning.appendTo("Saved.", List.of(tag)));
        }
    }

    @Test
    public void appendTo_markerItself_namesTheLabelAndTheFlagThatSetsIt() {
        assertEquals("Saved.\n\nNOTE: \"Needs follow-up\" looks like the follow-up marker. "
                + "To flag the student, use --follow-up.",
                FollowUpTagWarning.appendTo("Saved.", List.of(new Tag(FollowUpFlag.FOLLOW_UP_NAME))));
    }

    @Test
    public void appendTo_emptyOrUnrelatedTags_returnsSuccessMessage() {
        // Empty tag collection: zero candidates, so no advice.
        assertEquals("Saved.", FollowUpTagWarning.appendTo("Saved.", List.of()));
        // Non-empty collection of unrelated labels: still no advice.
        assertEquals("Saved.", FollowUpTagWarning.appendTo("Saved.", List.of(
                new Tag("Needs follow-up soon"), new Tag("follow up"), new Tag("Lab 3"))));
    }

    @Test
    public void appendTo_multipleSimilarTags_listsEachInAlphabeticalOrder() {
        String advice = "\" looks like the follow-up marker. To flag the student, use --follow-up.";
        assertEquals("Saved.\n\nNOTE: \"Needs follow up" + advice + "\nNOTE: \"Needs followup" + advice,
                FollowUpTagWarning.appendTo("Saved.", List.of(new Tag("Needs followup"), new Tag("Needs follow up"))));
    }

    @Test
    public void appendTo_givenFlagCommand_namesThatCommandRatherThanTheOption() {
        assertEquals("Saved.\n\nNOTE: \"Needs_follow_up\" looks like the follow-up marker. "
                + "To flag the student, use edit 1 --follow-up.",
                FollowUpTagWarning.appendTo("Saved.", List.of(new Tag("Needs_follow_up")),
                        "edit 1 --follow-up"));
    }
}
