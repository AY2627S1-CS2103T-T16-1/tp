package seedu.tab.model.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

}
