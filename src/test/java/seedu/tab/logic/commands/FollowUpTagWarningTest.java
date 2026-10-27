package seedu.tab.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

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
            assertEquals(new Tag(name), tag);
        }
    }

    @Test
    public void appendTo_emptyOrUnrelatedTags_returnsSuccessMessage() {
        assertEquals("Saved.", FollowUpTagWarning.appendTo("Saved.", List.of()));
        assertEquals("Saved.", FollowUpTagWarning.appendTo("Saved.", List.of(
                new Tag("Needs follow-up soon"), new Tag("follow up"), new Tag("Lab 3"))));
    }

    @Test
    public void appendTo_multipleSimilarTags_listsEachInAlphabeticalOrder() {
        String advice = "\" looks like the follow-up marker. To flag the student, use --follow-up.";
        assertEquals("Saved.\n\nNOTE: \"Needs follow up" + advice + "\nNOTE: \"Needs followup" + advice,
                FollowUpTagWarning.appendTo("Saved.", List.of(new Tag("Needs followup"), new Tag("Needs follow up"))));
    }
}
