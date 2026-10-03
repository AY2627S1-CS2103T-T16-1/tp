package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
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
}
