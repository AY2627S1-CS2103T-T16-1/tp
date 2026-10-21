package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class FlagTest {

    @Test
    public void constructor_inheritsTagBehavior() {
        Tag flag = new Flag();
        assertEquals(Flag.FOLLOW_UP_NAME, flag.tagName);
        assertEquals("[Needs follow-up]", flag.toString());
        assertTrue(Tag.isValidTagName(flag.tagName));
    }

    @Test
    public void equals_sameStatusButDifferentTagType_remainsDistinct() {
        Tag flag = new Flag();
        Tag tag = new Tag(Flag.FOLLOW_UP_NAME);
        assertEquals(new Flag(), flag);
        assertEquals(flag.hashCode(), new Flag().hashCode());
        assertNotEquals(tag, flag);
        assertNotEquals(flag, tag);

        Set<Tag> tags = new HashSet<>();
        tags.add(tag);
        tags.add(flag);
        assertEquals(2, tags.size());
    }

    @Test
    public void matchesKeyword_wholeLabelOrWord_ignoringCase() {
        Flag flag = new Flag();
        assertTrue(flag.matchesKeyword("Needs follow-up"));
        assertTrue(flag.matchesKeyword("needs FOLLOW-UP"));
        assertTrue(flag.matchesKeyword("needs"));
        assertTrue(flag.matchesKeyword("FOLLOW-UP"));
    }

    @Test
    public void matchesKeyword_openingOfWord_ignoringCase() {
        Flag flag = new Flag();
        for (String opening : List.of("need", "fol", "follow", "follow-", "follo", "FOLL", "n")) {
            assertTrue(flag.matchesKeyword(opening), opening + " opens a word of the label");
        }
    }

    @Test
    public void matchesKeyword_phraseOpeningTheWholeLabel_ignoringCase() {
        // a phrase quoted as one search term reaches the flag whole rather than word by word,
        // and it should open the label the way each of its unquoted words does
        Flag flag = new Flag();
        for (String opening : List.of("needs follow", "Needs Fo", "NEEDS FOLLOW")) {
            assertTrue(flag.matchesKeyword(opening), opening + " opens the whole label");
        }
        assertFalse(flag.matchesKeyword("follow needs"), "the words in reverse open nothing");
    }

    @Test
    public void matchesKeyword_fragmentFromMiddleOfWord_returnsFalse() {
        Flag flag = new Flag();
        for (String fragment : List.of("low", "ee", "ed", "lo", "ow", "up", "ollow-up")) {
            assertFalse(flag.matchesKeyword(fragment), fragment + " begins mid-word in the label");
        }
    }

    @Test
    public void matchesKeyword_empty_returnsFalse() {
        assertFalse(new Flag().matchesKeyword(""));
    }
}
