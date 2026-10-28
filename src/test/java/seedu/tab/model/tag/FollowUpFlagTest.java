package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class FollowUpFlagTest {

    @Test
    public void constructor_inheritsTagBehavior() {
        Tag flag = new FollowUpFlag();
        assertEquals(FollowUpFlag.FOLLOW_UP_NAME, flag.getTagName());
        assertEquals("[Needs follow-up]", flag.toString());
        assertTrue(Tag.isValidTagName(flag.getTagName()));
    }

    @Test
    public void equals_sameStatusButDifferentTagType_remainsDistinct() {
        Tag flag = new FollowUpFlag();
        Tag tag = new Tag(FollowUpFlag.FOLLOW_UP_NAME);
        assertEquals(new FollowUpFlag(), flag);
        assertEquals(flag.hashCode(), new FollowUpFlag().hashCode());
        assertNotEquals(tag, flag);
        assertNotEquals(flag, tag);

        Set<Tag> tags = new HashSet<>();
        tags.add(tag);
        tags.add(flag);
        assertEquals(2, tags.size());
    }

    @Test
    public void isFollowUpName_exactMatch_returnsTrue() {
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up"));
        assertTrue(FollowUpFlag.isFollowUpName("needs follow-up"));
        assertTrue(FollowUpFlag.isFollowUpName("NEEDS FOLLOW-UP"));
    }

    @Test
    public void isFollowUpName_unicodeHyphens_returnsTrue() {
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow\u2010up")); // U+2010
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow\u2011up")); // U+2011
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow\u2013up")); // U+2013 en dash
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow\u2015up"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow\uff0dup"));
    }

    @Test
    public void isFollowUpName_spaceInsteadOfHyphen_returnsTrue() {
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow up"));
    }

    @Test
    public void isFollowUpName_trailingPunctuation_returnsTrue() {
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up."));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up!"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up?"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up,"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up;"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up:"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up\u2026"));
        assertTrue(FollowUpFlag.isFollowUpName("Needs follow-up\uff0c"));
    }

    @Test
    public void isFollowUpName_noSeparator_returnsTrue() {
        assertTrue(FollowUpFlag.isFollowUpName("Needs followup"));
    }

    @Test
    public void isFollowUpName_differentTags_returnsFalse() {
        assertFalse(FollowUpFlag.isFollowUpName("Needs follow-up soon"));
        assertFalse(FollowUpFlag.isFollowUpName("Follow-up"));
        assertFalse(FollowUpFlag.isFollowUpName("needs follow up now"));
        assertFalse(FollowUpFlag.isFollowUpName("Needs_follow_up"));
        assertFalse(FollowUpFlag.isFollowUpName("Needs follow-up soon."));
    }

}
